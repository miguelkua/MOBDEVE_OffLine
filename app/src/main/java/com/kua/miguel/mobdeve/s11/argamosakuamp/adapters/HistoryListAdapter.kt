package com.kua.miguel.mobdeve.s11.argamosakuamp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.HistoryLayoutBinding
import com.kua.miguel.mobdeve.s11.argamosakuamp.models.HistoryEntryModel

class HistoryListAdapter(private val historyEntries: List<HistoryEntryModel>) :
    RecyclerView.Adapter<HistoryListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryListViewHolder {
        val binding = HistoryLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryListViewHolder, position: Int) {
        holder.bind(historyEntries[position])
    }

    override fun getItemCount(): Int = historyEntries.size
}
