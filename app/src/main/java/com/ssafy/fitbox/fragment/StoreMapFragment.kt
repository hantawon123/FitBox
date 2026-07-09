package com.ssafy.fitbox.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.ssafy.fitbox.R
import com.ssafy.fitbox.databinding.FragmentStoreMapBinding
import com.ssafy.fitbox.dto.PickupPoint
import com.ssafy.fitbox.dto.Store
import com.ssafy.fitbox.viewmodel.StoreViewModel
import java.util.Locale
import kotlin.math.roundToInt

class StoreMapFragment : Fragment() {

    private var _binding: FragmentStoreMapBinding? = null
    private val binding get() = _binding!!

    private val storeViewModel: StoreViewModel by viewModels()

    private var googleMap: GoogleMap? = null
    private var selectedStore: Store? = null
    private var selectedPickupPoint: PickupPoint? = null
    private var stores: List<Store> = emptyList()
    private var pickupPoints: List<PickupPoint> = emptyList()
    private var currentLocation: Location? = null
    private var isShowingPickupPoints: Boolean = false
    private var mapFilter: MapFilter = MapFilter.ALL
    private var lastPickupPointBoundsKey: String = ""

    private var isMapFilterExpanded: Boolean = false

    private var orderMode: String = ""
    private var pickupDate: String = ""

    private var dateStart: String = ""
    private var dateEnd: String = ""
    private var mon: Boolean = false
    private var tue: Boolean = false
    private var wed: Boolean = false
    private var thu: Boolean = false
    private var fri: Boolean = false
    private var sat: Boolean = false
    private var sun: Boolean = false

