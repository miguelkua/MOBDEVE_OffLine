package com.kua.miguel.mobdeve.s11.argamosakuamp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.CheckoutLayoutBinding
import com.kua.miguel.mobdeve.s11.argamosakuamp.models.EntryModel
import com.kua.miguel.mobdeve.s11.argamosakuamp.viewholders.CheckoutViewHolder

class CheckoutAdapter(private val data: ArrayList<EntryModel>) : RecyclerView.Adapter<CheckoutViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CheckoutViewHolder {
        val itemViewBinding = CheckoutLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CheckoutViewHolder(itemViewBinding)
    }

    override fun onBindViewHolder(holder: CheckoutViewHolder, position: Int) {
        holder.bindData(data[position])
    }

    override fun getItemCount(): Int {
        return data.size
    }
}
