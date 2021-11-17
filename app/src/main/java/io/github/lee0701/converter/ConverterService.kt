package io.github.lee0701.converter

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.preference.PreferenceManager
import androidx.room.Room
import io.github.lee0701.converter.CharacterSet.isHangul
import io.github.lee0701.converter.CharacterSet.isHanja
import io.github.lee0701.converter.candidates.CandidatesWindow
import io.github.lee0701.converter.candidates.CandidatesWindowHider
import io.github.lee0701.converter.candidates.HorizontalCandidatesWindow
import io.github.lee0701.converter.candidates.VerticalCandidatesWindow
import io.github.lee0701.converter.dictionary.DiskDictionary
import io.github.lee0701.converter.engine.ComposingText
import io.github.lee0701.converter.engine.HanjaConverter
import io.github.lee0701.converter.engine.OutputFormat
import io.github.lee0701.converter.engine.Predictor
import io.github.lee0701.converter.history.HistoryDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import io.github.lee0701.converter.settings.SettingsActivity
import kotlin.math.abs
import kotlin.math.min

class ConverterService: AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    private var outputFormat: OutputFormat? = null
    private val rect = Rect()
    private var ignoreText: CharSequence? = null

    private lateinit var hanjaConverter: HanjaConverter
    private var predictor: Predictor? = null
    private lateinit var candidatesWindow: CandidatesWindow

    private var composingText = ComposingText("", 0)

    private var sortByContext = false
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
        val dictionary = DiskDictionary(assets.open("dict.bin"))
        val database = if(BuildConfig.IS_DONATION && preferences.getBoolean("use_learned_word", false)) {
            Room.databaseBuilder(applicationContext, HistoryDatabase::class.java, DB_HISTORY).build()
        } else null
        hanjaConverter = HanjaConverter(dictionary, database, preferences.getBoolean("freeze_learning", false))
        predictor = if(BuildConfig.IS_DONATION && preferences.getBoolean("use_prediction", false)) {
            Predictor(this)
        } else null
        sortByContext = preferences.getBoolean("sort_by_context", false)
        candidatesWindow = when(preferences.getString("window_type", "horizontal")) {
            "horizontal" -> HorizontalCandidatesWindow(this)
            else -> VerticalCandidatesWindow(this)
        }
        enableAutoHiding = preferences.getBoolean("enable_auto_hiding", false)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when(event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val isHideEvent = CandidatesWindowHider.of(event.packageName.toString())?.isHideEvent(event)
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

                val beforeText = event.beforeText
                val fromIndex = event.fromIndex.let { if(it == -1) firstDifference(beforeText, text) else it }
                val addedCount = event.addedCount
                val removedCount = event.removedCount

                val addedText = text.drop(fromIndex).take(addedCount)
                val toIndex = fromIndex + addedCount

                if(addedText.isNotEmpty() && addedText.all { isHangul(it) }) {
                    if(composingText.composing.isEmpty()) {
                        // Create composing text if not exists
                        composingText = ComposingText(text, fromIndex, toIndex)
                    } else {
                        val spaceIndex = composingText.composing.lastIndexOfAny(charArrayOf(' ', '\t', '\r', '\n'))
                        val from = composingText.from + if(spaceIndex > -1) spaceIndex + 1 else 0
                        composingText = composingText.copy(text = text, from = from, to = toIndex)
                    }
                } else {
                    // Reset composing if non-hangul
                    composingText = ComposingText(text, toIndex)
                }
                convert(source)
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                val source = event.source ?: return
                val start = source.textSelectionStart
                val end = source.textSelectionEnd
                if(start == -1 || end == -1) return
                if(start == end && abs(start - composingText.to) > 1) {
                    composingText = ComposingText(composingText.text, start)
                    convert(source)
                }
            }
            else -> {}
        }
    }

    private fun convert(source: AccessibilityNodeInfo) {
        GlobalScope.launch {
            if(composingText.composing.isNotEmpty()) {
                var converted = hanjaConverter.convertPrefixAsync(composingText.composing.toString()).await()
                val predictor = this@ConverterService.predictor
                if(predictor != null && sortByContext) {
                    val prediction = predictor.predict(predictor.tokenize(composingText.textBeforeComposing.toString()))
                    converted = converted.map { list ->
                        list.sortedByDescending { predictor.getConfidence(prediction, it.text) }
                    }
                }
                val candidates = getExtraCandidates(composingText.composing) + converted.flatten()
                withContext(Dispatchers.Main) {
                    candidatesWindow.show(candidates, rect) { hanja ->
                        val hangul = composingText.composing.take(hanja.length).toString()
                        val replaced = composingText.replaced(hanja, outputFormat)
                        ignoreText = replaced.text
                        pasteFullText(source, replaced.text)
                        setSelection(source, replaced.to)
                        composingText = replaced
                        convert(source)
                        if(hanja.all { isHanja(it) }) learn(hangul, hanja)
                    }
                }
            } else {
                if(composingText.converted.isNotEmpty() && !composingText.converted.all { isHangul(it) }) {
                    learn(composingText.unconverted, composingText.converted)
                }

                val predictor = predictor
                if(predictor != null && composingText.textBeforeCursor.any { isHangul(it) }) {
                    val prediction = predictor.predict(predictor.tokenize(composingText.textBeforeCursor.toString()))
                    val candidates = predictor.output(prediction, 10)
                    withContext(Dispatchers.Main) {
                        candidatesWindow.show(candidates, rect) { prediction ->
                            val inserted = composingText.inserted(prediction)
                            ignoreText = inserted.text
                            pasteFullText(source, inserted.text)
                            handler.post { setSelection(source, inserted.to) }
                            composingText = inserted
                            convert(source)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        candidatesWindow.destroy()
                    }
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

    private fun learn(input: String, result: String) {
        hanjaConverter.learnAsync(input, result)
    }

    private fun firstDifference(a: CharSequence, b: CharSequence): Int {
        val len = min(a.length, b.length)
        for(i in 0 until len) {
            if(a[i] != b[i]) return i
        }
        return len
    }

    private fun getExtraCandidates(hangul: CharSequence): List<CandidatesWindow.Candidate> {
        val list = mutableListOf<CharSequence>()
        val nonHangulIndex = hangul.indexOfFirst { c -> !isHangul(c) }
        list += if(nonHangulIndex > 0) hangul.slice(0 until nonHangulIndex) else hangul
        if(isHangul(hangul[0])) list.add(0, hangul[0].toString())
        return list.map { CandidatesWindow.Candidate(it.toString()) }
    }

    companion object {
        var INSTANCE: ConverterService? = null

        const val DB_HISTORY = "history"
    }

}