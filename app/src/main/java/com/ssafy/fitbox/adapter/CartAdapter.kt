package com.ssafy.fitbox.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ssafy.fitbox.databinding.ItemCartBinding
import com.ssafy.fitbox.dto.CartItem
import com.ssafy.fitbox.dto.MealType
import com.ssafy.fitbox.util.DisplayFormatter
import com.ssafy.fitbox.util.ImageUrlHelper
import com.ssafy.fitbox.R


class CartAdapter(
    private var cartItems: List<CartItem>,
    private val onRemoveClick: (CartItem) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    inner class CartViewHolder(
        private val binding: ItemCartBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(cartItem: CartItem) {
            Glide.with(binding.ivCartItemImage.context)
                .load(ImageUrlHelper.getFullImageUrl(cartItem.imageUrl))
                .placeholder(R.drawable.logo_full_background_remove)
                .error(R.drawable.logo_full_background_remove)
                .into(binding.ivCartItemImage)

            binding.tvCartItemName.text = cartItem.name

            binding.tvCartItemTypeQuantity.text =
                "${getMealTypeText(cartItem.mealType)} · 수량 ${cartItem.quantity}개"

            binding.tvCartItemNutrition.text =
                "${formatNutrition(cartItem.calories)}kcal · 탄수화물 ${formatNutrition(cartItem.carbohydrate)}g · 단백질 ${formatNutrition(cartItem.protein)}g · 지방 ${formatNutrition(cartItem.fat)}g"

            binding.tvCartItemPrice.text =
                "${formatPrice(cartItem.price * cartItem.quantity)}원"

            binding.btnDeleteCartItem.setOnClickListener {
                onRemoveClick(cartItem)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding = ItemCartBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(cartItems[position])
    }

    override fun getItemCount(): Int = cartItems.size

    fun submitItems(newItems: List<CartItem>) {
        cartItems = newItems
        notifyDataSetChanged()
    }

    private fun getMealTypeText(mealType: String): String {
        return when (mealType) {
            MealType.PRODUCT -> "완제품"
            MealType.CUSTOM -> "커스텀 식단"
            else -> "식단"
        }
    }

    private fun formatNutrition(value: Double): String {
        return DisplayFormatter.formatNutrition(value)
    }

    private fun formatPrice(value: Int): String {
        return DisplayFormatter.formatPrice(value)
    }
}
