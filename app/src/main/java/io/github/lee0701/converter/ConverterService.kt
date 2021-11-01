package io.github.lee0701.converter

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.preference.PreferenceManager
import io.github.lee0701.converter.candidates.CandidatesWindow
import io.github.lee0701.converter.candidates.HorizontalCandidatesWindow
import io.github.lee0701.converter.candidates.VerticalCandidatesWindow
import io.github.lee0701.converter.engine.HanjaConverter
import io.github.lee0701.converter.engine.Predictor
import kotlin.math.abs

class ConverterService: AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var source: AccessibilityNodeInfo

    private var outputFormat: OutputFormat? = null
    private val rect = Rect()

    private lateinit var hanjaConverter: HanjaConverter
    private var predictor: Predictor? = null
    private lateinit var candidatesWindow: CandidatesWindow

    private var text: String = ""
    private var cursor = 0
    private var lastCursor = 0
    private var startIndex = 0
    private var endIndex = Integer.MAX_VALUE
    private var cursorMovedByConversion = false

    private val backSpaced get() = cursor - lastCursor < 0
    private val cursorManuallyMoved get() = !cursorMovedByConversion && (cursor < startIndex || cursor > endIndex || abs(cursor - lastCursor) > 1)

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
        if(event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            && event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
                return
        }

        source = event.source ?: return
        if(cursorMovedByConversion) {
            setTextCursor(cursor)
            cursorMovedByConversion = false
        } else {
            text = event.text?.firstOrNull()?.toString() ?: ""
            cursor = source.textSelectionStart
            if(cursor == -1) cursor = 0
        }


        if(cursorManuallyMoved && !backSpaced) {
            resetInput()
            if(startIndex > 0) startIndex -= 1
        }

        when(event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                source.getBoundsInScreen(rect)

                // Word break inserted
                if(getCurrentWord().isEmpty()) {
                    resetInput()
                }
                cursorMovedByConversion = false

                onInput()
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                source.getBoundsInScreen(rect)

                // backspace or manual movement
                if(backSpaced || cursorManuallyMoved) {
                    resetInput()
                }

            }
            else -> { }
        }
        lastCursor = cursor
    }

    private fun resetInput() {
        startIndex = cursor
        endIndex = Integer.MAX_VALUE
    }

    private fun onInput() {
        handler.removeCallbacksAndMessages(null)
        val word = getCurrentWord()
        val targetWord = hanjaConverter.preProcessWord(word)
        if(text.isEmpty()) candidatesWindow.destroy()
        else if(targetWord.isEmpty()) {
            val candidates = predictor?.let { it.predict(it.tokenize(getTextBeforeCursor())) } ?: emptyList()
            if(candidates.isEmpty()) candidatesWindow.destroy()
            else showPrediction(candidates)
        }
        else showCandidates(word, targetWord, hanjaConverter.convert(targetWord))
    }

    private fun showCandidates(word: String, targetWord: String, candidates: List<CandidatesWindow.Candidate>) {
        val lengthDiff = word.length - targetWord.length
        candidatesWindow.show(candidates, rect) { hanja ->
            val hangul = word.drop(lengthDiff).take(hanja.length)
            val formatted = (outputFormat?.let { it(hanja, hangul) } ?: hanja)
            onReplacement(formatted, lengthDiff, hanja.length)
        }
    }

    private fun showPrediction(candidates: List<CandidatesWindow.Candidate>) {
        candidatesWindow.show(candidates, rect) { prediction ->
            onPrediction(prediction)
        }
    }

    private fun onReplacement(replacement: String, index: Int, length: Int) {
        val word = getCurrentWord()
        val diff = replacement.length - length
        endIndex = cursor
        val pasteText = text.take(startIndex) + word.take(index) + replacement + word.drop(index + length) + text.drop(endIndex)
        pasteFullText(pasteText)
        // For some apps that trigger cursor change event already
        text = pasteText
        cursor += diff
        endIndex = cursor
        setTextCursor(cursor)
        startIndex += replacement.length + index
        cursorMovedByConversion = true
        handler.post { onInput() }
    }

    private fun onPrediction(prediction: String) {
        val pasteText = getTextBeforeCursor() + prediction + getTextAfterCursor()
        pasteFullText(pasteText)
        // For some apps that trigger cursor change event already
        text = pasteText
        cursor += prediction.length
        setTextCursor(cursor)
        startIndex += prediction.length
        cursorMovedByConversion = true
        handler.post { onInput() }
    }

    private fun getTextBeforeCursor(): String {
        return text.take(cursor)
    }

    private fun getTextAfterCursor(): String {
        return text.drop(cursor)
    }

    private fun getCurrentWord(): String {
        return getTextBeforeCursor().drop(startIndex).split("\\s".toRegex()).lastOrNull() ?: ""
    }

    private fun pasteFullText(fullText: String) {
        val arguments = Bundle()
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, fullText)
        source.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
    }

    private fun setTextCursor(cursor: Int) {
        val arguments = Bundle()
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, cursor)
        arguments.putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, cursor)
        source.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, arguments)
    }

    companion object {
        var INSTANCE: ConverterService? = null
    }

}