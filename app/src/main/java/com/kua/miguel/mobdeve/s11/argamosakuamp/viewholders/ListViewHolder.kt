package com.kua.miguel.mobdeve.s11.argamosakuamp.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.ListLayoutBinding
import com.kua.miguel.mobdeve.s11.argamosakuamp.models.EntryModel

class ListViewHolder(private val viewBinding: ListLayoutBinding) : RecyclerView.ViewHolder(viewBinding.root) {
    fun bindData(entryData: EntryModel) {
        viewBinding.ivItem.setImageResource(entryData.productPicture)
        viewBinding.tvItemQuantity.text = "${entryData.productQuantity}x"
        viewBinding.tvItemName.text = entryData.productName
    }
}
