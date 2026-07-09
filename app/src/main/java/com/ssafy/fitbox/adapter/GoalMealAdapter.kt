package com.ssafy.fitbox.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ssafy.fitbox.R
import com.ssafy.fitbox.databinding.ItemGoalMealBinding
import com.ssafy.fitbox.dto.Product
import com.ssafy.fitbox.util.DisplayFormatter
import com.ssafy.fitbox.util.ImageUrlHelper

class GoalMealAdapter(
    private val onItemClick: (Product) -> Unit
) : ListAdapter<Product, GoalMealAdapter.GoalMealViewHolder>(GoalMealDiffCallback) {

    inner class GoalMealViewHolder(
        private val binding: ItemGoalMealBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product, rank: Int) {
            binding.tvGoalRank.text = rank.toString()
            val isTopRank = rank == 1
            binding.root.setBackgroundResource(
                if (isTopRank) R.drawable.bg_goal_meal_row_rank_first
                else R.drawable.bg_goal_meal_row_rank_default
            )
            binding.ivGoalRankCrown.visibility = if (isTopRank) View.VISIBLE else View.GONE
            binding.tvGoalMealName.text = product.name
            binding.tvGoalMealNutrition.text =
                "${formatNutrition(product.calories)}kcal · 탄수화물 ${formatNutrition(product.carbohydrate)}g · 단백질 ${formatNutrition(product.protein)}g · 지방 ${formatNutrition(product.fat)}g"
            binding.tvGoalMealPrice.text = "${DisplayFormatter.formatPrice(product.price)}원"

            Glide.with(binding.ivGoalMealImage.context)
                .load(ImageUrlHelper.getFullImageUrl(product.imageUrl))
                .placeholder(product.imageRes)
                .error(product.imageRes)
                .into(binding.ivGoalMealImage)

            binding.root.setOnClickListener {
                onItemClick(product)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GoalMealViewHolder {
        val binding = ItemGoalMealBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return GoalMealViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GoalMealViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    private fun formatNutrition(value: Double): String {
        return DisplayFormatter.formatNutrition(value)
    }

    object GoalMealDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }
}
