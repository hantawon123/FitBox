package com.ssafy.fitbox.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ssafy.fitbox.dto.Ingredient
import com.ssafy.fitbox.dto.IngredientCategory
import com.ssafy.fitbox.dto.SelectedIngredient

class CustomMealViewModel : ViewModel() {

    private val _selectedIngredients = MutableLiveData<Map<Int, SelectedIngredient>>(emptyMap())
    val selectedIngredients: LiveData<Map<Int, SelectedIngredient>> = _selectedIngredients
    private val selectedIngredientOrder = mutableListOf<Int>()

    fun increaseIngredient(
        ingredient: Ingredient,
        amountGram: Int = SelectedIngredient.DEFAULT_QUANTITY_STEP
    ) {
        val currentMap = getCurrentSelectedMap()
        val currentSelectedIngredient = currentMap[ingredient.id]

        currentMap[ingredient.id] =
            if (currentSelectedIngredient == null) {
                SelectedIngredient(
                    ingredient = ingredient,
                    quantityGram = amountGram
                )
            } else {
                currentSelectedIngredient.increase(amountGram)
            }

        markIngredientSelectedLast(ingredient.id)
        updateSelectedIngredients(currentMap)
    }

    fun decreaseIngredient(
        ingredient: Ingredient,
        amountGram: Int = SelectedIngredient.DEFAULT_QUANTITY_STEP
    ) {
        val currentMap = getCurrentSelectedMap()
        val currentSelectedIngredient = currentMap[ingredient.id] ?: return

        val decreasedIngredient = currentSelectedIngredient.decrease(amountGram)

        if (decreasedIngredient.isEmpty()) {
            currentMap.remove(ingredient.id)
            selectedIngredientOrder.remove(ingredient.id)
        } else {
            currentMap[ingredient.id] = decreasedIngredient
        }

        updateSelectedIngredients(currentMap)
    }

    fun getQuantityGram(ingredient: Ingredient): Int {
        return _selectedIngredients.value
            .orEmpty()[ingredient.id]
            ?.quantityGram ?: 0
    }

    fun getSelectedByCategory(category: IngredientCategory): List<SelectedIngredient> {
        val selectedMap = _selectedIngredients.value.orEmpty()
        return selectedIngredientOrder.mapNotNull { ingredientId ->
            selectedMap[ingredientId]
        }.filter { selectedIngredient ->
            selectedIngredient.ingredient.category == category
        }
    }

    fun clearSelectedIngredients() {
        selectedIngredientOrder.clear()
        _selectedIngredients.value = emptyMap()
    }

    private fun getCurrentSelectedMap(): MutableMap<Int, SelectedIngredient> {
        return _selectedIngredients.value
            .orEmpty()
            .toMutableMap()
    }

    private fun updateSelectedIngredients(
        selectedIngredientMap: Map<Int, SelectedIngredient>
    ) {
        _selectedIngredients.value = selectedIngredientMap
    }

    private fun markIngredientSelectedLast(ingredientId: Int) {
        selectedIngredientOrder.remove(ingredientId)
        selectedIngredientOrder.add(ingredientId)
    }

}
