package com.ssafy.fitbox.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.fitbox.databinding.ItemSubscriptionHistoryBinding
import com.ssafy.fitbox.network.response.SubscriptionResponse

class SubscriptionHistoryAdapter(
    private val onCancel: (SubscriptionResponse) -> Unit,
    private val onResubscribe: (SubscriptionResponse) -> Unit,
    private val onAddFavorite: (SubscriptionResponse) -> Unit
) :
    RecyclerView.Adapter<SubscriptionHistoryAdapter.ViewHolder>() {
    private val items = mutableListOf<SubscriptionResponse>()

    fun submitList(subscriptions: List<SubscriptionResponse>) {
        items.clear()
        items.addAll(subscriptions)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemSubscriptionHistoryBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size

    inner class ViewHolder(
        private val binding: ItemSubscriptionHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SubscriptionResponse) {
            binding.tvSubscriptionStatus.text = "구독 상태: ${statusText(item.status)}"
            binding.tvSubscriptionStatus.setTextColor(
                if (item.status == STATUS_CANCELED) 0xFFB91C1C.toInt()
                else 0xFF24563A.toInt()
            )
            binding.tvSubscriptionPeriod.text =
                "월 정기구독 · 시작 ${item.subscriptionStartDate}\n" +
                        "다음 달 일정 생성 ${item.nextOrderMonth ?: "-"}"
            binding.tvSubscriptionReceive.text = if (item.receiveType == "PICKUP") {
                "픽업: ${item.storeName ?: "선택 매장"} ${item.storeAddress.orEmpty()}"
            } else {
                "배송: ${item.address ?: "주소 정보 없음"}"
            }
            binding.tvSubscriptionMenus.text = item.templates.orEmpty()
                .sortedWith(compareBy({ it.weekOfMonth }, { it.dayOfWeek }))
                .joinToString("\n") {
                    "${it.weekOfMonth}주차 ${it.dayOfWeekText} · ${it.mealName} ${it.quantity}개"
                }
            binding.btnCancelSubscription.setOnClickListener { onCancel(item) }
            binding.btnAddSubscriptionFavorite.setOnClickListener { onAddFavorite(item) }
            binding.btnCancelSubscription.visibility =
                if (item.status == STATUS_CANCELED) android.view.View.GONE
                else android.view.View.VISIBLE
            binding.btnResubscribe.visibility =
                if (item.status == STATUS_CANCELED) android.view.View.VISIBLE
                else android.view.View.GONE
            if (item.status == STATUS_CANCELED) {
                binding.btnResubscribe.setOnClickListener { onResubscribe(item) }
                binding.root.setOnClickListener { onResubscribe(item) }
                binding.root.isClickable = true
            } else {
                binding.btnResubscribe.setOnClickListener(null)
                binding.root.setOnClickListener(null)
                binding.root.isClickable = false
            }
        }

        private fun statusText(status: String) = when (status) {
            "ACTIVE" -> "구독 중"
            "PAUSED" -> "일시 정지"
            STATUS_CANCELED -> "취소됨"
            else -> status
        }
    }

    companion object {
        private const val STATUS_CANCELED = "CANCELED"
    }
}
