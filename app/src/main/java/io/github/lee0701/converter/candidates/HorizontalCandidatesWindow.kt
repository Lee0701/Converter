package io.github.lee0701.converter.candidates

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.*
import android.widget.TextView
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.github.lee0701.converter.R
import kotlinx.android.synthetic.main.candidate_item_horizontal.view.*
import kotlinx.android.synthetic.main.candidates_view_horizontal.view.*

class HorizontalCandidatesWindow(private val context: Context): CandidatesWindow(context) {

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val windowY = preferences.getInt("horizontal_window_y", 500)
    private val windowHeight = preferences.getInt("horizontal_window_height", 200)

    private var candidatesView: View? = null
    private var windowShown = false

    @SuppressLint("InflateParams")
    override fun show(candidates: List<Candidate>, rect: Rect, onItemClick: (String) -> Unit) {
        if(candidatesView == null) {
            val candidatesView = LayoutInflater.from(context).inflate(R.layout.candidates_view_horizontal, null)
            candidatesView.close.setOnClickListener { destroy() }
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

    class CandidateListAdapter(private val data: Array<Candidate>, private val onItemClick: (String) -> Unit)
        : RecyclerView.Adapter<CandidateListAdapter.CandidateItemViewHolder>() {

        class CandidateItemViewHolder(val view: View): RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateItemViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.candidate_item_horizontal, parent, false)
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