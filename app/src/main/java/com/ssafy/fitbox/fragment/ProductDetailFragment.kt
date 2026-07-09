package com.ssafy.fitbox.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.ssafy.fitbox.R
import com.ssafy.fitbox.databinding.FragmentProductDetailBinding
import com.ssafy.fitbox.dto.Product
import com.ssafy.fitbox.network.request.CartItemRequest
import com.ssafy.fitbox.repository.CartRepository
import com.ssafy.fitbox.repository.MealRepository
import com.ssafy.fitbox.util.DisplayFormatter
import com.ssafy.fitbox.util.SessionManager
import com.ssafy.fitbox.util.ImageUrlHelper
import com.ssafy.fitbox.viewmodel.CartViewModel
import kotlinx.coroutines.launch

class ProductDetailFragment : Fragment() {

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!
    private val cartViewModel: CartViewModel by activityViewModels()
    private val mealRepository = MealRepository()
    private val cartRepository = CartRepository()

    private var productId = 0
    private var name = ""
    private var imageRes = 0
    private var calories = 0.0
    private var carbohydrate = 0.0
    private var protein = 0.0
    private var fat = 0.0
    private var price = 0
    private var imageUrl: String? = null
    private var description = ""
    private var selectedQuantity = MIN_QUANTITY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            productId = it.getInt(ARG_PRODUCT_ID)
            name = it.getString(ARG_NAME).orEmpty()
            imageRes = it.getInt(ARG_IMAGE_RES)
            calories = it.getDouble(ARG_CALORIES)
            carbohydrate = it.getDouble(ARG_CARBOHYDRATE)
            protein = it.getDouble(ARG_PROTEIN)
            fat = it.getDouble(ARG_FAT)
            price = it.getInt(ARG_PRICE)
            description = it.getString(ARG_DESCRIPTION).orEmpty()
            imageUrl = it.getString(ARG_IMAGE_URL)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindProductInfo()
        loadDetailedProduct()
        initQuantityPicker()
        initClickEvents()
    }

    private fun loadDetailedProduct() {
        if (productId <= 0) return
        viewLifecycleOwner.lifecycleScope.launch {
            mealRepository.getMealById(productId.toLong()).onSuccess { product ->
                name = product.name
                calories = product.calories
                carbohydrate = product.carbohydrate
                protein = product.protein
                fat = product.fat
                price = product.price
                description = product.description
                imageUrl = product.imageUrl ?: imageUrl
                bindProductInfo()
            }
        }
    }

    private fun bindProductInfo() {
        Glide.with(this)
            .load(ImageUrlHelper.getFullImageUrl(imageUrl))
            .placeholder(imageRes)
            .error(imageRes)
            .into(binding.ivDetailImage)
        binding.tvDetailName.text = name
        binding.tvDetailDescription.text = description.ifBlank {
            "등록된 식재료 정보를 불러오는 중입니다."
        }
        bindNutrition()
        binding.tvDetailPrice.text = "${DisplayFormatter.formatPrice(price)}원"
    }

    private fun bindNutrition() {
        val (carbRatio, proteinRatio, fatRatio) =
            DisplayFormatter.macroRatios(carbohydrate, protein, fat)
        binding.tvDetailCalories.text =
            "${DisplayFormatter.formatNutrition(calories)} kcal"
        binding.tvDetailCarbohydrate.text =
            "${DisplayFormatter.formatNutrition(carbohydrate)}g"
        binding.tvDetailCarbohydrateRatio.text = "$carbRatio%"
        binding.tvDetailProtein.text = "${DisplayFormatter.formatNutrition(protein)}g"
        binding.tvDetailProteinRatio.text = "$proteinRatio%"
        binding.tvDetailFat.text = "${DisplayFormatter.formatNutrition(fat)}g"
        binding.tvDetailFatRatio.text = "$fatRatio%"
    }

    private fun initQuantityPicker() {
        updateQuantity()
        binding.btnDecreaseQuantity.setOnClickListener {
            if (selectedQuantity > MIN_QUANTITY) {
                selectedQuantity--
                updateQuantity()
            }
        }
        binding.btnIncreaseQuantity.setOnClickListener {
            if (selectedQuantity < MAX_QUANTITY) {
                selectedQuantity++
                updateQuantity()
            }
        }
    }

    private fun updateQuantity() {
        binding.tvQuantityValue.text = selectedQuantity.toString()
        binding.btnDecreaseQuantity.isEnabled = selectedQuantity > MIN_QUANTITY
        binding.btnIncreaseQuantity.isEnabled = selectedQuantity < MAX_QUANTITY
    }

    private fun initClickEvents() {
        binding.btnAddCart.setOnClickListener {
            if (productId <= 0) {
                Toast.makeText(requireContext(), "상품 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val userId = SessionManager(requireContext()).getUser()?.id
            if (userId == null) {
                Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewLifecycleOwner.lifecycleScope.launch {
                cartRepository.addCartItem(
                    userId = userId,
                    request = CartItemRequest(
                        mealId = productId.toLong(),
                        quantity = selectedQuantity
                    )
                ).onSuccess {
                    cartViewModel.loadCartItems()
                    Toast.makeText(
                        requireContext(),
                        "장바구니에 담겼습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }.onFailure {
                    Toast.makeText(
                        requireContext(),
                        it.message ?: "장바구니 담기에 실패했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_PRODUCT_ID = "productId"
        private const val ARG_NAME = "name"
        private const val ARG_IMAGE_RES = "imageRes"
        private const val ARG_CALORIES = "calories"
        private const val ARG_CARBOHYDRATE = "carbohydrate"
        private const val ARG_PROTEIN = "protein"
        private const val ARG_FAT = "fat"
        private const val ARG_PRICE = "price"
        private const val ARG_DESCRIPTION = "description"
        private const val ARG_IMAGE_URL = "imageUrl"
        private const val MIN_QUANTITY = 1
        private const val MAX_QUANTITY = 20

        fun newInstance(product: Product) = ProductDetailFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_PRODUCT_ID, product.id)
                putString(ARG_NAME, product.name)
                putInt(ARG_IMAGE_RES, product.imageRes)
                putDouble(ARG_CALORIES, product.calories)
                putDouble(ARG_CARBOHYDRATE, product.carbohydrate)
                putDouble(ARG_PROTEIN, product.protein)
                putDouble(ARG_FAT, product.fat)
                putInt(ARG_PRICE, product.price)
                putString(ARG_DESCRIPTION, product.description)
                putString(ARG_IMAGE_URL, product.imageUrl)
            }
        }
    }
}
