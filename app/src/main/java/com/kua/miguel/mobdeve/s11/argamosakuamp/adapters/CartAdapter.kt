package com.kua.miguel.mobdeve.s11.argamosakuamp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.CartLayoutBinding
import com.kua.miguel.mobdeve.s11.argamosakuamp.models.EntryModel
import com.kua.miguel.mobdeve.s11.argamosakuamp.viewholders.CartViewHolder

class CartAdapter(private val data: ArrayList<EntryModel>) : RecyclerView.Adapter<CartViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val itemViewBinding = CartLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(itemViewBinding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bindData(data[position])
    }

    override fun getItemCount(): Int {
        return data.size
    }
}
