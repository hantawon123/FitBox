package com.ssafy.fitbox.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.fitbox.databinding.ItemFavoriteMealBinding
import com.ssafy.fitbox.dto.FavoriteMeal
import com.ssafy.fitbox.util.DisplayFormatter

class FavoriteMealAdapter(
    private val onAddToCartClick: (FavoriteMeal) -> Unit,
    private val onRemoveClick: (FavoriteMeal) -> Unit
) : RecyclerView.Adapter<FavoriteMealAdapter.FavoriteMealViewHolder>() {

    private val favorites = mutableListOf<FavoriteMeal>()

    fun submitList(items: List<FavoriteMeal>) {
        favorites.clear()
        favorites.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteMealViewHolder {
        return FavoriteMealViewHolder(
            ItemFavoriteMealBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: FavoriteMealViewHolder, position: Int) {
        holder.bind(favorites[position])
    }

    override fun getItemCount(): Int = favorites.size

    inner class FavoriteMealViewHolder(
        private val binding: ItemFavoriteMealBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(favorite: FavoriteMeal) {
            binding.tvFavoriteType.text = when (favorite.sourceType) {
                FavoriteMeal.SOURCE_AI -> "AI 추천 식단"
                FavoriteMeal.SOURCE_CUSTOM -> "커스텀 식단"
                else -> "완제품 식단"
            }
            binding.tvFavoriteName.text = favorite.name
            binding.tvFavoritePrice.text = "${formatPrice(favorite.price)}원"
            bindNutrition(favorite)
            bindIngredients(favorite)

            binding.btnAddFavoriteToCart.setOnClickListener { onAddToCartClick(favorite) }
            binding.btnRemoveFavorite.setOnClickListener { onRemoveClick(favorite) }
        }

        private fun bindNutrition(favorite: FavoriteMeal) {
            binding.tvFavoriteNutrition.visibility = View.VISIBLE
            if (favorite.calories <= 0.0) {
                binding.tvFavoriteNutrition.text =
                    favorite.description ?: "식단 영양 정보를 불러올 수 없습니다."
                return
            }

            val ratios = DisplayFormatter.macroRatios(
                favorite.carbohydrate,
                favorite.protein,
                favorite.fat
            )
            binding.tvFavoriteNutrition.text =
                "${favorite.calories.format1()}kcal · " +
                    "탄수화물 ${favorite.carbohydrate.format1()}g (${ratios.first}%) · " +
                    "단백질 ${favorite.protein.format1()}g (${ratios.second}%) ·\n" +
                    "지방 ${favorite.fat.format1()}g (${ratios.third}%)"
        }

        private fun bindIngredients(favorite: FavoriteMeal) {
            val ingredientText = if (favorite.ingredients.isNotEmpty()) {
                favorite.ingredients.joinToString("\n") {
                    "• ${it.name} ${it.amount}g"
                }
            } else {
                favorite.description.orEmpty()
            }
            binding.tvFavoriteIngredients.text = ingredientText
            binding.tvFavoriteIngredients.visibility =
                if (ingredientText.isBlank()) View.GONE else View.VISIBLE
        }
    }

    private fun Double.format1(): String = String.format("%.1f", this)

    private fun formatPrice(value: Int): String = DisplayFormatter.formatPrice(value)
}
