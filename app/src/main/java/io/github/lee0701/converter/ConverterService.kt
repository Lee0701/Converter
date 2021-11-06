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
import kotlin.math.abs
import kotlin.math.min

class ConverterService: AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    private var outputFormat: OutputFormat? = null
    private val rect = Rect()
    private var ignoreText: String? = null

    private lateinit var hanjaConverter: HanjaConverter
    private var predictor: Predictor? = null
    private lateinit var candidatesWindow: CandidatesWindow

    private var composingText = ComposingText("", 0)

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
        if(BuildConfig.IS_DONATION
            && preferences.getBoolean("use_prediction", false)) predictor = Predictor(this)
        else predictor = null
        candidatesWindow = when(preferences.getString("window_type", "horizontal")) {
            "horizontal" -> HorizontalCandidatesWindow(this)
            else -> VerticalCandidatesWindow(this)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        when(event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                val source = event.source ?: return
                source.getBoundsInScreen(rect)

                val ignoreText = this.ignoreText
                this.ignoreText = null
                val text = event.text.firstOrNull()?.toString() ?: ""
                if(text == ignoreText) return

                val beforeText = event.beforeText.toString()
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
                        composingText = composingText.copy(text = text, to = toIndex)
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
        }
    }

    private fun convert(source: AccessibilityNodeInfo) {
        if(composingText.composing.isNotEmpty()) {
            val candidates = hanjaConverter.convert(composingText.composing)
            candidatesWindow.show(candidates, rect) { hanja ->
                val hangul = composingText.composing.take(hanja.length)
                val formatted = outputFormat?.getOutput(hanja, hangul) ?: hanja
                val replaced = composingText.replaced(formatted, hanja.length)
                ignoreText = replaced.text
                pasteFullText(source, replaced.text)
                handler.post { setSelection(source, replaced.to) }
                composingText = replaced
                convert(source)
            }
        } else {
            val predictor = this.predictor
            if(predictor != null && composingText.textBeforeCursor.any { isHangul(it) }) {
                val candidates = predictor.predict(predictor.tokenize(composingText.textBeforeCursor))
                val convertedCandidates = candidates.flatMap { candidate ->
                    if(candidate.text.length > 1 && candidate.text.all { isHangul(it) }) {
                        listOf(candidate) + hanjaConverter.convertExact(candidate.text)
                    } else listOf(candidate)
                }
                candidatesWindow.show(convertedCandidates, rect) { prediction ->
                    val inserted = composingText.inserted(prediction)
                    ignoreText = inserted.text
                    pasteFullText(source, inserted.text)
                    handler.post { setSelection(source, inserted.to) }
                    composingText = inserted
                    convert(source)
                }
            } else {
                candidatesWindow.destroy()
            }
        }
    }

    private fun pasteFullText(source: AccessibilityNodeInfo, fullText: String) {
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