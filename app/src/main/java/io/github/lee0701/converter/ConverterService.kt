package io.github.lee0701.converter

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.preference.PreferenceManager
import androidx.room.Room
import io.github.lee0701.converter.CharacterSet.isHangul
import io.github.lee0701.converter.candidates.view.CandidatesWindow
import io.github.lee0701.converter.candidates.view.CandidatesWindowHider
import io.github.lee0701.converter.candidates.view.HorizontalCandidatesWindow
import io.github.lee0701.converter.candidates.view.VerticalCandidatesWindow
import io.github.lee0701.converter.engine.*
import io.github.lee0701.converter.history.HistoryDatabase
import io.github.lee0701.converter.settings.SettingsActivity
import kotlinx.coroutines.*
import kotlin.math.min

class ConverterService: AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var job: Job? = null

    private lateinit var converter: Converter
    private var predictor: Predictor? = null
    private lateinit var candidatesWindow: CandidatesWindow

    private var composingText = ComposingText("", 0)

    private var outputFormat: OutputFormat? = null
    private val rect = Rect()
    private var ignoreText: CharSequence? = null

    private var enableAutoHiding = false

    override fun onCreate() {
        super.onCreate()
        SettingsActivity.PREFERENCE_LIST.forEach {
            PreferenceManager.setDefaultValues(this, it, false)
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
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        outputFormat =
            preferences.getString("output_format", "hanja_only")?.let { OutputFormat.of(it) }
        val sortByContext = preferences.getBoolean("sort_by_context", false)
        val usePrediction = preferences.getBoolean("use_prediction", false)

        val tfLitePredictor = if(BuildConfig.IS_DONATION && (usePrediction || sortByContext)) {
            TFLitePredictor(this)
        } else null

        val converters = mutableListOf<HanjaConverter>()
        if(BuildConfig.IS_DONATION && preferences.getBoolean("use_learned_word", false)) {
            val database = Room.databaseBuilder(applicationContext, HistoryDatabase::class.java, DB_HISTORY).build()
            val historyHanjaConverter = HistoryHanjaConverter(database, preferences.getBoolean("freeze_learning", false))
            CoroutineScope(Dispatchers.IO).launch { historyHanjaConverter.deleteOldWords() }
            converters += historyHanjaConverter
        }
        val additional = preferences.getStringSet("additional_dictionaries", setOf())?.toList() ?: listOf()
        val dictionaries = DictionaryManager.loadCompoundDictionary(assets, listOf("base") + additional)
        val dictionaryHanjaConverter = DictionaryHanjaConverter(dictionaries)
        if(tfLitePredictor != null && sortByContext) {
            converters += ContextSortingHanjaConverter(dictionaryHanjaConverter, tfLitePredictor)
        } else {
            converters += dictionaryHanjaConverter
        }
        converter = Converter(CompoundHanjaConverter(converters.toList()))
        if(tfLitePredictor != null && usePrediction) predictor = Predictor(tfLitePredictor)
        else predictor = null

        candidatesWindow = when(preferences.getString("window_type", "horizontal")) {
            "horizontal" -> HorizontalCandidatesWindow(this)
            else -> VerticalCandidatesWindow(this)
        }
        enableAutoHiding = preferences.getBoolean("enable_auto_hiding", false)
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
                val addedCount = event.addedCount
                val removedCount = event.removedCount

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
            if(composingText.composing.isNotEmpty()) {
                val candidates = converter.convertAsync(scope, composingText).await()
                if(candidates.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        candidatesWindow.show(candidates, rect) { hanja ->
                            val hangul = composingText.composing.take(hanja.length).toString()
                            val replaced = composingText.replaced(hanja, outputFormat)
                            ignoreText = replaced.text
                            pasteFullText(source, replaced.text)
                            setSelection(source, replaced.to)
                            composingText = replaced
                            convert(source)
                            if(hanja.all { CharacterSet.isHanja(it) }) learn(hangul, hanja)
                        }
                    }
                } else {
                    candidatesWindow.destroy()
                }
            } else if(predictor != null) {
                learnConverted()
                val anyHangul = composingText.textBeforeCursor.any { isHangul(it) }
                if(anyHangul) {
                    val candidates = predictor?.predictAsync(scope, composingText)?.await()
                    if(candidates != null) withContext(Dispatchers.Main) {
                        candidatesWindow.show(candidates, rect) { prediction ->
                            val inserted = composingText.inserted(prediction)
                            ignoreText = inserted.text
                            pasteFullText(source, inserted.text)
                            setSelection(source, inserted.to)
                            composingText = inserted
                            convert(source)
                        }
                    }
                } else {
                    candidatesWindow.destroy()
                }
            } else {
                candidatesWindow.destroy()
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
        CoroutineScope(Dispatchers.IO).launch { converter.learn(input, result) }
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
    }

}