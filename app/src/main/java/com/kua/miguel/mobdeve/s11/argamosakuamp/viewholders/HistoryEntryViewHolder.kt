package com.kua.miguel.mobdeve.s11.argamosakuamp.adapters

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.ListLayoutBinding
import com.kua.miguel.mobdeve.s11.argamosakuamp.models.EntryModel

class HistoryEntryViewHolder(private val viewBinding: ListLayoutBinding) :
    RecyclerView.ViewHolder(viewBinding.root) {

    fun bind(entry: EntryModel) {
        // Load image using Glide
        if (entry.productPicture != null) {
            Glide.with(viewBinding.root.context)
                .load(entry.productPicture)
                .into(viewBinding.ivItem)
        }

        viewBinding.tvItemQuantity.text = "${entry.productQuantity}x"
        viewBinding.tvItemName.text = entry.productName
    }
}
