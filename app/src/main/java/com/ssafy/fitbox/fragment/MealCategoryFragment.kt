package com.ssafy.fitbox.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.ssafy.fitbox.R
import com.ssafy.fitbox.adapter.ProductAdapter
import com.ssafy.fitbox.databinding.FragmentMealCategoryBinding
import com.ssafy.fitbox.dto.MealCategory
import com.ssafy.fitbox.dto.Product
import com.ssafy.fitbox.repository.MealRepository
import kotlinx.coroutines.launch

class MealCategoryFragment : Fragment() {

    private var _binding: FragmentMealCategoryBinding? = null
    private val binding get() = _binding!!

    private val mealRepository = MealRepository()
    private val category by lazy {
        MealCategory.fromName(arguments?.getString(ARG_CATEGORY))
    }
    private lateinit var productAdapter: ProductAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMealCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        productAdapter = ProductAdapter(::openProductDetail).apply {
            setGridMode(true)
        }
        binding.rvCategoryMeals.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = productAdapter
            setHasFixedSize(false)
        }

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.tvCategoryTitle.text = category.displayName
        binding.tvCategoryDescription.text = category.description
        loadMeals()
    }

    private fun loadMeals() {
        binding.progressCategory.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            mealRepository.getProductMealsCached()
                .onSuccess { products ->
                    val filteredProducts = products.filter(category::matches)
                    productAdapter.submitList(filteredProducts)
                    binding.layoutCategoryEmpty.visibility =
                        if (filteredProducts.isEmpty()) View.VISIBLE else View.GONE
                    binding.rvCategoryMeals.visibility =
                        if (filteredProducts.isEmpty()) View.GONE else View.VISIBLE
                }
                .onFailure { error ->
                    productAdapter.submitList(emptyList())
                    binding.layoutCategoryEmpty.visibility = View.VISIBLE
                    binding.rvCategoryMeals.visibility = View.GONE
                    Toast.makeText(
                        requireContext(),
                        error.message ?: "식단을 불러오지 못했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            binding.progressCategory.visibility = View.GONE
        }
    }

    private fun openProductDetail(product: Product) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, ProductDetailFragment.newInstance(product))
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_CATEGORY = "meal_category"

        fun newInstance(category: MealCategory) = MealCategoryFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_CATEGORY, category.name)
            }
        }
    }
}
