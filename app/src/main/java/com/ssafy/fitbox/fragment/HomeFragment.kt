package com.ssafy.fitbox.fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.ssafy.fitbox.adapter.GoalMealAdapter
import com.ssafy.fitbox.R
import com.ssafy.fitbox.adapter.ProductAdapter
import com.ssafy.fitbox.databinding.FragmentHomeBinding
import com.ssafy.fitbox.dto.Product
import com.ssafy.fitbox.dto.MealCategory
import com.ssafy.fitbox.network.request.CartItemRequest
import com.ssafy.fitbox.repository.MealRepository
import com.ssafy.fitbox.repository.NotificationRepository
import com.ssafy.fitbox.notification.NotificationEvents
import com.ssafy.fitbox.util.DisplayFormatter
import com.ssafy.fitbox.util.ImageUrlHelper
import com.ssafy.fitbox.viewmodel.CartViewModel
import kotlinx.coroutines.launch
import com.ssafy.fitbox.util.SessionManager

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var productAdapter: ProductAdapter
    private lateinit var categoryProductAdapter: ProductAdapter
    private lateinit var goalMealAdapter: GoalMealAdapter

    private val cartViewModel: CartViewModel by activityViewModels()
    private val mealRepository = MealRepository()
    private val notificationRepository = NotificationRepository()

    private var productList: List<Product> = emptyList()
    private var allProductMeals: List<Product> = emptyList()
    private var todayRecommendProduct: Product? = null
    private var monthlyPopularProducts: List<Product> = emptyList()
    private var monthlyPopularIndex: Int = 0
    private var selectedMealCategory: MealCategory = MealCategory.ALL
    private val carouselHandler = Handler(Looper.getMainLooper())
    private val monthlyPopularCarouselRunnable = object : Runnable {
        override fun run() {
            if (monthlyPopularProducts.size > 1 && _binding != null) {
                showMonthlyPopularProduct(monthlyPopularIndex + 1)
                carouselHandler.postDelayed(this, POPULAR_CAROUSEL_INTERVAL_MS)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initRecyclerView()
        initSearch()
        initButton()
        observeCartCount()
        observeNotificationEvents()

        cartViewModel.loadCartItems()
        loadAllProductsForSearch()
        loadMonthlyPopularMeals()
        loadPopularMeals()
        loadUnreadNotificationCount()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            loadUnreadNotificationCount()
        }
    }

    private fun initRecyclerView() {
        productAdapter = ProductAdapter { product ->
            moveToProductDetail(product)
        }
        categoryProductAdapter = ProductAdapter { product ->
            moveToProductDetail(product)
        }
        goalMealAdapter = GoalMealAdapter { product ->
            moveToProductDetail(product)
        }

        binding.rvPopularProduct.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
            )
            adapter = goalMealAdapter
            setHasFixedSize(false)
        }

        binding.rvCategoryProduct.apply {
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
            adapter = categoryProductAdapter
            setHasFixedSize(true)
        }
    }

    private fun initSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
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
                filterProducts(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })
    }

    private fun initButton() {
        binding.btnGoCustom.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, CustomMealFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnCart.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, CartFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnNotification.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, NotificationFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardRecommendProduct.setOnClickListener {
            val product = todayRecommendProduct ?: return@setOnClickListener
            moveToProductDetail(product)
        }

        binding.btnPopularPrev.setOnClickListener {
            showMonthlyPopularProduct(monthlyPopularIndex - 1)
            restartMonthlyPopularAutoScroll()
        }

        binding.btnPopularNext.setOnClickListener {
            showMonthlyPopularProduct(monthlyPopularIndex + 1)
            restartMonthlyPopularAutoScroll()
        }

        binding.btnAddRecommend.setOnClickListener {
            val product = todayRecommendProduct

            if (product == null) {
                Toast.makeText(
                    requireContext(),
                    "추천 식단을 불러오는 중입니다",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val request = CartItemRequest(
                mealId = product.id.toLong(),
                quantity = 1
            )

            cartViewModel.addCartItem(request)

            Toast.makeText(
                requireContext(),
                "장바구니에 담겼습니다",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.btnCategoryAll.setOnClickListener {
            selectMealCategory(MealCategory.ALL)
        }
        binding.btnCategoryDiet.setOnClickListener {
            selectMealCategory(MealCategory.DIET)
        }
        binding.btnCategoryBulking.setOnClickListener {
            selectMealCategory(MealCategory.BULKING)
        }
        binding.btnCategoryMaintain.setOnClickListener {
            selectMealCategory(MealCategory.MAINTAIN)
        }
        binding.btnCategoryPostWorkout.setOnClickListener {
            selectMealCategory(MealCategory.POST_WORKOUT)
        }
        updateCategoryTabs(selectedMealCategory)
    }

    private fun observeCartCount() {
        cartViewModel.cartItems.observe(viewLifecycleOwner) { cartItems ->
            val totalQuantity = cartItems.sumOf { cartItem -> cartItem.quantity }
            binding.tvCartCountBadge.visibility =
                if (totalQuantity > 0) View.VISIBLE else View.GONE
            binding.tvCartCountBadge.text =
                if (totalQuantity > 99) "99+" else totalQuantity.toString()
        }
    }

    private fun observeNotificationEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            NotificationEvents.arrived.collect {
                loadUnreadNotificationCount()
            }
        }
    }

    private fun loadUnreadNotificationCount() {
        val userId = SessionManager(requireContext()).getUser()?.id
        if (userId == null) {
            binding.tvNotificationCountBadge.visibility = View.GONE
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            notificationRepository.getUnreadCount(userId)
                .onSuccess(::renderNotificationBadge)
        }
    }

    private fun renderNotificationBadge(count: Int) {
        binding.tvNotificationCountBadge.visibility =
            if (count > 0) View.VISIBLE else View.GONE
        binding.tvNotificationCountBadge.text =
            if (count > 99) "99+" else count.toString()
    }

    private fun loadPopularMeals() {
        val loginUser = SessionManager(requireContext()).getUser()

        if (loginUser == null) {
            binding.tvPopularTitle.text = "다른 사용자들이 많이 주문한 식단"
            loadGlobalPopularMeals()
        } else {
            binding.tvPopularTitle.text = "나와 같은 목표의 사용자들이 많이 주문한 식단"
            loadPopularMealsByPurpose(loginUser.id)
        }
    }

    private fun loadGlobalPopularMeals() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = mealRepository.getPopularMealsCached()
            if (_binding == null) return@launch

            result.onSuccess { products ->
                if (products.isEmpty()) {
                    loadProductsFromDb()
                    return@onSuccess
                }

                submitGoalMeals(products)

            }.onFailure { exception ->
                Toast.makeText(
                    requireContext(),
                    exception.message ?: "인기 식단을 불러오지 못했습니다",
                    Toast.LENGTH_SHORT
                ).show()

                loadProductsFromDb()
            }
        }
    }

    private fun loadPopularMealsByPurpose(userId: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = mealRepository.getPopularMealsBySamePurposeCached(userId)
            if (_binding == null) return@launch

            result.onSuccess { products ->
                if (products.isEmpty()) {
                    loadProductsFromDb()
                    return@onSuccess
                }

                submitGoalMeals(products)

            }.onFailure { exception ->
                Toast.makeText(
                    requireContext(),
                    exception.message ?: "같은 목표 인기 식단을 불러오지 못했습니다",
                    Toast.LENGTH_SHORT
                ).show()

                loadProductsFromDb()
            }
        }
    }

    private fun loadProductsFromDb() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = mealRepository.getProductMealsCached()
            if (_binding == null) return@launch

            result.onSuccess { products ->
                submitGoalMeals(products)
            }.onFailure { exception ->
                Toast.makeText(
                    requireContext(),
                    exception.message ?: "완제품 식단을 불러오지 못했습니다",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadTodayRecommendedProduct() {
        viewLifecycleOwner.lifecycleScope.launch {
            val result = mealRepository.getTodayRecommendedProductCached()
            if (_binding == null) return@launch

            result.onSuccess { product ->
                todayRecommendProduct = product
                bindRecommendProduct(product)
            }.onFailure { exception ->
                Toast.makeText(
                    requireContext(),
                    exception.message ?: "추천 식단을 불러오지 못했습니다",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun bindRecommendProduct(product: Product) {
        Glide.with(binding.ivRecommendProduct.context)
            .load(ImageUrlHelper.getFullImageUrl(product.imageUrl))
            .placeholder(product.imageRes)
            .error(product.imageRes)
            .transition(DrawableTransitionOptions.withCrossFade(350))
            .into(binding.ivRecommendProduct)

        binding.tvRecommendName.text = product.name

        binding.tvRecommendNutrition.text =
            "${formatNutrition(product.calories)}kcal · 탄수화물 ${formatNutrition(product.carbohydrate)}g · 단백질 ${formatNutrition(product.protein)}g · 지방 ${formatNutrition(product.fat)}g"

        binding.tvRecommendPrice.text =
            "${DisplayFormatter.formatPrice(product.price)}원"
    }

    private fun filterProducts(keyword: String) {
        val trimmedKeyword = keyword.trim()
        val isSearchMode = trimmedKeyword.isNotEmpty()

        val filteredList = if (!isSearchMode) {
            emptyList()
        } else {
            productList.filter { product ->
                product.name.contains(trimmedKeyword, ignoreCase = true)
            }
        }

        updateSearchMode(isSearchMode)

        if (isSearchMode) {
            productAdapter.submitList(filteredList)
        } else {
            productAdapter.submitList(emptyList())
            loadMonthlyPopularMeals()
            loadPopularMeals()
        }

        binding.layoutSearchEmpty.visibility =
            if (isSearchMode && filteredList.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        binding.rvPopularProduct.visibility =
            if (binding.layoutSearchEmpty.visibility == View.VISIBLE) View.GONE else View.VISIBLE
    }

    private fun updateSearchMode(isSearchMode: Boolean) {
        binding.layoutCustomMealCta.visibility = if (isSearchMode) View.GONE else View.VISIBLE
        binding.tvAiRecommendTitle.visibility = if (isSearchMode) View.GONE else View.VISIBLE
        binding.tvAiRecommendDescription.visibility = if (isSearchMode) View.GONE else View.VISIBLE
        binding.cardRecommendProduct.visibility = if (isSearchMode) View.GONE else View.VISIBLE
        binding.layoutMealCategories.visibility = if (isSearchMode) View.GONE else View.VISIBLE
        binding.tvPopularTitle.visibility = if (isSearchMode) View.GONE else View.VISIBLE
        binding.tvSearchResultTitle.visibility = if (isSearchMode) View.VISIBLE else View.GONE

        binding.rvPopularProduct.adapter = if (isSearchMode) productAdapter else goalMealAdapter
        productAdapter.setGridMode(isSearchMode)
        binding.rvPopularProduct.layoutManager = if (isSearchMode) {
            GridLayoutManager(requireContext(), 2)
        } else {
            LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.VERTICAL,
                false
            )
        }

        binding.rvPopularProduct.layoutParams =
            binding.rvPopularProduct.layoutParams.apply {
                height = if (isSearchMode) {
                    ViewGroup.LayoutParams.WRAP_CONTENT
                } else {
                    (282 * resources.displayMetrics.density).toInt()
                }
            }
    }

    private fun submitGoalMeals(products: List<Product>) {
        val rankedProducts = products.take(GOAL_MEAL_RANK_LIMIT)
        goalMealAdapter.submitList(rankedProducts)
        binding.rvPopularProduct.visibility =
            if (rankedProducts.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun selectMealCategory(category: MealCategory) {
        selectedMealCategory = category
        updateCategoryTabs(category)
        renderCategoryProducts()
    }

    private fun renderCategoryProducts() {
        if (_binding == null) return

        val filteredProducts = allProductMeals.filter(selectedMealCategory::matches)
        categoryProductAdapter.submitList(filteredProducts)
        binding.rvCategoryProduct.visibility =
            if (filteredProducts.isEmpty()) View.GONE else View.VISIBLE
        binding.tvCategoryEmpty.visibility =
            if (filteredProducts.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun updateCategoryTabs(selectedCategory: MealCategory) {
        setCategoryTabSelected(binding.btnCategoryAll, selectedCategory == MealCategory.ALL)
        setCategoryTabSelected(binding.btnCategoryDiet, selectedCategory == MealCategory.DIET)
        setCategoryTabSelected(binding.btnCategoryBulking, selectedCategory == MealCategory.BULKING)
        setCategoryTabSelected(binding.btnCategoryMaintain, selectedCategory == MealCategory.MAINTAIN)
        setCategoryTabSelected(
            binding.btnCategoryPostWorkout,
            selectedCategory == MealCategory.POST_WORKOUT
        )
    }

    private fun setCategoryTabSelected(tab: TextView, selected: Boolean) {
        tab.setBackgroundResource(
            if (selected) {
                R.drawable.bg_home_category_selected
            } else {
                R.drawable.bg_home_category_plain
            }
        )
    }

    private fun loadAllProductsForSearch() {
        viewLifecycleOwner.lifecycleScope.launch {
            mealRepository.getProductMealsCached()
                .onSuccess { products ->
                    if (_binding == null) return@onSuccess

                    productList = products
                    allProductMeals = products
                    renderCategoryProducts()
                    if (binding.etSearch.text?.isNotBlank() == true) {
                        filterProducts(binding.etSearch.text.toString())
                    }
                }
        }
    }

    private fun loadMonthlyPopularMeals() {
        if (binding.etSearch.text?.isNotBlank() == true) return

        binding.tvAiRecommendTitle.text = "이번 달 가장 인기 있는 식단"

        viewLifecycleOwner.lifecycleScope.launch {
            val result = mealRepository.getMonthlyPopularMeals()
            if (_binding == null) return@launch

            result
                .onSuccess { products ->
                    monthlyPopularProducts = products
                    binding.cardRecommendProduct.visibility =
                        if (products.isEmpty()) View.GONE else View.VISIBLE
                    binding.tvAiRecommendTitle.text =
                        if (products.isEmpty()) "이번 달 주문 데이터가 아직 없어요" else "이번 달 가장 인기 있는 식단"
                    showMonthlyPopularProduct(0)
                    restartMonthlyPopularAutoScroll()
                }
                .onFailure {
                    monthlyPopularProducts = emptyList()
                    todayRecommendProduct = null
                    binding.cardRecommendProduct.visibility = View.GONE
                    binding.tvAiRecommendTitle.text = "이번 달 인기 식단을 불러오지 못했어요"
                    stopMonthlyPopularAutoScroll()
                }
        }
    }

    private fun showMonthlyPopularProduct(index: Int) {
        if (_binding == null || monthlyPopularProducts.isEmpty()) return

        monthlyPopularIndex =
            (index % monthlyPopularProducts.size + monthlyPopularProducts.size) % monthlyPopularProducts.size
        val product = monthlyPopularProducts[monthlyPopularIndex]
        todayRecommendProduct = product
        bindRecommendProduct(product)
    }

    private fun restartMonthlyPopularAutoScroll() {
        stopMonthlyPopularAutoScroll()
        if (monthlyPopularProducts.size > 1) {
            carouselHandler.postDelayed(
                monthlyPopularCarouselRunnable,
                POPULAR_CAROUSEL_INTERVAL_MS
            )
        }
    }

    private fun stopMonthlyPopularAutoScroll() {
        carouselHandler.removeCallbacks(monthlyPopularCarouselRunnable)
    }

    private fun moveToProductDetail(product: Product) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, ProductDetailFragment.newInstance(product))
            .addToBackStack(null)
            .commit()
    }

    private fun formatNutrition(value: Double): String {
        return DisplayFormatter.formatNutrition(value)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopMonthlyPopularAutoScroll()
        _binding = null
    }

    private fun loadMainRecommendProduct() {
        val loginUser = SessionManager(requireContext()).getUser()

        if (loginUser == null) {
            binding.tvAiRecommendTitle.text = "AI가 분석한 맞춤 조합"
            loadTodayRecommendedProduct()
            return
        }

        binding.tvAiRecommendTitle.text = "나와 같은 목표의 사용자들이 많이 주문한 식단"

        viewLifecycleOwner.lifecycleScope.launch {
            val result = mealRepository.getPopularMealsBySamePurposeCached(loginUser.id)
            if (_binding == null) return@launch

            result.onSuccess { products ->
                if (products.isEmpty()) {
                    binding.tvAiRecommendTitle.text = "AI가 분석한 맞춤 조합"
                    loadTodayRecommendedProduct()
                    return@onSuccess
                }

                val product = products.first()
                todayRecommendProduct = product
                bindRecommendProduct(product)

            }.onFailure {
                binding.tvAiRecommendTitle.text = "AI가 분석한 맞춤 조합"
                loadTodayRecommendedProduct()
            }
        }
    }

    companion object {
        private const val POPULAR_CAROUSEL_INTERVAL_MS = 5_000L
        private const val GOAL_MEAL_RANK_LIMIT = 3
    }
}
