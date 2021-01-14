package io.github.lee0701.converter

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.preference.PreferenceManager
import kotlin.math.abs

class ConverterService: AccessibilityService(), HanjaConverter.Listener {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var source: AccessibilityNodeInfo

    private lateinit var hanjaConverter: HanjaConverter

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
        hanjaConverter = HanjaConverter(this, this)
        INSTANCE = this
    }

    override fun onDestroy() {
        super.onDestroy()
        INSTANCE = null
    }

    override fun onInterrupt() {
    }

    fun restartHanjaConverter() {
        hanjaConverter = HanjaConverter(this, this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if(event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            && event.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
                return
        }

        source = event.source ?: return
        text = event.text?.firstOrNull()?.toString() ?: ""
        cursor = source.textSelectionStart
        if(cursor == -1) cursor = 0

        if(cursorManuallyMoved && !backSpaced) {
            resetInput()
            if(startIndex > 0) startIndex -= 1
        }

        when(event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                source.getBoundsInScreen(hanjaConverter.rect)

                // Word break inserted
                if(getCurrentWord().isEmpty()) {
                    resetInput()
                }
                cursorMovedByConversion = false

                onInput()
            }
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                source.getBoundsInScreen(hanjaConverter.rect)

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
        hanjaConverter.onWord(word)
    }

    override fun onReplacement(replacement: String, index: Int, length: Int) {
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
        handler.post { hanjaConverter.onWord(getCurrentWord()) }
    }

    private fun getCurrentWord(): String {
        return text.take(cursor).drop(startIndex).split("\\s".toRegex()).lastOrNull() ?: ""
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