    private var deliveryLatitude: Double = 0.0
    private var deliveryLongitude: Double = 0.0

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isAdded || _binding == null) return@registerForActivityResult
            if (isGranted) {
                enableMyLocation()
            } else {
                val context = context ?: return@registerForActivityResult
                Toast.makeText(
                    context,
                    "현재 위치 권한이 없어 기본 위치로 지도를 표시합니다.",
                    Toast.LENGTH_SHORT
                ).show()
                moveCameraToDefaultLocation()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let { bundle ->
            orderMode = bundle.getString(ARG_ORDER_MODE).orEmpty()
            pickupDate = bundle.getString(ARG_PICKUP_DATE).orEmpty()

            dateStart = bundle.getString(ARG_DATE_START).orEmpty()
            dateEnd = bundle.getString(ARG_DATE_END).orEmpty()
            mon = bundle.getBoolean(ARG_MON, false)
            tue = bundle.getBoolean(ARG_TUE, false)
            wed = bundle.getBoolean(ARG_WED, false)
            thu = bundle.getBoolean(ARG_THU, false)
            fri = bundle.getBoolean(ARG_FRI, false)
            sat = bundle.getBoolean(ARG_SAT, false)
            sun = bundle.getBoolean(ARG_SUN, false)

            deliveryLatitude = bundle.getDouble(ARG_DELIVERY_LATITUDE, 0.0)
            deliveryLongitude = bundle.getDouble(ARG_DELIVERY_LONGITUDE, 0.0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStoreMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        initMap()
        initClickEvents()
        updateMapFilterVisibility()
        observeStores()
        observePickupPoints()
        loadStoresByOrderMode()
    }

    private fun loadStoresByOrderMode() {
        when (orderMode) {
            ORDER_MODE_SINGLE -> {
                if (pickupDate.isBlank()) {
                    Toast.makeText(
                        requireContext(),
                        "픽업 날짜 정보가 없습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                    parentFragmentManager.popBackStack()
                    return
                }

                mapFilter = MapFilter.STORES
                updateMapFilterFabText()
                updateMapFilterButtonStyles()
                Log.d(TAG, "single pickupDate = $pickupDate")
                storeViewModel.loadStores(pickupDate)
            }

            ORDER_MODE_SUBSCRIPTION -> {
                if (dateStart.isBlank() || dateEnd.isBlank()) {
                    Toast.makeText(
                        requireContext(),
                        "구독 날짜 정보가 없습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                    parentFragmentManager.popBackStack()
                    return
                }

                if (!mon && !tue && !wed && !thu && !fri && !sat && !sun) {
                    Toast.makeText(
                        requireContext(),
                        "구독 요일 정보가 없습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                    parentFragmentManager.popBackStack()
                    return
                }

                Log.d(
                    TAG,
                    "subscription dateStart=$dateStart, dateEnd=$dateEnd, mon=$mon, tue=$tue, wed=$wed, thu=$thu, fri=$fri, sat=$sat, sun=$sun"
                )

                storeViewModel.loadStoresForSubscription(
                    dateStart = dateStart,
                    dateEnd = dateEnd,
                    mon = mon,
                    tue = tue,
                    wed = wed,
                    thu = thu,
                    fri = fri,
                    sat = sat,
                    sun = sun
                )
            }

            ORDER_MODE_DELIVERY -> {
                if (pickupDate.isBlank()) {
                    Toast.makeText(
                        requireContext(),
                        "배달 날짜 정보가 없습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                    parentFragmentManager.popBackStack()
                    return
                }

                if (deliveryLatitude == 0.0 || deliveryLongitude == 0.0) {
                    Toast.makeText(
                        requireContext(),
                        "배송지 위치 정보가 없습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                    parentFragmentManager.popBackStack()
                    return
                }

                mapFilter = MapFilter.STORES
                storeViewModel.loadStores(pickupDate)
            }

            else -> {
                Toast.makeText(
                    requireContext(),
                    "주문 유형 정보가 없습니다.",
                    Toast.LENGTH_SHORT
                ).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun initMap() {
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.googleMap) as SupportMapFragment

        mapFragment.getMapAsync { map ->
            googleMap = map

            googleMap?.uiSettings?.isZoomControlsEnabled = true
            googleMap?.uiSettings?.isMyLocationButtonEnabled = true
            googleMap?.setOnCameraIdleListener {
                if (shouldLoadPickupPoints()) {
                    loadPickupPointsForVisibleRegion()
                }
            }
            googleMap?.setOnMapClickListener {
                clearMapSelection()
            }

            checkLocationPermission()

            if (stores.isNotEmpty()) {
                renderMarkers(moveCameraToFirstStore = true)
            }
        }
    }

    private fun initClickEvents() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnMapFilterFab.setOnClickListener {
            toggleMapFilterMenu()
        }

        binding.btnShowStoresOnly.setOnClickListener {
            applyMapFilter(MapFilter.STORES)
        }

        binding.btnShowPickupOnly.setOnClickListener {
            applyMapFilter(MapFilter.PICKUP_POINTS)
        }

        binding.btnShowAllMarkers.setOnClickListener {
            applyMapFilter(MapFilter.ALL)
        }

        binding.btnChooseStore.setOnClickListener {
            val store = selectedStore

            if (!validateSelectedStore(store)) {
                return@setOnClickListener
            }

            sendStoreResult(store = store!!, pickupPoint = null)
        }

        binding.btnShowPickupPoints.setOnClickListener {
            if (!validateSelectedStore(selectedStore)) {
                return@setOnClickListener
            }

            enterPickupPointMode()
        }

        binding.btnChoosePickupPoint.setOnClickListener {
            val pickupPoint = selectedPickupPoint

            if (pickupPoint == null) {
                Toast.makeText(
                    requireContext(),
                    "픽업 포인트를 먼저 선택해주세요.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val orderStore = selectedStore
            if (!validateSelectedStore(orderStore)) {
                return@setOnClickListener
            }

            if (!validateSelectedPickupPoint(pickupPoint)) {
                return@setOnClickListener
            }

            sendStoreResult(store = orderStore!!, pickupPoint = pickupPoint)
        }
    }

    private fun updateMapFilterVisibility() {
        if (orderMode == ORDER_MODE_DELIVERY || orderMode == ORDER_MODE_SINGLE) {
            binding.btnMapFilterFab.visibility = View.GONE
            binding.layoutMapFilterOptions.visibility = View.GONE
            isMapFilterExpanded = false
        } else {
            binding.btnMapFilterFab.visibility = View.VISIBLE
        }
    }

    private fun observeStores() {
        storeViewModel.stores.observe(viewLifecycleOwner) { storeList ->
            stores = if (orderMode == ORDER_MODE_DELIVERY) {
                filterStoresWithinDeliveryRadius(storeList)
            } else {
                storeList
            }

            if (orderMode == ORDER_MODE_DELIVERY && stores.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "배송지 3km 이내 매장이 없습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }

            renderMarkers(moveCameraToFirstStore = true)
        }

        storeViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun filterStoresWithinDeliveryRadius(storeList: List<Store>): List<Store> {
        val deliveryLocation = LatLng(deliveryLatitude, deliveryLongitude)

        return storeList.filter { store ->
            distanceMeter(
                from = deliveryLocation,
                to = LatLng(store.latitude, store.longitude)
            ) <= DELIVERY_RADIUS_METER
        }.sortedBy { store ->
            distanceMeter(
                from = deliveryLocation,
                to = LatLng(store.latitude, store.longitude)
            )
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

    private fun observePickupPoints() {
        storeViewModel.pickupPoints.observe(viewLifecycleOwner) { pickupPoints ->
            this.pickupPoints = pickupPoints
            renderMarkers(moveCameraToFirstStore = false)
        }
    }

    private fun enterPickupPointMode() {
        val store = selectedStore
        if (store == null) {
            Toast.makeText(
                requireContext(),
                "매장을 먼저 선택해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        isShowingPickupPoints = true
        mapFilter = MapFilter.PICKUP_POINTS
        updateMapFilterFabText()
        updateMapFilterButtonStyles()
        collapseMapFilterMenu()

        selectedPickupPoint = null
        binding.cardStoreInfo.visibility = View.GONE
        lastPickupPointBoundsKey = ""
        renderMarkers(moveCameraToFirstStore = false)

        val map = googleMap
        val location = currentLocation
        if (map != null && location != null) {
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(location.latitude, location.longitude),
                    14f
                )
            )
        } else {
            loadPickupPointsForVisibleRegion()
        }
    }

    private fun loadPickupPointsForVisibleRegion(force: Boolean = false) {
        val bounds = googleMap?.projection?.visibleRegion?.latLngBounds ?: return
        val south = bounds.southwest.latitude
        val north = bounds.northeast.latitude
        val west = bounds.southwest.longitude
        val east = bounds.northeast.longitude
        val boundsKey = listOf(south, north, west, east)
            .joinToString("|") { "%.4f".format(Locale.US, it) }

        if (!force && boundsKey == lastPickupPointBoundsKey) {
            return
        }

        lastPickupPointBoundsKey = boundsKey
        storeViewModel.loadPickupPointsInBounds(
            south = south,
            north = north,
            west = west,
            east = east,
            pickupDate = pickupDate
        )
    }

    private fun shouldLoadPickupPoints(): Boolean {
        return orderMode == ORDER_MODE_SINGLE &&
            mapFilter == MapFilter.PICKUP_POINTS
    }

    private fun getAvailablePickupPoints(pickupPoints: List<PickupPoint>): List<PickupPoint> {
        return pickupPoints.filter { pickupPoint -> pickupPoint.remainCnt > 0 }
    }

    private fun renderMarkers(moveCameraToFirstStore: Boolean) {
        val map = googleMap ?: return

        map.clear()
        isShowingPickupPoints = mapFilter != MapFilter.STORES

        if (orderMode == ORDER_MODE_DELIVERY) {
            val deliveryPosition = LatLng(deliveryLatitude, deliveryLongitude)

            map.addMarker(
                MarkerOptions()
                    .position(deliveryPosition)
                    .title("주문 배송지")
                    .snippet("선택한 배송 주소 기준 위치입니다.")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
            )
        }

        if (mapFilter == MapFilter.STORES || mapFilter == MapFilter.ALL) {
            stores.forEach { store ->
                val position = LatLng(store.latitude, store.longitude)
                val marker = map.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(store.name)
                        .snippet(store.address)
                )
                marker?.tag = store
            }
        }

        if (mapFilter == MapFilter.PICKUP_POINTS || mapFilter == MapFilter.ALL) {
            getAvailablePickupPoints(pickupPoints).forEach { pickupPoint ->
                val position = LatLng(pickupPoint.latitude, pickupPoint.longitude)
                val marker = map.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(pickupPoint.name)
                        .snippet("Remain ${pickupPoint.remainCnt} / Total ${pickupPoint.totalCnt}")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                )
                marker?.tag = pickupPoint
            }
        }

        map.setOnMarkerClickListener { marker ->
            when (val tag = marker.tag) {
                is Store -> {
                    selectedStore = tag
                    selectedPickupPoint = null
                    bindStoreInfo(tag)
                }
                is PickupPoint -> {
                    selectedPickupPoint = tag
                    bindPickupPointInfo(tag)
                }
                else -> return@setOnMarkerClickListener true
            }
            true
        }

        if (moveCameraToFirstStore && stores.isNotEmpty()) {
            if (orderMode == ORDER_MODE_DELIVERY) {
                val deliveryPosition = LatLng(deliveryLatitude, deliveryLongitude)
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(deliveryPosition, 13f)
                )
            } else {
                val firstStore = stores.first()
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(firstStore.latitude, firstStore.longitude),
                        13f
                    )
                )
            }
        }
    }

    private fun formatDistanceFromDeliveryAddress(
        latitude: Double,
        longitude: Double
    ): String {
        val result = FloatArray(1)

        Location.distanceBetween(
            deliveryLatitude,
            deliveryLongitude,
            latitude,
            longitude,
            result
        )

        val meter = result.first()

        return if (meter < 1000f) {
            "${meter.roundToInt()}m"
        } else {
            String.format(Locale.KOREA, "%.1fkm", meter / 1000f)
        }
    }

    private fun bindStoreInfo(store: Store) {
        binding.cardStoreInfo.visibility = View.VISIBLE
        binding.tvStoreName.text = store.name
        binding.tvStoreAddress.text = store.address

        binding.tvDistance.text = if (orderMode == ORDER_MODE_DELIVERY) {
            "배송지에서 ${formatDistanceFromDeliveryAddress(store.latitude, store.longitude)}"
        } else {
            "현재 위치에서 ${formatDistanceTo(store.latitude, store.longitude)}"
        }

        binding.tvStoreStock.text = when (orderMode) {
            ORDER_MODE_DELIVERY -> "배송 담당 매장으로 선택할 수 있습니다."
            ORDER_MODE_SUBSCRIPTION -> "구독 기간: $dateStart ~ $dateEnd"
            else -> "선택 날짜: $pickupDate"
        }

        binding.btnChooseStore.text = when (orderMode) {
            ORDER_MODE_DELIVERY -> "이 매장을 배송 담당 매장으로 선택"
            ORDER_MODE_SUBSCRIPTION -> "이 매장 선택"
            else -> "이 매장에서 픽업하기"
        }
        binding.btnChooseStore.visibility = View.VISIBLE
        binding.btnShowPickupPoints.visibility =
            if (orderMode == ORDER_MODE_SINGLE) View.VISIBLE else View.GONE
        binding.btnShowPickupPoints.text = "픽업 포인트에서 픽업하기"
        binding.btnChoosePickupPoint.visibility = View.GONE
    }

    private fun bindPickupPointInfo(pickupPoint: PickupPoint) {
        val orderStore = selectedStore
        binding.cardStoreInfo.visibility = View.VISIBLE
        binding.tvStoreName.text = pickupPoint.name
        binding.tvStoreAddress.text = pickupPoint.address
        binding.tvDistance.text =
            "현재 위치에서 ${formatDistanceTo(pickupPoint.latitude, pickupPoint.longitude)}"
        binding.tvStoreStock.text = orderStore?.let { store ->
            "주문 매장: ${store.name}\n선택 날짜: $pickupDate\n남은 칸 ${pickupPoint.remainCnt} / 전체 ${pickupPoint.totalCnt}"
        } ?: "선택 날짜: $pickupDate\n남은 칸 ${pickupPoint.remainCnt} / 전체 ${pickupPoint.totalCnt}"

        binding.btnChooseStore.visibility = View.GONE
        binding.btnShowPickupPoints.visibility = View.GONE
        binding.btnChoosePickupPoint.visibility = View.VISIBLE
    }

    private fun validateSelectedStore(store: Store?): Boolean {
        if (store == null) {
            Toast.makeText(
                requireContext(),
                "매장을 먼저 선택해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        return true
    }

    private fun validateSelectedPickupPoint(pickupPoint: PickupPoint): Boolean {
        if (pickupPoint.remainCnt <= 0) {
            Toast.makeText(
                requireContext(),
                "선택한 날짜에 남은 픽업 포인트 칸이 없습니다.",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        return true
    }

    private fun sendStoreResult(
        store: Store,
        pickupPoint: PickupPoint?
    ) {
        parentFragmentManager.setFragmentResult(
            REQUEST_KEY_STORE,
            Bundle().apply {
                putLong(KEY_STORE_ID, store.id)
                putString(KEY_STORE_NAME, store.name)
                putString(KEY_STORE_ADDRESS, store.address)
                putInt(KEY_STORE_TOTAL_COUNT, store.totalCnt)
                putInt(KEY_STORE_REMAIN_COUNT, store.remainCnt)
                putDouble(KEY_STORE_LATITUDE, store.latitude)
                putDouble(KEY_STORE_LONGITUDE, store.longitude)
                putString(
                    KEY_RECEIVE_TYPE,
                    when {
                        orderMode == ORDER_MODE_DELIVERY -> RECEIVE_TYPE_DELIVERY
                        pickupPoint == null -> RECEIVE_TYPE_PICKUP
                        else -> RECEIVE_TYPE_PICKUP_POINT
                    }
                )
                pickupPoint?.let { point ->
                    putLong(KEY_PICKUP_POINT_ID, point.id)
                    putString(KEY_PICKUP_POINT_NAME, point.name)
                    putString(KEY_PICKUP_POINT_ADDRESS, point.address)
                }
            }
        )

        parentFragmentManager.popBackStack()
    }

    private fun formatDistanceTo(latitude: Double, longitude: Double): String {
        val location = currentLocation ?: return "거리 정보 없음"
        val result = FloatArray(1)
        Location.distanceBetween(
            location.latitude,
            location.longitude,
            latitude,
            longitude,
            result
        )

        val meter = result.first()
        return if (meter < 1000f) {
            "${meter.roundToInt()}m"
        } else {
            String.format("%.1fkm", meter / 1000f)
        }
    }

    private fun checkLocationPermission() {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        val context = context ?: return

        if (ContextCompat.checkSelfPermission(context, permission)
            == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        } else {
            locationPermissionLauncher.launch(permission)
        }
    }

    private fun enableMyLocation() {
        val map = googleMap ?: return
        val context = context ?: return

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        map.isMyLocationEnabled = true

        val fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(context.applicationContext)

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (!isAdded || _binding == null) return@addOnSuccessListener
            if (location != null) {
                currentLocation = location

                if (orderMode != ORDER_MODE_DELIVERY) {
                    val myPosition = LatLng(location.latitude, location.longitude)
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(myPosition, 14f)
                    )
                }

                if (isShowingPickupPoints) {
                    return@addOnSuccessListener
                }

                selectedPickupPoint?.let(::bindPickupPointInfo)
                if (selectedPickupPoint == null) {
                    selectedStore?.let(::bindStoreInfo)
                }
            } else {
                if (orderMode == ORDER_MODE_DELIVERY) {
                    googleMap?.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(deliveryLatitude, deliveryLongitude),
                            14f
                        )
                    )
                } else {
                    moveCameraToDefaultLocation()
                }
            }
        }
    }

    private fun moveCameraToDefaultLocation() {
        val defaultPosition = LatLng(36.1284, 128.3356)
        googleMap?.moveCamera(
            CameraUpdateFactory.newLatLngZoom(defaultPosition, 13f)
        )
    }

    private fun toggleMapFilterMenu() {
        if (isMapFilterExpanded) {
            collapseMapFilterMenu()
        } else {
            expandMapFilterMenu()
        }
    }

    private fun expandMapFilterMenu() {
        isMapFilterExpanded = true

        binding.layoutMapFilterOptions.visibility = View.VISIBLE
        binding.layoutMapFilterOptions.alpha = 0f
        binding.layoutMapFilterOptions.translationY = 24f

        binding.layoutMapFilterOptions.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(160L)
            .start()
    }

    private fun collapseMapFilterMenu() {
        if (!isMapFilterExpanded &&
            binding.layoutMapFilterOptions.visibility != View.VISIBLE
        ) {
            return
        }

        isMapFilterExpanded = false

        binding.layoutMapFilterOptions.animate()
            .alpha(0f)
            .translationY(24f)
            .setDuration(140L)
            .withEndAction {
                binding.layoutMapFilterOptions.visibility = View.GONE
            }
            .start()
    }

    private fun applyMapFilter(filter: MapFilter) {
        mapFilter = filter

        clearMapSelection()

        renderMarkers(moveCameraToFirstStore = false)

        if (shouldLoadPickupPoints()) {
            loadPickupPointsForVisibleRegion(force = true)
        }

        updateMapFilterFabText()
        updateMapFilterButtonStyles()
        collapseMapFilterMenu()
    }

    private fun clearMapSelection() {
        if (orderMode != ORDER_MODE_SINGLE || mapFilter != MapFilter.PICKUP_POINTS) {
            selectedStore = null
        }
        selectedPickupPoint = null
        binding.cardStoreInfo.visibility = View.GONE
    }

    private fun updateMapFilterFabText() {
        binding.btnMapFilterFab.text = when (mapFilter) {
            MapFilter.STORES -> "매장"
            MapFilter.PICKUP_POINTS -> "픽업"
            MapFilter.ALL -> "전체"
        }
    }

    private fun updateMapFilterButtonStyles() {
        setMapFilterButtonStyle(
            button = binding.btnShowStoresOnly,
            selected = mapFilter == MapFilter.STORES
        )
        setMapFilterButtonStyle(
            button = binding.btnShowPickupOnly,
            selected = mapFilter == MapFilter.PICKUP_POINTS
        )
        setMapFilterButtonStyle(
            button = binding.btnShowAllMarkers,
            selected = mapFilter == MapFilter.ALL
        )
    }

    private fun setMapFilterButtonStyle(
        button: com.google.android.material.button.MaterialButton,
        selected: Boolean
    ) {
        val fern = ContextCompat.getColor(requireContext(), R.color.mypage_fern)
        val white = ContextCompat.getColor(requireContext(), android.R.color.white)
        val warmWhite = ContextCompat.getColor(requireContext(), R.color.mypage_warm_white)

        if (selected) {
            button.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    fern
                )
            button.strokeColor =
                android.content.res.ColorStateList.valueOf(
                    fern
                )
            button.strokeWidth = dpToPx(1)
            button.setTextColor(warmWhite)
        } else {
            button.backgroundTintList =
                android.content.res.ColorStateList.valueOf(
                    white
                )
            button.strokeColor =
                android.content.res.ColorStateList.valueOf(
                    fern
                )
            button.strokeWidth = dpToPx(1)
            button.setTextColor(fern)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroyView() {
        googleMap = null
        super.onDestroyView()
        _binding = null
    }

    private enum class MapFilter {
        STORES,
        PICKUP_POINTS,
        ALL
    }

    companion object {
        const val REQUEST_KEY_STORE = "selectedStore"
        const val KEY_STORE_ID = "storeId"
        const val KEY_STORE_NAME = "storeName"
        const val KEY_STORE_ADDRESS = "storeAddress"
        const val KEY_STORE_TOTAL_COUNT = "storeTotalCount"
        const val KEY_STORE_REMAIN_COUNT = "storeRemainCount"
        const val KEY_RECEIVE_TYPE = "receiveType"
        const val KEY_PICKUP_POINT_ID = "pickupPointId"
        const val KEY_PICKUP_POINT_NAME = "pickupPointName"
        const val KEY_PICKUP_POINT_ADDRESS = "pickupPointAddress"

        const val KEY_STORE_LATITUDE = "storeLatitude"
        const val KEY_STORE_LONGITUDE = "storeLongitude"

        private const val ARG_ORDER_MODE = "orderMode"
        private const val ARG_PICKUP_DATE = "pickupDate"
        private const val ARG_DATE_START = "dateStart"
        private const val ARG_DATE_END = "dateEnd"
        private const val ARG_MON = "mon"
        private const val ARG_TUE = "tue"
        private const val ARG_WED = "wed"
        private const val ARG_THU = "thu"
        private const val ARG_FRI = "fri"
        private const val ARG_SAT = "sat"
        private const val ARG_SUN = "sun"

        private const val ORDER_MODE_SINGLE = "SINGLE"
        private const val ORDER_MODE_SUBSCRIPTION = "SUBSCRIPTION"
        private const val RECEIVE_TYPE_PICKUP = "PICKUP"
        private const val RECEIVE_TYPE_PICKUP_POINT = "PICKUP_POINT"
        private const val TAG = "StoreMapFragment"

        private const val ORDER_MODE_DELIVERY = "DELIVERY"
        private const val RECEIVE_TYPE_DELIVERY = "DELIVERY"

        private const val ARG_DELIVERY_LATITUDE = "deliveryLatitude"
        private const val ARG_DELIVERY_LONGITUDE = "deliveryLongitude"
        private const val DELIVERY_RADIUS_METER = 3_000f

        fun newSingleInstance(pickupDate: String): StoreMapFragment {
            return StoreMapFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDER_MODE, ORDER_MODE_SINGLE)
                    putString(ARG_PICKUP_DATE, pickupDate)
                }
            }
        }

        fun newDeliveryInstance(
            deliveryDate: String,
            deliveryLatitude: Double,
            deliveryLongitude: Double
        ): StoreMapFragment {
            return StoreMapFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDER_MODE, ORDER_MODE_DELIVERY)
                    putString(ARG_PICKUP_DATE, deliveryDate)
                    putDouble(ARG_DELIVERY_LATITUDE, deliveryLatitude)
                    putDouble(ARG_DELIVERY_LONGITUDE, deliveryLongitude)
                }
            }
        }

        fun newSubscriptionInstance(
            dateStart: String,
            dateEnd: String,
            mon: Boolean,
            tue: Boolean,
            wed: Boolean,
            thu: Boolean,
            fri: Boolean,
            sat: Boolean,
            sun: Boolean
        ): StoreMapFragment {
            return StoreMapFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDER_MODE, ORDER_MODE_SUBSCRIPTION)
                    putString(ARG_DATE_START, dateStart)
                    putString(ARG_DATE_END, dateEnd)
                    putBoolean(ARG_MON, mon)
                    putBoolean(ARG_TUE, tue)
                    putBoolean(ARG_WED, wed)
                    putBoolean(ARG_THU, thu)
                    putBoolean(ARG_FRI, fri)
                    putBoolean(ARG_SAT, sat)
                    putBoolean(ARG_SUN, sun)
                }
            }
        }
    }
}
