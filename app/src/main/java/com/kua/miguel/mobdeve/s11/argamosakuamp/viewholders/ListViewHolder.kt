package com.kua.miguel.mobdeve.s11.argamosakuamp.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.ListLayoutBinding
import com.kua.miguel.mobdeve.s11.argamosakuamp.models.EntryModel

class ListViewHolder(private val viewBinding: ListLayoutBinding) : RecyclerView.ViewHolder(viewBinding.root) {
    fun bindData(entryData: EntryModel) {
        if (entryData.productPicture != null) {
            Glide.with(viewBinding.root.context)
                .load(entryData.productPicture) // Assuming this is a URL or URI
                .into(viewBinding.ivItem)
        }
        viewBinding.tvItemQuantity.text = "${entryData.productQuantity}x"
        viewBinding.tvItemName.text = entryData.productName
    }
}
