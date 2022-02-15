package io.github.lee0701.converter.assistant

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.KeyEvent
import android.widget.LinearLayout
import androidx.annotation.RequiresApi
import io.github.lee0701.converter.ConverterTileService

@RequiresApi(Build.VERSION_CODES.N)
class InputAssistantView(
    context: Context,
    attributeSet: AttributeSet,
): LinearLayout(context, attributeSet) {

    // Back button Handling
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if(event != null) {
            if(event.keyCode == KeyEvent.KEYCODE_BACK) {
                    ConverterTileService.INSTANCE?.closeWindow()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    // Home button | Apps button Handling
    fun onCloseSystemDialogs(reason: String) {
        ConverterTileService.INSTANCE?.closeWindow()
    }

}