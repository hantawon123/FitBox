package com.ssafy.fitbox.fragment

import android.app.DatePickerDialog
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.ssafy.fitbox.R
import com.ssafy.fitbox.activity.MainActivity
import com.ssafy.fitbox.databinding.FragmentOrderBinding
import com.ssafy.fitbox.network.request.DirectOrderRequest
import com.ssafy.fitbox.network.request.OrderCartRequest
import com.ssafy.fitbox.network.request.SubscriptionOrderRequest
import com.ssafy.fitbox.util.AddressParts
import com.ssafy.fitbox.util.DisplayFormatter
import com.ssafy.fitbox.util.LoginRequiredDialog
import com.ssafy.fitbox.util.SessionManager
import com.ssafy.fitbox.viewmodel.OrderViewModel
import com.ssafy.fitbox.viewmodel.AddressViewModel
import com.ssafy.fitbox.viewmodel.CartViewModel
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.lifecycle.lifecycleScope
import com.ssafy.fitbox.util.DeliveryAddressFormManager
import kotlinx.coroutines.launch
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.ssafy.fitbox.dto.Store
import com.ssafy.fitbox.repository.StoreRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OrderFragment : Fragment() {

    private var _binding: FragmentOrderBinding? = null
    private val binding get() = _binding!!

    private val orderViewModel: OrderViewModel by viewModels()
    private val cartViewModel: CartViewModel by activityViewModels()
    private val addressViewModel: AddressViewModel by viewModels()

    private var orderType: String = ORDER_TYPE_DIRECT
    private var mealId: Long = -1L
    private var quantity: Int = 1

    private var orderItemText: String = ""
    private var totalQuantity: Int = 1

    private var orderAmount: Int = 0
    private val deliveryFee: Int = 3000

    private var selectedOrderMode: String = ORDER_MODE_SINGLE
    private var selectedReceiveType: String = RECEIVE_TYPE_PICKUP

    private var selectedReceiveDate: String? = null
    private var subscriptionStartDate: String? = null
    private var subscriptionEndDate: String? = null

    private var selectedStoreId: Long? = null
    private var selectedStoreName: String = ""

    private val storeRepository = StoreRepository()
    private var selectedStoreAddress: String = ""
    private var selectedStoreTotalCount: Int = 0
    private var selectedStoreRemainCount: Int = 0
    private var selectedPickupPointId: Long? = null
    private var selectedPickupPointName: String = ""
    private var selectedPickupPointAddress: String = ""
    private var isOrderSubmitting: Boolean = false

    private var selectedDeliveryStore: Store? = null

    private var cachedDeliveryRoadAddress: String = ""
    private var cachedDeliveryDetailAddress: String = ""
    private var cachedDeliveryLatLng: LatLng? = null
    private var isReturningFromDeliveryStoreMap: Boolean = false
    private var isRestoringDeliveryAddress: Boolean = false

    private var deliveryAddressLatLng: LatLng? = null
    private var deliveryCandidateStores: List<Store> = emptyList()

    private lateinit var deliveryAddressFormManager: DeliveryAddressFormManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        orderType = arguments?.getString(ARG_ORDER_TYPE) ?: ORDER_TYPE_DIRECT
        mealId = arguments?.getLong(ARG_MEAL_ID) ?: -1L
        quantity = arguments?.getInt(ARG_QUANTITY) ?: 1
        orderItemText = arguments?.getString(ARG_ORDER_ITEM_TEXT) ?: "주문 상품 정보 없음"
        totalQuantity = arguments?.getInt(ARG_TOTAL_QUANTITY) ?: quantity
        orderAmount = arguments?.getInt(ARG_ORDER_AMOUNT) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderBinding.inflate(inflater, container, false)
        return binding.root
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

        initStoreResultListener()
        initAddressSearchResultListener()
        observeAddresses()
        initView()
        initClickEvents()
        observeOrderResult()
        loadSavedAddresses()
    }

    private fun observeAddresses() {
        addressViewModel.addresses.observe(viewLifecycleOwner) { addresses ->
            deliveryAddressFormManager.submitSavedAddresses(
                addresses = addresses,
                applyLatestSavedAddress = !isReturningFromDeliveryStoreMap &&
                        cachedDeliveryRoadAddress.isBlank()
            )

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

    private fun initStoreResultListener() {
        parentFragmentManager.setFragmentResultListener(
            StoreMapFragment.REQUEST_KEY_STORE,
            viewLifecycleOwner
        ) { _, bundle ->
            selectedStoreId = bundle.getLong(StoreMapFragment.KEY_STORE_ID)
            selectedStoreName = bundle.getString(StoreMapFragment.KEY_STORE_NAME).orEmpty()
            selectedStoreAddress = bundle.getString(StoreMapFragment.KEY_STORE_ADDRESS).orEmpty()
            selectedStoreTotalCount = bundle.getInt(StoreMapFragment.KEY_STORE_TOTAL_COUNT)
            selectedStoreRemainCount = bundle.getInt(StoreMapFragment.KEY_STORE_REMAIN_COUNT)
            val selectedStoreLatitude = bundle.getDouble(StoreMapFragment.KEY_STORE_LATITUDE, 0.0)
            val selectedStoreLongitude = bundle.getDouble(StoreMapFragment.KEY_STORE_LONGITUDE, 0.0)
            selectedPickupPointId =
                if (bundle.containsKey(StoreMapFragment.KEY_PICKUP_POINT_ID)) {
                    bundle.getLong(StoreMapFragment.KEY_PICKUP_POINT_ID)
                } else {
                    null
                }
            selectedPickupPointName = bundle
                .getString(StoreMapFragment.KEY_PICKUP_POINT_NAME)
                .orEmpty()
            selectedPickupPointAddress = bundle
                .getString(StoreMapFragment.KEY_PICKUP_POINT_ADDRESS)
                .orEmpty()
            val receiveTypeFromMap = bundle
                .getString(StoreMapFragment.KEY_RECEIVE_TYPE)
                ?: RECEIVE_TYPE_PICKUP

            if (receiveTypeFromMap == RECEIVE_TYPE_DELIVERY) {
                selectedReceiveType = RECEIVE_TYPE_DELIVERY

                binding.layoutAddress.visibility = View.VISIBLE
                binding.layoutPickup.visibility = View.GONE
                binding.cardDeliveryStore.visibility = View.VISIBLE

                restoreCachedDeliveryAddressIfNeeded()

                selectedDeliveryStore = Store(
                    id = selectedStoreId ?: return@setFragmentResultListener,
                    name = selectedStoreName,
                    address = selectedStoreAddress,
                    longitude = selectedStoreLongitude,
                    latitude = selectedStoreLatitude,
                    totalCnt = selectedStoreTotalCount,
                    remainCnt = selectedStoreRemainCount
                )

                binding.root.post {
                    restoreCachedDeliveryAddressIfNeeded()
                    bindSelectedDeliveryStore()
                }

                bindSelectedDeliveryStore()
                updateReceiveTypeUi()
                updateReceiveDateTitle()
                updateOrderDateArea()
                updatePaymentSummary()
                return@setFragmentResultListener
            }

            selectedReceiveType = receiveTypeFromMap
            bindSelectedStore()
            updateOrderDateArea()
            updatePaymentSummary()
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


    private fun initView() {
        binding.cardDeliveryStore.visibility = View.GONE

        binding.tvOrderType.text = when (orderType) {
            ORDER_TYPE_DIRECT -> "바로 주문"
            ORDER_TYPE_CART -> "장바구니 주문"
            else -> "주문"
        }

        binding.tvQuantity.text = when (orderType) {
            ORDER_TYPE_DIRECT -> "주문 상품: $orderItemText\n수량: $quantity"
            ORDER_TYPE_CART -> "주문 상품\n$orderItemText\n총 수량: $totalQuantity"
            else -> "주문 상품: $orderItemText"
        }

        selectedOrderMode = ORDER_MODE_SINGLE
        selectedReceiveType = RECEIVE_TYPE_PICKUP

        binding.radioPickup.isChecked = true


        binding.layoutAddress.visibility = View.GONE
        binding.layoutPickup.visibility = View.VISIBLE
        binding.layoutSubscriptionOrder.visibility = View.GONE
        binding.cardReceiveDate.visibility = View.VISIBLE
        selectedReceiveDate = getTomorrowString()

        showPickupEmptyState()
        binding.btnPay.text = "지도에서 픽업 위치 선택"

        updateOrderDateArea()
        updateReceiveTypeUi()
        updateReceiveDateTitle()
        updatePaymentSummary()
        updateSubscriptionDateText()
    }

    private fun initClickEvents() {
        binding.etAddress.setOnClickListener { openAddressSearch() }
        binding.tilAddressSearch.setEndIconOnClickListener { openAddressSearch() }

        binding.btnChangeDeliveryStore.setOnClickListener {
            openDeliveryStoreMap()
        }

        binding.cardPickupOption.setOnClickListener {
            binding.radioPickup.isChecked = true
        }

        binding.cardDeliveryOption.setOnClickListener {
            binding.radioDelivery.isChecked = true
        }

        binding.radioGroupReceiveType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                binding.radioPickup.id -> {
                    selectedReceiveType = RECEIVE_TYPE_PICKUP
                    clearSelectedStore()
                    selectedDeliveryStore = null

                    binding.layoutAddress.visibility = View.GONE
                    binding.layoutPickup.visibility = View.VISIBLE
                    binding.cardDeliveryStore.visibility = View.GONE
                }

                binding.radioDelivery.id -> {
                    selectedReceiveType = RECEIVE_TYPE_DELIVERY
                    clearSelectedStore()
                    selectedDeliveryStore = null

                    if (isToday(selectedReceiveDate)) {
                        selectedReceiveDate = null
                    }

                    binding.layoutAddress.visibility = View.VISIBLE
                    binding.layoutPickup.visibility = View.GONE
                    binding.cardDeliveryStore.visibility = View.VISIBLE

                    autoSelectNearestDeliveryStore()
                }
            }

            updateReceiveTypeUi()
            updateReceiveDateTitle()
            updateOrderDateArea()
            updatePaymentSummary()
        }

        binding.btnSelectReceiveDate.setOnClickListener {
            showSingleReceiveDatePicker()
        }

        binding.btnSelectSubscriptionStartDate.setOnClickListener {
            showSubscriptionStartDatePicker()
        }

        binding.btnSelectSubscriptionEndDate.setOnClickListener {
            showSubscriptionEndDatePicker()
        }

        binding.btnSelectStore.setOnClickListener {
            if (!isPickupReceiveType(selectedReceiveType)) {
                return@setOnClickListener
            }

            if (selectedOrderMode == ORDER_MODE_SINGLE) {
                openSinglePickupMap()
                return@setOnClickListener
            }

            if (selectedOrderMode == ORDER_MODE_SUBSCRIPTION) {
                if (subscriptionStartDate == null) {
                    Toast.makeText(
                        requireContext(),
                        "구독 시작일을 먼저 선택해주세요.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                if (subscriptionEndDate == null) {
                    Toast.makeText(
                        requireContext(),
                        "구독 종료일을 먼저 선택해주세요.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                if (getSelectedSubscriptionDays().isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "수령 요일을 하나 이상 선택해주세요.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                parentFragmentManager.beginTransaction()
                    .replace(
                        R.id.main_container,
                        StoreMapFragment.newSubscriptionInstance(
                            dateStart = subscriptionStartDate!!,
                            dateEnd = subscriptionEndDate!!,
                            mon = binding.checkMon.isChecked,
                            tue = binding.checkTue.isChecked,
                            wed = binding.checkWed.isChecked,
                            thu = binding.checkThu.isChecked,
                            fri = binding.checkFri.isChecked,
                            sat = binding.checkSat.isChecked,
                            sun = binding.checkSun.isChecked
                        )
                    )
                    .addToBackStack(null)
                    .commit()
            }
        }

        binding.btnPay.setOnClickListener {
            if (isOrderSubmitting) return@setOnClickListener

            if (selectedOrderMode == ORDER_MODE_SUBSCRIPTION) {
                showMockPaymentDialog {
                    orderSubscription()
                }
                return@setOnClickListener
            }

            if (isPickupReceiveType(selectedReceiveType) && selectedStoreId == null) {
                openSinglePickupMap()
                return@setOnClickListener
            }

            when (orderType) {
                ORDER_TYPE_DIRECT -> showMockPaymentDialog { paymentMethod ->
                    orderDirect(paymentMethod)
                }

                ORDER_TYPE_CART -> showMockPaymentDialog { paymentMethod ->
                    orderFromCart(paymentMethod = paymentMethod)
                }
            }
        }

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val subscriptionDayChangeListener = { _: android.widget.CompoundButton, _: Boolean ->
            updatePaymentSummary()
        }

        binding.checkMon.setOnCheckedChangeListener(subscriptionDayChangeListener)
        binding.checkTue.setOnCheckedChangeListener(subscriptionDayChangeListener)
        binding.checkWed.setOnCheckedChangeListener(subscriptionDayChangeListener)
        binding.checkThu.setOnCheckedChangeListener(subscriptionDayChangeListener)
        binding.checkFri.setOnCheckedChangeListener(subscriptionDayChangeListener)
        binding.checkSat.setOnCheckedChangeListener(subscriptionDayChangeListener)
        binding.checkSun.setOnCheckedChangeListener(subscriptionDayChangeListener)
    }

    private fun openAddressSearch() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, AddressSearchFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun getStoreIdForOrder(receiveType: String = selectedReceiveType): Long? {
        return when {
            isPickupReceiveType(receiveType) -> selectedStoreId
            receiveType == RECEIVE_TYPE_DELIVERY -> selectedDeliveryStore?.id
            else -> null
        }
    }

    private fun orderDirect(paymentMethod: String = "MOCK") {
        if (mealId <= 0) {
            Toast.makeText(
                requireContext(),
                "주문할 상품 정보를 찾을 수 없습니다.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (!validateSingleOrderDate()) return

        val userId = getUserId() ?: return
        val address = deliveryAddressFormManager.getAddressIfDelivery(
            isDelivery = selectedReceiveType == RECEIVE_TYPE_DELIVERY
        )
        if (selectedReceiveType == RECEIVE_TYPE_DELIVERY && address == null) return

        deliveryAddressFormManager.prepareDeliveryAddressSave(
            isDelivery = selectedReceiveType == RECEIVE_TYPE_DELIVERY,
            address = address
        )

        val storeId = getStoreIdForOrder()

        if (selectedReceiveType == RECEIVE_TYPE_DELIVERY && storeId == null) {
            Toast.makeText(
                requireContext(),
                "배송 담당 매장을 선택해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (selectedReceiveType == RECEIVE_TYPE_PICKUP && storeId == null) {
            Toast.makeText(
                requireContext(),
                "픽업 매장을 선택해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }


        if (selectedReceiveType == RECEIVE_TYPE_PICKUP_POINT && selectedPickupPointId == null) {
            Toast.makeText(
                requireContext(),
                "\uD53D\uC5C5 \uD3EC\uC778\uD2B8\uB97C \uC120\uD0DD\uD574\uC8FC\uC138\uC694.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        val request = DirectOrderRequest(
            userId = userId,
            mealId = mealId,
            quantity = quantity,
            receiveType = selectedReceiveType,
            storeId = storeId,
            pickupPointId = if (selectedReceiveType == RECEIVE_TYPE_PICKUP_POINT) {
                selectedPickupPointId
            } else {
                null
            },
            address = address,
            receiveDate = selectedReceiveDate,
            paymentMethod = paymentMethod
        )

        if (!beginOrderSubmission()) return
        orderViewModel.orderDirect(request)
    }

    private fun orderFromCart(
        receiveTypeOverride: String? = null,
        paymentMethod: String = "MOCK"
    ) {
        if (!validateSingleOrderDate()) return

        val requestReceiveType = receiveTypeOverride ?: selectedReceiveType
        val userId = getUserId() ?: return
        val storeId = getStoreIdForOrder(requestReceiveType)

        val address = deliveryAddressFormManager.getAddressIfDelivery(
            isDelivery = requestReceiveType == RECEIVE_TYPE_DELIVERY
        )
        if (requestReceiveType == RECEIVE_TYPE_DELIVERY && address == null) return

        deliveryAddressFormManager.prepareDeliveryAddressSave(
            isDelivery = requestReceiveType == RECEIVE_TYPE_DELIVERY,
            address = address
        )

        if (requestReceiveType == RECEIVE_TYPE_DELIVERY && storeId == null) {
            Toast.makeText(
                requireContext(),
                "배송 담당 매장을 선택해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (isPickupReceiveType(requestReceiveType) && storeId == null) {
            Toast.makeText(
                requireContext(),
                "픽업 매장을 선택해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (requestReceiveType == RECEIVE_TYPE_PICKUP_POINT && selectedPickupPointId == null) {
            Toast.makeText(
                requireContext(),
                "픽업 포인트를 선택해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val request = OrderCartRequest(
            userId = userId,
            receiveType = requestReceiveType,
            storeId = storeId,
            pickupPointId = if (requestReceiveType == RECEIVE_TYPE_PICKUP_POINT) {
                selectedPickupPointId
            } else {
                null
            },
            address = address,
            receiveDate = selectedReceiveDate,
            paymentMethod = paymentMethod
        )

        if (!beginOrderSubmission()) return
        orderViewModel.orderFromCart(request)
    }

    private fun orderSubscription() {
        if (!validateSubscriptionOrder()) return

        if (orderType != ORDER_TYPE_DIRECT) {
            Toast.makeText(
                requireContext(),
                "장바구니 구독 주문은 다음 단계에서 지원합니다.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (mealId <= 0) {
            Toast.makeText(
                requireContext(),
                "구독할 상품 정보를 찾을 수 없습니다.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val userId = getUserId() ?: return

        val address = deliveryAddressFormManager.getAddressIfDelivery(
            isDelivery = selectedReceiveType == RECEIVE_TYPE_DELIVERY
        )
        if (selectedReceiveType == RECEIVE_TYPE_DELIVERY && address == null) return

        deliveryAddressFormManager.prepareDeliveryAddressSave(
            isDelivery = selectedReceiveType == RECEIVE_TYPE_DELIVERY,
            address = address
        )

        val storeId = getStoreIdForOrder()

        if (selectedReceiveType == RECEIVE_TYPE_DELIVERY && storeId == null) {
            Toast.makeText(
                requireContext(),
                "배송 담당 매장을 선택해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (selectedReceiveType == RECEIVE_TYPE_PICKUP && storeId == null) {
            Toast.makeText(
                requireContext(),
                "픽업 매장을 선택해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val request = SubscriptionOrderRequest(
            userId = userId,
            dateStart = subscriptionStartDate!!,
            dateEnd = subscriptionEndDate!!,
            mon = if (binding.checkMon.isChecked) mealId else null,
            tue = if (binding.checkTue.isChecked) mealId else null,
            wed = if (binding.checkWed.isChecked) mealId else null,
            thu = if (binding.checkThu.isChecked) mealId else null,
            fri = if (binding.checkFri.isChecked) mealId else null,
            sat = if (binding.checkSat.isChecked) mealId else null,
            sun = if (binding.checkSun.isChecked) mealId else null,
            receiveType = selectedReceiveType,
            storeId = storeId,
            address = address
        )

        if (!beginOrderSubmission()) return
        orderViewModel.orderSubscription(request)
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

        val targetDate = selectedReceiveDate ?: getTomorrowString()

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
                binding.tvDeliveryStoreDistance.text = "선택한 배송지 3km 이내에 주문 가능한 매장이 없습니다."
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

    private fun bindSelectedDeliveryStore() {
        val store = selectedDeliveryStore

        if (store == null) {
            binding.tvDeliveryStoreName.text = "배송지를 선택하면 가장 가까운 매장이 자동 선택됩니다."
            binding.tvDeliveryStoreAddress.text = ""
            binding.tvDeliveryStoreDistance.text = ""
            return
        }

        val addressLocation = deliveryAddressLatLng
        val distanceText = if (addressLocation != null && store.latitude != 0.0 && store.longitude != 0.0) {
            "배송지에서 ${formatDistanceMeter(distanceMeter(addressLocation, LatLng(store.latitude, store.longitude)))}"
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
        binding.btnChangeDeliveryStore.isEnabled = true
        bindSelectedDeliveryStore()
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
                    deliveryDate = selectedReceiveDate ?: getTomorrowString(),
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

    private fun formatDistanceMeter(meter: Float): String {
        return if (meter < 1000f) {
            "${meter.toInt()}m"
        } else {
            String.format(Locale.KOREA, "%.1fkm", meter / 1000f)
        }
    }

    private fun validateSingleOrderDate(): Boolean {
        if (selectedReceiveDate == null) {
            Toast.makeText(
                requireContext(),
                "수령 날짜를 선택해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        return true
    }

    private fun openSinglePickupMap() {
        if (selectedReceiveDate == null) {
            Toast.makeText(
                requireContext(),
                "픽업 날짜를 먼저 선택해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        parentFragmentManager.beginTransaction()
            .replace(
                R.id.main_container,
                StoreMapFragment.newSingleInstance(selectedReceiveDate!!)
            )
            .addToBackStack(null)
            .commit()
    }

    private fun submitSingleOrderAfterPickupSelection(receiveTypeFromMap: String) {
        if (isOrderSubmitting) return
        showMockPaymentDialog { paymentMethod ->
            when (orderType) {
                ORDER_TYPE_DIRECT -> orderDirect(paymentMethod)
                ORDER_TYPE_CART -> orderFromCart(receiveTypeFromMap, paymentMethod)
            }
        }
    }

    private fun validateSubscriptionOrder(): Boolean {
        if (subscriptionStartDate == null) {
            Toast.makeText(
                requireContext(),
                "구독 시작일을 선택해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        if (subscriptionEndDate == null) {
            Toast.makeText(
                requireContext(),
                "구독 종료일을 선택해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        if (subscriptionEndDate!! < subscriptionStartDate!!) {
            Toast.makeText(
                requireContext(),
                "구독 종료일은 시작일보다 빠를 수 없습니다.",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        if (getSelectedSubscriptionDays().isEmpty()) {
            Toast.makeText(
                requireContext(),
                "수령 요일을 하나 이상 선택해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        val address = deliveryAddressFormManager.getAddressIfDelivery(
            isDelivery = selectedReceiveType == RECEIVE_TYPE_DELIVERY
        )
        if (selectedReceiveType == RECEIVE_TYPE_DELIVERY && address == null) {
            return false
        }

        val storeId = getStoreIdForOrder()
        if (selectedReceiveType == RECEIVE_TYPE_PICKUP && storeId == null) {
            Toast.makeText(
                requireContext(),
                "픽업 매장을 선택해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        return true
    }

    private fun showMockPaymentDialog(onConfirmed: (String) -> Unit) {
        if (isOrderSubmitting) return

        val paymentMethods = arrayOf(
            "신용/체크카드",
            "간편결제",
            "무통장입금"
        )
        val paymentCodes = arrayOf(
            "MOCK_CARD",
            "MOCK_EASY_PAY",
            "MOCK_BANK_TRANSFER"
        )
        var selectedIndex = 0
        val amount = calculateFinalPaymentAmount()

        AlertDialog.Builder(requireContext())
            .setTitle("모의 결제")
            .setMessage(
                "실제 결제는 진행되지 않습니다.\n" +
                        "결제 수단을 선택한 뒤 결제 완료를 누르면 주문이 생성됩니다.\n\n" +
                        "결제 금액: ${formatPrice(amount)}원"
            )
            .setSingleChoiceItems(paymentMethods, selectedIndex) { _, which ->
                selectedIndex = which
            }
            .setNegativeButton("취소", null)
            .setPositiveButton("결제 완료") { _, _ ->
                onConfirmed(paymentCodes[selectedIndex])
            }
            .show()
    }

    private fun beginOrderSubmission(): Boolean {
        if (isOrderSubmitting) return false
        isOrderSubmitting = true
        binding.btnPay.isEnabled = false
        binding.btnPay.text = "주문 처리 중..."
        return true
    }

    private fun finishOrderSubmission() {
        isOrderSubmitting = false
        binding.btnPay.isEnabled = true
        updateOrderDateArea()
    }

    private fun observeOrderResult() {
        orderViewModel.directOrderResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { order ->
                viewLifecycleOwner.lifecycleScope.launch {
                    getUserId()?.let { userId ->
                        deliveryAddressFormManager.savePendingDeliveryAddressIfNeeded(userId)
                    }

                    Toast.makeText(
                        requireContext(),
                        "주문 완료: ${order.mealName}",
                        Toast.LENGTH_SHORT
                    ).show()

                    cartViewModel.loadCartItems()
                    moveToOrderList()
                }
            }

            result.onFailure { throwable ->
                finishOrderSubmission()
                deliveryAddressFormManager.clearPendingDeliveryAddressSave()

                Toast.makeText(
                    requireContext(),
                    throwable.message ?: "주문에 실패했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        orderViewModel.cartOrderResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { orders ->
                viewLifecycleOwner.lifecycleScope.launch {
                    getUserId()?.let { userId ->
                        deliveryAddressFormManager.savePendingDeliveryAddressIfNeeded(userId)
                    }

                    Toast.makeText(
                        requireContext(),
                        "장바구니 주문 완료: ${orders.size}개",
                        Toast.LENGTH_SHORT
                    ).show()

                    cartViewModel.loadCartItems()
                    moveToOrderList()
                }
            }

            result.onFailure { throwable ->
                finishOrderSubmission()
                deliveryAddressFormManager.clearPendingDeliveryAddressSave()

                Toast.makeText(
                    requireContext(),
                    throwable.message ?: "장바구니 주문에 실패했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        orderViewModel.subscriptionOrderResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess {
                viewLifecycleOwner.lifecycleScope.launch {
                    getUserId()?.let { userId ->
                        deliveryAddressFormManager.savePendingDeliveryAddressIfNeeded(userId)
                    }

                    Toast.makeText(
                        requireContext(),
                        "구독 주문이 완료되었습니다.",
                        Toast.LENGTH_SHORT
                    ).show()

                    parentFragmentManager.popBackStack()
                }
            }

            result.onFailure { throwable ->
                finishOrderSubmission()
                deliveryAddressFormManager.clearPendingDeliveryAddressSave()

                Toast.makeText(
                    requireContext(),
                    throwable.message ?: "구독 주문에 실패했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun bindSelectedStore() {
        binding.layoutPickupEmptyState.visibility = View.GONE
        binding.layoutPickupStoreDetail.visibility = View.VISIBLE
        binding.tvPickupStoreName.text = selectedStoreName
        binding.tvPickupStoreAddress.text = selectedStoreAddress

        val hasPickupPoint = selectedPickupPointName.isNotBlank() ||
                selectedPickupPointAddress.isNotBlank()
        binding.layoutPickupPointDetail.visibility =
            if (hasPickupPoint) View.VISIBLE else View.GONE
        binding.tvPickupPointName.text = selectedPickupPointName
        binding.tvPickupPointAddress.text = selectedPickupPointAddress
    }

    private fun showPickupEmptyState() {
        binding.layoutPickupEmptyState.visibility = View.VISIBLE
        binding.layoutPickupStoreDetail.visibility = View.GONE
        binding.layoutPickupPointDetail.visibility = View.GONE
        binding.tvStoreInfo.text = "아직 선택된 매장이 없습니다.\n지도에서 픽업할 매장을 선택해주세요."
        binding.tvPickupStoreName.text = ""
        binding.tvPickupStoreAddress.text = ""
        binding.tvPickupPointName.text = ""
        binding.tvPickupPointAddress.text = ""
    }

    private fun moveToOrderList() {
        (activity as? MainActivity)?.moveToOrderListFragment()
            ?: parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, OrderListFragment())
                .commit()
    }

    private fun showSingleReceiveDatePicker() {
        showDatePicker { selectedDate ->
            selectedReceiveDate = selectedDate
            clearSelectedStore()
            updateReceiveDateTitle()
            updateOrderDateArea()
        }
    }

    private fun showSubscriptionStartDatePicker() {
        showDatePicker { selectedDate ->
            subscriptionStartDate = selectedDate
            clearSelectedStore()

            if (subscriptionEndDate != null && subscriptionEndDate!! < selectedDate) {
                subscriptionEndDate = null
            }

            updateSubscriptionDateText()
            updatePaymentSummary()
        }
    }

    private fun showSubscriptionEndDatePicker() {
        showDatePicker { selectedDate ->
            subscriptionEndDate = selectedDate
            clearSelectedStore()
            updateSubscriptionDateText()
            updatePaymentSummary()
        }
    }

    private fun showDatePicker(
        onDateSelected: (String) -> Unit
    ) {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DATE, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedMonth = month + 1
                val selectedDate = String.format(
                    "%04d-%02d-%02d",
                    year,
                    selectedMonth,
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

    private fun isToday(date: String?): Boolean {
        if (date.isNullOrBlank()) return false
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)
            .format(Calendar.getInstance().time)
        return date == today
    }

    private fun updateReceiveDateTitle() {
        val title = if (isPickupReceiveType(selectedReceiveType)) {
            "픽업 날짜"
        } else {
            "배달 날짜"
        }

        val emptyText = if (isPickupReceiveType(selectedReceiveType)) {
            "픽업할 날짜를 선택해주세요."
        } else {
            "배달 받을 날짜를 선택해주세요."
        }

        binding.tvReceiveDateTitle.text = title
        binding.tvSelectedReceiveDate.text = selectedReceiveDate ?: emptyText
    }

    private fun updateSubscriptionDateText() {
        binding.tvSelectedSubscriptionStartDate.text =
            subscriptionStartDate ?: "시작일이 선택되지 않았습니다."

        binding.tvSelectedSubscriptionEndDate.text =
            subscriptionEndDate ?: "종료일이 선택되지 않았습니다."
    }

    private fun updateOrderDateArea() {
        val isSingle = selectedOrderMode == ORDER_MODE_SINGLE

        binding.cardReceiveDate.visibility = if (isSingle) {
            View.VISIBLE
        } else {
            View.GONE
        }

        binding.layoutSubscriptionOrder.visibility = if (isSingle) {
            View.GONE
        } else {
            View.VISIBLE
        }

        binding.btnPay.text = when {
            selectedOrderMode == ORDER_MODE_SINGLE &&
                    isPickupReceiveType(selectedReceiveType) &&
                    selectedStoreId == null -> "\uC9C0\uB3C4\uC5D0\uC11C \uD53D\uC5C5 \uC704\uCE58 \uC120\uD0DD"

            selectedOrderMode == ORDER_MODE_SINGLE &&
                    isPickupReceiveType(selectedReceiveType) -> "\uACB0\uC81C\uD558\uAE30"

            selectedOrderMode == ORDER_MODE_SINGLE &&
                    selectedReceiveType == RECEIVE_TYPE_DELIVERY -> "\uACB0\uC81C\uD558\uAE30"

            selectedOrderMode == ORDER_MODE_SUBSCRIPTION &&
                    selectedReceiveType == RECEIVE_TYPE_PICKUP -> "\uD53D\uC5C5 \uAD6C\uB3C5\uD558\uAE30"

            else -> "\uBC30\uB2EC \uAD6C\uB3C5\uD558\uAE30"
        }

        updatePaymentSummary()
    }

    private fun updateReceiveTypeUi() {
        val isPickup = isPickupReceiveType(selectedReceiveType)

        binding.cardPickupOption.setCardBackgroundColor(
            if (isPickup) SELECTED_CARD_COLOR else UNSELECTED_CARD_COLOR
        )

        binding.cardDeliveryOption.setCardBackgroundColor(
            if (!isPickup) SELECTED_CARD_COLOR else UNSELECTED_CARD_COLOR
        )

        binding.tvPickupTitle.setTextColor(
            if (isPickup) SELECTED_TEXT_COLOR else UNSELECTED_TEXT_COLOR
        )

        binding.tvDeliveryTitle.setTextColor(
            if (!isPickup) SELECTED_TEXT_COLOR else UNSELECTED_TEXT_COLOR
        )
    }

    private fun updatePaymentSummary() {
        val orderCount = if (selectedOrderMode == ORDER_MODE_SUBSCRIPTION) {
            getSubscriptionOrderCount()
        } else {
            1
        }

        val productTotalAmount = orderAmount * orderCount

        val finalDeliveryFee = if (selectedReceiveType == RECEIVE_TYPE_DELIVERY &&
            productTotalAmount < FREE_DELIVERY_THRESHOLD
        ) {
            deliveryFee * orderCount
        } else {
            0
        }

        val deliveryOptionFee = if (productTotalAmount < FREE_DELIVERY_THRESHOLD) {
            deliveryFee * orderCount
        } else {
            0
        }

        val finalPrice = productTotalAmount + finalDeliveryFee

        binding.tvProductAmount.text = if (selectedOrderMode == ORDER_MODE_SUBSCRIPTION) {
            "${formatPrice(orderAmount)}원 x ${orderCount}회 = ${formatPrice(productTotalAmount)}원"
        } else {
            "${formatPrice(productTotalAmount)}원"
        }

        binding.tvDeliveryFee.text = "${formatPrice(finalDeliveryFee)}원"
        binding.tvFinalPaymentPrice.text = "${formatPrice(finalPrice)}원"
        binding.tvDeliveryOptionFee.text = "배송비 ${formatPrice(deliveryOptionFee)}원"

        binding.tvFinalPaymentGuide.text = when {
            selectedOrderMode == ORDER_MODE_SUBSCRIPTION &&
                    selectedReceiveType == RECEIVE_TYPE_PICKUP -> {
                "구독 기간과 선택한 요일 기준으로 총 ${orderCount}회 주문됩니다."
            }

            selectedOrderMode == ORDER_MODE_SUBSCRIPTION &&
                    selectedReceiveType == RECEIVE_TYPE_DELIVERY -> {
                "구독 기간과 선택한 요일 기준으로 총 ${orderCount}회 주문되며, 배송비는 회차별로 계산됩니다."
            }

            isPickupReceiveType(selectedReceiveType) -> {
                "픽업 주문은 배송비가 추가되지 않습니다."
            }

            else -> {
                "배달 주문은 배송비 ${formatPrice(deliveryFee)}원이 포함됩니다."
            }
        }
    }

    private fun calculateFinalPaymentAmount(): Int {
        val orderCount = if (selectedOrderMode == ORDER_MODE_SUBSCRIPTION) {
            getSubscriptionOrderCount()
        } else {
            1
        }
        val productTotalAmount = orderAmount * orderCount
        val finalDeliveryFee = if (selectedReceiveType == RECEIVE_TYPE_DELIVERY &&
            productTotalAmount < FREE_DELIVERY_THRESHOLD
        ) {
            deliveryFee * orderCount
        } else {
            0
        }
        return productTotalAmount + finalDeliveryFee
    }

    private fun getSelectedSubscriptionDays(): List<String> {
        val selectedDays = mutableListOf<String>()

        if (binding.checkMon.isChecked) selectedDays.add("MON")
        if (binding.checkTue.isChecked) selectedDays.add("TUE")
        if (binding.checkWed.isChecked) selectedDays.add("WED")
        if (binding.checkThu.isChecked) selectedDays.add("THU")
        if (binding.checkFri.isChecked) selectedDays.add("FRI")
        if (binding.checkSat.isChecked) selectedDays.add("SAT")
        if (binding.checkSun.isChecked) selectedDays.add("SUN")

        return selectedDays
    }


    private fun getUserId(): Int? {
        val userId = SessionManager(requireContext()).getUser()?.id
        if (userId == null) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
        return userId
    }

    private fun isPickupReceiveType(receiveType: String): Boolean {
        return receiveType == RECEIVE_TYPE_PICKUP || receiveType == RECEIVE_TYPE_PICKUP_POINT
    }

    private fun formatPrice(value: Int): String {
        return DisplayFormatter.formatPrice(value)
    }

    private fun getTomorrowString(): String {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DATE, 1)
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(calendar.time)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getSubscriptionOrderCount(): Int {
        if (subscriptionStartDate == null || subscriptionEndDate == null) {
            return 1
        }

        val selectedDayOfWeeks = getSelectedCalendarDayOfWeeks()

        if (selectedDayOfWeeks.isEmpty()) {
            return 1
        }

        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.KOREA)

            val startDate = dateFormat.parse(subscriptionStartDate!!) ?: return 1
            val endDate = dateFormat.parse(subscriptionEndDate!!) ?: return 1

            val calendar = Calendar.getInstance()
            calendar.time = startDate

            val endCalendar = Calendar.getInstance()
            endCalendar.time = endDate

            var count = 0

            while (!calendar.after(endCalendar)) {
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

                if (selectedDayOfWeeks.contains(dayOfWeek)) {
                    count++
                }

                calendar.add(Calendar.DATE, 1)
            }

            if (count <= 0) 1 else count
        } catch (e: Exception) {
            1
        }
    }

    private fun getSelectedCalendarDayOfWeeks(): List<Int> {
        val selectedDays = mutableListOf<Int>()

        if (binding.checkSun.isChecked) selectedDays.add(Calendar.SUNDAY)
        if (binding.checkMon.isChecked) selectedDays.add(Calendar.MONDAY)
        if (binding.checkTue.isChecked) selectedDays.add(Calendar.TUESDAY)
        if (binding.checkWed.isChecked) selectedDays.add(Calendar.WEDNESDAY)
        if (binding.checkThu.isChecked) selectedDays.add(Calendar.THURSDAY)
        if (binding.checkFri.isChecked) selectedDays.add(Calendar.FRIDAY)
        if (binding.checkSat.isChecked) selectedDays.add(Calendar.SATURDAY)

        return selectedDays
    }

    private fun clearSelectedStore() {
        selectedStoreId = null
        selectedStoreName = ""
        selectedStoreAddress = ""
        selectedStoreTotalCount = 0
        selectedStoreRemainCount = 0
        selectedPickupPointId = null
        selectedPickupPointName = ""
        selectedPickupPointAddress = ""

        showPickupEmptyState()
    }

    companion object {
        private const val ARG_ORDER_AMOUNT = "orderAmount"
        private const val ARG_ORDER_TYPE = "orderType"
        private const val ARG_MEAL_ID = "mealId"
        private const val ARG_QUANTITY = "quantity"
        private const val ARG_ORDER_ITEM_TEXT = "orderItemText"
        private const val ARG_TOTAL_QUANTITY = "totalQuantity"

        private const val ORDER_MODE_SINGLE = "SINGLE"
        private const val ORDER_MODE_SUBSCRIPTION = "SUBSCRIPTION"

        const val ORDER_TYPE_DIRECT = "DIRECT"
        const val ORDER_TYPE_CART = "CART"

        private const val RECEIVE_TYPE_PICKUP = "PICKUP"
        private const val RECEIVE_TYPE_PICKUP_POINT = "PICKUP_POINT"
        private const val RECEIVE_TYPE_DELIVERY = "DELIVERY"
        private const val FREE_DELIVERY_THRESHOLD = 30000
        private const val DELIVERY_RADIUS_METER = 3_000f

        private const val SELECTED_CARD_COLOR = 0xFFEAF4EC.toInt()
        private const val UNSELECTED_CARD_COLOR = 0xFFFFFFFF.toInt()
        private const val SELECTED_TEXT_COLOR = 0xFF24563A.toInt()
        private const val UNSELECTED_TEXT_COLOR = 0xFF686D69.toInt()

        fun newDirectOrderInstance(
            mealId: Long,
            quantity: Int,
            orderItemText: String,
            orderAmount: Int
        ): OrderFragment {
            return OrderFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDER_TYPE, ORDER_TYPE_DIRECT)
                    putLong(ARG_MEAL_ID, mealId)
                    putInt(ARG_QUANTITY, quantity)
                    putString(ARG_ORDER_ITEM_TEXT, orderItemText)
                    putInt(ARG_TOTAL_QUANTITY, quantity)
                    putInt(ARG_ORDER_AMOUNT, orderAmount)
                }
            }
        }

        fun newCartOrderInstance(
            orderItemText: String,
            totalQuantity: Int,
            orderAmount: Int
        ): OrderFragment {
            return OrderFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDER_TYPE, ORDER_TYPE_CART)
                    putString(ARG_ORDER_ITEM_TEXT, orderItemText)
                    putInt(ARG_TOTAL_QUANTITY, totalQuantity)
                    putInt(ARG_ORDER_AMOUNT, orderAmount)
                }
            }
        }
    }
}
