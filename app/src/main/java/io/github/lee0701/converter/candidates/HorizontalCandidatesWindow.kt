package io.github.lee0701.converter.candidates

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.lee0701.converter.R
import kotlinx.android.synthetic.main.candidates_view_horizontal.view.*

class HorizontalCandidatesWindow(private val context: Context): CandidatesWindow(context) {

    private val landscape get() = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    private val windowY get() = preferences.getInt(if(landscape) Key.Y_LANDSCAPE else Key.Y_PORTRAIT, 500)
    private val windowHeight get() = preferences.getInt(if(landscape) Key.HEIGHT_LANDSCAPE else Key.HEIGHT_PORTRAIT, 200)

    private var candidatesView: View? = null
    private var windowShown = false

    @SuppressLint("InflateParams")
    override fun show(candidates: List<Candidate>, rect: Rect, onItemClick: (String) -> Unit) {
        if(candidatesView == null) {
            val candidatesView = LayoutInflater.from(context).inflate(R.layout.candidates_view_horizontal, null)
            candidatesView.setBackgroundColor(windowColor)
            candidatesView.close.setOnClickListener { destroy() }
            candidatesView.close.backgroundTintList = ColorStateList.valueOf(textColor)
            candidatesView.close.alpha = CandidateWindowColor.ALPHA
            candidatesView.count.setTextColor(textColor)
            candidatesView.count.alpha = CandidateWindowColor.ALPHA
            candidatesView.list.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            candidatesView.list.addOnScrollListener(object: RecyclerView.OnScrollListener() {
                @SuppressLint("SetTextI18n")
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val p = (candidatesView.list.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
                    val count = candidatesView.list.adapter?.itemCount ?: 0
                    candidatesView.count.text = "$p / $count"
                }
            })

            val type = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, windowHeight,
                0, windowY,
                type, flags, PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.START
            try {
                windowManager.addView(candidatesView, params)
            } catch(ex: WindowManager.BadTokenException) {
                Toast.makeText(context, R.string.overlay_permission_required, Toast.LENGTH_LONG).show()
            }
            this.candidatesView = candidatesView
        }
        val layout = if(showExtra) R.layout.candidate_item_horizontal else R.layout.candidate_item_horizontal_without_extra
        val view = candidatesView ?: return
        view.list.adapter =
            CandidateListAdapter(layout, textColor, extraColor,
                candidates.toTypedArray(), onItemClick)
        view.list.scrollToPosition(0)
        windowShown = true
    }

    override fun destroy() {
        if(candidatesView != null) windowManager.removeView(candidatesView)
        candidatesView = null
        windowShown = false
    }

    object Key {
        const val Y_PORTRAIT = "horizontal_window_y_portrait"
        const val HEIGHT_PORTRAIT = "horizontal_window_height_portrait"
        const val Y_LANDSCAPE = "horizontal_window_y_landscape"
        const val HEIGHT_LANDSCAPE = "horizontal_window_height_landscape"
    }

}