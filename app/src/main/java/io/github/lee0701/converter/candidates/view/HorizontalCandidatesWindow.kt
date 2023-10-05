package io.github.lee0701.converter.candidates.view

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Rect
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexWrap
import com.google.android.flexbox.FlexboxLayoutManager
import io.github.lee0701.converter.R
import io.github.lee0701.converter.databinding.CandidatesViewHorizontalBinding
import io.github.lee0701.converter.engine.Candidate

class HorizontalCandidatesWindow(private val context: Context): CandidatesWindow(context) {

    private val landscape get() = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    private val windowY get() = preferences.getInt(if(landscape) Key.Y_LANDSCAPE else Key.Y_PORTRAIT, 500)
    private val windowHeight get() = preferences.getInt(if(landscape) Key.HEIGHT_LANDSCAPE else Key.HEIGHT_PORTRAIT, 200)

    private var candidatesView: CandidatesViewHorizontalBinding? = null
    private var windowShown = false
    private var expanded = false

    private val type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
    private val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
    private val layoutParams: WindowManager.LayoutParams get() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT, windowHeight,
        0, windowY,
        type, flags, PixelFormat.TRANSLUCENT
    ).apply { gravity = Gravity.TOP or Gravity.START }

    override fun show(candidates: List<Candidate>, rect: Rect, onItemClick: (Candidate) -> Unit) {
        if(candidatesView == null) {
            val candidatesView = CandidatesViewHorizontalBinding.inflate(LayoutInflater.from(context))

            candidatesView.root.setBackgroundColor(windowColor)

            val params = LinearLayoutCompat.LayoutParams(LinearLayoutCompat.LayoutParams.WRAP_CONTENT, windowHeight)
            candidatesView.expandWrapper.layoutParams = params
            candidatesView.closeWrapper.layoutParams = params

            candidatesView.expand.setOnClickListener { toggleExpand() }
            candidatesView.expand.imageTintList = ColorStateList.valueOf(textColor)
            candidatesView.expand.alpha = textAlpha

            candidatesView.close.setOnClickListener { destroy() }
            candidatesView.close.imageTintList = ColorStateList.valueOf(textColor)
            candidatesView.close.alpha = textAlpha

            candidatesView.list.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)

            try {
                windowManager.addView(candidatesView.root, layoutParams)
            } catch(ex: WindowManager.BadTokenException) {
                Toast.makeText(context, R.string.overlay_permission_required, Toast.LENGTH_LONG).show()
            }
            this.candidatesView = candidatesView
        }
        val view = candidatesView ?: return
        view.list.adapter =
            HorizontalCandidateListAdapter(showExtra, textColor, extraColor, textAlpha, windowHeight,
                candidates.toTypedArray(), onItemClick)
        view.list.scrollToPosition(0)
        windowShown = true
        expanded = false
    }

    override fun destroy() {
        candidatesView?.root?.let { windowManager.removeView(it) }
        candidatesView = null
        windowShown = false
    }

    private fun toggleExpand() {
        expanded = !expanded
        val candidatesView = candidatesView ?: return
        if(expanded) {
            val displayHeight = context.resources.displayMetrics.heightPixels
            windowManager.updateViewLayout(candidatesView.root, layoutParams.apply { height = displayHeight - y })
            candidatesView.list.layoutManager = FlexboxLayoutManager(context, FlexDirection.ROW, FlexWrap.WRAP)
            candidatesView.expand.setBackgroundResource(R.drawable.ic_baseline_arrow_drop_up_24)
        } else {
            windowManager.updateViewLayout(candidatesView.root, layoutParams)
            candidatesView.list.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            candidatesView.expand.setBackgroundResource(R.drawable.ic_baseline_arrow_drop_down_24)
        }
        val adapter = candidatesView.list.adapter
        candidatesView.list.adapter = null
        candidatesView.list.adapter = adapter
    }

    object Key {
        const val Y_PORTRAIT = "horizontal_window_y_portrait"
        const val HEIGHT_PORTRAIT = "horizontal_window_height_portrait"
        const val Y_LANDSCAPE = "horizontal_window_y_landscape"
        const val HEIGHT_LANDSCAPE = "horizontal_window_height_landscape"
    }

}