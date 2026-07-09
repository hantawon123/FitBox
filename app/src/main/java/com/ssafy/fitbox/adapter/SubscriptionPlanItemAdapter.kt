package com.ssafy.fitbox.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.fitbox.databinding.ItemSubscriptionPlanBinding
import com.ssafy.fitbox.dto.SubscriptionPlanItem
import com.ssafy.fitbox.util.DisplayFormatter

class SubscriptionPlanItemAdapter(
    private val onRemoveClick: (SubscriptionPlanItem) -> Unit
) : RecyclerView.Adapter<SubscriptionPlanItemAdapter.SubscriptionPlanViewHolder>() {

    private val items = mutableListOf<SubscriptionPlanItem>()

    fun submitList(newItems: List<SubscriptionPlanItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SubscriptionPlanViewHolder {
        val binding = ItemSubscriptionPlanBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SubscriptionPlanViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: SubscriptionPlanViewHolder,
        position: Int
    ) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class SubscriptionPlanViewHolder(
        private val binding: ItemSubscriptionPlanBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SubscriptionPlanItem) {
            binding.tvSchedule.text =
                "매월 ${item.weekOfMonth}주차 ${item.dayOfWeekText}"
            binding.tvMenuName.text = item.product.name
            binding.tvQuantity.text = "회당 ${item.quantity}개"
            binding.tvPrice.text =
                "월 예상 ${formatPrice(item.product.price * item.quantity)}원"

            binding.btnRemovePlanItem.setOnClickListener {
                onRemoveClick(item)
            }
        }
    }

    private fun formatPrice(price: Int): String {
        return DisplayFormatter.formatPrice(price)
    }
}
