package com.ssafy.fitbox.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ssafy.fitbox.R
import com.ssafy.fitbox.adapter.CartAdapter
import com.ssafy.fitbox.databinding.FragmentCartBinding
import com.ssafy.fitbox.network.request.OrderCartRequest
import com.ssafy.fitbox.fragment.OrderFragment
import com.ssafy.fitbox.util.DisplayFormatter
import com.ssafy.fitbox.util.LoginRequiredDialog
import com.ssafy.fitbox.util.SessionManager
import com.ssafy.fitbox.viewmodel.CartViewModel
import com.ssafy.fitbox.viewmodel.OrderViewModel

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private val orderViewModel: OrderViewModel by viewModels()
    private val cartViewModel: CartViewModel by activityViewModels()
    private lateinit var cartAdapter: CartAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initRecyclerView()
        initClickEvents()
        observeViewModel()
        observeOrderResult()

        cartViewModel.loadCartItems()
    }

    private fun initRecyclerView() {
        cartAdapter = CartAdapter(
            cartItems = emptyList(),
            onRemoveClick = { cartItem ->
                cartViewModel.deleteCartItem(cartItem)
            }
        )

        binding.rvCartItems.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cartAdapter
            setHasFixedSize(false)
        }
    }

    private fun initClickEvents() {
        binding.btnEmptyCartShop.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, HomeFragment())
                .commit()
        }

        binding.btnCartOrder.setOnClickListener {
            if (!SessionManager(requireContext()).isLoggedIn()) {
                LoginRequiredDialog.show(this)
                return@setOnClickListener
            }
            val cartItems = cartViewModel.cartItems.value.orEmpty()

            if (cartItems.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "장바구니가 비어 있습니다.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val groupedItems = cartItems
                .groupBy { it.mealId }
                .map { (_, items) ->
                    val first = items.first()
                    first.copy(
                        quantity = items.sumOf { it.quantity }
                    )
                }

            val totalQuantity = groupedItems.sumOf { it.quantity }

            val orderItemText = groupedItems.joinToString("\n") { item ->
                "- ${item.name} ${item.quantity}개"
            }

            val totalAmount = cartItems.sumOf { item ->
                item.price * item.quantity
            }

            parentFragmentManager.beginTransaction()
                .replace(
                    R.id.main_container,
                    OrderFragment.newCartOrderInstance(
                        orderItemText = orderItemText,
                        totalQuantity = totalQuantity,
                        orderAmount = totalAmount
                    )
                )
                .addToBackStack(null)
                .commit()
        }
    }

    private fun observeViewModel() {
        cartViewModel.cartItems.observe(viewLifecycleOwner) { cartItems ->
            cartAdapter.submitItems(cartItems)
            updateCartView(cartItems.isEmpty())
            updateTotalPrice()
        }
    }

    private fun updateCartView(isEmpty: Boolean) {
        binding.layoutEmptyCart.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvCartItems.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.layoutCartBottom.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun updateTotalPrice() {
        binding.tvCartTotalPrice.text =
            "${DisplayFormatter.formatPrice(cartViewModel.getTotalPrice())}원"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeOrderResult() {
        orderViewModel.cartOrderResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { orders ->
                Toast.makeText(
                    requireContext(),
                    "주문 완료: ${orders.size}개",
                    Toast.LENGTH_SHORT
                ).show()

                cartViewModel.loadCartItems()
            }

            result.onFailure { throwable ->
                Toast.makeText(
                    requireContext(),
                    throwable.message ?: "주문에 실패했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
