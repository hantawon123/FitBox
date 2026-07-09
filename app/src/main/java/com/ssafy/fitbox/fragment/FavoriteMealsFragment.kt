package com.ssafy.fitbox.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ssafy.fitbox.adapter.FavoriteMealAdapter
import com.ssafy.fitbox.databinding.FragmentFavoriteMealsBinding
import com.ssafy.fitbox.dto.FavoriteMeal
import com.ssafy.fitbox.dto.FavoriteIngredient
import com.ssafy.fitbox.dto.Product
import com.ssafy.fitbox.network.request.CartItemRequest
import com.ssafy.fitbox.network.request.CustomMealCreateRequest
import com.ssafy.fitbox.network.request.CustomMealIngredientRequest
import com.ssafy.fitbox.repository.CartRepository
import com.ssafy.fitbox.repository.IngredientRepository
import com.ssafy.fitbox.repository.MealRepository
import com.ssafy.fitbox.util.FavoriteMealStore
import com.ssafy.fitbox.util.SessionManager
import kotlinx.coroutines.launch

class FavoriteMealsFragment : Fragment() {

    private var _binding: FragmentFavoriteMealsBinding? = null
    private val binding get() = _binding!!

    private val cartRepository = CartRepository()
    private val ingredientRepository = IngredientRepository()
    private val mealRepository = MealRepository()

    private lateinit var adapter: FavoriteMealAdapter
    private var userId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFavoriteMealsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        FavoriteMealStore.initialize(requireContext())
        userId = SessionManager(requireContext()).getUser()?.id

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        adapter = FavoriteMealAdapter(
            onAddToCartClick = ::addFavoriteToCart,
            onRemoveClick = ::removeFavorite
        )
        binding.rvFavoriteMeals.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFavoriteMeals.adapter = adapter

        loadFavorites()
    }

    private fun loadFavorites() {
        val currentUserId = userId
        val favorites = if (currentUserId == null) {
            emptyList()
        } else {
            FavoriteMealStore.getFavorites(currentUserId)
        }
        adapter.submitList(favorites)
        binding.layoutFavoriteEmpty.visibility = if (favorites.isEmpty()) View.VISIBLE else View.GONE
        binding.rvFavoriteMeals.visibility = if (favorites.isEmpty()) View.GONE else View.VISIBLE

        if (favorites.isNotEmpty()) {
            viewLifecycleOwner.lifecycleScope.launch {
                adapter.submitList(enrichFavorites(favorites))
            }
        }
    }

    private suspend fun enrichFavorites(favorites: List<FavoriteMeal>): List<FavoriteMeal> {
        val mealDetailCache = mutableMapOf<Long, Product?>()

        return favorites.map { favorite ->
            val mealId = favorite.mealId
            if (mealId == null || favorite.calories > 0.0) {
                favorite
            } else {
                val product = mealDetailCache.getOrPut(mealId) {
                    mealRepository.getMealById(mealId).getOrNull()
                }

                if (product == null) {
                    favorite
                } else {
                    favorite.copy(
                        name = favorite.name.ifBlank { product.name },
                        calories = product.calories,
                        carbohydrate = product.carbohydrate,
                        protein = product.protein,
                        fat = product.fat,
                        price = if (favorite.price > 0) favorite.price else product.price,
                        description = product.description,
                        sourceType = if (product.mealType == "CUSTOM") {
                            FavoriteMeal.SOURCE_CUSTOM
                        } else {
                            favorite.sourceType
                        },
                        ingredients = product.ingredients.map {
                            FavoriteIngredient(
                                name = it.name,
                                amount = it.amount,
                                calories = it.calories
                            )
                        }
                    )
                }
            }
        }
    }

    private fun addFavoriteToCart(favorite: FavoriteMeal) {
        val currentUserId = userId
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val mealIdResult = if (favorite.mealId != null) {
                Result.success(favorite.mealId)
            } else {
                createMealFromFavorite(favorite)
            }

            mealIdResult
                .onSuccess { mealId ->
                    cartRepository.addCartItem(
                        currentUserId,
                        CartItemRequest(mealId = mealId, quantity = 1)
                    ).onSuccess {
                        Toast.makeText(requireContext(), "장바구니에 담았습니다.", Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        Toast.makeText(requireContext(), it.message ?: "장바구니 담기 실패", Toast.LENGTH_SHORT).show()
                    }
                }
                .onFailure {
                    Toast.makeText(requireContext(), it.message ?: "즐겨찾기 식단 생성 실패", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private suspend fun createMealFromFavorite(favorite: FavoriteMeal): Result<Long> {
        val ingredientResult = ingredientRepository.getIngredients()
        val ingredients = ingredientResult.getOrElse { return Result.failure(it) }

        val requests = favorite.ingredients.mapNotNull { favoriteIngredient ->
            val ingredient = ingredients.find {
                it.name.replace(" ", "") == favoriteIngredient.name.replace(" ", "")
            }
            ingredient?.let {
                CustomMealIngredientRequest(
                    ingredientId = it.id,
                    amount = favoriteIngredient.amount
                )
            }
        }

        if (requests.size != favorite.ingredients.size) {
            return Result.failure(Exception("일부 재료를 찾을 수 없어 식단을 만들 수 없습니다."))
        }

        val request = CustomMealCreateRequest(
            name = favorite.name,
            price = favorite.price,
            calories = favorite.calories.toInt(),
            carbohydrate = favorite.carbohydrate,
            protein = favorite.protein,
            fat = favorite.fat,
            ingredients = requests
        )

        return mealRepository.createCustomMeal(request).map { it.mealId }
    }

    private fun removeFavorite(favorite: FavoriteMeal) {
        val currentUserId = userId ?: return
        FavoriteMealStore.removeFavorite(currentUserId, favorite.id)
        loadFavorites()
        Toast.makeText(requireContext(), "즐겨찾기에서 삭제했습니다.", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
