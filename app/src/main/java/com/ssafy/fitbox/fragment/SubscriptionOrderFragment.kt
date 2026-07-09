package com.ssafy.fitbox.fragment

import android.app.DatePickerDialog
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ssafy.fitbox.R
import com.ssafy.fitbox.adapter.ProductAdapter
import com.ssafy.fitbox.adapter.SubscriptionPlanItemAdapter
import com.ssafy.fitbox.databinding.FragmentSubscriptionOrderBinding
import com.ssafy.fitbox.dto.Product
import com.ssafy.fitbox.dto.MealCategory
import com.ssafy.fitbox.dto.SubscriptionPlanItem
import com.ssafy.fitbox.network.request.SubscriptionCreateRequest
import com.ssafy.fitbox.network.request.CustomMealCreateRequest
import com.ssafy.fitbox.network.request.CustomMealIngredientRequest
import com.ssafy.fitbox.network.request.SubscriptionTemplateRequest
import com.ssafy.fitbox.repository.IngredientRepository
import com.ssafy.fitbox.repository.MealRepository
import com.ssafy.fitbox.util.DisplayFormatter
import com.ssafy.fitbox.util.FavoriteMealStore
import com.ssafy.fitbox.util.LoginRequiredDialog
import com.ssafy.fitbox.util.SessionManager
import com.ssafy.fitbox.viewmodel.MealViewModel
import com.ssafy.fitbox.viewmodel.OrderViewModel
import com.ssafy.fitbox.viewmodel.AddressViewModel
import com.ssafy.fitbox.dto.FavoriteMeal
import com.ssafy.fitbox.network.response.SubscriptionResponse
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.ssafy.fitbox.util.AddressParts
import com.ssafy.fitbox.util.DeliveryAddressFormManager
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.ssafy.fitbox.dto.Store
import com.ssafy.fitbox.repository.StoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SubscriptionOrderFragment : Fragment() {
    private var _binding: FragmentSubscriptionOrderBinding? = null
    private val binding get() = _binding!!
    private val orderViewModel: OrderViewModel by viewModels()
    private val mealViewModel: MealViewModel by viewModels()
    private val addressViewModel: AddressViewModel by viewModels()
    private val mealRepository = MealRepository()
    private val ingredientRepository = IngredientRepository()
    private var resubscribeSource: SubscriptionResponse? = null
    private lateinit var productAdapter: ProductAdapter
    private lateinit var planItemAdapter: SubscriptionPlanItemAdapter
    private var allProducts: List<Product> = emptyList()
    private var selectedProduct: Product? = null
    private var favoriteProductSourceByTempId: Map<Int, FavoriteMeal> = emptyMap()
    private val subscriptionPlanItems = mutableListOf<SubscriptionPlanItem>()
    private var selectedQuantity: Int = 1
    private var selectedSubscriptionStartDate: String = ""
    private var updatingWeekChecks: Boolean = false
    private var selectedReceiveType: String = RECEIVE_TYPE_DELIVERY
    private var currentSubscriptionStep: Int = STEP_START_DATE
    private lateinit var deliveryAddressFormManager: DeliveryAddressFormManager
    private var pendingDeliveryAddress: String? = null
    private val storeRepository = StoreRepository()

    private var selectedDeliveryStore: Store? = null
    private var deliveryAddressLatLng: LatLng? = null
    private var deliveryCandidateStores: List<Store> = emptyList()

    private var cachedDeliveryRoadAddress: String = ""
    private var cachedDeliveryDetailAddress: String = ""
    private var cachedDeliveryLatLng: LatLng? = null
    private var isReturningFromDeliveryStoreMap: Boolean = false
    private var isRestoringDeliveryAddress: Boolean = false


    private val weekItems = listOf(
        WeekOption(1, "1주차"),
        WeekOption(2, "2주차"),
        WeekOption(3, "3주차"),
        WeekOption(4, "4주차"),
        WeekOption(5, "5주차")
    )

    private val dayItems = listOf(
        DayOption(1, "일요일"),
        DayOption(2, "월요일"),
        DayOption(3, "화요일"),
        DayOption(4, "수요일"),
        DayOption(5, "목요일"),
        DayOption(6, "금요일"),
        DayOption(7, "토요일")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscriptionOrderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resubscribeSource = arguments?.getString(ARG_RESUBSCRIBE_SOURCE)
            ?.let {
                runCatching {
                    Gson().fromJson(
                        it,
                        SubscriptionResponse::class.java
                    )
                }.getOrNull()
            }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        if (!SessionManager(requireContext()).isLoggedIn()) {
            LoginRequiredDialog.show(this)
            parentFragmentManager.popBackStack()
            return
        }

        deliveryAddressFormManager = DeliveryAddressFormManager(
            fragment = this,
            spinner = binding.spinnerSavedAddress,
            roadAddressInput = binding.etAddress,
            detailAddressInput = binding.etAddressDetail,
            onRoadAddressChanged = {
                if (isRestoringDeliveryAddress) {
                    return@DeliveryAddressFormManager
                }

                if (selectedReceiveType == RECEIVE_TYPE_DELIVERY) {
                    selectedDeliveryStore = null
                    deliveryAddressLatLng = null
                    bindSelectedDeliveryStore()
                    autoSelectNearestDeliveryStore()
                }
            }
        )

        deliveryAddressFormManager.setupSpinner()

        initProductRecyclerView()
        initPlanRecyclerView()
        initScheduleSelectors()
        initStoreResultListener()
        initAddressSearchResultListener()
        initCustomMealResultListener()
        observeAddresses()
        initView()
        initClickEvents()
        applyResubscribeSource()
        observeMealData()
        observeOrderResult()

        mealViewModel.loadProductMeals()
        loadSavedAddresses()
    }

    private fun observeAddresses() {
        addressViewModel.addresses.observe(viewLifecycleOwner) { addresses ->
            deliveryAddressFormManager.submitSavedAddresses(
                addresses = addresses,
                applyLatestSavedAddress = !isReturningFromDeliveryStoreMap &&
                        cachedDeliveryRoadAddress.isBlank()
            )

            pendingDeliveryAddress?.let { address ->
                deliveryAddressFormManager.applyPendingAddress(address)
                pendingDeliveryAddress = null
            }

            if (isReturningFromDeliveryStoreMap) {
                binding.root.post {
                    if (_binding == null) return@post
                    restoreCachedDeliveryAddressIfNeeded()
                }

                binding.root.postDelayed({
                    if (_binding == null) return@postDelayed
                    restoreCachedDeliveryAddressIfNeeded()
                    isReturningFromDeliveryStoreMap = false
                }, 100L)
            }
        }

        addressViewModel.message.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }
    }
    private fun loadSavedAddresses() {
        SessionManager(requireContext()).getUser()?.id?.let(addressViewModel::loadAddresses)
    }

    private fun initProductRecyclerView() {
        productAdapter = ProductAdapter { product ->
            selectedProduct = product
            updateSelectedMenuText()
        }

        binding.rvProductMeals.apply {
            adapter = productAdapter
            layoutManager = LinearLayoutManager(
                requireContext(),
                LinearLayoutManager.HORIZONTAL,
                false
            )
        }
    }

    private fun initPlanRecyclerView() {
        planItemAdapter = SubscriptionPlanItemAdapter { item ->
            subscriptionPlanItems.remove(item)
            planItemAdapter.submitList(subscriptionPlanItems.toList())
            clearSelectedStore()
            updatePlanSummary()
            updatePlanCalendar()
        }

        binding.rvSubscriptionPlan.apply {
            adapter = planItemAdapter
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
            visibility = View.GONE
        }
    }

    private fun initScheduleSelectors() {
        binding.checkWeekAll.text = "전체"
        binding.checkWeek1.text = "1주차"
        binding.checkWeek2.text = "2주차"
        binding.checkWeek3.text = "3주차"
        binding.checkWeek4.text = "4주차"
        binding.checkWeek5.text = "5주차"

        binding.checkDayMon.text = "월"
        binding.checkDayTue.text = "화"
        binding.checkDayWed.text = "수"
        binding.checkDayThu.text = "목"
        binding.checkDayFri.text = "금"
        binding.checkDaySat.text = "토"
        binding.checkDaySun.text = "일"

        binding.checkWeekAll.setOnCheckedChangeListener { _, isChecked ->
            if (updatingWeekChecks) return@setOnCheckedChangeListener

            updatingWeekChecks = true
            weekCheckBoxes().forEach { it.isChecked = isChecked }
            updatingWeekChecks = false
        }

        weekCheckBoxes().forEach { checkBox ->
            checkBox.setOnCheckedChangeListener { _, _ ->
                if (updatingWeekChecks) return@setOnCheckedChangeListener

                updatingWeekChecks = true
                binding.checkWeekAll.isChecked = weekCheckBoxes().all { it.isChecked }
                updatingWeekChecks = false
            }
        }
    }

    private fun initStoreResultListener() {
        parentFragmentManager.setFragmentResultListener(
            StoreMapFragment.REQUEST_KEY_STORE,
            viewLifecycleOwner
        ) { _, bundle ->
            val selectedStoreId = bundle.getLong(StoreMapFragment.KEY_STORE_ID)
            val selectedStoreName = bundle.getString(StoreMapFragment.KEY_STORE_NAME).orEmpty()
            val selectedStoreAddress = bundle.getString(StoreMapFragment.KEY_STORE_ADDRESS).orEmpty()
            val selectedStoreTotalCount = bundle.getInt(StoreMapFragment.KEY_STORE_TOTAL_COUNT)
            val selectedStoreRemainCount = bundle.getInt(StoreMapFragment.KEY_STORE_REMAIN_COUNT)

            val selectedStoreLatitude =
                bundle.getDouble(StoreMapFragment.KEY_STORE_LATITUDE, 0.0)
            val selectedStoreLongitude =
                bundle.getDouble(StoreMapFragment.KEY_STORE_LONGITUDE, 0.0)

            val receiveTypeFromMap = bundle
                .getString(StoreMapFragment.KEY_RECEIVE_TYPE)
                ?: RECEIVE_TYPE_DELIVERY

            if (receiveTypeFromMap == RECEIVE_TYPE_DELIVERY) {
                selectedReceiveType = RECEIVE_TYPE_DELIVERY

                restoreCachedDeliveryAddressIfNeeded()

                selectedDeliveryStore = Store(
                    id = selectedStoreId,
                    name = selectedStoreName,
                    address = selectedStoreAddress,
                    longitude = selectedStoreLongitude,
                    latitude = selectedStoreLatitude,
                    totalCnt = selectedStoreTotalCount,
                    remainCnt = selectedStoreRemainCount
                )

                binding.cardDeliveryStore.visibility = View.VISIBLE

                binding.root.post {
                    restoreCachedDeliveryAddressIfNeeded()
                    bindSelectedDeliveryStore()
                }

                bindSelectedDeliveryStore()
                updateSubscriptionStepUi()
                return@setFragmentResultListener
            }
        }
    }

    private fun initAddressSearchResultListener() {
        parentFragmentManager.setFragmentResultListener(
            AddressSearchFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val zoneCode = bundle.getString(AddressSearchFragment.KEY_ZONE_CODE).orEmpty()
            val address = bundle.getString(AddressSearchFragment.KEY_ADDRESS).orEmpty()

            deliveryAddressFormManager.applyAddressSearchResult(
                zoneCode = zoneCode,
                address = address
            )
        }
    }

    private fun initCustomMealResultListener() {
        parentFragmentManager.setFragmentResultListener(
            CustomMealFragment.REQUEST_KEY_CUSTOM_MEAL,
            viewLifecycleOwner
        ) { _, bundle ->
            val mealId = bundle.getLong(CustomMealFragment.KEY_CUSTOM_MEAL_ID)
            val name = bundle.getString(CustomMealFragment.KEY_CUSTOM_MEAL_NAME)
                ?: "커스텀 식단"
            val price = bundle.getInt(CustomMealFragment.KEY_CUSTOM_MEAL_PRICE)
            val calories = bundle.getInt(CustomMealFragment.KEY_CUSTOM_MEAL_CALORIES)
            val carbohydrate = bundle.getDouble(CustomMealFragment.KEY_CUSTOM_MEAL_CARBOHYDRATE)
            val protein = bundle.getDouble(CustomMealFragment.KEY_CUSTOM_MEAL_PROTEIN)
            val fat = bundle.getDouble(CustomMealFragment.KEY_CUSTOM_MEAL_FAT)

            selectedProduct = Product(
                id = mealId.toInt(),
                name = name,
                imageRes = R.drawable.logo_full_background_remove,
                imageUrl = null,
                calories = calories.toDouble(),
                carbohydrate = carbohydrate,
                protein = protein,
                fat = fat,
                price = price,
                description = "사용자가 직접 구성한 커스텀 식단입니다."
            )

            updateSelectedMenuText()

            Toast.makeText(
                requireContext(),
                "커스텀 식단이 구독 메뉴로 선택되었습니다.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun initView() {
        selectedReceiveType = RECEIVE_TYPE_DELIVERY
        binding.layoutAddress.visibility = View.VISIBLE

        selectedSubscriptionStartDate = getTomorrowString()
        updateSubscriptionStartDateText()

        updateSelectedMenuText()
        updateQuantityText()
        updatePlanSummary()
        clearSelectedStore()
        updateSubscriptionStepUi()
    }

    private fun applyResubscribeSource() {
        val source = resubscribeSource ?: return
        binding.tvSubscriptionTitle.text = "다시 구독하기"
        binding.btnSubscriptionOrder.text = "새 월 정기구독 시작하기"

        selectedSubscriptionStartDate = getTomorrowString()
        updateSubscriptionStartDateText()

        subscriptionPlanItems.clear()
        source.templates.orEmpty().forEach { template ->
            subscriptionPlanItems.add(
                SubscriptionPlanItem(
                    weekOfMonth = template.weekOfMonth,
                    dayOfWeek = template.dayOfWeek,
                    dayOfWeekText = template.dayOfWeekText,
                    product = Product(
                        id = template.mealId.toInt(),
                        name = template.mealName,
                        imageRes = R.drawable.logo_full_background_remove,
                        imageUrl = null,
                        calories = 0.0,
                        carbohydrate = 0.0,
                        protein = 0.0,
                        fat = 0.0,
                        price = template.mealPrice,
                        description = "기존 구독에서 불러온 식단입니다."
                    ),
                    quantity = template.quantity
                )
            )
        }
        subscriptionPlanItems.sortWith(
            compareBy<SubscriptionPlanItem> { it.weekOfMonth }
                .thenBy { it.dayOfWeek }
                .thenBy { it.product.name }
        )
        planItemAdapter.submitList(subscriptionPlanItems.toList())
        updatePlanSummary()
        updatePlanCalendar()

        selectedProduct = subscriptionPlanItems.firstOrNull()?.product
        selectedQuantity = subscriptionPlanItems.firstOrNull()?.quantity ?: 1
        updateSelectedMenuText()
        updateQuantityText()
        applyTemplateChecks()

        selectedReceiveType = RECEIVE_TYPE_DELIVERY
        pendingDeliveryAddress = source.address
        deliveryAddressFormManager.applySavedAddress(source.address.orEmpty())
        clearSelectedStore()
    }

    private fun applyTemplateChecks() {
        val weeks = subscriptionPlanItems.map { it.weekOfMonth }.toSet()
        val days = subscriptionPlanItems.map { it.dayOfWeek }.toSet()

        updatingWeekChecks = true
        weekCheckBoxes().forEachIndexed { index, checkBox ->
            checkBox.isChecked = index + 1 in weeks
        }
        binding.checkWeekAll.isChecked = weeks.size == weekItems.size
        updatingWeekChecks = false

        binding.checkDaySun.isChecked = Calendar.SUNDAY in days
        binding.checkDayMon.isChecked = Calendar.MONDAY in days
        binding.checkDayTue.isChecked = Calendar.TUESDAY in days
        binding.checkDayWed.isChecked = Calendar.WEDNESDAY in days
        binding.checkDayThu.isChecked = Calendar.THURSDAY in days
        binding.checkDayFri.isChecked = Calendar.FRIDAY in days
        binding.checkDaySat.isChecked = Calendar.SATURDAY in days
    }

    private fun initClickEvents() {
        binding.btnChangeDeliveryStore.setOnClickListener {
            openDeliveryStoreMap()
        }
        binding.btnFilterAll.setOnClickListener {
            applyProductFilter(ProductFilter.ALL)
        }

        binding.btnFilterFavorites.setOnClickListener {
            applyProductFilter(ProductFilter.FAVORITES)
        }

        binding.btnFilterDiet.setOnClickListener {
            applyProductFilter(ProductFilter.DIET)
        }

        binding.btnFilterBulking.setOnClickListener {
            applyProductFilter(ProductFilter.BULKING)
        }

        binding.btnFilterSalad.setOnClickListener {
            applyProductFilter(ProductFilter.SALAD)
        }

        binding.btnFilterSimple.setOnClickListener {
            applyProductFilter(ProductFilter.SIMPLE)
        }

        binding.btnCreateCustomMenu.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.main_container,
                    CustomMealFragment.newInstance(CustomMealFragment.MODE_SUBSCRIPTION)
                )
                .addToBackStack(null)
                .commit()
        }

        binding.btnSelectSubscriptionStartDate.setOnClickListener {
            showDatePicker { selectedDate ->
                selectedSubscriptionStartDate = selectedDate
                clearSelectedStore()
                updateSubscriptionStartDateText()
            }
        }

        binding.btnSearchDeliveryAddress.setOnClickListener {
            openAddressSearch()
        }

        binding.btnSubscriptionPrevious.setOnClickListener {
            if (currentSubscriptionStep > STEP_START_DATE) {
                currentSubscriptionStep--
                updateSubscriptionStepUi()
            }
        }

        binding.btnSubscriptionNext.setOnClickListener {
            when (currentSubscriptionStep) {
                STEP_START_DATE -> {
                    if (selectedSubscriptionStartDate.isBlank()) {
                        Toast.makeText(
                            requireContext(),
                            "구독 시작일을 선택해주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                }

                STEP_MEAL_PLAN -> {
                    if (subscriptionPlanItems.isEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            "구독 식단표를 하나 이상 추가해주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                }
            }

            if (currentSubscriptionStep < STEP_RECEIVE) {
                currentSubscriptionStep++
                updateSubscriptionStepUi()
            }
        }

        binding.btnDecreaseQuantity.setOnClickListener {
            if (selectedQuantity > 1) {
                selectedQuantity--
                updateQuantityText()
            }
        }

        binding.btnIncreaseQuantity.setOnClickListener {
            selectedQuantity++
            updateQuantityText()
        }

        binding.btnAddPlanItem.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                addPlanItem()
            }
        }

        binding.btnSubscriptionOrder.setOnClickListener {
            createMonthlySubscription()
        }
    }

    private fun observeMealData() {
        mealViewModel.productMeals.observe(viewLifecycleOwner) { products ->
            allProducts = products
            productAdapter.submitList(products)
        }

        mealViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(
                requireContext(),
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun observeOrderResult() {
        orderViewModel.monthlySubscriptionResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { response ->
                viewLifecycleOwner.lifecycleScope.launch {
                    getUserId()?.let { userId ->
                        deliveryAddressFormManager.savePendingDeliveryAddressIfNeeded(userId)
                    }

                    Toast.makeText(
                        requireContext(),
                        "매월 반복되는 정기구독이 시작되었습니다.",
                        Toast.LENGTH_SHORT
                    ).show()

                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.main_container, OrderListFragment())
                        .commit()
                }
            }

            result.onFailure { throwable ->
                deliveryAddressFormManager.clearPendingDeliveryAddressSave()

                Toast.makeText(
                    requireContext(),
                    throwable.message ?: "정기 구독 생성에 실패했습니다.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private suspend fun resolveProductForPlan(product: Product?): Product? {
        if (product == null || product.id > 0) {
            return product
        }

        val favorite = favoriteProductSourceByTempId[product.id] ?: return product
        if (favorite.mealId != null) {
            return product.copy(id = favorite.mealId.toInt())
        }

        val ingredientResult = ingredientRepository.getIngredients()
        val ingredients = ingredientResult.getOrElse {
            Toast.makeText(requireContext(), it.message ?: "재료 정보를 불러오지 못했습니다.", Toast.LENGTH_SHORT)
                .show()
            return null
        }

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

        if (requests.size != favorite.ingredients.size || requests.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "즐겨찾기 식단을 구독용 식단으로 만들 수 없습니다.",
                Toast.LENGTH_SHORT
            ).show()
            return null
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

        return mealRepository.createCustomMeal(request)
            .map {
                product.copy(id = it.mealId.toInt(), mealType = "CUSTOM")
            }
            .onFailure {
                Toast.makeText(
                    requireContext(),
                    it.message ?: "즐겨찾기 식단 생성에 실패했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .getOrNull()
    }

    private suspend fun addPlanItem() {
        val product = resolveProductForPlan(selectedProduct)

        if (product == null) {
            Toast.makeText(
                requireContext(),
                "구독 식단표에 추가할 메뉴를 선택해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val selectedWeeks = getSelectedWeeks()
        val selectedDays = getSelectedDays()

        if (selectedWeeks.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "주차를 하나 이상 선택해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (selectedDays.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "요일을 하나 이상 선택해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        selectedWeeks.forEach { week ->
            selectedDays.forEach { day ->
                val sameItemIndex = subscriptionPlanItems.indexOfFirst { item ->
                    item.weekOfMonth == week.value &&
                            item.dayOfWeek == day.value &&
                            item.product.id == product.id
                }

                if (sameItemIndex >= 0) {
                    val oldItem = subscriptionPlanItems[sameItemIndex]
                    subscriptionPlanItems[sameItemIndex] = oldItem.copy(
                        quantity = oldItem.quantity + selectedQuantity
                    )
                } else {
                    subscriptionPlanItems.add(
                        SubscriptionPlanItem(
                            weekOfMonth = week.value,
                            dayOfWeek = day.value,
                            dayOfWeekText = day.label,
                            product = product,
                            quantity = selectedQuantity
                        )
                    )
                }
            }
        }

        subscriptionPlanItems.sortWith(
            compareBy<SubscriptionPlanItem> { it.weekOfMonth }
                .thenBy { it.dayOfWeek }
                .thenBy { it.product.name }
        )

        clearSelectedStore()
        planItemAdapter.submitList(subscriptionPlanItems.toList())
        updatePlanSummary()
        updatePlanCalendar()

        Toast.makeText(
            requireContext(),
            "매월 반복할 ${selectedWeeks.size * selectedDays.size}개 일정이 추가되었습니다.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun autoSelectNearestDeliveryStore() {
        val roadAddress = binding.etAddress.text.toString().trim()

        if (roadAddress.isBlank()) {
            clearDeliveryStore()
            return
        }

        val geocodingAddress = AddressParts.parse(roadAddress).roadAddress.ifBlank {
            roadAddress
        }

        val targetDate = selectedSubscriptionStartDate.ifBlank {
            getTomorrowString()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val latLng = geocodeAddress(geocodingAddress)

            if (latLng == null) {
                clearDeliveryStore()
                binding.btnChangeDeliveryStore.isEnabled = false
                binding.tvDeliveryStoreName.text = "배송지 위치를 찾을 수 없습니다."
                binding.tvDeliveryStoreAddress.text = ""
                binding.tvDeliveryStoreDistance.text = "도로명 주소를 다시 확인해주세요."

                Toast.makeText(
                    requireContext(),
                    "배송지 위치를 찾을 수 없습니다.",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            deliveryAddressLatLng = latLng

            val stores = storeRepository.getStores(targetDate)
                .getOrElse {
                    clearDeliveryStore()
                    binding.btnChangeDeliveryStore.isEnabled = false
                    binding.tvDeliveryStoreName.text = "담당 매장을 불러오지 못했습니다."
                    binding.tvDeliveryStoreAddress.text = ""
                    binding.tvDeliveryStoreDistance.text =
                        it.message ?: "매장 목록을 불러오지 못했습니다."

                    Toast.makeText(
                        requireContext(),
                        it.message ?: "매장 목록을 불러오지 못했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

            val availableStores = stores
                .map { store ->
                    store to distanceMeter(
                        from = latLng,
                        to = LatLng(store.latitude, store.longitude)
                    )
                }
                .filter { (_, distance) ->
                    distance <= DELIVERY_RADIUS_METER
                }
                .sortedBy { (_, distance) ->
                    distance
                }

            deliveryCandidateStores = availableStores.map { it.first }

            val nearestStore = availableStores.firstOrNull()?.first

            if (nearestStore == null) {
                selectedDeliveryStore = null
                binding.btnChangeDeliveryStore.isEnabled = false
                binding.tvDeliveryStoreName.text = "주문 가능한 담당 매장이 없습니다."
                binding.tvDeliveryStoreAddress.text = ""
                binding.tvDeliveryStoreDistance.text =
                    "선택한 배송지 3km 이내에 주문 가능한 매장이 없습니다."
                return@launch
            }

            binding.btnChangeDeliveryStore.isEnabled = true
            selectedDeliveryStore = nearestStore
            bindSelectedDeliveryStore()
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun geocodeAddress(address: String): LatLng? {
        val appContext = requireContext().applicationContext
        return withContext(Dispatchers.IO) {
            runCatching {
                val geocoder = Geocoder(appContext, Locale.KOREA)
                val results = geocoder.getFromLocationName(address, 1)
                val location = results?.firstOrNull() ?: return@withContext null
                LatLng(location.latitude, location.longitude)
            }.getOrNull()
        }
    }

    private fun openDeliveryStoreMap() {
        if (!binding.btnChangeDeliveryStore.isEnabled) {
            Toast.makeText(
                requireContext(),
                "선택한 배송지 3km 이내에 주문 가능한 매장이 없습니다.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        cacheCurrentDeliveryAddress()
        isReturningFromDeliveryStoreMap = true

        val latLng = deliveryAddressLatLng

        if (latLng == null) {
            Toast.makeText(
                requireContext(),
                "배송지를 먼저 선택해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            autoSelectNearestDeliveryStore()
            return
        }

        parentFragmentManager.beginTransaction()
            .replace(
                R.id.main_container,
                StoreMapFragment.newDeliveryInstance(
                    deliveryDate = selectedSubscriptionStartDate.ifBlank {
                        getTomorrowString()
                    },
                    deliveryLatitude = latLng.latitude,
                    deliveryLongitude = latLng.longitude
                )
            )
            .addToBackStack(null)
            .commit()
    }

    private fun cacheCurrentDeliveryAddress() {
        cachedDeliveryRoadAddress = binding.etAddress.text.toString()
        cachedDeliveryDetailAddress = binding.etAddressDetail.text.toString()
        cachedDeliveryLatLng = deliveryAddressLatLng
    }

    private fun restoreCachedDeliveryAddressIfNeeded() {
        if (cachedDeliveryRoadAddress.isBlank()) return

        isRestoringDeliveryAddress = true

        binding.etAddress.setText(cachedDeliveryRoadAddress)
        binding.etAddressDetail.setText(cachedDeliveryDetailAddress)
        deliveryAddressLatLng = cachedDeliveryLatLng

        isRestoringDeliveryAddress = false
    }

    private fun bindSelectedDeliveryStore() {
        val store = selectedDeliveryStore

        if (store == null) {
            binding.tvDeliveryStoreName.text = "배송지를 선택하면 가장 가까운 매장이 자동 선택됩니다."
            binding.tvDeliveryStoreAddress.text = ""
            binding.tvDeliveryStoreDistance.text = ""
            return
        }

        val addressLocation = deliveryAddressLatLng

        val distanceText = if (
            addressLocation != null &&
            store.latitude != 0.0 &&
            store.longitude != 0.0
        ) {
            "배송지에서 ${
                formatDistanceMeter(
                    distanceMeter(
                        from = addressLocation,
                        to = LatLng(store.latitude, store.longitude)
                    )
                )
            }"
        } else {
            "선택된 담당 매장"
        }

        binding.tvDeliveryStoreName.text = store.name
        binding.tvDeliveryStoreAddress.text = store.address
        binding.tvDeliveryStoreDistance.text = distanceText
    }

    private fun clearDeliveryStore() {
        selectedDeliveryStore = null
        deliveryAddressLatLng = null
        deliveryCandidateStores = emptyList()

        if (_binding != null) {
            binding.btnChangeDeliveryStore.isEnabled = true
            bindSelectedDeliveryStore()
        }
    }

    private fun distanceMeter(
        from: LatLng,
        to: LatLng
    ): Float {
        val result = FloatArray(1)

        Location.distanceBetween(
            from.latitude,
            from.longitude,
            to.latitude,
            to.longitude,
            result
        )

        return result.first()
    }

    private fun formatDistanceMeter(meter: Float): String {
        return if (meter < 1000f) {
            "${meter.toInt()}m"
        } else {
            String.format(Locale.KOREA, "%.1fkm", meter / 1000f)
        }
    }

    private fun createMonthlySubscription() {
        if (!validateSubscriptionOrder()) return

        selectedReceiveType = RECEIVE_TYPE_DELIVERY
        val userId = getUserId() ?: return

        val address = deliveryAddressFormManager.getAddressIfDelivery(
            isDelivery = selectedReceiveType == RECEIVE_TYPE_DELIVERY
        ) ?: return

        val storeId = selectedDeliveryStore?.id

        if (storeId == null) {
            Toast.makeText(
                requireContext(),
                "배송 담당 매장을 선택해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        deliveryAddressFormManager.prepareDeliveryAddressSave(
            isDelivery = selectedReceiveType == RECEIVE_TYPE_DELIVERY,
            address = address
        )

        val templates = subscriptionPlanItems.map { item ->
            SubscriptionTemplateRequest(
                weekOfMonth = item.weekOfMonth,
                dayOfWeek = item.dayOfWeek,
                mealId = item.product.id.toLong(),
                quantity = item.quantity
            )
        }

        val request = SubscriptionCreateRequest(
            userId = userId,
            subscriptionStartDate = selectedSubscriptionStartDate,
            receiveType = selectedReceiveType,
            storeId = storeId,
            address = address,
            templates = templates
        )

        orderViewModel.createMonthlySubscription(request)
    }

    private fun applyProductFilter(filter: ProductFilter) {
        val filteredProducts = when (filter) {
            ProductFilter.ALL -> allProducts
            ProductFilter.FAVORITES -> getFavoriteProducts()

            ProductFilter.DIET -> allProducts.filter(MealCategory.DIET::matches)
            ProductFilter.BULKING -> allProducts.filter(MealCategory.BULKING::matches)
            ProductFilter.SALAD -> allProducts.filter(MealCategory.SALAD::matches)
            ProductFilter.SIMPLE -> allProducts.filter(MealCategory.SIMPLE::matches)
        }

        productAdapter.submitList(filteredProducts)
        updateFilterButtonUi(filter)

        if (filteredProducts.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "해당 목적의 식단이 없습니다.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateFilterButtonUi(selectedFilter: ProductFilter) {
        val selectedText = ContextCompat.getColor(requireContext(), R.color.mypage_fern)
        val unselectedText = ContextCompat.getColor(requireContext(), R.color.mypage_text_secondary)

        listOf(
            binding.btnFilterAll to ProductFilter.ALL,
            binding.btnFilterFavorites to ProductFilter.FAVORITES,
            binding.btnFilterDiet to ProductFilter.DIET,
            binding.btnFilterBulking to ProductFilter.BULKING,
            binding.btnFilterSalad to ProductFilter.SALAD,
            binding.btnFilterSimple to ProductFilter.SIMPLE
        ).forEach { (button, filter) ->
            val isSelected = selectedFilter == filter
            button.backgroundTintList = null
            button.setBackgroundResource(
                if (isSelected) R.drawable.bg_home_category_selected
                else R.drawable.bg_home_category_plain
            )
            button.setTextColor(if (isSelected) selectedText else unselectedText)
        }
    }

    private fun getFavoriteProducts(): List<Product> {
        val userId = getUserId() ?: return emptyList()
        FavoriteMealStore.initialize(requireContext())
        val tempSources = mutableMapOf<Int, FavoriteMeal>()
        val products = FavoriteMealStore.getFavorites(userId)
            .map { favorite ->
                val mealId = favorite.mealId
                val tempId = mealId?.toInt() ?: -kotlin.math.abs(favorite.id.hashCode())
                if (mealId == null) {
                    tempSources[tempId] = favorite
                }
                val existing = allProducts.firstOrNull { it.id.toLong() == mealId }
                existing ?: Product(
                    id = tempId,
                    name = favorite.name,
                    imageRes = R.drawable.logo_full_background_remove,
                    imageUrl = null,
                    calories = favorite.calories,
                    carbohydrate = favorite.carbohydrate,
                    protein = favorite.protein,
                    fat = favorite.fat,
                    price = favorite.price,
                    description = favorite.description
                        ?: "\uC990\uACA8\uCC3E\uAE30\uC5D0 \uC800\uC7A5\uB41C \uC2DD\uB2E8\uC785\uB2C8\uB2E4."
                )
            }
        favoriteProductSourceByTempId = tempSources
        return products
    }

    private fun validateBeforeStoreSelect(): Boolean {
        if (selectedSubscriptionStartDate.isBlank()) {
            Toast.makeText(
                requireContext(),
                "구독 시작일을 선택해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        if (subscriptionPlanItems.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "구독 식단표를 하나 이상 추가해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        return true
    }

    private fun validateSubscriptionOrder(): Boolean {
        if (!validateBeforeStoreSelect()) {
            return false
        }

        selectedReceiveType = RECEIVE_TYPE_DELIVERY

        val address = deliveryAddressFormManager.getAddressIfDelivery(
            isDelivery = true
        )

        if (address == null) {
            return false
        }

        if (selectedDeliveryStore == null) {
            Toast.makeText(
                requireContext(),
                "배송 담당 매장을 선택해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        return true
    }

    private fun showDatePicker(
        onDateSelected: (String) -> Unit
    ) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, 1)
        clearTime(calendar)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format(
                    Locale.getDefault(),
                    "%04d-%02d-%02d",
                    year,
                    month + 1,
                    dayOfMonth
                )

                onDateSelected(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.datePicker.minDate = calendar.timeInMillis
        datePickerDialog.show()
    }

    private fun openAddressSearch() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, AddressSearchFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun calculateFirstCycleReceiveDates(): List<String> {
        if (selectedSubscriptionStartDate.isBlank()) {
            return emptyList()
        }

        val startCalendar = parseDateToCalendar(selectedSubscriptionStartDate)
            ?: return emptyList()

        val endCalendar = parseDateToCalendar(selectedSubscriptionStartDate)
            ?: return emptyList()

        endCalendar.add(Calendar.MONTH, 1)
        endCalendar.add(Calendar.DAY_OF_MONTH, -1)

        val resultDates = mutableListOf<String>()

        subscriptionPlanItems.forEach { item ->
            val targetDate = findDateByRelativeWeekAndDayOfWeek(
                startCalendar = startCalendar,
                relativeWeek = item.weekOfMonth,
                dayOfWeek = item.dayOfWeek
            )

            if (!targetDate.after(endCalendar)) {
                resultDates.add(formatCalendarToDateString(targetDate))
            }
        }

        return resultDates.distinct().sorted()
    }

    private fun findDateByRelativeWeekAndDayOfWeek(
        startCalendar: Calendar,
        relativeWeek: Int,
        dayOfWeek: Int
    ): Calendar {
        val weekStart = (startCalendar.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, (relativeWeek - 1) * 7)
        }
        val daysUntilTarget =
            (dayOfWeek - weekStart.get(Calendar.DAY_OF_WEEK) + 7) % 7

        return weekStart.apply {
            add(Calendar.DAY_OF_MONTH, daysUntilTarget)
        }
    }

    private fun getWeekDayFlagsFromDates(dates: List<String>): WeekDayFlags {
        val flags = WeekDayFlags()

        dates.forEach { date ->
            val calendar = parseDateToCalendar(date) ?: return@forEach

            when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> flags.sun = true
                Calendar.MONDAY -> flags.mon = true
                Calendar.TUESDAY -> flags.tue = true
                Calendar.WEDNESDAY -> flags.wed = true
                Calendar.THURSDAY -> flags.thu = true
                Calendar.FRIDAY -> flags.fri = true
                Calendar.SATURDAY -> flags.sat = true
            }
        }

        return flags
    }

    private fun updateSubscriptionStartDateText() {
        if (selectedSubscriptionStartDate.isBlank()) {
            binding.tvSubscriptionStartDate.text =
                "월 정기구독 시작일을 선택해주세요.\n" +
                        "선택 후 시작일과 만료일이 여기에 표시됩니다."
            binding.btnSelectSubscriptionStartDate.text = "구독 시작일 선택"
        } else {
            val firstCycleEndDate = getFirstCycleEndDateText()
            binding.tvSubscriptionStartDate.text =
                "구독 시작일 설정 완료\n" +
                        "시작일: $selectedSubscriptionStartDate\n" +
                        "만료일: $firstCycleEndDate\n" +
                        "첫 달 적용 기간: $selectedSubscriptionStartDate ~ $firstCycleEndDate\n" +
                        "이후 매월 같은 주차·요일 규칙으로 자동 반복됩니다."
            binding.btnSelectSubscriptionStartDate.text = "구독 시작일 변경"
        }
    }

    private fun getFirstCycleEndDateText(): String {
        val calendar = parseDateToCalendar(selectedSubscriptionStartDate)
            ?: return "-"

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.DAY_OF_MONTH, -1)

        return formatCalendarToDateString(calendar)
    }

    private fun updateSelectedMenuText() {
        val product = selectedProduct

        binding.tvSelectedMenu.text = if (product == null) {
            "선택된 메뉴가 없습니다."
        } else {
            "선택 메뉴: ${product.name}\n" +
                    "회당 가격: ${formatPrice(product.price)}원\n" +
                    "영양: ${formatNutrition(product.calories)}kcal · " +
                    "탄수화물 ${formatNutrition(product.carbohydrate)}g · " +
                    "단백질 ${formatNutrition(product.protein)}g · " +
                    "지방 ${formatNutrition(product.fat)}g"
        }
    }

    private fun updateQuantityText() {
        binding.tvSelectedQuantity.text = "${selectedQuantity}개"
    }

    private fun updatePlanSummary() {
        updatePlanCalendar()

        if (subscriptionPlanItems.isEmpty()) {
            binding.tvPlanSummary.text =
                "\uC544\uC9C1 \uBC18\uBCF5 \uADDC\uCE59\uC774 \uC5C6\uC2B5\uB2C8\uB2E4.\n\uCD94\uAC00\uD55C \uC2DD\uB2E8\uC740 \uB9E4\uC6D4 \uAC19\uC740 \uC8FC\uCC28\uC640 \uC694\uC77C\uC5D0 \uC790\uB3D9 \uBC18\uBCF5\uB429\uB2C8\uB2E4."
            binding.tvPlanMonthlyTotal.visibility = View.GONE
            return
        }

        val firstCycleItems = getFirstCyclePlanItems()
        val totalQuantity = firstCycleItems.sumOf { it.quantity }
        val productTotalPrice = firstCycleItems.sumOf { it.product.price * it.quantity }
        val finalDeliveryFee = if (
            productTotalPrice in 1 until FREE_DELIVERY_THRESHOLD
        ) {
            DELIVERY_FEE
        } else {
            0
        }
        val finalPrice = productTotalPrice + finalDeliveryFee

        binding.tvPlanSummary.text =
            "첫 달 ${firstCycleItems.size}개 반복 일정 · 예상 총 ${totalQuantity}개\n" +
                    "\uC0C1\uD488 ${formatPrice(productTotalPrice)}\uC6D0 \u00B7 \uBC30\uC1A1\uBE44 ${
                        formatPrice(
                            finalDeliveryFee
                        )
                    }\uC6D0 \u00B7 \uC608\uC0C1 \uACB0\uC81C \uAE08\uC561 ${formatPrice(finalPrice)}\uC6D0"
        binding.tvPlanMonthlyTotal.visibility = View.VISIBLE
        binding.tvPlanMonthlyTotal.text = "월 구독 총액 ${formatPrice(finalPrice)}원"
    }

    private fun getFirstCyclePlanItems(): List<SubscriptionPlanItem> {
        val startCalendar = parseDateToCalendar(
            selectedSubscriptionStartDate.ifBlank { getTomorrowString() }
        ) ?: return emptyList()

        val endCalendar = (startCalendar.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            add(Calendar.DAY_OF_MONTH, -1)
        }

        return buildSubscriptionCalendarItemsByDate(startCalendar, endCalendar)
            .toSortedMap()
            .values
            .flatten()
    }

    private fun updatePlanCalendar() {
        val calendarGrid = binding.gridSubscriptionCalendar
        calendarGrid.removeAllViews()

        if (subscriptionPlanItems.isEmpty()) {
            binding.layoutSubscriptionCalendarCard.visibility = View.GONE
            return
        }

        val startCalendar = parseDateToCalendar(
            selectedSubscriptionStartDate.ifBlank { getTomorrowString() }
        ) ?: return

        val endCalendar = (startCalendar.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            add(Calendar.DAY_OF_MONTH, -1)
        }

        val gridStartCalendar = (startCalendar.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, -(get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY))
        }

        val visibleDays = countDaysInclusive(gridStartCalendar, endCalendar)
        val weekRows = maxOf(5, (visibleDays + 6) / 7)
        val itemsByDate = buildSubscriptionCalendarItemsByDate(startCalendar, endCalendar)
        val startDateKey = formatCalendarToDateString(startCalendar)
        val endDateKey = formatCalendarToDateString(endCalendar)

        binding.layoutSubscriptionCalendarCard.visibility = View.VISIBLE
        binding.tvSubscriptionCalendarTitle.text =
            "첫 달 구독 달력 · ${formatMonthDay(startCalendar)} ~ ${formatMonthDay(endCalendar)}"

        calendarGrid.columnCount = 7
        calendarGrid.rowCount = weekRows + 1

        listOf("일", "월", "화", "수", "목", "금", "토").forEach { label ->
            calendarGrid.addView(createCalendarHeaderView(label))
        }

        val cursor = gridStartCalendar.clone() as Calendar
        repeat(weekRows * 7) {
            val dateKey = formatCalendarToDateString(cursor)
            val items = itemsByDate[dateKey].orEmpty()
            val inCycle = !cursor.before(startCalendar) && !cursor.after(endCalendar)
            calendarGrid.addView(
                createCalendarDayView(
                    calendar = cursor,
                    inCycle = inCycle,
                    items = items,
                    isStartDate = dateKey == startDateKey,
                    isEndDate = dateKey == endDateKey
                )
            )
            cursor.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun buildSubscriptionCalendarItemsByDate(
        startCalendar: Calendar,
        endCalendar: Calendar
    ): Map<String, List<SubscriptionPlanItem>> {
        val groupedItems = linkedMapOf<String, MutableList<SubscriptionPlanItem>>()

        subscriptionPlanItems.forEach { item ->
            val targetDate = findDateByRelativeWeekAndDayOfWeek(
                startCalendar = startCalendar,
                relativeWeek = item.weekOfMonth,
                dayOfWeek = item.dayOfWeek
            )

            if (targetDate.before(startCalendar) || targetDate.after(endCalendar)) {
                return@forEach
            }

            val dateKey = formatCalendarToDateString(targetDate)
            groupedItems.getOrPut(dateKey) { mutableListOf() }.add(item)
        }

        return groupedItems
    }

    private fun createCalendarHeaderView(label: String): TextView {
        return TextView(requireContext()).apply {
            text = label
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(requireContext(), R.color.mypage_text_secondary))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = createCalendarCellLayoutParams(heightDp = 24)
        }
    }

    private fun createCalendarDayView(
        calendar: Calendar,
        inCycle: Boolean,
        items: List<SubscriptionPlanItem>,
        isStartDate: Boolean,
        isEndDate: Boolean
    ): TextView {
        val totalQuantity = items.sumOf { it.quantity }
        val hasPlan = items.isNotEmpty()
        val statusLabel = when {
            isStartDate -> "시작"
            isEndDate -> "만료"
            hasPlan -> "${totalQuantity}개"
//            hasPlan -> ""
            else -> ""
        }
        val dateLabel = if (calendar.get(Calendar.DAY_OF_MONTH) == 1) {
            "${calendar.get(Calendar.MONTH) + 1}/1"
        } else {
            calendar.get(Calendar.DAY_OF_MONTH).toString()
        }

        return TextView(requireContext()).apply {
            text = if (statusLabel.isNotBlank()) "$dateLabel\n$statusLabel" else dateLabel
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            includeFontPadding = false
            setPadding(0, dp(8), 0, 0)
            textSize = if (statusLabel.isNotBlank()) 11f else 12f
            typeface = if (statusLabel.isNotBlank()) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    when {
                        isStartDate || isEndDate -> R.color.mypage_primary_dark
                        hasPlan -> R.color.mypage_warm_white
                        inCycle -> R.color.mypage_text
                        else -> R.color.mypage_text_secondary
                    }
                )
            )
            background = createCalendarDayBackground(
                hasPlan = hasPlan,
                inCycle = inCycle,
                isStartDate = isStartDate,
                isEndDate = isEndDate
            )
            isEnabled = hasPlan
            alpha = if (inCycle || hasPlan) 1f else 0.45f
            layoutParams = createCalendarCellLayoutParams(heightDp = 48)

            if (hasPlan) {
                setOnClickListener {
                    showSubscriptionPlanDayDialog(formatCalendarToDateString(calendar), items)
                }
            }
        }
    }

    private fun createCalendarCellLayoutParams(heightDp: Int): GridLayout.LayoutParams {
        return GridLayout.LayoutParams().apply {
            width = 0
            height = dp(heightDp)
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(dp(2), dp(2), dp(2), dp(2))
        }
    }

    private fun createCalendarDayBackground(
        hasPlan: Boolean,
        inCycle: Boolean,
        isStartDate: Boolean,
        isEndDate: Boolean
    ): GradientDrawable {
        val fillColor = when {
            isStartDate -> R.color.subscription_start_day
            isEndDate -> R.color.subscription_end_day
            hasPlan -> R.color.mypage_fern
            inCycle -> R.color.mypage_warm_white
            else -> R.color.mypage_surface
        }
        val strokeColor = when {
            isStartDate -> R.color.subscription_start_day
            isEndDate -> R.color.subscription_end_day
            hasPlan -> R.color.mypage_fern
            else -> R.color.mypage_surface
        }

        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(5).toFloat()
            setColor(ContextCompat.getColor(requireContext(), fillColor))
            setStroke(dp(1), ContextCompat.getColor(requireContext(), strokeColor))
        }
    }

    private fun showSubscriptionPlanDayDialog(
        date: String,
        items: List<SubscriptionPlanItem>
    ) {
        val detailText = items.joinToString(separator = "\n\n") { item ->
            "매월 ${item.weekOfMonth}주차 ${item.dayOfWeekText}\n" +
                    "${item.product.name}\n" +
                    "회당 ${item.quantity}개 · 월 예상 ${formatPrice(item.product.price * item.quantity)}원"
        }

        AlertDialog.Builder(requireContext())
            .setTitle("$date 구독 상세")
            .setMessage(detailText)
            .setPositiveButton("닫기", null)
            .setNegativeButton("이 날짜 일정 삭제") { _, _ ->
                subscriptionPlanItems.removeAll(items.toSet())
                planItemAdapter.submitList(subscriptionPlanItems.toList())
                clearSelectedStore()
                updatePlanSummary()
            }
            .show()
    }

    private fun countDaysInclusive(
        startCalendar: Calendar,
        endCalendar: Calendar
    ): Int {
        val cursor = startCalendar.clone() as Calendar
        var count = 0
        while (!cursor.after(endCalendar)) {
            count++
            cursor.add(Calendar.DAY_OF_MONTH, 1)
        }
        return count
    }

    private fun formatMonthDay(calendar: Calendar): String {
        return "${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.DAY_OF_MONTH)}"
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun updateSubscriptionStepUi() {
        val isStartStep = currentSubscriptionStep == STEP_START_DATE
        val isMealStep = currentSubscriptionStep == STEP_MEAL_PLAN
        val isReceiveStep = currentSubscriptionStep == STEP_RECEIVE

        binding.cardSubscriptionStartStep.visibility =
            if (isStartStep) View.VISIBLE else View.GONE

        binding.btnCreateCustomMenu.visibility =
            if (isMealStep) View.VISIBLE else View.GONE
        binding.cardSubscriptionMealStep.visibility =
            if (isMealStep) View.VISIBLE else View.GONE
        binding.cardSubscriptionScheduleStep.visibility =
            if (isMealStep) View.VISIBLE else View.GONE
        binding.cardSubscriptionPlanStep.visibility =
            if (isMealStep) View.VISIBLE else View.GONE

        binding.tvSubscriptionReceiveStepTitle.visibility =
            if (isReceiveStep) View.VISIBLE else View.GONE
        binding.layoutAddress.visibility =
            if (isReceiveStep) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.cardDeliveryStore.visibility =
            if (isReceiveStep) View.VISIBLE else View.GONE

        if (isReceiveStep) {
            binding.root.post {
                val roadAddress = binding.etAddress.text.toString().trim()

                if (roadAddress.isNotBlank() && selectedDeliveryStore == null) {
                    autoSelectNearestDeliveryStore()
                } else {
                    bindSelectedDeliveryStore()
                }
            }
        }

        binding.btnSubscriptionOrder.visibility =
            if (isReceiveStep) View.VISIBLE else View.GONE
        binding.btnSubscriptionPrevious.visibility =
            if (isStartStep) View.GONE else View.VISIBLE
        binding.btnSubscriptionNext.visibility =
            if (isReceiveStep) View.GONE else View.VISIBLE
        (binding.btnSubscriptionNext.layoutParams as ViewGroup.MarginLayoutParams).apply {
            marginStart = if (isStartStep) {
                0
            } else {
                dp(10)
            }
            binding.btnSubscriptionNext.layoutParams = this
        }

        updateStepIndicator(
            step = STEP_START_DATE,
            indicator = binding.tvSubscriptionStepOneIndicator,
            label = binding.tvSubscriptionStepOneLabel
        )
        updateStepIndicator(
            step = STEP_MEAL_PLAN,
            indicator = binding.tvSubscriptionStepTwoIndicator,
            label = binding.tvSubscriptionStepTwoLabel
        )
        updateStepIndicator(
            step = STEP_RECEIVE,
            indicator = binding.tvSubscriptionStepThreeIndicator,
            label = binding.tvSubscriptionStepThreeLabel
        )

        binding.subscriptionScrollView.post {
            binding.subscriptionScrollView.smoothScrollTo(0, 0)
        }
    }

    private fun updateStepIndicator(
        step: Int,
        indicator: android.widget.TextView,
        label: android.widget.TextView
    ) {
        val isReached = step <= currentSubscriptionStep
        val isCurrent = step == currentSubscriptionStep

        indicator.setBackgroundResource(
            if (isReached) {
                R.drawable.bg_subscription_step_active
            } else {
                R.drawable.bg_subscription_step
            }
        )
        indicator.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isReached) R.color.fit_surface else R.color.fit_primary
            )
        )
        label.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isCurrent) R.color.fit_primary else R.color.fit_text_secondary
            )
        )
        label.setTypeface(
            label.typeface,
            if (isCurrent) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
        )
    }

    private fun weekCheckBoxes() = listOf(
        binding.checkWeek1,
        binding.checkWeek2,
        binding.checkWeek3,
        binding.checkWeek4,
        binding.checkWeek5
    )

    private fun getSelectedWeeks(): List<WeekOption> {
        return weekItems.filterIndexed { index, _ ->
            weekCheckBoxes()[index].isChecked
        }
    }

    private fun getSelectedDays(): List<DayOption> {
        val selectedDayValues = buildSet {
            if (binding.checkDaySun.isChecked) add(Calendar.SUNDAY)
            if (binding.checkDayMon.isChecked) add(Calendar.MONDAY)
            if (binding.checkDayTue.isChecked) add(Calendar.TUESDAY)
            if (binding.checkDayWed.isChecked) add(Calendar.WEDNESDAY)
            if (binding.checkDayThu.isChecked) add(Calendar.THURSDAY)
            if (binding.checkDayFri.isChecked) add(Calendar.FRIDAY)
            if (binding.checkDaySat.isChecked) add(Calendar.SATURDAY)
        }

        return dayItems.filter { it.value in selectedDayValues }
    }

    private fun getUserId(): Int? {
        val userId = SessionManager(requireContext()).getUser()?.id
        if (userId == null) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
        return userId
    }

    private fun clearSelectedStore() {
        selectedDeliveryStore = null
        deliveryAddressLatLng = null
        deliveryCandidateStores = emptyList()

        if (_binding != null) {
            if (::deliveryAddressFormManager.isInitialized) {
                bindSelectedDeliveryStore()
            }
        }
    }

    private fun getTodayString(): String {
        return formatCalendarToDateString(Calendar.getInstance())
    }

    private fun getTomorrowString(): String {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DATE, 1)
        }
        return formatCalendarToDateString(calendar)
    }

    private fun parseDateToCalendar(date: String): Calendar? {
        return try {
            val formatter = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            val parsedDate = formatter.parse(date) ?: return null

            Calendar.getInstance().apply {
                time = parsedDate
                clearTime(this)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun formatCalendarToDateString(calendar: Calendar): String {
        val formatter = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        return formatter.format(calendar.time)
    }

    private fun clearTime(calendar: Calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
    }

    private fun formatPrice(price: Int): String {
        return DisplayFormatter.formatPrice(price)
    }

    private fun formatNutrition(value: Double): String {
        return DisplayFormatter.formatNutrition(value)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private enum class ProductFilter {
        ALL,
        FAVORITES,
        DIET,
        BULKING,
        SALAD,
        SIMPLE
    }

    private data class WeekOption(
        val value: Int,
        val label: String
    )

    private data class DayOption(
        val value: Int,
        val label: String
    )

    private data class WeekDayFlags(
        var mon: Boolean = false,
        var tue: Boolean = false,
        var wed: Boolean = false,
        var thu: Boolean = false,
        var fri: Boolean = false,
        var sat: Boolean = false,
        var sun: Boolean = false
    )

    companion object {
        private const val RECEIVE_TYPE_DELIVERY = "DELIVERY"
        private const val FREE_DELIVERY_THRESHOLD = 30000
        private const val DELIVERY_FEE = 3000

        private const val DATE_FORMAT = "yyyy-MM-dd"
        private const val ARG_RESUBSCRIBE_SOURCE = "resubscribe_source"
        private const val DELIVERY_RADIUS_METER = 3_000f

        private const val SELECTED_BUTTON_COLOR = 0xFF2F6B45.toInt()
        private const val UNSELECTED_BUTTON_COLOR = 0xFFF7F8F7.toInt()
        private const val SELECTED_BUTTON_TEXT_COLOR = 0xFFFFFFFF.toInt()
        private const val UNSELECTED_BUTTON_TEXT_COLOR = 0xFF222522.toInt()

        private const val STEP_START_DATE = 1
        private const val STEP_MEAL_PLAN = 2
        private const val STEP_RECEIVE = 3

        fun newResubscribeInstance(subscription: SubscriptionResponse): SubscriptionOrderFragment {
            return SubscriptionOrderFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_RESUBSCRIBE_SOURCE, Gson().toJson(subscription))
                }
            }
        }
    }
}
