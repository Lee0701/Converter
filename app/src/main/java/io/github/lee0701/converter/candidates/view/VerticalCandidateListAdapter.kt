package io.github.lee0701.converter.candidates.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.github.lee0701.converter.databinding.CandidateItemVerticalBinding
import io.github.lee0701.converter.engine.Candidate

class VerticalCandidateListAdapter(
    private val showExtra: Boolean,
    private val textColor: Int,
    private val extraColor: Int,
    private val textAlpha: Float,
    private val data: Array<Candidate>,
    private val onItemClick: (Candidate) -> Unit
): RecyclerView.Adapter<VerticalCandidateListAdapter.CandidateItemViewHolder>() {

    class CandidateItemViewHolder(val view: CandidateItemVerticalBinding): RecyclerView.ViewHolder(view.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CandidateItemViewHolder {
        val view = CandidateItemVerticalBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        view.text.alpha = textAlpha
        if(!showExtra) view.root.removeView(view.extra)
        else {
            view.extra.setTextColor(extraColor)
            view.extra.alpha = textAlpha
        }
        return CandidateItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: CandidateItemViewHolder, position: Int) {
        holder.view.text.setTextColor(data[position].color ?: textColor)
        holder.view.text.text = data[position].hanja
        holder.view.extra.text = data[position].extra
        holder.view.root.setOnClickListener { this.onItemClick(data[position]) }
    }

    override fun getItemCount(): Int {
        return data.size
    }
}