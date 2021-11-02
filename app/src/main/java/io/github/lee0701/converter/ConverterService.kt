package io.github.lee0701.converter

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.preference.PreferenceManager
import io.github.lee0701.converter.CharacterSet.isHangul
import io.github.lee0701.converter.candidates.CandidatesWindow
import io.github.lee0701.converter.candidates.HorizontalCandidatesWindow
import io.github.lee0701.converter.candidates.VerticalCandidatesWindow
import io.github.lee0701.converter.engine.HanjaConverter
import io.github.lee0701.converter.engine.Predictor
import kotlin.math.min

class ConverterService: AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var source: AccessibilityNodeInfo

    private var outputFormat: OutputFormat? = null
    private val rect = Rect()
    private var ignoreText: String? = null

    private lateinit var hanjaConverter: HanjaConverter
    private var predictor: Predictor? = null
    private lateinit var candidatesWindow: CandidatesWindow

    private var composingText: ComposingText? = null

    override fun onCreate() {
        super.onCreate()
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false)
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
        hanjaConverter = HanjaConverter(this, outputFormat)
        if(BuildConfig.IS_DONATION) predictor = Predictor(this)
        candidatesWindow = when(preferences.getString("window_type", "horizontal")) {
            "horizontal" -> HorizontalCandidatesWindow(this)
            else -> VerticalCandidatesWindow(this)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when(event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                source = event.source ?: return
                source.getBoundsInScreen(rect)

                val ignoreText = this.ignoreText
                this.ignoreText = null
                val text = event.text.firstOrNull()?.toString() ?: ""
                if(text == ignoreText) return

                val beforeText = event.beforeText.toString()
                val fromIndex = event.fromIndex.let { if(it == -1) firstDifference(beforeText, text) else it }
                val addedCount = event.addedCount
                val removedCount = event.removedCount

                println("text=$text, before=$beforeText, from=$fromIndex, added=$addedCount, removed=$removedCount")

                val currentComposingText = composingText

                val addedText = text.drop(fromIndex).take(addedCount)
                val toIndex = fromIndex + addedCount

                if(addedText.isNotEmpty() && addedText.all { isHangul(it) }) {
                    if(currentComposingText == null) {
                        // Create composing text if not exists
                        val newComposingText = ComposingText(text, toIndex, fromIndex, toIndex)
                        composingText = newComposingText
                    } else {
                        val newComposingText = currentComposingText.copy(text = text, cursor = toIndex, to = toIndex)
                        composingText = newComposingText
                    }
                    println("$currentComposingText => $composingText")
                    println("${currentComposingText?.composing} => ${composingText?.composing}")
                } else {
                    // Reset composing if non-hangul
                    composingText = null
                    candidatesWindow.destroy()
                }
                convert()
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                val source = event.source
                val start = source.textSelectionStart
                val end = source.textSelectionEnd
                println("$start .. $end")
            }
        }
    }

    private fun convert() {
        val composingText = this.composingText
        if(composingText != null) {
            if(composingText.composing.isNotEmpty()) {
                val candidates = hanjaConverter.convert(composingText.composing)
                candidatesWindow.show(candidates, rect) { hanja ->
                    val replaced = composingText.replaced(hanja)
                    ignoreText = replaced.text
                    pasteFullText(replaced.text)
                    handler.post { setSelection(replaced.to) }
                    this.composingText = replaced
                    convert()
                }
            } else {
                candidatesWindow.destroy()
            }
        }
    }

    private fun showCandidates(word: String, targetWord: String, candidates: List<CandidatesWindow.Candidate>) {
        val lengthDiff = word.length - targetWord.length
        candidatesWindow.show(candidates, rect) { hanja ->
            val hangul = word.drop(lengthDiff).take(hanja.length)
            val formatted = (outputFormat?.let { it(hanja, hangul) } ?: hanja)
//            onReplacement(formatted, lengthDiff, hanja.length)
        }
    }

    private fun showPrediction(candidates: List<CandidatesWindow.Candidate>) {
        candidatesWindow.show(candidates, rect) { prediction ->
//            onPrediction(prediction)
        }
    }


    private fun pasteFullText(fullText: String) {
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, fullText)
        source.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    private fun setSelection(start: Int, end: Int = start) {
        val arguments = Bundle()
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, start)
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, end)
        source.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, arguments)
    }

    private fun firstDifference(a: String, b: String): Int {
        val len = min(a.length, b.length)
        for(i in 0 until len) {
            if(a[i] != b[i]) return i
        }
        return len
    }

    companion object {
        var INSTANCE: ConverterService? = null
    }

}