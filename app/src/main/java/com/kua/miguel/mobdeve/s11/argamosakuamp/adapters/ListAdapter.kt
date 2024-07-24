package com.kua.miguel.mobdeve.s11.argamosakuamp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.ListLayoutBinding
import com.kua.miguel.mobdeve.s11.argamosakuamp.models.EntryModel
import com.kua.miguel.mobdeve.s11.argamosakuamp.viewholders.ListViewHolder

class ListAdapter(private val data: ArrayList<EntryModel>) : RecyclerView.Adapter<ListViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val itemViewBinding = ListLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ListViewHolder(itemViewBinding)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        holder.bindData(data[position])
    }

    override fun getItemCount(): Int {
        return data.size
    }
}