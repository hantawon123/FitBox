package com.ssafy.fitbox.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ssafy.fitbox.adapter.IngredientAdapter
import com.ssafy.fitbox.databinding.FragmentIngredientSelectBinding
import com.ssafy.fitbox.dto.Ingredient
import com.ssafy.fitbox.dto.IngredientCategory
import com.ssafy.fitbox.repository.IngredientRepository
import com.ssafy.fitbox.util.DisplayFormatter
import com.ssafy.fitbox.viewmodel.CustomMealViewModel
import kotlinx.coroutines.launch

class IngredientSelectFragment : Fragment() {

    private var _binding: FragmentIngredientSelectBinding? = null
    private val binding get() = _binding!!

    private val customMealViewModel: CustomMealViewModel by activityViewModels()
    private val ingredientRepository = IngredientRepository()

    private lateinit var category: IngredientCategory
    private lateinit var ingredientAdapter: IngredientAdapter

    // 현재 카테고리에 해당하는 재료만 저장하는 리스트
    private var ingredientList: List<Ingredient> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        category = getCategoryFromArguments()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIngredientSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        initHeader()
        initRecyclerView()
        initIngredientSearch()
        initClickEvents()
        observeSelectedIngredients()
        loadIngredients()
        updateBottomSummary()
    }

    private fun getCategoryFromArguments(): IngredientCategory {
        val categoryName = arguments?.getString(ARG_CATEGORY)
            ?: IngredientCategory.PROTEIN.name

        return runCatching {
            IngredientCategory.valueOf(categoryName)
        }.getOrDefault(IngredientCategory.PROTEIN)
    }

    private fun initHeader() {
        binding.tvSelectTitle.text = "${category.displayName} 선택"
        binding.tvSelectDescription.text = "원하는 ${category.displayName}과 양을 선택해주세요"
    }

    private fun initRecyclerView() {
        ingredientAdapter = IngredientAdapter(
            ingredientList = emptyList(),
            getQuantityGram = { ingredient ->
                customMealViewModel.getQuantityGram(ingredient)
            },
            onIncreaseClick = { ingredient ->
                customMealViewModel.increaseIngredient(ingredient)
                ingredientAdapter.refreshIngredient(ingredient)
                updateBottomSummary()
            },
            onDecreaseClick = { ingredient ->
                customMealViewModel.decreaseIngredient(ingredient)
                ingredientAdapter.refreshIngredient(ingredient)
                updateBottomSummary()
            }
        )

        binding.rvIngredientSelect.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ingredientAdapter
            setHasFixedSize(true)
        }
    }

    private fun initIngredientSearch() {
        binding.etSearchIngredient.hint = "${getCategoryDisplayName()} 재료 검색"

        binding.etSearchIngredient.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {
            }

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                filterIngredients(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
    }

    private fun initClickEvents() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnDone.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadIngredients() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = ingredientRepository.getIngredients()

            result.onSuccess { ingredients ->
                val filteredIngredients = ingredients.filter { ingredient ->
                    ingredient.category == category
                }

                // 검색 기준이 되는 현재 카테고리 재료 리스트 저장
                ingredientList = filteredIngredients

                // 처음 화면 진입 시 현재 카테고리 전체 재료 표시
                ingredientAdapter.updateList(ingredientList)

                // 혹시 검색창에 기존 입력값이 있으면 다시 필터링
                filterIngredients(binding.etSearchIngredient.text.toString())

            }.onFailure { exception ->
                Toast.makeText(
                    requireContext(),
                    exception.message ?: "재료 정보를 불러오지 못했습니다",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun observeSelectedIngredients() {
        customMealViewModel.selectedIngredients.observe(viewLifecycleOwner) {
            if (::ingredientAdapter.isInitialized) {
                ingredientAdapter.notifyDataSetChanged()
            }

            updateBottomSummary()
        }
    }

    private fun filterIngredients(keyword: String) {
        if (!::ingredientAdapter.isInitialized) return

        val trimmedKeyword = keyword.trim()

        val filteredList = if (trimmedKeyword.isEmpty()) {
            ingredientList
        } else {
            ingredientList.filter { ingredient ->
                ingredient.name.contains(trimmedKeyword, ignoreCase = true)
            }
        }

        ingredientAdapter.updateList(filteredList)
    }

    private fun getCategoryDisplayName(): String {
        return category.displayName
    }

    private fun updateBottomSummary() {
        val selectedIngredientsInCategory = customMealViewModel
            .getSelectedByCategory(category)

        val totalQuantityGram = selectedIngredientsInCategory.sumOf { selectedIngredient ->
            selectedIngredient.quantityGram
        }

        val totalPrice = selectedIngredientsInCategory.sumOf { selectedIngredient ->
            selectedIngredient.totalPrice()
        }

        binding.tvSelectedCategoryAmount.text =
            "선택한 ${category.displayName} 총량 ${totalQuantityGram}g"

        binding.tvSelectedCategoryPrice.text =
            "${DisplayFormatter.formatPrice(totalPrice)}원"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CATEGORY = "category"

        fun newInstance(category: IngredientCategory): IngredientSelectFragment {
            return IngredientSelectFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY, category.name)
                }
            }
        }
    }
}
