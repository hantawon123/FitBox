package com.ssafy.fitbox.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ssafy.fitbox.R
import com.ssafy.fitbox.adapter.OrderAdapter
import com.ssafy.fitbox.databinding.FragmentOrderListBinding
import com.ssafy.fitbox.dto.OrderHistoryItem
import com.ssafy.fitbox.network.response.OrderResponse
import com.ssafy.fitbox.util.LoginRequiredDialog
import com.ssafy.fitbox.util.SessionManager
import com.ssafy.fitbox.viewmodel.OrderViewModel

class OrderListFragment : Fragment() {

    private var _binding: FragmentOrderListBinding? = null
    private val binding get() = _binding!!

    private val orderViewModel: OrderViewModel by viewModels()

    private lateinit var orderAdapter: OrderAdapter
    private val singleOrdersOnly: Boolean
        get() = arguments?.getBoolean(ARG_SINGLE_ONLY) ?: false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOrderListBinding.inflate(inflater, container, false)
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

        if (singleOrdersOnly) {
            binding.tvOrderListTitle.text = "일반 주문 내역"
            binding.tvOrderListEmpty.text = "아직 일반 주문 내역이 없습니다."
        }
        binding.btnOrderListEmptyAction.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, HomeFragment())
                .commit()
        }
        initRecyclerView()
        observeOrders()
        loadOrdersFromServer()
    }

    private fun initRecyclerView() {
        orderAdapter = OrderAdapter { order ->
            moveToOrderDetail(order)
        }

        binding.rvOrderList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = orderAdapter
        }
    }

    private fun observeOrders() {
        orderViewModel.userOrders.observe(viewLifecycleOwner) { orders ->
            val visibleOrders = if (singleOrdersOnly) {
                orders.filter { it.orderType == ORDER_TYPE_SINGLE }
            } else {
                orders
            }
            val historyItems = groupOrderHistoryItems(visibleOrders)
            orderAdapter.submitList(historyItems)
            updateEmptyView(historyItems)
        }

        orderViewModel.orderListErrorMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(
                requireContext(),
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun loadOrdersFromServer() {
        val userId = SessionManager(requireContext()).getUser()?.id

        if (userId == null) {
            orderAdapter.submitList(emptyList())
            updateEmptyView(emptyList())

            Toast.makeText(
                requireContext(),
                "로그인이 필요합니다.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        orderViewModel.getUserOrders(userId.toLong())
    }

    private fun moveToOrderDetail(item: OrderHistoryItem) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, OrderDetailFragment.newInstance(item.orders))
            .addToBackStack(null)
            .commit()
    }

    private fun updateEmptyView(items: List<OrderHistoryItem>) {
        if (items.isEmpty()) {
            binding.layoutOrderListEmpty.visibility = View.VISIBLE
            binding.rvOrderList.visibility = View.GONE
        } else {
            binding.layoutOrderListEmpty.visibility = View.GONE
            binding.rvOrderList.visibility = View.VISIBLE
        }
    }

    private fun groupOrderHistoryItems(orders: List<OrderResponse>): List<OrderHistoryItem> {
        val singleGroups = linkedMapOf<String, MutableList<OrderResponse>>()
        val result = mutableListOf<OrderHistoryItem>()

        orders.forEach { order ->
            if (order.orderType == ORDER_TYPE_SINGLE) {
                singleGroups.getOrPut(getSingleOrderGroupKey(order)) { mutableListOf() }.add(order)
            } else {
                result.add(OrderHistoryItem(listOf(order)))
            }
        }

        result.addAll(singleGroups.values.map { OrderHistoryItem(it) })

        return result.sortedWith(
            compareByDescending<OrderHistoryItem> { it.representative.orderTime }
                .thenByDescending { it.representative.orderId }
        )
    }

    private fun getSingleOrderGroupKey(order: OrderResponse): String {
        return listOf(
            order.userId,
            order.orderTime,
            order.receiveType,
            order.receiveDate.orEmpty(),
            order.storeId?.toString().orEmpty(),
            order.pickupPointId?.toString().orEmpty(),
            order.address.orEmpty(),
            order.paymentStatus.orEmpty()
        ).joinToString("|")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_SINGLE_ONLY = "single_orders_only"
        private const val ORDER_TYPE_SINGLE = "SINGLE"

        fun newSingleOrderInstance(): OrderListFragment {
            return OrderListFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_SINGLE_ONLY, true)
                }
            }
        }
    }
}
