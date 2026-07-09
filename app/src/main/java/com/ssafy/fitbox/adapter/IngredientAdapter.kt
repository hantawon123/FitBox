package com.ssafy.fitbox.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ssafy.fitbox.R
import com.ssafy.fitbox.databinding.ItemIngredientBinding
import com.ssafy.fitbox.dto.Ingredient
import com.ssafy.fitbox.util.DisplayFormatter
import com.ssafy.fitbox.util.ImageUrlHelper

class IngredientAdapter(
    ingredientList: List<Ingredient>,
    private val getQuantityGram: (Ingredient) -> Int,
    private val onIncreaseClick: (Ingredient) -> Unit,
    private val onDecreaseClick: (Ingredient) -> Unit
) : RecyclerView.Adapter<IngredientAdapter.IngredientViewHolder>() {

    private val items: MutableList<Ingredient> = ingredientList.toMutableList()

    inner class IngredientViewHolder(
        private val binding: ItemIngredientBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(ingredient: Ingredient) {
            val quantityGram = getQuantityGram(ingredient)

            Glide.with(binding.ivIngredientImage.context)
                .load(ImageUrlHelper.getFullImageUrl(ingredient.imageUrl))
                .placeholder(R.drawable.logo_full_background_remove)
                .error(R.drawable.logo_full_background_remove)
                .into(binding.ivIngredientImage)

            binding.tvIngredientName.text = ingredient.name
            binding.tvIngredientNutrition.text = createNutritionText(ingredient)
            binding.tvIngredientPrice.text = createPriceText(ingredient)
            binding.tvIngredientQuantity.text = "${quantityGram}g"

            binding.btnIncreaseIngredient.setOnClickListener {
                onIncreaseClick(ingredient)
            }

            binding.btnDecreaseIngredient.setOnClickListener {
                onDecreaseClick(ingredient)
            }

            binding.cardIngredient.alpha =
                if (quantityGram > 0) SELECTED_ALPHA else UNSELECTED_ALPHA
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientViewHolder {
        val binding = ItemIngredientBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return IngredientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IngredientViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun updateList(newList: List<Ingredient>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    fun refreshIngredient(ingredient: Ingredient) {
        val position = items.indexOfFirst { item ->
            item.id == ingredient.id
        }

        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(position)
        }
    }

    private fun createNutritionText(ingredient: Ingredient): String {
        val proteinPerDisplayUnit = ingredient.protein * DISPLAY_GRAM
        val caloriesPerDisplayUnit = ingredient.calories * DISPLAY_GRAM

        return "${DISPLAY_GRAM}g 당 ${formatNutrition(proteinPerDisplayUnit)}g 단백질 · ${formatNutrition(caloriesPerDisplayUnit)}kcal"
    }

    private fun createPriceText(ingredient: Ingredient): String {
        val pricePerStep = ingredient.price * QUANTITY_STEP_GRAM
        return "+ ${formatPrice(pricePerStep)}원 (${QUANTITY_STEP_GRAM}g)"
    }

    private fun formatNutrition(value: Double): String {
        return DisplayFormatter.formatNutrition(value)
    }

    private fun formatPrice(value: Int): String {
        return DisplayFormatter.formatPrice(value)
    }

    companion object {
        private const val DISPLAY_GRAM = 100
        private const val QUANTITY_STEP_GRAM = 50
        private const val SELECTED_ALPHA = 1.0f
        private const val UNSELECTED_ALPHA = 0.75f
    }
}
