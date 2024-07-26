package com.kua.miguel.mobdeve.s11.argamosakuamp.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kua.miguel.mobdeve.s11.argamosakuamp.databinding.CheckoutLayoutBinding
import com.kua.miguel.mobdeve.s11.argamosakuamp.models.EntryModel

class CheckoutViewHolder(private val viewBinding: CheckoutLayoutBinding) : RecyclerView.ViewHolder(viewBinding.root) {
    fun bindData(entryData: EntryModel) {
        if (entryData.productPicture != null) {
            Glide.with(viewBinding.root.context)
                .load(entryData.productPicture)
                .into(viewBinding.ivCheckoutItem)
        }

        viewBinding.tvCheckoutItemQuantity.text = "${entryData.productQuantity}x"
        viewBinding.tvCheckoutItemName.text = entryData.productName
    }
}
