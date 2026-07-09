package com.ssafy.fitbox.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.fitbox.databinding.ItemAdminOrderBinding
import com.ssafy.fitbox.dto.AdminOrder
import com.ssafy.fitbox.dto.AdminOrderStatus
import com.ssafy.fitbox.dto.AdminPickupMode

class AdminOrderAdapter(
    private val onAction: (AdminOrder) -> Unit
) : RecyclerView.Adapter<AdminOrderAdapter.AdminOrderViewHolder>() {

    private val items = mutableListOf<AdminOrder>()

    fun submitList(orders: List<AdminOrder>) {
        items.clear()
        items.addAll(orders)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminOrderViewHolder {
        return AdminOrderViewHolder(
            ItemAdminOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: AdminOrderViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class AdminOrderViewHolder(
        private val binding: ItemAdminOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AdminOrder) {
            val order = item.order
            binding.tvOrderNumber.text = if (item.orders.size > 1) {
                "주문 #${item.orders.minOf { it.orderId }} 외 ${item.orders.size - 1}건"
            } else {
                "주문 #${order.orderId}"
            }
            binding.tvOrderStatus.text = item.status.displayName
            binding.tvOrderMeal.text = mealText(item)
            binding.tvOrderCustomer.text = "고객 ${maskName(order.customerName)}"
            binding.tvPickupInfo.text = pickupInfo(item)
            binding.tvReceiveDate.text = when (item.pickupMode) {
                AdminPickupMode.DELIVERY ->
                    "배달 예정일 · ${order.receiveDate ?: "날짜 미확인"}"

                else ->
                    "픽업 예정일 · ${order.receiveDate ?: "날짜 미확인"}"
            }
            binding.tvLastPush.text = "최근 알림: ${item.lastPushMessage.orEmpty()}"
            binding.tvLastPush.visibility =
                if (item.lastPushMessage.isNullOrBlank()) View.GONE else View.VISIBLE
            binding.btnAdminAction.text = actionText(item)
            binding.btnAdminAction.isEnabled =
                item.status != AdminOrderStatus.PICKED_UP &&
                    !(item.status == AdminOrderStatus.READY &&
                        item.pickupMode == AdminPickupMode.PICKUP_POINT)
            binding.btnAdminAction.setOnClickListener {
                binding.btnAdminAction.isEnabled = false
                onAction(item)
            }
        }

        private fun mealText(item: AdminOrder): String {
            val firstOrder = item.orders.minBy { it.orderId }
            val totalQuantity = item.orders.sumOf { it.quantity }
            return if (item.orders.size > 1) {
                "${firstOrder.mealName} 외 ${item.orders.size - 1}개 메뉴 · 총 ${totalQuantity}개"
            } else {
                "${firstOrder.mealName} · ${firstOrder.quantity}개"
            }
        }

        private fun pickupInfo(item: AdminOrder): String {
            return when (item.pickupMode) {
                AdminPickupMode.STORE ->
                    "매장 픽업 · ${item.order.storeName ?: "지정 매장"}"

                AdminPickupMode.DELIVERY ->
                    "배달 주문 · ${item.order.storeName ?: "지정 매장"}"

                AdminPickupMode.PICKUP_POINT ->
                    buildString {
                        append("픽업 포인트 · ")
                        append(item.order.pickupPointName ?: item.pickupPoint ?: "지정 포인트")
                        item.order.lockerNumber?.let { append(" · ${it}번 사물함") }
                    }
            }
        }

        private fun actionText(item: AdminOrder): String {
            return when (item.status) {
                AdminOrderStatus.ORDER_REVIEW -> "주문 받기"
                AdminOrderStatus.PREPARING -> "준비 완료"
                AdminOrderStatus.READY ->
                    when (item.pickupMode) {
                        AdminPickupMode.STORE -> "매장 픽업 완료"
                        AdminPickupMode.DELIVERY -> "배달 완료"
                        AdminPickupMode.PICKUP_POINT -> "NFC 픽업 대기"
                    }

                AdminOrderStatus.PICKED_UP -> "픽업 완료"
            }
        }

        private fun maskName(name: String?): String {
            val value = name?.trim().orEmpty()
            return when {
                value.isEmpty() -> "이름 미확인"
                value.length == 1 -> value
                value.length == 2 -> "${value.first()}*"
                else -> "${value.first()}${"*".repeat(value.length - 2)}${value.last()}"
            }
        }
    }
}
