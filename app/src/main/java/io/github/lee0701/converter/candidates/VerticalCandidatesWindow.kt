package io.github.lee0701.converter.candidates

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.util.TypedValue
import android.view.*
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.lee0701.converter.R
import kotlinx.android.synthetic.main.candidate_item_vertical.view.*
import kotlinx.android.synthetic.main.candidates_view_vertical.view.*

class VerticalCandidatesWindow(
    private val context: Context
): CandidatesWindow(context) {

    private val resources = context.resources
    private val statusBarHeight
        get() = resources.getDimensionPixelSize(
            resources.getIdentifier("status_bar_height", "dimen", "android")
        )

    private val windowSizeMultiplier = 40

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val columnCount = preferences.getInt("column_count", 2)
    private val windowWidth = getWindowSize(preferences.getInt("window_width", 5))
    private val windowHeight = getWindowSize(preferences.getInt("window_height", 4))

    private var candidatesView: View? = null
    private var windowShown = false

    @SuppressLint("InflateParams")
    override fun show(candidates: List<Candidate>, rect: Rect, onItemClick: (String) -> Unit) {
        if(candidatesView == null) {
            val candidatesView = LayoutInflater.from(context).inflate(R.layout.candidates_view_vertical, null)
            candidatesView.alpha = 0.75f
            candidatesView.close.setOnClickListener { destroy() }
            candidatesView.list.layoutManager = GridLayoutManager(context, columnCount)
            candidatesView.list.addOnScrollListener(object: RecyclerView.OnScrollListener() {
                @SuppressLint("SetTextI18n")
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    val p = (candidatesView.list.layoutManager as GridLayoutManager).findFirstVisibleItemPosition()
                    val count = candidatesView.list.adapter?.itemCount ?: 0
                    candidatesView.count.text = "$p / $count"
                }
            })

            val type = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            val flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL

            val xPos = rect.left
            val yPos =
                if(rect.centerY() < context.resources.displayMetrics.heightPixels / 2) rect.bottom - statusBarHeight
                else rect.top - windowHeight - statusBarHeight

            val params = WindowManager.LayoutParams(
                windowWidth, windowHeight, xPos, yPos,
                type, flags, PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP or Gravity.START
            windowManager.addView(candidatesView, params)
            this.candidatesView = candidatesView
        }
        val view = candidatesView ?: return
        view.list.adapter =
            CandidateListAdapter(candidates.toTypedArray(), onItemClick)
        view.list.scrollToPosition(0)
        windowShown = true
    }

    override fun destroy() {
        if(candidatesView != null) windowManager.removeView(candidatesView)
        candidatesView = null
        windowShown = false
    }

    private fun getWindowSize(sizeInPref: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        (sizeInPref * windowSizeMultiplier).toFloat(),
        context.resources.displayMetrics
    ).toInt()

    class CandidateListAdapter(private val data: Array<Candidate>, private val onItemClick: (String) -> Unit)
        : RecyclerView.Adapter<CandidateListAdapter.CandidateItemViewHolder>() {

        class CandidateItemViewHolder(val view: View): RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateItemViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.candidate_item_vertical, parent, false)
            return CandidateItemViewHolder(view)
        }

        override fun onBindViewHolder(holder: CandidateItemViewHolder, position: Int) {
            val text = holder.view.text as TextView
            val extra = holder.view.extra as TextView
            text.text = data[position].text
            extra.text = data[position].extra
            holder.view.setOnClickListener { this.onItemClick(data[position].text) }
        }

        override fun getItemCount(): Int {
            return data.size
        }
    }

}