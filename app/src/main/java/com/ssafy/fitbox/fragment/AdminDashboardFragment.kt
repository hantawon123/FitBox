package com.ssafy.fitbox.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.ssafy.fitbox.R
import com.ssafy.fitbox.adapter.AdminOrderAdapter
import com.ssafy.fitbox.databinding.DialogLockerSelectionBinding
import com.ssafy.fitbox.databinding.FragmentAdminDashboardBinding
import com.ssafy.fitbox.dto.AdminOrder
import com.ssafy.fitbox.dto.AdminOrderStatus
import com.ssafy.fitbox.dto.AdminPickupMode
import com.ssafy.fitbox.notification.FirebasePushNotificationSender
import com.ssafy.fitbox.notification.NotificationEvents
import com.ssafy.fitbox.notification.PushNotificationSender
import com.ssafy.fitbox.repository.NotificationRepository
import com.ssafy.fitbox.repository.OrderRepository
import com.ssafy.fitbox.repository.StoreRepository
import com.ssafy.fitbox.util.AdminOrderStore
import com.ssafy.fitbox.util.SessionManager
import kotlinx.coroutines.launch

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    private val orderRepository = OrderRepository()
    private val notificationRepository = NotificationRepository()
    private val storeRepository = StoreRepository()
    private val pushSender: PushNotificationSender by lazy {
        FirebasePushNotificationSender()
    }
    private lateinit var adapter: AdminOrderAdapter
    private var orders: List<AdminOrder> = emptyList()
    private var selectedSection = AdminOrderSection.WAITING

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val sessionManager = SessionManager(requireContext())
        val admin = sessionManager.getUser()
        binding.tvAdminName.text = "${admin?.name ?: "관리자"}님, 주문을 확인해주세요."

        adapter = AdminOrderAdapter(::handleOrderAction)
        binding.rvAdminOrders.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAdminOrders.adapter = adapter

        binding.btnAdminNotifications.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, AdminNotificationFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.toggleOrderStatus.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            selectedSection = when (checkedId) {
                R.id.btnWaitingOrders -> AdminOrderSection.WAITING
                R.id.btnCompletedOrders -> AdminOrderSection.COMPLETED
                else -> AdminOrderSection.IN_PROGRESS
            }
            renderOrders()
        }

        binding.btnAdminLogout.setOnClickListener {
            (requireActivity() as? com.ssafy.fitbox.activity.MainActivity)?.onLogoutSuccess()
                ?: run {
                    sessionManager.clearSession()
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.main_container, MyPageFragment())
                        .commit()
                }
        }

        observeNotificationEvents()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            loadOrders()
            loadUnreadNotificationCount()
        }
    }

    private fun loadOrders() {
        binding.progressAdminOrders.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val serverOrders = orderRepository.getAdminOrders().getOrDefault(emptyList())
            orders = AdminOrderStore.merge(requireContext(), serverOrders)
                .sortedBy { it.order.orderId }
            renderOrders()
            binding.progressAdminOrders.visibility = View.GONE
        }
    }

    private fun handleOrderAction(item: AdminOrder) {
        when (item.status) {
            AdminOrderStatus.ORDER_REVIEW -> updateWithPush(
                item = item,
                nextStatus = AdminOrderStatus.PREPARING,
                message = "주문이 접수되었습니다."
            )

            AdminOrderStatus.PREPARING -> {
                if (item.pickupMode == AdminPickupMode.PICKUP_POINT) {
                    showLockerNumberDialog(item)
                } else {
                    updateWithPush(
                        item = item,
                        nextStatus = AdminOrderStatus.READY,
                        message = "주문하신 메뉴 준비가 완료되었습니다."
                    )
                }
            }

            AdminOrderStatus.READY -> {
                when (item.pickupMode) {
                    AdminPickupMode.PICKUP_POINT -> {
                        Toast.makeText(
                            requireContext(),
                            "픽업 포인트 주문은 사용자의 NFC 인증 완료 시 픽업 완료 처리됩니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    AdminPickupMode.DELIVERY -> {
                        updateWithPush(
                            item = item,
                            nextStatus = AdminOrderStatus.PICKED_UP,
                            message = "배달이 완료되었습니다."
                        )
                    }

                    AdminPickupMode.STORE -> {
                        updateWithPush(
                            item = item,
                            nextStatus = AdminOrderStatus.PICKED_UP,
                            message = "매장 픽업이 완료되었습니다."
                        )
                    }
                }
            }

            AdminOrderStatus.PICKED_UP -> Unit
        }
    }

    private fun showLockerNumberDialog(item: AdminOrder) {
        val pickupPointId = item.order.pickupPointId
        val receiveDate = item.order.receiveDate
        if (pickupPointId == null || receiveDate.isNullOrBlank()) {
            Toast.makeText(requireContext(), "픽업 포인트와 픽업 날짜 정보가 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val lockerCount = storeRepository.getPickupPointCapacity(pickupPointId, receiveDate)
                .getOrNull()
                ?.totalCnt
                ?.takeIf { it > 0 }
                ?: LOCKER_COUNT
            showLockerSelectionDialog(item, lockerCount)
        }
    }

    private fun showLockerSelectionDialog(item: AdminOrder, lockerCount: Int) {
        val dialogBinding = DialogLockerSelectionBinding.inflate(layoutInflater)
        dialogBinding.gridLockers.columnCount = if (lockerCount <= 12) 4 else 5
        dialogBinding.tvPickupDate.text = "픽업 예정일 · ${item.order.receiveDate ?: "날짜 미확인"}"

        val occupiedLockers = orders
            .filter { order ->
                order.orders.none { it.orderId in item.orders.map { current -> current.orderId }.toSet() } &&
                    order.order.pickupPointId == item.order.pickupPointId &&
                    order.order.receiveDate == item.order.receiveDate &&
                    order.status == AdminOrderStatus.READY
            }
            .mapNotNull { it.order.lockerNumber }
            .toSet()

        var selectedLocker = item.order.lockerNumber
        val lockerButtons = mutableMapOf<String, MaterialButton>()

        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(item.order.pickupPointName ?: "픽업 사물함 선택")
            .setView(dialogBinding.root)
            .setNegativeButton("취소", null)
            .setPositiveButton("선택 완료", null)
            .create()

        (1..lockerCount).forEach { number ->
            val lockerNumber = number.toString()
            val button = MaterialButton(
                requireContext(),
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = lockerNumber
                textSize = 12f
                isAllCaps = false
                insetTop = 0
                insetBottom = 0
                cornerRadius = resources.getDimensionPixelSize(R.dimen.fit_space_sm)
                isEnabled = lockerNumber !in occupiedLockers
                alpha = if (isEnabled) 1f else 0.35f
                setOnClickListener {
                    selectedLocker = lockerNumber
                    dialogBinding.tvSelectedLocker.text = "${lockerNumber}번 사물함을 선택했습니다."
                    lockerButtons.forEach { (value, lockerButton) ->
                        styleLockerButton(
                            lockerButton,
                            selected = value == lockerNumber,
                            enabled = lockerButton.isEnabled
                        )
                    }
                }
            }
            button.layoutParams = GridLayout.LayoutParams().apply {
                width = resources.getDimensionPixelSize(R.dimen.fit_space_2xl)
                height = resources.getDimensionPixelSize(R.dimen.fit_space_2xl)
                setMargins(5, 5, 5, 5)
            }
            lockerButtons[lockerNumber] = button
            dialogBinding.gridLockers.addView(button)
            styleLockerButton(
                button,
                selected = lockerNumber == selectedLocker,
                enabled = button.isEnabled
            )
        }

        selectedLocker?.let {
            dialogBinding.tvSelectedLocker.text = "${it}번 사물함을 선택했습니다."
        }

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val lockerNumber = selectedLocker
                    if (lockerNumber.isNullOrBlank()) {
                        Toast.makeText(
                            requireContext(),
                            "사물함을 선택해주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                    dialog.dismiss()
                    assignLocker(item, lockerNumber)
                }
        }
        dialog.show()
    }

    private fun styleLockerButton(
        button: MaterialButton,
        selected: Boolean,
        enabled: Boolean
    ) {
        val backgroundColor = when {
            !enabled -> R.color.fit_surface_variant
            selected -> R.color.fit_primary
            else -> R.color.fit_surface
        }
        val textColor = when {
            !enabled -> R.color.fit_text_hint
            selected -> R.color.white
            else -> R.color.fit_primary
        }
        button.backgroundTintList =
            ContextCompat.getColorStateList(requireContext(), backgroundColor)
        button.setTextColor(ContextCompat.getColor(requireContext(), textColor))
        button.strokeColor =
            ContextCompat.getColorStateList(requireContext(), R.color.fit_outline)
    }

    private fun assignLocker(item: AdminOrder, lockerNumber: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            orderRepository.assignLocker(item.order.orderId, lockerNumber)
                .onSuccess { updatedOrder ->
                    val message = "${updatedOrder.pickupPointName} ${updatedOrder.lockerNumber}번 사물함에 보관되어 있습니다."
                    val updatedOrders = item.orders.map { order ->
                        order.copy(
                            lockerNumber = updatedOrder.lockerNumber,
                            orderStatus = AdminOrderStatus.READY.name
                        )
                    }
                    saveOrder(
                        item.copy(
                            order = updatedOrders.minBy { it.orderId },
                            orders = updatedOrders,
                            status = AdminOrderStatus.READY,
                            lastPushMessage = message
                        )
                    )
                    Toast.makeText(
                        requireContext(),
                        "사물함 할당과 사용자 알림이 완료되었습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .onFailure { error ->
                    Toast.makeText(
                        requireContext(),
                        error.message ?: "사물함 할당에 실패했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun updateWithPush(
        item: AdminOrder,
        nextStatus: AdminOrderStatus,
        message: String
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            orderRepository.updateOrderStatus(item.order.orderId, nextStatus.name)
                .onSuccess { updatedOrder ->
                    val pushResult = pushSender.send(
                        userId = item.order.userId,
                        title = "FitBox 주문 안내",
                        message = message
                    )
                    val lastPushMessage = if (pushResult.isSuccess) message else item.lastPushMessage
                    if (pushResult.isFailure) {
                        Toast.makeText(
                            requireContext(),
                            pushResult.exceptionOrNull()?.message ?: "푸시 알림 발송에 실패했습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    val updatedOrders = item.orders.map { order ->
                        if (order.orderId == updatedOrder.orderId) {
                            updatedOrder
                        } else {
                            order.copy(orderStatus = nextStatus.name)
                        }
                    }
                    saveOrder(
                        item.copy(
                            order = updatedOrders.minBy { it.orderId },
                            orders = updatedOrders,
                            status = nextStatus,
                            lastPushMessage = lastPushMessage
                        )
                    )
                    Toast.makeText(
                        requireContext(),
                        "주문 상태를 ${nextStatus.displayName}(으)로 변경했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }.onFailure {
                    Toast.makeText(
                        requireContext(),
                        it.message ?: "주문 상태 변경에 실패했습니다.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun saveOrder(updated: AdminOrder) {
        AdminOrderStore.save(requireContext(), updated)
        val updatedIds = updated.orders.map { it.orderId }.toSet()
        orders = orders.map { current ->
            if (current.orders.any { it.orderId in updatedIds }) updated else current
        }
        renderOrders()
    }

    private fun renderOrders() {
        val visibleOrders = orders.filter { order ->
            when (selectedSection) {
                AdminOrderSection.WAITING ->
                    order.status == AdminOrderStatus.ORDER_REVIEW
                AdminOrderSection.IN_PROGRESS ->
                    order.status == AdminOrderStatus.PREPARING ||
                        order.status == AdminOrderStatus.READY
                AdminOrderSection.COMPLETED ->
                    order.status == AdminOrderStatus.PICKED_UP
            }
        }

        adapter.submitList(visibleOrders.sortedBy { it.order.orderId })
        val waitingCount = orders.count { it.status == AdminOrderStatus.ORDER_REVIEW }
        renderCountBadge(binding.tvWaitingOrderCountBadge, waitingCount)
        binding.tvAdminOrderEmpty.text = when (selectedSection) {
            AdminOrderSection.WAITING -> "수락 대기 중인 주문이 없습니다."
            AdminOrderSection.IN_PROGRESS -> "진행 중인 주문이 없습니다."
            AdminOrderSection.COMPLETED -> "픽업 완료된 주문이 없습니다."
        }
        binding.tvAdminOrderEmpty.visibility =
            if (visibleOrders.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun observeNotificationEvents() {
        viewLifecycleOwner.lifecycleScope.launch {
            NotificationEvents.arrived.collect {
                loadUnreadNotificationCount()
                loadOrders()
            }
        }
    }

    private fun loadUnreadNotificationCount() {
        val userId = SessionManager(requireContext()).getUser()?.id ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            notificationRepository.getUnreadCount(userId)
                .onSuccess { count ->
                    renderCountBadge(binding.tvAdminNotificationCountBadge, count)
                }
        }
    }

    private fun renderCountBadge(view: android.widget.TextView, count: Int) {
        view.visibility = if (count > 0) View.VISIBLE else View.GONE
        view.text = if (count > 99) "99+" else count.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private enum class AdminOrderSection {
        WAITING,
        IN_PROGRESS,
        COMPLETED
    }

    companion object {
        private const val LOCKER_COUNT = 30
    }
}
