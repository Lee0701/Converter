package io.github.lee0701.converter.candidates.view

import android.content.Context
import android.util.AttributeSet
import android.view.KeyEvent
import androidx.appcompat.widget.LinearLayoutCompat
import io.github.lee0701.converter.ConverterAccessibilityService

class CandidatesViewLayout(
    context: Context,
    attributeSet: AttributeSet?,
): LinearLayoutCompat(context, attributeSet) {

    // Back button Handling
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if(event != null) {
            if(event.keyCode == KeyEvent.KEYCODE_BACK) {
                ConverterAccessibilityService.INSTANCE?.closeCandidatesWindow()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    // Home button | Apps button Handling
    fun onCloseSystemDialogs(reason: String) {
        ConverterAccessibilityService.INSTANCE?.closeCandidatesWindow()
    }

}