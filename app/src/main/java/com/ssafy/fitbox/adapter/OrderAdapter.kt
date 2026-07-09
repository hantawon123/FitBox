package com.ssafy.fitbox.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.fitbox.databinding.ItemOrderBinding
import com.ssafy.fitbox.dto.OrderHistoryItem
import com.ssafy.fitbox.network.response.OrderResponse
import com.ssafy.fitbox.util.DisplayFormatter

class OrderAdapter(
    private val onOrderClick: (OrderHistoryItem) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    private val orders = mutableListOf<OrderHistoryItem>()

    fun submitList(newOrders: List<OrderHistoryItem>) {
        orders.clear()
        orders.addAll(newOrders)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): OrderViewHolder {
        val binding = ItemOrderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: OrderViewHolder,
        position: Int
    ) {
        holder.bind(orders[position])
    }

    override fun getItemCount(): Int = orders.size

    inner class OrderViewHolder(
        private val binding: ItemOrderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: OrderHistoryItem) {
            val order = item.representative
            binding.tvOrderMealName.text = getMealNameText(item)
            binding.tvOrderType.text = getOrderTypeText(item)
            binding.tvOrderType.setTextColor(
                if (order.subscriptionStatus == STATUS_CANCELED) 0xFFB91C1C.toInt()
                else 0xFF24563A.toInt()
            )
            binding.tvOrderReceiveType.text = getReceiveTypeText(order.receiveType)
            binding.tvOrderStatus.text = getOrderStatusText(order)
            binding.tvOrderStatus.visibility =
                if (order.orderType == ORDER_TYPE_SINGLE) android.view.View.VISIBLE
                else android.view.View.GONE
            binding.tvOrderQuantity.text = getQuantityText(item)
            binding.tvOrderDate.text = getDateText(order)
            binding.tvOrderPrice.text = "${formatPrice(item.totalPrice)}원"

            binding.root.setOnClickListener {
                onOrderClick(item)
            }
        }
    }

    private fun getMealNameText(item: OrderHistoryItem): String {
        val order = item.representative
        if (item.isGrouped && order.orderType == ORDER_TYPE_SINGLE) {
            val firstName = order.mealName.ifBlank { "주문 식단" }
            return "$firstName 외 ${item.orders.size - 1}개"
        }

        return order.mealName.ifBlank {
            if (order.orderType == ORDER_TYPE_SUBSCRIPTION) "구독 주문" else "주문 상품"
        }
    }

    private fun getQuantityText(item: OrderHistoryItem): String {
        val order = item.representative
        return when {
            item.isGrouped && order.orderType == ORDER_TYPE_SINGLE -> {
                "총 ${item.orders.size}개 메뉴 · 수량 ${item.totalQuantity}개"
            }

            order.orderType == ORDER_TYPE_SUBSCRIPTION -> {
                "총 ${order.quantity}개"
            }

            else -> {
                "수량 ${order.quantity}개"
            }
        }
    }

    private fun getDateText(order: OrderResponse): String {
        return if (order.orderType == ORDER_TYPE_SUBSCRIPTION) {
            getSubscriptionDateRangeText(order)
        } else {
            order.receiveDate ?: "수령 날짜 없음"
        }
    }

    private fun getSubscriptionDateRangeText(order: OrderResponse): String {
        val startDate = order.dateStart
        val endDate = order.dateEnd

        return when {
            startDate.isNullOrBlank() && endDate.isNullOrBlank() -> {
                "수령 날짜 없음"
            }

            startDate == endDate -> {
                "수령 날짜: $startDate"
            }

            else -> {
                "수령 기간: ${startDate ?: "-"} ~ ${endDate ?: "-"}"
            }
        }
    }

    private fun getOrderTypeText(item: OrderHistoryItem): String {
        val order = item.representative
        if (item.isGrouped && order.orderType == ORDER_TYPE_SINGLE) {
            return "복수 주문"
        }

        return when (order.orderType) {
            ORDER_TYPE_SUBSCRIPTION -> {
                if (order.subscriptionStatus == STATUS_CANCELED) "구독 주문 · 취소됨"
                else "구독 주문"
            }

            ORDER_TYPE_SINGLE -> "단건 주문"
            else -> order.orderType
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

    companion object {
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
    }
}
