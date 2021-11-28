package io.github.lee0701.converter.candidates.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.lee0701.converter.candidates.Candidate
import io.github.lee0701.converter.databinding.CandidateItemHorizontalBinding

class HorizontalCandidateListAdapter(
    private val showExtra: Boolean,
    private val textColor: Int,
    private val extraColor: Int,
    private val textAlpha: Float,
    private val windowHeight: Int,
    private val data: Array<Candidate>,
    private val onItemClick: (String) -> Unit
): RecyclerView.Adapter<HorizontalCandidateListAdapter.CandidateItemViewHolder>() {

    class CandidateItemViewHolder(val view: CandidateItemHorizontalBinding): RecyclerView.ViewHolder(view.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateItemViewHolder {
        val view = CandidateItemHorizontalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        view.text.setTextColor(textColor)
        view.text.alpha = textAlpha
        if(!showExtra) view.root.removeView(view.extra)
        else {
            view.extra.setTextColor(extraColor)
            view.extra.alpha = textAlpha
        }
        view.root.layoutParams.height = windowHeight
        return CandidateItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: CandidateItemViewHolder, position: Int) {
        holder.view.text.text = data[position].text
        holder.view.extra.text = data[position].extra
        holder.view.root.setOnClickListener { this.onItemClick(data[position].text) }
    }

    override fun getItemCount(): Int {
        return data.size
    }
}