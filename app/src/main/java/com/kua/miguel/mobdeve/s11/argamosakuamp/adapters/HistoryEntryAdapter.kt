package com.kua.miguel.mobdeve.s11.argamosakuamp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.ListLayoutBinding
import com.kua.miguel.mobdeve.s11.argamosakuamp.models.EntryModel

class HistoryEntryAdapter(private val entries: List<EntryModel>) :
    RecyclerView.Adapter<HistoryEntryViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryEntryViewHolder {
        val binding = ListLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryEntryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryEntryViewHolder, position: Int) {
        holder.bind(entries[position])
    }

    override fun getItemCount(): Int = entries.size
}
