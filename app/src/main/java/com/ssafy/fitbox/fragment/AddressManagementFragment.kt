package com.ssafy.fitbox.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.ssafy.fitbox.adapter.AddressAdapter
import com.ssafy.fitbox.databinding.FragmentAddressManagementBinding
import com.ssafy.fitbox.dto.Address
import com.ssafy.fitbox.util.AddressParts
import com.ssafy.fitbox.util.SessionManager
import com.ssafy.fitbox.viewmodel.AddressViewModel

class AddressManagementFragment : Fragment() {
    private var _binding: FragmentAddressManagementBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddressViewModel by viewModels()
    private lateinit var adapter: AddressAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddressManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val userId = SessionManager(requireContext()).getUser()?.id ?: return
        parentFragmentManager.setFragmentResultListener(
            AddressSearchFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val zoneCode = bundle.getString(AddressSearchFragment.KEY_ZONE_CODE).orEmpty()
            val address = bundle.getString(AddressSearchFragment.KEY_ADDRESS).orEmpty()
            binding.etRoadAddress.setText(
                if (zoneCode.isBlank()) address else "[$zoneCode] $address"
            )
            binding.etAddressDetail.requestFocus()
        }

        adapter = AddressAdapter(
            onDelete = { viewModel.deleteAddress(userId, it.id) },
            onViewMap = ::openAddressOnMap
        )
        binding.rvAddresses.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAddresses.adapter = adapter

        binding.etRoadAddress.setOnClickListener {
            openAddressSearch()
        }

        binding.btnAddAddress.setOnClickListener {
            val roadAddress = binding.etRoadAddress.text.toString().trim()
            val detailAddress = binding.etAddressDetail.text.toString().trim()
            if (roadAddress.isBlank()) {
                Toast.makeText(requireContext(), "도로명 주소를 검색해주세요.", Toast.LENGTH_SHORT).show()
            } else if (detailAddress.isBlank()) {
                Toast.makeText(requireContext(), "상세주소를 입력해주세요.", Toast.LENGTH_SHORT).show()
            } else {
                val parts = AddressParts.parse(roadAddress)
                viewModel.addAddress(
                    userId,
                    AddressParts.compose(
                        zoneCode = parts.zoneCode,
                        roadAddress = parts.roadAddress,
                        detailAddress = detailAddress
                    )
                )
            }
        }

        viewModel.addresses.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            binding.tvAddressEmpty.visibility = if (it.isEmpty()) View.VISIBLE else View.GONE
            binding.rvAddresses.visibility = if (it.isEmpty()) View.GONE else View.VISIBLE
        }
        viewModel.message.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            if (it == "배송지가 추가되었습니다.") {
                binding.etRoadAddress.text?.clear()
                binding.etAddressDetail.text?.clear()
            }
        }
        viewModel.loadAddresses(userId)
    }

    private fun openAddressSearch() {
        parentFragmentManager.beginTransaction()
            .replace(com.ssafy.fitbox.R.id.main_container, AddressSearchFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun openAddressOnMap(address: Address) {
        val displayAddress = address.displayAddress.trim()
        if (displayAddress.isBlank()) {
            Toast.makeText(requireContext(), "확인할 주소가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = Uri.parse("geo:0,0?q=${Uri.encode(displayAddress)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), "지도를 열 수 있는 앱이 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
