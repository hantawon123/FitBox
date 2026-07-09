package com.ssafy.fitbox.util

import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.ssafy.fitbox.dto.Address
import com.ssafy.fitbox.repository.AddressRepository

class DeliveryAddressFormManager(
    private val fragment: Fragment,
    private val spinner: Spinner,
    private val roadAddressInput: EditText,
    private val detailAddressInput: EditText,
    private val addressRepository: AddressRepository = AddressRepository(),
    private val onRoadAddressChanged: (() -> Unit)? = null
) {
    private var savedAddresses: List<Address> = emptyList()
    private var searchedRoadAddress: String? = null
    private var suppressAddressSelection: Boolean = false
    private var pendingDeliveryAddressToSave: String? = null

    val addresses: List<Address>
        get() = savedAddresses

    fun setupSpinner() {
        spinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    if (suppressAddressSelection) return

                    if (position >= savedAddresses.size) {
                        val roadAddress = searchedRoadAddress

                        if (!roadAddress.isNullOrBlank()) {
                            applyNewRoadAddressToInputs(roadAddress)
                        } else {
                            clearAddressInputs()
                        }
                    } else {
                        searchedRoadAddress = null
                        applySavedAddress(savedAddresses[position].displayAddress)
                    }
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            }
    }

    fun submitSavedAddresses(
        addresses: List<Address>,
        applyLatestSavedAddress: Boolean = true
    ) {
        savedAddresses = addresses

        suppressAddressSelection = true

        spinner.adapter = ArrayAdapter(
            fragment.requireContext(),
            android.R.layout.simple_spinner_item,
            addresses.map { it.displayAddress } + NEW_ADDRESS_LABEL
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        if (applyLatestSavedAddress) {
            applyLatestSavedAddressIfPossible()
        } else {
            // 중요:
            // 지도에서 담당 매장 변경 후 돌아온 경우에는
            // spinner가 0번 저장주소를 자동 선택해서 주소를 덮어쓰지 못하게
            // "새 배송지 검색" 항목을 선택 상태로 둔다.
            spinner.setSelection(savedAddresses.size, false)
        }

        spinner.post {
            suppressAddressSelection = false
        }

        if (applyLatestSavedAddress) {
            searchedRoadAddress?.let { applyNewRoadAddress(it) }
        }
    }

    fun applyAddressSearchResult(
        zoneCode: String,
        address: String
    ) {
        val roadAddress = if (zoneCode.isBlank()) {
            address
        } else {
            "[$zoneCode] $address"
        }

        applyNewRoadAddress(roadAddress)
    }

    fun applyNewRoadAddress(roadAddress: String) {
        searchedRoadAddress = roadAddress
        suppressAddressSelection = true

        applyNewRoadAddressToInputs(roadAddress)

        spinner.post {
            if (spinner.adapter != null) {
                spinner.setSelection(savedAddresses.size, false)
            }

            applyNewRoadAddressToInputs(roadAddress)
            suppressAddressSelection = false
        }
    }

    fun applySavedAddress(fullAddress: String) {
        val parts = AddressParts.parse(fullAddress)
        roadAddressInput.setText(parts.roadAddressWithZone)
        detailAddressInput.setText(parts.detailAddress)
        onRoadAddressChanged?.invoke()
    }

    fun applyPendingAddress(address: String?) {
        if (address.isNullOrBlank()) return

        val savedIndex = savedAddresses.indexOfFirst { it.displayAddress == address }

        if (savedIndex >= 0) {
            spinner.setSelection(savedIndex)
        } else {
            spinner.setSelection(savedAddresses.size)
            applySavedAddress(address)
        }
    }

    fun getAddressIfDelivery(
        isDelivery: Boolean
    ): String? {
        if (!isDelivery) {
            return null
        }

        val roadAddress = roadAddressInput.text.toString().trim()
        val detailAddress = detailAddressInput.text.toString().trim()

        if (roadAddress.isBlank()) {
            Toast.makeText(
                fragment.requireContext(),
                "배송 주소를 입력해주세요.",
                Toast.LENGTH_SHORT
            ).show()
            return null
        }

        val parts = AddressParts.parse(roadAddress)
        return AddressParts.compose(
            zoneCode = parts.zoneCode,
            roadAddress = parts.roadAddress,
            detailAddress = detailAddress
        )
    }

    fun prepareDeliveryAddressSave(
        isDelivery: Boolean,
        address: String?
    ) {
        pendingDeliveryAddressToSave = if (
            isDelivery &&
            !address.isNullOrBlank() &&
            shouldSaveDeliveryAddress(address)
        ) {
            address
        } else {
            null
        }
    }

    suspend fun savePendingDeliveryAddressIfNeeded(
        userId: Int
    ) {
        val address = pendingDeliveryAddressToSave ?: return
        pendingDeliveryAddressToSave = null

        if (!shouldSaveDeliveryAddress(address)) return

        addressRepository.createAddress(userId, address)
            .onSuccess { savedAddress ->
                savedAddresses = savedAddresses + savedAddress
            }
            .onFailure { error ->
                Toast.makeText(
                    fragment.requireContext(),
                    error.message ?: "배송지를 저장하지 못했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    fun clearPendingDeliveryAddressSave() {
        pendingDeliveryAddressToSave = null
    }

    private fun applyLatestSavedAddressIfPossible() {
        if (savedAddresses.isEmpty()) return
        if (searchedRoadAddress != null) return
        if (roadAddressInput.text?.isNotBlank() == true) return

        val latestAddress = savedAddresses.maxByOrNull { it.id } ?: return
        val latestIndex = savedAddresses.indexOfFirst { it.id == latestAddress.id }

        if (latestIndex >= 0) {
            spinner.setSelection(latestIndex, false)
        }
        applySavedAddress(latestAddress.displayAddress)
    }

    private fun applyNewRoadAddressToInputs(roadAddress: String) {
        roadAddressInput.setText(roadAddress)
        detailAddressInput.text?.clear()
        detailAddressInput.requestFocus()
        onRoadAddressChanged?.invoke()
    }

    private fun clearAddressInputs() {
        roadAddressInput.text?.clear()
        detailAddressInput.text?.clear()
        onRoadAddressChanged?.invoke()
    }

    private fun shouldSaveDeliveryAddress(address: String): Boolean {
        val normalizedAddress = normalizeAddress(address)

        return normalizedAddress.isNotBlank() &&
                savedAddresses.none {
                    normalizeAddress(it.displayAddress) == normalizedAddress
                }
    }

    private fun normalizeAddress(address: String): String {
        return address.replace("\\s+".toRegex(), " ").trim()
    }

    companion object {
        private const val NEW_ADDRESS_LABEL = "새 배송지 검색"
    }
}
