package com.kua.miguel.mobdeve.s11.argamosakuamp.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.CartLayoutBinding
import com.kua.miguel.mobdeve.s11.argamosakuamp.models.EntryModel

class CartViewHolder(private val viewBinding: CartLayoutBinding) : RecyclerView.ViewHolder(viewBinding.root) {
    fun bindData(entryData: EntryModel) {
        viewBinding.tvCartItemQuantity.text = "${entryData.productQuantity}"
        viewBinding.tvCartItemName.text = entryData.productName
    }
}
