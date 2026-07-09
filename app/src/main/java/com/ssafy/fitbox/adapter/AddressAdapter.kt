package com.ssafy.fitbox.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.fitbox.databinding.ItemAddressBinding
import com.ssafy.fitbox.dto.Address

class AddressAdapter(
    private val onDelete: (Address) -> Unit,
    private val onViewMap: (Address) -> Unit
) : RecyclerView.Adapter<AddressAdapter.ViewHolder>() {
    private val items = mutableListOf<Address>()
    private var expandedAddressId: Int? = null

    fun submitList(addresses: List<Address>) {
        items.clear()
        items.addAll(addresses)
        if (expandedAddressId != null && addresses.none { it.id == expandedAddressId }) {
            expandedAddressId = null
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemAddressBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])
    override fun getItemCount(): Int = items.size

    inner class ViewHolder(
        private val binding: ItemAddressBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Address) {
            val isExpanded = item.id == expandedAddressId
            binding.tvAddress.text = item.displayAddress
            binding.viewAddressActionDivider.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.layoutAddressActions.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.root.setOnClickListener {
                val previousExpandedId = expandedAddressId
                expandedAddressId = if (isExpanded) null else item.id

                previousExpandedId?.let { previousId ->
                    val previousIndex = items.indexOfFirst { it.id == previousId }
                    if (previousIndex != -1) notifyItemChanged(previousIndex)
                }

                val currentIndex = bindingAdapterPosition
                if (currentIndex != RecyclerView.NO_POSITION) {
                    notifyItemChanged(currentIndex)
                }
            }
            binding.btnViewAddressMap.setOnClickListener { onViewMap(item) }
            binding.btnDeleteAddress.setOnClickListener { onDelete(item) }
        }
    }
}
