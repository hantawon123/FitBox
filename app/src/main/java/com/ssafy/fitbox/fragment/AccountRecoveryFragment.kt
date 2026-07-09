package com.ssafy.fitbox.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ssafy.fitbox.R
import com.ssafy.fitbox.databinding.FragmentAccountRecoveryBinding
import com.ssafy.fitbox.repository.UserRepository
import kotlinx.coroutines.launch

class AccountRecoveryFragment : Fragment() {

    private var _binding: FragmentAccountRecoveryBinding? = null
    private val binding get() = _binding!!

    private val userRepository = UserRepository()
    private val mode: RecoveryMode by lazy {
        RecoveryMode.from(arguments?.getString(ARG_MODE))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAccountRecoveryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupModeUi()
        setupPhoneInputFormatter()
        setupClickEvents()
    }

    private fun setupPhoneInputFormatter() {
        binding.etRecoveryPhone.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) {
                    return
                }

                val digits = s?.toString().orEmpty()
                    .filter { it.isDigit() }
                    .take(11)
                val formatted = formatPhoneNumber(digits)

                if (s?.toString() == formatted) {
                    return
                }

                isFormatting = true
                binding.etRecoveryPhone.setText(formatted)
                binding.etRecoveryPhone.setSelection(formatted.length)
                isFormatting = false
            }
        })
    }

    private fun setupModeUi() {
        val isFindId = mode == RecoveryMode.FIND_ID
        binding.tvRecoveryTitle.text = if (isFindId) "아이디 찾기" else "비밀번호 찾기"
        binding.tvRecoveryDescription.text = if (isFindId) {
            "가입 시 입력한 이름과 전화번호를 입력해주세요."
        } else {
            "아이디, 이름, 전화번호가 일치하면 새 비밀번호로 변경할 수 있어요."
        }
        binding.btnRecoverySubmit.text = if (isFindId) "아이디 찾기" else "비밀번호 변경하기"
        binding.tilRecoveryUserId.visibility = if (isFindId) View.GONE else View.VISIBLE
        binding.tilRecoveryNewPassword.visibility = if (isFindId) View.GONE else View.VISIBLE
        binding.tilRecoveryNewPasswordConfirm.visibility = if (isFindId) View.GONE else View.VISIBLE
    }

    private fun setupClickEvents() {
        binding.btnRecoverySubmit.setOnClickListener {
            when (mode) {
                RecoveryMode.FIND_ID -> findUserId()
                RecoveryMode.RESET_PASSWORD -> resetPassword()
            }
        }

        binding.btnRecoveryBackToLogin.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun findUserId() {
        clearErrors()
        val name = binding.etRecoveryName.text.toString().trim()
        val phone = binding.etRecoveryPhone.text.toString().trim()

        if (!validateNameAndPhone(name, phone)) {
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            val result = userRepository.findUserId(name, phone)
            setLoading(false)
            result.onSuccess { userId ->
                binding.tvRecoveryResult.visibility = View.VISIBLE
                binding.tvRecoveryResult.text = "가입된 아이디는 $userId 입니다."
            }.onFailure { error ->
                binding.tvRecoveryResult.visibility = View.GONE
                Toast.makeText(
                    requireContext(),
                    error.message ?: "아이디 찾기에 실패했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun resetPassword() {
        clearErrors()
        val userId = binding.etRecoveryUserId.text.toString().trim()
        val name = binding.etRecoveryName.text.toString().trim()
        val phone = binding.etRecoveryPhone.text.toString().trim()
        val newPassword = binding.etRecoveryNewPassword.text.toString().trim()
        val confirmPassword = binding.etRecoveryNewPasswordConfirm.text.toString().trim()

        var isValid = true
        if (userId.isBlank()) {
            binding.tilRecoveryUserId.error = "아이디를 입력해주세요."
            isValid = false
        }
        if (!validateNameAndPhone(name, phone)) {
            isValid = false
        }
        if (!isPasswordValid(newPassword)) {
            binding.tilRecoveryNewPassword.error =
                "비밀번호는 8~16자리 영문(대문자 포함), 숫자, 특수문자 조합이어야 합니다."
            isValid = false
        }
        if (confirmPassword.isBlank()) {
            binding.tilRecoveryNewPasswordConfirm.error = "비밀번호 확인을 입력해주세요."
            isValid = false
        } else if (newPassword != confirmPassword) {
            binding.tilRecoveryNewPasswordConfirm.error = "비밀번호가 일치하지 않습니다."
            isValid = false
        }

        if (!isValid) {
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            val result = userRepository.resetPassword(userId, name, phone, newPassword)
            setLoading(false)
            result.onSuccess {
                Toast.makeText(requireContext(), "비밀번호가 변경되었습니다.", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }.onFailure { error ->
                Toast.makeText(
                    requireContext(),
                    error.message ?: "비밀번호 변경에 실패했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun validateNameAndPhone(name: String, phone: String): Boolean {
        var isValid = true
        if (name.isBlank()) {
            binding.tilRecoveryName.error = "이름을 입력해주세요."
            isValid = false
        }
        if (phone.isBlank()) {
            binding.tilRecoveryPhone.error = "전화번호를 입력해주세요."
            isValid = false
        }
        if (phone.isNotBlank() && !phone.matches(Regex("^010-\\d{4}-\\d{4}$"))) {
            binding.tilRecoveryPhone.error = "010-xxxx-xxxx 또는 010xxxxxxxx 형식으로 입력해주세요."
            isValid = false
        }
        return isValid
    }

    private fun formatPhoneNumber(digits: String): String {
        return when {
            digits.length <= 3 -> digits
            digits.length <= 7 -> "${digits.substring(0, 3)}-${digits.substring(3)}"
            else -> "${digits.substring(0, 3)}-${digits.substring(3, 7)}-${digits.substring(7)}"
        }
    }

    private fun clearErrors() {
        binding.tilRecoveryUserId.error = null
        binding.tilRecoveryName.error = null
        binding.tilRecoveryPhone.error = null
        binding.tilRecoveryNewPassword.error = null
        binding.tilRecoveryNewPasswordConfirm.error = null
    }

    private fun isPasswordValid(password: String): Boolean {
        val passwordRegex = Regex("^(?=.*[A-Za-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*#?&])[A-Za-z\\d@\$!%*#?&]{8,16}$")
        return password.matches(passwordRegex)
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnRecoverySubmit.isEnabled = !isLoading
        binding.btnRecoverySubmit.text = if (isLoading) {
            "확인 중..."
        } else {
            if (mode == RecoveryMode.FIND_ID) "아이디 찾기" else "비밀번호 변경하기"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    enum class RecoveryMode {
        FIND_ID,
        RESET_PASSWORD;

        companion object {
            fun from(value: String?): RecoveryMode {
                return entries.firstOrNull { it.name == value } ?: FIND_ID
            }
        }
    }

    companion object {
        private const val ARG_MODE = "mode"

        fun newFindIdInstance(): AccountRecoveryFragment {
            return newInstance(RecoveryMode.FIND_ID)
        }

        fun newResetPasswordInstance(): AccountRecoveryFragment {
            return newInstance(RecoveryMode.RESET_PASSWORD)
        }

        private fun newInstance(mode: RecoveryMode): AccountRecoveryFragment {
            return AccountRecoveryFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_MODE, mode.name)
                }
            }
        }
    }
}
