package com.kua.miguel.mobdeve.s11.argamosakuamp.adapters

import android.content.Intent
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.kua.miguel.mobdeve.s11.argamosakuamp.activities.HistoryEntryActivity
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.HistoryLayoutBinding
import com.kua.miguel.mobdeve.s11.argamosakuamp.models.HistoryEntryModel

class HistoryListViewHolder(private val viewBinding: HistoryLayoutBinding) :
    RecyclerView.ViewHolder(viewBinding.root) {

    fun bind(entry: HistoryEntryModel) {
        viewBinding.tvHistoryEntry.text = entry.dateTime // Set the combined date and time

        // Set click listener for each history entry
        viewBinding.root.setOnClickListener {
            val context = viewBinding.root.context
            val intent = Intent(context, HistoryEntryActivity::class.java)
            intent.putExtra("pastListId", entry.pastListId)
            context.startActivity(intent)
        }
    }
}
