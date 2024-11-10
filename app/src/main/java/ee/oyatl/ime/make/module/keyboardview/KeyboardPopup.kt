package ee.oyatl.ime.make.module.keyboardview

import android.content.Context
import android.view.View
import android.widget.PopupWindow

abstract class KeyboardPopup(
    val context: Context,
    val key: KeyboardView.KeyWrapper,
) {
    protected val popupWindow: PopupWindow = PopupWindow(context, null)

    abstract val offsetX: Int
    abstract val offsetY: Int
    abstract val width: Int
    abstract val height: Int

    abstract fun show(parent: View, parentX: Int, parentY: Int)
    abstract fun touchMove(x: Int, y: Int)
    abstract fun touchUp()
    abstract fun dismiss()
    abstract fun cancel()

}