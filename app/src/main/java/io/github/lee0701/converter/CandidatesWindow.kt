package io.github.lee0701.converter

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.candidate_item.view.*
import kotlinx.android.synthetic.main.candidates_view.view.*

class CandidatesWindow(
    private val context: Context,
    private val columnCount: Int,
    private val windowWidth: Int,
    private val windowHeight: Int
) {

    private val windowManager = context.getSystemService(AccessibilityService.WINDOW_SERVICE) as WindowManager
    private var candidatesView: View? = null
    private var windowShown = false

    @SuppressLint("InflateParams")
    fun show(candidates: List<Candidate>, xPos: Int, yPos: Int, onItemClick: (String) -> Unit) {
        if(candidatesView == null) {
            val candidatesView = LayoutInflater.from(context).inflate(R.layout.candidates_view, null)
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

    fun destroy() {
        if(candidatesView != null) windowManager.removeView(candidatesView)
        candidatesView = null
        windowShown = false
    }

    class CandidateListAdapter(private val data: Array<Candidate>, private val onItemClick: (String) -> Unit)
        : RecyclerView.Adapter<CandidateListAdapter.CandidateItemViewHolder>() {

        class CandidateItemViewHolder(val view: View): RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateItemViewHolder {
            val textView = LayoutInflater.from(parent.context).inflate(R.layout.candidate_item, parent, false)
            return CandidateItemViewHolder(textView)
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

    data class Candidate(
        val text: String,
        val extra: String
    )

}