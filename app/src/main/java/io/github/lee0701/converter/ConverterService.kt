package io.github.lee0701.converter

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.preference.PreferenceManager
import androidx.room.Room
import io.github.lee0701.converter.CharacterSet.isHangul
import io.github.lee0701.converter.candidates.Candidate
import io.github.lee0701.converter.candidates.view.CandidatesWindow
import io.github.lee0701.converter.candidates.view.CandidatesWindowHider
import io.github.lee0701.converter.candidates.view.HorizontalCandidatesWindow
import io.github.lee0701.converter.candidates.view.VerticalCandidatesWindow
import io.github.lee0701.converter.dictionary.UserDictionaryDictionary
import io.github.lee0701.converter.engine.*
import io.github.lee0701.converter.history.HistoryDatabase
import io.github.lee0701.converter.settings.SettingsActivity
import io.github.lee0701.converter.userdictionary.UserDictionaryDatabase
import kotlinx.coroutines.*
import kotlin.math.max
import kotlin.math.min

class ConverterService: AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null

    private lateinit var converter: HanjaConverter
    private var predictor: Predictor? = null
    private lateinit var candidatesWindow: CandidatesWindow

    private var composingText = ComposingText("", 0)

    private var outputFormat: OutputFormat? = null
    private val rect = Rect()
    private var ignoreText: CharSequence? = null

    private var enableAutoHiding = false

    override fun onCreate() {
        super.onCreate()
        if(!getSharedPreferences(PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES, Context.MODE_PRIVATE)
                .getBoolean(PreferenceManager.KEY_HAS_SET_DEFAULT_VALUES, false)) {
            SettingsActivity.PREFERENCE_LIST.forEach {
                PreferenceManager.setDefaultValues(this, it, true)
            }
        }
        restartService()
        INSTANCE = this
    }

    override fun onDestroy() {
        super.onDestroy()
        INSTANCE = null
    }

    override fun onInterrupt() {
    }

    fun restartService() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this@ConverterService)

        outputFormat = preferences.getString("output_format", "hanja_only")?.let { OutputFormat.of(it) }
        enableAutoHiding = preferences.getBoolean("enable_auto_hiding", false)

        outputFormat =
            preferences.getString("output_format", "hanja_only")?.let { OutputFormat.of(it) }
        val sortByContext = preferences.getBoolean("sort_by_context", false)
        val usePrediction = preferences.getBoolean("use_prediction", false)
        val autoComplete = preferences.getBoolean("use_autocomplete", false)

        val tfLitePredictor = if(BuildConfig.IS_DONATION && (usePrediction || sortByContext)) {
                TFLitePredictor(
                    assets.open("ml/wordlist.txt"),
                    assets.openFd("ml/model.tflite")
                )
        } else null

        val converters = mutableListOf<HanjaConverter>()

        val userDictionaryDatabase = Room.databaseBuilder(applicationContext, UserDictionaryDatabase::class.java, DB_USER_DICTIONARY).build()
        val userDictionaryHanjaConverter = DictionaryHanjaConverter(UserDictionaryDictionary(userDictionaryDatabase))
        converters += userDictionaryHanjaConverter

        if(BuildConfig.IS_DONATION && preferences.getBoolean("use_learned_word", false)) {
            val historyDatabase = Room.databaseBuilder(applicationContext, HistoryDatabase::class.java, DB_HISTORY).build()
            val historyHanjaConverter = HistoryHanjaConverter(historyDatabase, preferences.getBoolean("freeze_learning", false))
            CoroutineScope(Dispatchers.IO).launch { historyHanjaConverter.deleteOldWords() }
            converters += historyHanjaConverter
        }

        val additional = preferences.getStringSet("additional_dictionaries", setOf())?.toList() ?: listOf()
        val dictionaries = DictionaryManager.loadCompoundDictionary(assets, listOf("base") + additional)
        val dictionaryHanjaConverter: HanjaConverter = DictionaryHanjaConverter(dictionaries)

        if(tfLitePredictor != null && sortByContext) {
            converters += ContextSortingHanjaConverter(dictionaryHanjaConverter, tfLitePredictor)
        } else {
            converters += dictionaryHanjaConverter
        }

        val hanjaConverter: HanjaConverter = CompoundHanjaConverter(converters.toList())

        converter = hanjaConverter
        if(tfLitePredictor != null) {
            predictor = ResortingPredictor(
                DictionaryPredictor(dictionaries),
                tfLitePredictor
            )
        } else {
            predictor = null
        }

        candidatesWindow = when(preferences.getString("window_type", "horizontal")) {
            "horizontal" -> HorizontalCandidatesWindow(this@ConverterService)
            else -> VerticalCandidatesWindow(this@ConverterService)
        }

    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if(event == null) return
        when(event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val isHideEvent = CandidatesWindowHider.of(event.packageName?.toString() ?: "")?.isHideEvent(event)
                if(enableAutoHiding && isHideEvent == true) {
                    candidatesWindow.destroy()
                }
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val source = event.source ?: return
                source.getBoundsInScreen(rect)

                val ignoreText = this.ignoreText
                this.ignoreText = null
                val text = event.text.firstOrNull() ?: ""
                if(text == ignoreText) return

                val beforeText = event.beforeText ?: ""
                val fromIndex = event.fromIndex.let { if(it == -1) firstDifference(beforeText, text) else it }
                val addedCount = max(event.addedCount, 0)
                val removedCount = max(event.removedCount, 0)

                val newComposingText = composingText.textChanged(text, fromIndex, addedCount, removedCount)
                if(newComposingText.from != composingText.from) learnConverted()
                composingText = newComposingText

                convert(source)
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                val source = event.source ?: return
                val start = source.textSelectionStart
                val end = source.textSelectionEnd
                val newComposingText = composingText.textSelectionChanged(start, end)
                if(composingText != newComposingText) {
                    this.composingText = newComposingText
                    convert(source)
                }
            }
            else -> {}
        }
    }

    private fun convert(source: AccessibilityNodeInfo) {
        job?.cancel()
        job = scope.launch {
            val predictor = predictor
            val converted = converter.convertPrefix(composingText).flatten()
            val predicted = predictor?.predict(composingText)?.top(10) ?: listOf()
            val candidates = getExtraCandidates(composingText.composing) +
                    (if(converted.isNotEmpty()) predicted.take(1) else predicted) +
                    converted
            if(candidates.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    candidatesWindow.show(candidates, rect) { candidate ->
                        val hanja = candidate.hanja
                        val hangul = candidate.hangul.ifEmpty { composingText.composing.take(hanja.length).toString() }
                        val replaced = composingText.replaced(hangul, hanja, candidate.input.length, outputFormat)
                        ignoreText = replaced.text
                        pasteFullText(source, replaced.text)
                        setSelection(source, replaced.to)
                        composingText = replaced
                        convert(source)
                        if(!predicted.contains(candidate) && hanja.all { CharacterSet.isHanja(it) }) {
                            learn(hangul, hanja)
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    candidatesWindow.destroy()
                }
            }
        }
    }

    private fun pasteFullText(source: AccessibilityNodeInfo, fullText: CharSequence) {
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, fullText)
        source.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    private fun setSelection(source: AccessibilityNodeInfo, start: Int, end: Int = start) {
        val arguments = Bundle()
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, start)
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, end)
        source.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, arguments)
    }

    private fun learnConverted() {
        if(composingText.converted.isNotEmpty() && !composingText.converted.all { isHangul(it) }) {
            learn(composingText.unconverted, composingText.converted)
        }
    }

    private fun learn(input: String, result: String) {
        val converter = converter
        if(converter !is LearningHanjaConverter) return
        CoroutineScope(Dispatchers.IO).launch { converter.learn(input, result) }
    }

    private fun getExtraCandidates(hangul: CharSequence): List<Candidate> {
        if(hangul.isEmpty()) return emptyList()
        val list = mutableListOf<CharSequence>()
        val nonHangulIndex = hangul.indexOfFirst { c -> !CharacterSet.isHangul(c) }
        list += if(nonHangulIndex > 0) hangul.slice(0 until nonHangulIndex) else hangul
        if(CharacterSet.isHangul(hangul[0])) list.add(0, hangul[0].toString())
        return list.map { Candidate(it.toString(), it.toString(), "") }
    }

    private fun firstDifference(a: CharSequence, b: CharSequence): Int {
        val len = min(a.length, b.length)
        for(i in 0 until len) {
            if(a[i] != b[i]) return i
        }
        return len
    }

    companion object {
        var INSTANCE: ConverterService? = null

        const val DB_HISTORY = "history"
        const val DB_USER_DICTIONARY = "user_dictionary"
    }

}