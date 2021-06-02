package io.github.lee0701.converter.candidates

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.github.lee0701.converter.R
import kotlinx.android.synthetic.main.candidate_item_horizontal.view.*

class CandidateListAdapter(
    private val itemLayoutRes: Int,
    private val textColor: Int,
    private val extraColor: Int,
    private val data: Array<CandidatesWindow.Candidate>,
    private val onItemClick: (String) -> Unit
): RecyclerView.Adapter<CandidateListAdapter.CandidateItemViewHolder>() {

    class CandidateItemViewHolder(val view: View): RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(itemLayoutRes, parent, false)
        val text = view.text as TextView
        text.setTextColor(textColor)
        text.alpha = 0.54f
        val extra = view.extra as TextView
        extra.setTextColor(extraColor)
        extra.alpha = 0.54f
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