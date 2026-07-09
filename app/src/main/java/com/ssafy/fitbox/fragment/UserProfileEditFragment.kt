package com.ssafy.fitbox.fragment

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.ssafy.fitbox.R
import com.ssafy.fitbox.databinding.FragmentUserProfileEditBinding
import com.ssafy.fitbox.dto.User
import com.ssafy.fitbox.repository.UserRepository
import com.ssafy.fitbox.util.SessionManager
import com.ssafy.fitbox.viewmodel.MyPageViewModel
import kotlinx.coroutines.launch

class UserProfileEditFragment : Fragment() {

    private var _binding: FragmentUserProfileEditBinding? = null
    private val binding get() = _binding!!

    private val userRepository = UserRepository()
    private val myPageViewModel: MyPageViewModel by activityViewModels()
    private lateinit var sessionManager: SessionManager
    private var currentUser: User? = null

    private val commonAllergies = listOf(
        "계란", "우유", "유당불내증", "땅콩", "대두", "밀", "메밀",
        "새우", "게", "갑각류", "생선", "복숭아", "토마토", "호두", "잣"
    )
    private val purposeOptions = listOf("다이어트", "벌크업", "유지어터", "운동 후 식사")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserProfileEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())
        currentUser = sessionManager.getUser()?.toSafeUser()

        val user = currentUser
        if (user == null) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        setupPurposeDropdown()
        bindUser(user)
        setupAllergyChips(user.allergies)

        if (arguments?.getBoolean(ARG_FROM_KAKAO_LOGIN, false) == true) {
            Toast.makeText(
                requireContext(),
                "카카오 로그인 완료! 회원 정보를 추가로 입력해주세요.",
                Toast.LENGTH_SHORT
            ).show()

            binding.etPhone.requestFocus()
        }

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.btnAddCustomAllergy.setOnClickListener {
            addCustomAllergyInput()
        }
        binding.etCustomAllergy.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addCustomAllergyInput()
                true
            } else {
                false
            }
        }
        binding.btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun bindUser(user: User) {
        binding.etName.setText(user.name)

        // 카카오 자동가입 때 서버에서 임시 전화번호로 KAKAO_카카오ID를 넣기 때문에
        // 수정 화면에서는 빈칸으로 보여주고 실제 전화번호를 입력하게 한다.
        binding.etPhone.setText(
            if (user.phone.startsWith("KAKAO_", ignoreCase = true)) "" else user.phone
        )

        binding.etAge.setText(
            if (user.age <= 0) "" else user.age.toString()
        )

        binding.etHeight.setText(
            if (user.height <= 0.0) "" else formatDecimal(user.height)
        )

        binding.etWeight.setText(
            if (user.weight <= 0.0) "" else formatDecimal(user.weight)
        )

        binding.etActivityLevel.setText(
            if (user.activityLevel < 0) "" else user.activityLevel.toString()
        )

        when (user.gender) {
            "M" -> binding.rgGender.check(R.id.rbMale)
            "F" -> binding.rgGender.check(R.id.rbFemale)
            else -> binding.rgGender.clearCheck()
        }

        binding.actPurpose.setText(
            when {
                user.purpose.contains("벌크") -> "벌크업"
                user.purpose.contains("유지") -> "유지어터"
                user.purpose.contains("운동") -> "운동 후 식사"
                else -> "다이어트"
            },
            false
        )
    }

    private fun setupPurposeDropdown() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            purposeOptions
        )
        binding.actPurpose.setAdapter(adapter)
    }

    private fun setupAllergyChips(selectedAllergies: List<String>) {
        binding.chipGroupAllergy.removeAllViews()
        commonAllergies.forEach { allergy ->
            addAllergyChip(
                allergy = allergy,
                checked = selectedAllergies.any { it.equals(allergy, ignoreCase = true) }
            )
        }

        selectedAllergies
            .filterNot { selected ->
                commonAllergies.any { common -> common.equals(selected, ignoreCase = true) }
            }
            .forEach { addAllergyChip(it, checked = true) }
    }

    private fun addCustomAllergyInput() {
        val rawInput = binding.etCustomAllergy.text?.toString().orEmpty()
        val allergies = rawInput
            .split(",", "\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (allergies.isEmpty()) {
            Toast.makeText(requireContext(), "추가할 알러지를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        allergies.forEach { addOrCheckAllergyChip(it) }
        binding.etCustomAllergy.text?.clear()
    }

    private fun addOrCheckAllergyChip(allergy: String) {
        val existing = (0 until binding.chipGroupAllergy.childCount)
            .mapNotNull { binding.chipGroupAllergy.getChildAt(it) as? Chip }
            .firstOrNull { it.text.toString().equals(allergy, ignoreCase = true) }

        if (existing != null) {
            existing.isChecked = true
        } else {
            addAllergyChip(allergy, checked = true)
        }
    }

    private fun addAllergyChip(allergy: String, checked: Boolean) {
        val checkedState = intArrayOf(android.R.attr.state_checked)
        val defaultState = intArrayOf()
        val chipBackgroundColors = ColorStateList(
            arrayOf(checkedState, defaultState),
            intArrayOf(
                ContextCompat.getColor(requireContext(), R.color.mypage_fern),
                ContextCompat.getColor(requireContext(), R.color.mypage_warm_white)
            )
        )
        val chipTextColors = ColorStateList(
            arrayOf(checkedState, defaultState),
            intArrayOf(
                ContextCompat.getColor(requireContext(), R.color.mypage_warm_white),
                ContextCompat.getColor(requireContext(), R.color.mypage_text_secondary)
            )
        )
        val chipStrokeColors = ColorStateList(
            arrayOf(checkedState, defaultState),
            intArrayOf(
                ContextCompat.getColor(requireContext(), R.color.mypage_fern),
                ContextCompat.getColor(requireContext(), R.color.mypage_fennel)
            )
        )
        val chip = Chip(requireContext()).apply {
            text = allergy
            isCheckable = true
            isChecked = checked
            chipBackgroundColor = chipBackgroundColors
            setTextColor(chipTextColors)
            chipStrokeColor = chipStrokeColors
            chipStrokeWidth = resources.getDimension(R.dimen.fit_divider_height)
            chipCornerRadius = resources.getDimension(R.dimen.fit_space_sm)
            setEnsureMinTouchTargetSize(false)
            isCloseIconVisible = !commonAllergies.any { it.equals(allergy, ignoreCase = true) }
            closeIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_chip_close)
            closeIconSize = resources.getDimension(R.dimen.fit_space_lg)
            closeIconStartPadding = resources.getDimension(R.dimen.fit_space_sm)
            closeIconEndPadding = resources.getDimension(R.dimen.fit_space_sm)
            chipEndPadding = resources.getDimension(R.dimen.fit_space_sm)
            setOnCloseIconClickListener {
                binding.chipGroupAllergy.removeView(this)
            }
        }
        binding.chipGroupAllergy.addView(chip)
    }

    private fun saveProfile() {
        val user = currentUser ?: return

        val name = binding.etName.text?.toString()?.trim().orEmpty()
        val phone = binding.etPhone.text?.toString()?.trim().orEmpty()
        val age = binding.etAge.text?.toString()?.trim()?.toIntOrNull()
        val height = binding.etHeight.text?.toString()?.trim()?.toDoubleOrNull()
        val weight = binding.etWeight.text?.toString()?.trim()?.toDoubleOrNull()
        val activityLevel = binding.etActivityLevel.text?.toString()?.trim()?.toIntOrNull()

        if (name.isBlank() || phone.isBlank()) {
            Toast.makeText(requireContext(), "이름과 전화번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (age == null || age <= 0) {
            Toast.makeText(requireContext(), "나이를 정확히 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (height == null || height <= 0.0 || weight == null || weight <= 0.0) {
            Toast.makeText(requireContext(), "키와 몸무게를 정확히 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (activityLevel == null || activityLevel < 0) {
            Toast.makeText(requireContext(), "주당 운동 횟수를 정확히 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (binding.rgGender.checkedRadioButtonId == -1) {
            Toast.makeText(requireContext(), "성별을 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val allergies = getSelectedAllergies()
        val updatedUser = user.copy(
            name = name,
            phone = phone,
            gender = if (binding.rbMale.isChecked) "M" else "F",
            age = age,
            height = height,
            weight = weight,
            activityLevel = activityLevel,
            purpose = getSelectedPurpose(),
            allergies = allergies
        )

        binding.btnSave.isEnabled = false
        binding.btnSave.text = "저장 중..."

        viewLifecycleOwner.lifecycleScope.launch {
            val userResult = userRepository.updateUser(updatedUser)
            val allergyResult = userRepository.updateUserAllergies(updatedUser.id, allergies)

            binding.btnSave.isEnabled = true
            binding.btnSave.text = "저장하기"

            if (userResult.isSuccess && allergyResult.isSuccess) {
                sessionManager.createSession(updatedUser)
                myPageViewModel.resetAnalysis()
                parentFragmentManager.setFragmentResult(
                    REQUEST_KEY,
                    bundleOf(KEY_UPDATED to true)
                )
                Toast.makeText(requireContext(), "회원 정보가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                if (arguments?.getBoolean(ARG_FROM_KAKAO_LOGIN, false) == true) {
                    parentFragmentManager.popBackStack(
                        null,
                        androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
                    )

                    parentFragmentManager.beginTransaction()
                        .replace(R.id.main_container, MyPageFragment())
                        .commit()
                } else {
                    parentFragmentManager.popBackStack()
                }
            } else {
                val message = userResult.exceptionOrNull()?.message
                    ?: allergyResult.exceptionOrNull()?.message
                    ?: "회원 정보 저장에 실패했습니다."
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getSelectedAllergies(): List<String> {
        return (0 until binding.chipGroupAllergy.childCount)
            .mapNotNull { binding.chipGroupAllergy.getChildAt(it) as? Chip }
            .filter { it.isChecked }
            .map { it.text.toString().trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
    }

    private fun getSelectedPurpose(): String {
        return binding.actPurpose.text?.toString()
            ?.takeIf { it in purposeOptions }
            ?: "다이어트"
    }

    private fun formatDecimal(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.1f", value)
    }

    private fun User.toSafeUser(): User {
        return copy(allergies = allergies ?: emptyList())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val REQUEST_KEY = "user_profile_edit_result"
        const val KEY_UPDATED = "updated"

        private const val ARG_FROM_KAKAO_LOGIN = "fromKakaoLogin"

        fun newKakaoLoginInstance(): UserProfileEditFragment {
            return UserProfileEditFragment().apply {
                arguments = bundleOf(ARG_FROM_KAKAO_LOGIN to true)
            }
        }
    }
}
