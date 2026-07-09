package com.ssafy.fitbox.fragment

import android.os.Bundle
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ssafy.fitbox.databinding.FragmentOrderDetailBinding
import com.ssafy.fitbox.network.response.OrderResponse
import com.ssafy.fitbox.util.DisplayFormatter
import com.ssafy.fitbox.util.FavoriteMealStore
import com.ssafy.fitbox.util.SessionManager
import com.ssafy.fitbox.repository.OrderRepository
import kotlinx.coroutines.launch
import java.nio.charset.Charset

class OrderDetailFragment : Fragment() {

    private var _binding: FragmentOrderDetailBinding? = null
    private val binding get() = _binding!!

    private val gson = Gson()
    private val orderRepository = OrderRepository()
    private var orders: List<OrderResponse> = emptyList()
    private var nfcAdapter: NfcAdapter? = null
    private var isWaitingForNfc = false
    private var nfcGuideDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val json = arguments?.getString(ARG_ORDERS_JSON).orEmpty()
        val type = object : TypeToken<List<OrderResponse>>() {}.type
        orders = runCatching {
            gson.fromJson<List<OrderResponse>>(json, type)
        }.getOrNull().orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        initClickEvents()
        nfcAdapter = NfcAdapter.getDefaultAdapter(requireContext())
        bindOrderDetail()
    }

    private fun initClickEvents() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnAddFavorite.setOnClickListener {
            handleAddFavoriteClick()
        }

        binding.btnNfcPickup.setOnClickListener {
            startNfcScan()
        }
    }

    private fun startNfcScan() {
        val order = orders.firstOrNull() ?: return
        if (order.lockerNumber.isNullOrBlank()) {
            showResultDialog("픽업 준비 중", "관리자가 아직 사물함을 할당하지 않았습니다.")
            return
        }
        val adapter = nfcAdapter
        if (adapter == null) {
            showResultDialog("NFC 사용 불가", "이 기기는 NFC를 지원하지 않습니다.")
            return
        }
        if (!adapter.isEnabled) {
            showResultDialog("NFC가 꺼져 있습니다", "기기 설정에서 NFC를 켜주세요.")
            return
        }

        isWaitingForNfc = true
        enableNfcReader()
        nfcGuideDialog?.dismiss()
        nfcGuideDialog = AlertDialog.Builder(requireContext())
            .setTitle("NFC 태그")
            .setMessage("픽업 포인트의 NFC 태그에 휴대폰을 가까이 대주세요.")
            .setNegativeButton("취소") { _, _ ->
                isWaitingForNfc = false
                disableNfcReader()
            }
            .setOnCancelListener {
                isWaitingForNfc = false
                disableNfcReader()
            }
            .show()
    }

    private fun enableNfcReader() {
        val activity = activity ?: return
        nfcAdapter?.enableReaderMode(
            activity,
            ::handleNfcTag,
            NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V,
            null
        )
    }

    private fun disableNfcReader() {
        val activity = activity ?: return
        nfcAdapter?.disableReaderMode(activity)
    }

    private fun handleNfcTag(tag: Tag) {
        if (!isWaitingForNfc) return
        val payload = readNfcText(tag)
        val activity = activity ?: return
        activity.runOnUiThread {
            if (!isAdded || _binding == null) return@runOnUiThread
            isWaitingForNfc = false
            disableNfcReader()
            nfcGuideDialog?.dismiss()
            val parsed = parseNfcPayload(payload)
            if (parsed == null) {
                showResultDialog("픽업 실패", "주문정보와 일치하지 않습니다.")
            } else {
                verifyNfcPickup(parsed.first, parsed.second)
            }
        }
    }

    private fun readNfcText(tag: Tag): String? {
        val message = android.nfc.tech.Ndef.get(tag)
            ?.cachedNdefMessage
            ?: return null
        return message.records
            .mapNotNull(::decodeNdefRecord)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .joinToString("|")
            .ifBlank { null }
    }

    private fun decodeNdefRecord(record: NdefRecord): String? {
        if (record.tnf == NdefRecord.TNF_WELL_KNOWN &&
            record.type.contentEquals(NdefRecord.RTD_TEXT)
        ) {
            val payload = record.payload ?: return null
            if (payload.isEmpty()) return null
            val status = payload[0].toInt()
            val languageLength = status and 0x3F
            val charset = if (status and 0x80 == 0) Charsets.UTF_8 else Charset.forName("UTF-16")
            return payload.copyOfRange(1 + languageLength, payload.size)
                .toString(charset)
                .trim()
        }
        return record.payload?.toString(Charsets.UTF_8)?.trim()
    }

    private fun parseNfcPayload(payload: String?): Pair<String, String>? {
        val parts = payload
            ?.split('|', '\n')
            ?.map { normalizeNfcValue(it) }
            ?.filter { it.isNotBlank() }
            .orEmpty()
        val lockerNumber = parts.getOrNull(1)
            ?.removeSuffix("사물함")
            ?.trim()
            ?.removeSuffix("번")
            ?.trim()
        return if (parts.isNotEmpty() && !lockerNumber.isNullOrBlank()) {
            parts[0] to lockerNumber
        } else {
            null
        }
    }

    private fun normalizeNfcValue(value: String): String {
        return value
            .replace("\u0000", "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun verifyNfcPickup(pickupPointName: String, lockerNumber: String) {
        val order = orders.firstOrNull() ?: return
        val userId = SessionManager(requireContext()).getUser()?.id ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            orderRepository.completeNfcPickup(
                orderId = order.orderId,
                userId = userId,
                pickupPointName = pickupPointName,
                lockerNumber = lockerNumber
            ).onSuccess { updatedOrder ->
                orders = orders.map {
                    if (it.orderId == updatedOrder.orderId) updatedOrder else it
                }
                bindOrderDetail()
                showResultDialog("픽업 완료", "사물함이 열렸습니다.")
            }.onFailure {
                showResultDialog("픽업 실패", "주문정보와 일치하지 않습니다.")
            }
        }
    }

    private fun showResultDialog(title: String, message: String) {
        val context = context ?: return
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("확인", null)
            .show()
    }

    private fun bindOrderDetail() {
        val order = orders.firstOrNull() ?: return

        binding.tvOrderNumber.text = getOrderNumberText(order)
        binding.tvOrderType.text = getOrderTypeText(order)
        if (order.subscriptionStatus == STATUS_CANCELED) {
            binding.tvOrderType.setTextColor(0xFFB91C1C.toInt())
        }

        binding.tvMealName.text = getMealNameText()
        binding.tvQuantity.text = getQuantityText()
        binding.tvTotalPrice.text = "${formatPrice(orders.sumOf { it.totalPrice })}원"
        binding.tvReceiveType.text = getReceiveTypeText(order.receiveType)
        binding.tvOrderStatus.text = getOrderStatusText(order)
        binding.layoutOrderStatus.visibility =
            if (order.orderType == ORDER_TYPE_SINGLE) View.VISIBLE else View.GONE
        binding.tvReceiveDate.text = getReceiveDateText(order)
        binding.tvOrderTime.text = order.orderTime

        bindReceivePlace(order)
        bindPickupDetailUi(order)
        bindReceiveInfoBlock(order)
        bindOrderItems()
        bindFavoriteUi()
        bindNfcPickupUi(order)
    }

    private fun bindFavoriteUi() {
        val candidates = getFavoriteCandidates()
        binding.layoutFavoriteSelection.visibility = View.GONE
        binding.layoutFavoriteSelection.removeAllViews()

        binding.btnAddFavorite.visibility = if (candidates.isEmpty()) View.GONE else View.VISIBLE
        binding.btnAddFavorite.text = if (candidates.size > 1) {
            "즐겨찾기에 추가할 식단 선택"
        } else {
            "즐겨찾기에 추가"
        }
    }

    private fun handleAddFavoriteClick() {
        val candidates = getFavoriteCandidates()
        when (candidates.size) {
            0 -> {
                Toast.makeText(requireContext(), "즐겨찾기에 추가할 수 있는 주문이 없습니다.", Toast.LENGTH_SHORT).show()
            }

            1 -> {
                addOrdersToFavorite(candidates)
            }

            else -> {
                showFavoriteSelectionDialog(candidates)
            }
        }
    }

    private fun showFavoriteSelectionDialog(candidates: List<OrderResponse>) {
        val checkedItems = BooleanArray(candidates.size) { true }
        val labels = candidates.map { order ->
            "${order.mealName.ifBlank { "주문 식단" }} · ${order.quantity}개"
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("즐겨찾기에 넣을 식단 선택")
            .setMultiChoiceItems(labels, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("추가") { _, _ ->
                val selected = candidates.filterIndexed { index, _ -> checkedItems[index] }
                if (selected.isEmpty()) {
                    Toast.makeText(requireContext(), "식단을 하나 이상 선택해주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    addOrdersToFavorite(selected)
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun addOrdersToFavorite(selectedOrders: List<OrderResponse>) {
        val currentUserId = SessionManager(requireContext()).getUser()?.id
        if (currentUserId == null) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        FavoriteMealStore.initialize(requireContext())
        val addedCount = selectedOrders.count { order ->
            FavoriteMealStore.addFromOrder(currentUserId, order)
        }
        val duplicatedCount = selectedOrders.size - addedCount

        val message = when {
            addedCount > 0 && duplicatedCount > 0 -> {
                "${addedCount}개 식단을 즐겨찾기에 추가했습니다.\n${duplicatedCount}개는 이미 추가된 메뉴입니다."
            }

            addedCount > 0 -> {
                "${addedCount}개 식단을 즐겨찾기에 추가했습니다."
            }

            else -> {
                "이미 즐겨찾기에 추가된 메뉴입니다."
            }
        }

        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun getFavoriteCandidates(): List<OrderResponse> {
        return orders.filter { it.orderType == ORDER_TYPE_SINGLE && it.mealId != null }
    }

    private fun getOrderNumberText(order: OrderResponse): String {
        return when {
            orders.size > 1 && order.orderType == ORDER_TYPE_SINGLE -> {
                "묶음 주문 #${order.orderId} 외 ${orders.size - 1}개"
            }

            order.orderType == ORDER_TYPE_SUBSCRIPTION && order.subscriptionGroupId != null -> {
                "구독 주문 번호 #${order.subscriptionGroupId}"
            }

            else -> {
                "주문 번호 #${order.orderId}"
            }
        }
    }

    private fun getMealNameText(): String {
        val order = orders.first()
        if (orders.size > 1 && order.orderType == ORDER_TYPE_SINGLE) {
            return "${order.mealName.ifBlank { "주문 식단" }} 외 ${orders.size - 1}개"
        }

        return order.mealName.ifBlank {
            if (order.orderType == ORDER_TYPE_SUBSCRIPTION) "구독 주문" else "주문 상품"
        }
    }

    private fun getQuantityText(): String {
        val order = orders.first()
        return when {
            orders.size > 1 && order.orderType == ORDER_TYPE_SINGLE -> {
                "총 ${orders.size}개 메뉴 · 수량 ${orders.sumOf { it.quantity }}개"
            }

            order.orderType == ORDER_TYPE_SUBSCRIPTION -> {
                "총 ${order.quantity}개"
            }

            else -> {
                "${order.quantity}개"
            }
        }
    }

    private fun getReceiveDateText(order: OrderResponse): String {
        if (order.orderType != ORDER_TYPE_SUBSCRIPTION) {
            return order.receiveDate ?: "수령 날짜 없음"
        }

        return when {
            order.dateStart.isNullOrBlank() && order.dateEnd.isNullOrBlank() -> {
                "수령 날짜 없음"
            }

            order.dateStart == order.dateEnd -> {
                order.dateStart ?: "수령 날짜 없음"
            }

            else -> {
                "${order.dateStart ?: "-"} ~ ${order.dateEnd ?: "-"}"
            }
        }
    }

    private fun bindReceivePlace(order: OrderResponse) {
//        when (order.receiveType) {
//            RECEIVE_TYPE_PICKUP -> {
//                binding.tvReceivePlaceTitle.text = "픽업 매장"
//                binding.tvReceivePlace.text = buildPickupStoreText(order)
//            }
//
//            RECEIVE_TYPE_PICKUP_POINT -> {
//                binding.tvReceivePlaceTitle.text = "픽업 포인트"
//                binding.tvReceivePlace.text = buildPickupPointText(order)
//            }
//
//            else -> {
//                binding.tvReceivePlaceTitle.text = "배송 주소"
//                binding.tvReceivePlace.text = order.address ?: "주소 정보 없음"
//            }
//        }
    }

    private fun bindPickupDetailUi(order: OrderResponse) {
        val isPickup = order.receiveType == RECEIVE_TYPE_PICKUP ||
                order.receiveType == RECEIVE_TYPE_PICKUP_POINT

        binding.layoutReceivePlaceRow.visibility = if (isPickup) View.GONE else View.VISIBLE
        binding.layoutPickupDetail.visibility = if (isPickup) View.VISIBLE else View.GONE

        if (!isPickup) {
            return
        }

        binding.tvPickupStoreBadge.text = "준비 매장"
        binding.tvPickupStoreName.text =
            order.storeName?.takeIf { it.isNotBlank() } ?: "준비 매장 정보 없음"
        binding.tvPickupStoreAddress.text =
            order.storeAddress?.takeIf { it.isNotBlank() } ?: "매장 주소 정보 없음"

        val hasPickupPoint = order.receiveType == RECEIVE_TYPE_PICKUP_POINT
        binding.layoutPickupPointDetail.visibility =
            if (hasPickupPoint) View.VISIBLE else View.GONE

        if (!hasPickupPoint) {
            binding.tvPickupPointName.text = ""
            binding.tvPickupPointAddress.text = ""
            binding.tvPickupLocker.visibility = View.GONE
            binding.tvPickupLocker.text = ""
            return
        }

        binding.tvPickupPointLabel.text = "픽업 포인트"
        binding.tvPickupPointName.text =
            order.pickupPointName?.takeIf { it.isNotBlank() } ?: "픽업 포인트 정보 없음"
        binding.tvPickupPointAddress.text =
            order.pickupPointAddress?.takeIf { it.isNotBlank() } ?: "픽업 포인트 주소 정보 없음"

        val lockerText = order.lockerNumber
            ?.takeIf { it.isNotBlank() }
            ?.let { "사물함 ${it}번" }
        binding.tvPickupLocker.visibility =
            if (lockerText.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.tvPickupLocker.text = lockerText.orEmpty()
    }

    private fun bindReceiveInfoBlock(order: OrderResponse) {
        val isPickup = order.receiveType == RECEIVE_TYPE_PICKUP ||
                order.receiveType == RECEIVE_TYPE_PICKUP_POINT
        val isDelivery = order.receiveType == RECEIVE_TYPE_DELIVERY

        binding.layoutReceivePlaceRow.visibility =
            if (isPickup || isDelivery) View.GONE else View.VISIBLE
        binding.layoutPickupDetail.visibility =
            if (isPickup || isDelivery) View.VISIBLE else View.GONE

        if (isDelivery) {
            binding.tvReceiveDetailTitle.text = "배송 정보"
            binding.tvPickupStoreBadge.text = "배송 주소"
            binding.tvPickupStoreName.text = order.address ?: "배송 주소 정보 없음"
            binding.tvPickupStoreAddress.visibility = View.GONE
            binding.tvPickupStoreAddress.text = ""
            binding.layoutPickupPointDetail.visibility = View.GONE
            binding.tvPickupPointName.text = ""
            binding.tvPickupPointAddress.text = ""
            binding.tvPickupLocker.visibility = View.GONE
            binding.tvPickupLocker.text = ""
            return
        }

        if (!isPickup) {
            return
        }

        binding.tvReceiveDetailTitle.text = "픽업 정보"
        binding.tvPickupStoreBadge.text = "준비 매장"
        binding.tvPickupStoreName.text =
            order.storeName?.takeIf { it.isNotBlank() } ?: "준비 매장 정보 없음"
        binding.tvPickupStoreAddress.visibility = View.VISIBLE
        binding.tvPickupStoreAddress.text =
            order.storeAddress?.takeIf { it.isNotBlank() } ?: "매장 주소 정보 없음"

        val hasPickupPoint = order.receiveType == RECEIVE_TYPE_PICKUP_POINT
        binding.layoutPickupPointDetail.visibility =
            if (hasPickupPoint) View.VISIBLE else View.GONE

        if (!hasPickupPoint) {
            binding.tvPickupPointName.text = ""
            binding.tvPickupPointAddress.text = ""
            binding.tvPickupLocker.visibility = View.GONE
            binding.tvPickupLocker.text = ""
            return
        }

        binding.tvPickupPointLabel.text = "픽업 포인트"
        binding.tvPickupPointName.text =
            order.pickupPointName?.takeIf { it.isNotBlank() } ?: "픽업 포인트 정보 없음"
        binding.tvPickupPointAddress.text =
            order.pickupPointAddress?.takeIf { it.isNotBlank() } ?: "픽업 포인트 주소 정보 없음"

        val lockerText = order.lockerNumber
            ?.takeIf { it.isNotBlank() }
            ?.let { "사물함 ${it}번" }
        binding.tvPickupLocker.visibility =
            if (lockerText.isNullOrBlank()) View.GONE else View.VISIBLE
        binding.tvPickupLocker.text = lockerText.orEmpty()
    }

    private fun bindNfcPickupUi(order: OrderResponse) {
        val shouldShow =
            order.orderType == ORDER_TYPE_SINGLE &&
                    order.receiveType == RECEIVE_TYPE_PICKUP_POINT &&
                    order.orderStatus == ORDER_STATUS_READY

        binding.btnNfcPickup.visibility = if (shouldShow) View.VISIBLE else View.GONE
    }

    private fun bindOrderItems() {
        val order = orders.first()
        val isGroupedSingle = orders.size > 1 && order.orderType == ORDER_TYPE_SINGLE

        if (isGroupedSingle) {
            binding.tvSubscriptionItemsTitle.visibility = View.VISIBLE
            binding.tvSubscriptionItems.visibility = View.VISIBLE
            binding.tvSubscriptionItemsTitle.text = "주문 식단 목록"
            binding.tvSubscriptionItems.text = orders.joinToString("\n") {
                "${it.mealName.ifBlank { "주문 식단" }} / ${it.quantity}개 / ${formatPrice(it.totalPrice)}원"
            }
            return
        }

        if (order.orderType == ORDER_TYPE_SUBSCRIPTION) {
            binding.tvSubscriptionItemsTitle.visibility = View.VISIBLE
            binding.tvSubscriptionItems.visibility = View.VISIBLE
            binding.tvSubscriptionItemsTitle.text = "구독 식단 목록"
            binding.tvSubscriptionItems.text =
                order.subscriptionItemsText
                    ?.takeIf { it.isNotBlank() }
                    ?: "구독 식단 정보 없음"
        } else {
            binding.tvSubscriptionItemsTitle.visibility = View.GONE
            binding.tvSubscriptionItems.visibility = View.GONE
        }
    }

    private fun buildPickupStoreText(order: OrderResponse): String {
        val name = order.storeName.orEmpty()
        val storeAddressText = order.storeAddress.orEmpty()

        return when {
            name.isNotBlank() && storeAddressText.isNotBlank() -> "$name\n$storeAddressText"
            name.isNotBlank() -> name
            storeAddressText.isNotBlank() -> storeAddressText
            else -> "매장 정보 없음"
        }
    }

    private fun buildPickupPointText(order: OrderResponse): String {
        val pointName = order.pickupPointName.orEmpty()
        val pointAddress = order.pickupPointAddress.orEmpty()
        val lockerText = order.lockerNumber
            ?.takeIf { it.isNotBlank() }
            ?.let { "사물함: ${it}번" }
        val storeText = order.storeName?.takeIf { it.isNotBlank() }?.let { "준비 매장: $it" }

        return listOf(pointName, pointAddress, lockerText, storeText)
            .filterNot { it.isNullOrBlank() }
            .joinToString("\n")
            .ifBlank { "픽업 포인트 정보 없음" }
    }

    private fun getOrderTypeText(order: OrderResponse): String {
        if (orders.size > 1 && order.orderType == ORDER_TYPE_SINGLE) {
            return "복수 주문"
        }

        return when (order.orderType) {
            ORDER_TYPE_SUBSCRIPTION -> {
                if (order.subscriptionStatus == STATUS_CANCELED) "구독 주문 · 취소됨"
                else "구독 주문"
            }

            ORDER_TYPE_SINGLE -> "단건 주문"
            else -> "단건 주문"
        }
    }

    private fun getReceiveTypeText(receiveType: String): String {
        return when (receiveType) {
            RECEIVE_TYPE_PICKUP -> "픽업"
            RECEIVE_TYPE_PICKUP_POINT -> "픽업 포인트"
            RECEIVE_TYPE_DELIVERY -> "배달"
            else -> receiveType
        }
    }

    private fun getOrderStatusText(order: OrderResponse): String {
        return when (order.orderStatus) {
            ORDER_STATUS_REVIEW -> "주문 검토 중"
            ORDER_STATUS_PREPARING -> "준비중"
            ORDER_STATUS_READY -> "준비완료"
            ORDER_STATUS_PICKED_UP -> "픽업완료"
            else -> "주문 검토 중"
        }
    }

    private fun formatPrice(value: Int): String {
        return DisplayFormatter.formatPrice(value)
    }

    override fun onDestroyView() {
        isWaitingForNfc = false
        disableNfcReader()
        nfcGuideDialog?.dismiss()
        nfcGuideDialog = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_ORDERS_JSON = "ordersJson"

        private const val ORDER_TYPE_SUBSCRIPTION = "SUBSCRIPTION"
        private const val ORDER_TYPE_SINGLE = "SINGLE"
        private const val STATUS_CANCELED = "CANCELED"

        private const val RECEIVE_TYPE_PICKUP = "PICKUP"
        private const val RECEIVE_TYPE_PICKUP_POINT = "PICKUP_POINT"
        private const val RECEIVE_TYPE_DELIVERY = "DELIVERY"

        private const val ORDER_STATUS_REVIEW = "ORDER_REVIEW"
        private const val ORDER_STATUS_PREPARING = "PREPARING"
        private const val ORDER_STATUS_READY = "READY"
        private const val ORDER_STATUS_PICKED_UP = "PICKED_UP"

        fun newInstance(orders: List<OrderResponse>): OrderDetailFragment {
            return OrderDetailFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ORDERS_JSON, Gson().toJson(orders))
                }
            }
        }
    }
}
