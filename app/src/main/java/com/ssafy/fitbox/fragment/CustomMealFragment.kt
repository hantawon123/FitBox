package com.ssafy.fitbox.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.ssafy.fitbox.R
import com.ssafy.fitbox.databinding.FragmentCustomMealBinding
import com.ssafy.fitbox.dto.IngredientCategory
import com.ssafy.fitbox.dto.SelectedIngredient
import com.ssafy.fitbox.network.request.CartItemRequest
import com.ssafy.fitbox.network.request.CustomMealCreateRequest
import com.ssafy.fitbox.network.request.CustomMealIngredientRequest
import com.ssafy.fitbox.util.DisplayFormatter
import com.ssafy.fitbox.util.ImageUrlHelper
import com.ssafy.fitbox.viewmodel.CartViewModel
import com.ssafy.fitbox.viewmodel.CustomMealViewModel
import com.ssafy.fitbox.viewmodel.MealViewModel
import kotlin.math.roundToInt

class CustomMealFragment : Fragment() {

    private var _binding: FragmentCustomMealBinding? = null
    private val binding get() = _binding!!

    private val mealViewModel: MealViewModel by viewModels()

    private val cartViewModel: CartViewModel by activityViewModels()
    private val customMealViewModel: CustomMealViewModel by activityViewModels()
    private val entryMode: String
        get() = arguments?.getString(ARG_ENTRY_MODE) ?: MODE_DEFAULT

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomMealBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        updateEntryModeUi()
        initClickEvents()
        observeSelectedIngredients()
        observeCustomMealCreateResult()
    }

    private fun updateEntryModeUi() {
        if (entryMode == MODE_SUBSCRIPTION) {
            binding.tvCustomTitle.text = "구독용 커스텀 식단 만들기"
            binding.btnCustomComplete.text = "구독 메뉴로 선택"
        }
    }

    private fun initClickEvents() {
        binding.cardCarbohydrate.setOnClickListener {
            moveToIngredientSelect(IngredientCategory.CARBOHYDRATE)
        }

        binding.cardProtein.setOnClickListener {
            moveToIngredientSelect(IngredientCategory.PROTEIN)
        }

        binding.cardVegetable.setOnClickListener {
            moveToIngredientSelect(IngredientCategory.VEGETABLE)
        }

        binding.cardSauce.setOnClickListener {
            moveToIngredientSelect(IngredientCategory.SAUCE)
        }

        binding.btnDefaultMealName.setOnClickListener {
            val selectedIngredients = customMealViewModel.selectedIngredients.value
                .orEmpty()
                .values
                .toList()

            binding.etCustomMealName.setText(getDefaultCustomMealName(selectedIngredients))
        }

        binding.btnCustomComplete.setOnClickListener {
            createCustomMeal()
        }
    }

    private fun moveToIngredientSelect(category: IngredientCategory) {
        val fragment = IngredientSelectFragment.newInstance(category)

        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun observeSelectedIngredients() {
        customMealViewModel.selectedIngredients.observe(viewLifecycleOwner) { selectedMap ->
            val selectedList = selectedMap.values.toList()
            updateSummary(selectedList)
            updateCategoryCards()
        }
    }

    private fun updateSummary(selectedList: List<SelectedIngredient>) {
        if (selectedList.isEmpty()) {
            binding.tvSelectedIngredients.text = "아직 선택된 재료가 없습니다"
        } else {
            binding.tvSelectedIngredients.text = selectedList.joinToString(" + ") {
                "${it.ingredient.name} ${it.quantityGram}g"
            }
        }

        val totalCalories = selectedList.sumOf { it.totalCalories() }
        val totalCarbohydrate = selectedList.sumOf { it.totalCarbohydrate() }
        val totalProtein = selectedList.sumOf { it.totalProtein() }
        val totalFat = selectedList.sumOf { it.totalFat() }
        val totalPrice = selectedList.sumOf { it.totalPrice() }

        binding.tvTotalNutrition.text =
            "${formatNutrition(totalCalories)}kcal · 탄수화물 ${formatNutrition(totalCarbohydrate)}g · 단백질 ${formatNutrition(totalProtein)}g · 지방 ${formatNutrition(totalFat)}g"

        binding.tvTotalPrice.text =
            "${DisplayFormatter.formatPrice(totalPrice)}원"
    }

    private fun updateCategoryCards() {
        updateCategoryCard(
            category = IngredientCategory.CARBOHYDRATE,
            imageView = binding.ivCarbohydrateImage,
            emptyTextView = binding.tvCarbohydrateSelected
        )
        updateCategoryCard(
            category = IngredientCategory.PROTEIN,
            imageView = binding.ivProteinImage,
            emptyTextView = binding.tvProteinSelected
        )
        updateCategoryCard(
            category = IngredientCategory.VEGETABLE,
            imageView = binding.ivVegetableImage,
            emptyTextView = binding.tvVegetableSelected
        )
        updateCategoryCard(
            category = IngredientCategory.SAUCE,
            imageView = binding.ivSauceImage,
            emptyTextView = binding.tvSauceSelected
        )
    }

    private fun updateCategoryCard(
        category: IngredientCategory,
        imageView: ImageView,
        emptyTextView: TextView
    ) {
        val selectedList = customMealViewModel.getSelectedByCategory(category)
        val latestIngredient = selectedList.lastOrNull()?.ingredient
        emptyTextView.text = "선택 안함"
        imageView.visibility = if (latestIngredient == null) {
            Glide.with(imageView.context).clear(imageView)
            View.GONE
        } else {
            View.VISIBLE
        }
        emptyTextView.visibility = if (latestIngredient == null) View.VISIBLE else View.GONE

        if (latestIngredient == null) {
            return
        }

        Glide.with(imageView.context)
            .load(ImageUrlHelper.getFullImageUrl(latestIngredient?.imageUrl))
            .placeholder(R.drawable.logo_full_background_remove)
            .error(R.drawable.logo_full_background_remove)
            .into(imageView)
    }

    private fun createCustomMeal() {
        val selectedIngredients = customMealViewModel.selectedIngredients.value
            .orEmpty()
            .values
            .toList()

        if (selectedIngredients.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "선택된 재료가 없습니다.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val totalCalories = selectedIngredients.sumOf { it.totalCalories() }
        val totalCarbohydrate = selectedIngredients.sumOf { it.totalCarbohydrate() }
        val totalProtein = selectedIngredients.sumOf { it.totalProtein() }
        val totalFat = selectedIngredients.sumOf { it.totalFat() }
        val totalPrice = selectedIngredients.sumOf { it.totalPrice() }

        if (totalPrice < MIN_CUSTOM_MEAL_PRICE) {
            Toast.makeText(
                requireContext(),
                "재료 합계가 최소 6,000원 이상이어야 주문할 수 있습니다.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val customMealName = binding.etCustomMealName.text
            ?.toString()
            ?.trim()
            .orEmpty()

        if (customMealName.isBlank()) {
            Toast.makeText(
                requireContext(),
                "식단 이름을 입력하거나 기본값을 눌러주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val request = CustomMealCreateRequest(
            name = customMealName,
            price = totalPrice,
            calories = totalCalories.roundToInt(),
            carbohydrate = totalCarbohydrate,
            protein = totalProtein,
            fat = totalFat,
            ingredients = selectedIngredients.map { selectedIngredient ->
                CustomMealIngredientRequest(
                    ingredientId = selectedIngredient.ingredient.id,
                    amount = selectedIngredient.quantityGram
                )
            }
        )

        mealViewModel.createCustomMeal(request)
    }

    private fun getDefaultCustomMealName(selectedIngredients: List<SelectedIngredient>): String {
        if (selectedIngredients.isEmpty()) {
            return "나만의 커스텀 식단"
        }

        val ingredientNames = selectedIngredients.joinToString(" + ") {
            it.ingredient.name
        }.let { name ->
            if (name.length > 30) {
                name.take(30) + "..."
            } else {
                name
            }
        }

        return "나만의 커스텀 식단 - $ingredientNames"
    }

    private fun showMoveCartDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("장바구니에 담겼습니다")
            .setMessage("장바구니로 이동하시겠습니까?")
            .setPositiveButton("예") { _, _ ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, CartFragment())
                    .addToBackStack(null)
                    .commit()
            }
            .setNegativeButton("아니오") { _, _ ->
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, HomeFragment())
                    .commit()
            }
            .show()
    }

    private fun formatNutrition(value: Double): String {
        return DisplayFormatter.formatNutrition(value)
    }

    private fun observeCustomMealCreateResult() {
        mealViewModel.customMealCreateResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { response ->
                Toast.makeText(
                    requireContext(),
                    "커스텀 식단이 생성되었습니다.",
                    Toast.LENGTH_SHORT
                ).show()

                customMealViewModel.clearSelectedIngredients()

                if (entryMode == MODE_SUBSCRIPTION) {
                    parentFragmentManager.setFragmentResult(
                        REQUEST_KEY_CUSTOM_MEAL,
                        Bundle().apply {
                            putLong(KEY_CUSTOM_MEAL_ID, response.mealId)
                            putString(KEY_CUSTOM_MEAL_NAME, response.name)
                            putInt(KEY_CUSTOM_MEAL_PRICE, response.price)
                            putInt(KEY_CUSTOM_MEAL_CALORIES, response.calories)
                            putDouble(KEY_CUSTOM_MEAL_CARBOHYDRATE, response.carbohydrate)
                            putDouble(KEY_CUSTOM_MEAL_PROTEIN, response.protein)
                            putDouble(KEY_CUSTOM_MEAL_FAT, response.fat)
                        }
                    )
                    parentFragmentManager.popBackStack()
                } else {
                    cartViewModel.addCartItem(
                        CartItemRequest(
                            mealId = response.mealId,
                            quantity = 1
                        )
                    )
                    showMoveCartDialog()
                }
            }

            result.onFailure { throwable ->
                Toast.makeText(
                    requireContext(),
                    throwable.message ?: "커스텀 식단 생성에 실패했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val MIN_CUSTOM_MEAL_PRICE = 6000
        private const val ARG_ENTRY_MODE = "entry_mode"

        const val MODE_DEFAULT = "DEFAULT"
        const val MODE_SUBSCRIPTION = "SUBSCRIPTION"

        const val REQUEST_KEY_CUSTOM_MEAL = "request_key_custom_meal"
        const val KEY_CUSTOM_MEAL_ID = "key_custom_meal_id"
        const val KEY_CUSTOM_MEAL_NAME = "key_custom_meal_name"
        const val KEY_CUSTOM_MEAL_PRICE = "key_custom_meal_price"
        const val KEY_CUSTOM_MEAL_CALORIES = "key_custom_meal_calories"
        const val KEY_CUSTOM_MEAL_CARBOHYDRATE = "key_custom_meal_carbohydrate"
        const val KEY_CUSTOM_MEAL_PROTEIN = "key_custom_meal_protein"
        const val KEY_CUSTOM_MEAL_FAT = "key_custom_meal_fat"

        fun newInstance(entryMode: String): CustomMealFragment {
            return CustomMealFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ENTRY_MODE, entryMode)
                }
            }
        }
    }
}
