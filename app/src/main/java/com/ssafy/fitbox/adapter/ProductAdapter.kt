package com.ssafy.fitbox.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ssafy.fitbox.databinding.ItemProductBinding
import com.ssafy.fitbox.dto.Product
import com.ssafy.fitbox.util.DisplayFormatter
import com.ssafy.fitbox.util.ImageUrlHelper

class ProductAdapter(
    private val onItemClick: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback) {

    private var gridMode: Boolean = false

    inner class ProductViewHolder(
        private val binding: ItemProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            val density = binding.root.resources.displayMetrics.density
            binding.root.layoutParams = binding.root.layoutParams.apply {
                width = if (gridMode) {
                    val screenWidth = binding.root.resources.displayMetrics.widthPixels
                    (screenWidth - (68 * density).toInt()) / 2
                } else {
                    (184 * density).toInt()
                }

                if (this is ViewGroup.MarginLayoutParams) {
                    marginEnd = ((if (gridMode) 6 else 14) * density).toInt()
                    bottomMargin = ((if (gridMode) 12 else 0) * density).toInt()
                }
            }

            Glide.with(binding.ivProductImage.context)
                .load(ImageUrlHelper.getFullImageUrl(product.imageUrl))
                .placeholder(product.imageRes)
                .error(product.imageRes)
                .into(binding.ivProductImage)

            binding.tvProductName.text = product.name
            binding.tvProductNutrition.text = createNutritionText(product)
            binding.tvProductPrice.text = "${formatPrice(product.price)}원"

            binding.root.setOnClickListener {
                onItemClick(product)
            }
        }
    }

    fun setGridMode(enabled: Boolean) {
        if (gridMode == enabled) return
        gridMode = enabled
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    private fun createNutritionText(product: Product): String {
        return "${formatNutrition(product.calories)}kcal · " +
            "탄수화물 ${formatNutrition(product.carbohydrate)}g · " +
            "단백질 ${formatNutrition(product.protein)}g · " +
            "지방 ${formatNutrition(product.fat)}g"
    }

    private fun formatPrice(price: Int): String {
        return DisplayFormatter.formatPrice(price)
    }

    private fun formatNutrition(value: Double): String {
        return DisplayFormatter.formatNutrition(value)
    }

    object ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}
