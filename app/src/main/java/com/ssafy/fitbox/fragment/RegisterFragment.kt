package com.ssafy.fitbox.fragment

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.chip.Chip
import com.ssafy.fitbox.R
import com.ssafy.fitbox.activity.MainActivity
import com.ssafy.fitbox.databinding.FragmentRegisterBinding
import com.ssafy.fitbox.dto.User
import com.ssafy.fitbox.notification.FcmTokenRegistrar
import com.ssafy.fitbox.repository.UserRepository
import com.ssafy.fitbox.util.SessionManager
import kotlinx.coroutines.launch

class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private var selectedPurpose: String = ""
    private var isIdChecked = false
    private var isSubmitting = false
    private val userRepository = UserRepository()
    private lateinit var sessionManager: SessionManager
    private val isKakaoRegistration: Boolean
        get() = arguments?.getBoolean(ARG_FROM_KAKAO_LOGIN, false) == true

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        if (isKakaoRegistration) {
            bindKakaoUserForRegistration()
            binding.progressRegistration.max = KAKAO_TOTAL_STEPS
        }

        setupInputListeners()
        updateButtons()
        setupBackPressedHandler()

        // 다음 버튼 & 가입 완료 로직
        binding.btnNext.setOnClickListener {
            val currentStep = binding.viewFlipper.displayedChild

            if (currentStep == 0) {
                // 1단계에서 다음 누를 때 전화번호 중복 검사 실행
                val phone = binding.etRegPhone.text.toString().trim()
                if (shouldSkipPhoneDuplicateCheck(phone)) {
                    goToNextStep()
                    return@setOnClickListener
                }
                lifecycleScope.launch {
                    val result = userRepository.checkPhone(phone)
                    result.onSuccess { isDuplicate ->
                        if (isDuplicate) {
                            Toast.makeText(requireContext(), "이미 가입된 전화번호입니다.", Toast.LENGTH_SHORT).show()
                        } else {
                            goToNextStep()
                        }
                    }.onFailure { error ->
                        Toast.makeText(requireContext(), "통신 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else if (currentStep < binding.viewFlipper.childCount - 1) {
                goToNextStep() // 2단계, 3단계 통과
            } else {
                completeRegistration() // 마지막 4단계 통과 (가입)
            }
        }

        // 이전 버튼 로직
        binding.btnPrevious.setOnClickListener {
            if (binding.viewFlipper.displayedChild > 0) {
                goToPreviousStep()
            } else if (isKakaoRegistration) {
                showKakaoRegistrationExitDialog()
            } else {
                parentFragmentManager.popBackStack()
            }
        }

        // 아이디 중복확인 버튼 클릭 이벤트
        binding.btnCheckId.setOnClickListener {
            val userId = binding.etRegUserId.text.toString().trim()
            if (userId.isEmpty()) {
                binding.etRegUserId.error = "아이디를 입력해주세요"
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val result = userRepository.checkId(userId)

                result.onSuccess { isDuplicate ->
                    binding.tvIdCheckResult.visibility = View.VISIBLE
                    if (isDuplicate) {
                        binding.tvIdCheckResult.text = "중복된 아이디가 존재합니다."
                        binding.tvIdCheckResult.setTextColor(Color.RED)
                        isIdChecked = false
                    } else {
                        binding.tvIdCheckResult.text = "사용 가능한 아이디입니다."
                        binding.tvIdCheckResult.setTextColor(Color.parseColor("#4CAF50")) // 초록색
                        isIdChecked = true
                    }
                    updateNextButtonState()
                }.onFailure { error ->
                    Toast.makeText(requireContext(), "중복 확인 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.etRegUserId.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                isIdChecked = false
                binding.tvIdCheckResult.visibility = View.GONE
                updateNextButtonState()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.btnRegAddCustomAllergy.setOnClickListener {
            addCustomAllergyInput()
        }
        binding.etRegCustomAllergy.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addCustomAllergyInput()
                true
            } else {
                false
            }
        }
    }

    private fun setupBackPressedHandler() {
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isSubmitting) return

                    if (binding.viewFlipper.displayedChild > 0) {
                        goToPreviousStep()
                    } else if (isKakaoRegistration) {
                        showKakaoRegistrationExitDialog()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        )
    }

    private fun showKakaoRegistrationExitDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("회원가입을 취소할까요?")
            .setMessage("카카오 회원가입을 완료하지 않으면 아직 FitBox 계정이 생성되지 않습니다.")
            .setNegativeButton("계속 작성") { dialog, _ -> dialog.dismiss() }
            .setPositiveButton("취소하기") { _, _ ->
                sessionManager.clearSession()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, LoginFragment())
                    .commit()
            }
            .show()
    }

    private fun shouldSkipPhoneDuplicateCheck(phone: String): Boolean {
        if (!isKakaoRegistration) return false
        val currentPhone = sessionManager.getUser()?.phone.orEmpty()
        if (currentPhone.isBlank() || currentPhone.startsWith("KAKAO_", ignoreCase = true)) {
            return false
        }
        return phone == currentPhone
    }

    private fun goToNextStep() {
        binding.viewFlipper.inAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_right)
        binding.viewFlipper.outAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_out_left)
        binding.viewFlipper.displayedChild = nextStepIndex(binding.viewFlipper.displayedChild)
        updateButtons()
    }

    private fun goToPreviousStep() {
        binding.viewFlipper.inAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_in_left)
        binding.viewFlipper.outAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_out_right)
        binding.viewFlipper.displayedChild = previousStepIndex(binding.viewFlipper.displayedChild)
        updateButtons()
    }

    private fun nextStepIndex(currentStep: Int): Int {
        return if (isKakaoRegistration && currentStep == STEP_BODY) {
            STEP_PURPOSE
        } else {
            (currentStep + 1).coerceAtMost(binding.viewFlipper.childCount - 1)
        }
    }

    private fun previousStepIndex(currentStep: Int): Int {
        return if (isKakaoRegistration && currentStep == STEP_PURPOSE) {
            STEP_BODY
        } else {
            (currentStep - 1).coerceAtLeast(0)
        }
    }

    private fun bindKakaoUserForRegistration() {
        val user = sessionManager.getUser() ?: return

        binding.etRegUserId.setText(user.userId)
        binding.etRegPassword.setText(user.password)
        binding.etRegPasswordConfirm.setText(user.password)
        isIdChecked = true

        binding.etRegName.setText(user.name)
        binding.etRegPhone.setText(
            if (user.phone.startsWith("KAKAO_", ignoreCase = true)) "" else user.phone
        )
        if (user.age > 0) binding.etRegAge.setText(user.age.toString())
        if (user.height > 0.0) binding.etRegHeight.setText(formatDecimal(user.height))
        if (user.weight > 0.0) binding.etRegWeight.setText(formatDecimal(user.weight))
        if (user.activityLevel >= 0) binding.etRegActivityLevel.setText(user.activityLevel.toString())

        when (user.gender) {
            "M" -> binding.rgGender.check(R.id.rbMale)
            "F" -> binding.rgGender.check(R.id.rbFemale)
        }

        user.allergies.orEmpty().forEach { addOrCheckAllergyChip(it) }
    }

    // 🌟 알레르기 칩 그룹에서 선택된 텍스트들을 리스트로 추출하는 함수 추가
    private fun getSelectedAllergies(): List<String> {
        val selectedAllergies = mutableListOf<String>()
        binding.chipGroupAllergy.checkedChipIds.forEach { id ->
            val chip = binding.root.findViewById<Chip>(id)
            selectedAllergies.add(chip.text.toString())
        }
        return selectedAllergies.distinctBy { it.lowercase() }
    }

    private fun addCustomAllergyInput() {
        val allergies = binding.etRegCustomAllergy.text?.toString().orEmpty()
            .split(",", "\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (allergies.isEmpty()) {
            Toast.makeText(requireContext(), "추가할 알러지를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        allergies.forEach { addOrCheckAllergyChip(it) }
        binding.etRegCustomAllergy.text?.clear()
    }

    private fun addOrCheckAllergyChip(allergy: String) {
        val existing = (0 until binding.chipGroupAllergy.childCount)
            .mapNotNull { binding.chipGroupAllergy.getChildAt(it) as? Chip }
            .firstOrNull { it.text.toString().equals(allergy, ignoreCase = true) }

        if (existing != null) {
            existing.isChecked = true
        } else {
            addCustomAllergyChip(allergy)
        }
    }

    private fun addCustomAllergyChip(allergy: String) {
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
            id = View.generateViewId()
            text = allergy
            isCheckable = true
            isChecked = true
            chipBackgroundColor = chipBackgroundColors
            setTextColor(chipTextColors)
            chipStrokeColor = chipStrokeColors
            chipStrokeWidth = resources.getDimension(R.dimen.fit_divider_height)
            chipCornerRadius = resources.getDimension(R.dimen.fit_space_sm)
            setEnsureMinTouchTargetSize(false)
            isCloseIconVisible = true
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

    private fun setupInputListeners() {
        val defaultTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateNextButtonState()
            }
            override fun afterTextChanged(s: Editable?) {}
        }

        // 기존 전화번호 포맷팅 로직 복구
        binding.etRegPhone.addTextChangedListener(object : TextWatcher {
            private var isFormatting = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormatting) return
                isFormatting = true
                val originalText = s.toString()
                val digitsOnly = originalText.replace(Regex("\\D"), "")
                val formatted = StringBuilder()
                for (i in digitsOnly.indices) {
                    if (i == 3 || i == 7) formatted.append("-")
                    formatted.append(digitsOnly[i])
                }
                if (originalText != formatted.toString()) {
                    s?.replace(0, s.length, formatted.toString())
                }
                isFormatting = false
                updateNextButtonState()
            }
        })

        // 리스너 등록
        binding.etRegName.addTextChangedListener(defaultTextWatcher)
        binding.etRegAge.addTextChangedListener(defaultTextWatcher)
        binding.rgGender.setOnCheckedChangeListener { _, _ -> updateNextButtonState() }

        // 신체 정보 리스너 등록
        binding.etRegHeight.addTextChangedListener(defaultTextWatcher)
        binding.etRegWeight.addTextChangedListener(defaultTextWatcher)
        binding.etRegActivityLevel.addTextChangedListener(defaultTextWatcher)

        binding.etRegPassword.addTextChangedListener(defaultTextWatcher)
        binding.etRegPasswordConfirm.addTextChangedListener(defaultTextWatcher)

        // STEP 4 카드 선택 리스너
        val purposeCards = listOf(binding.cardDiet, binding.cardBulk, binding.cardMaintain, binding.cardPostWorkout)
        val purposeNames = listOf("다이어트", "벌크업", "유지어터", "운동 후 식사")

        purposeCards.forEachIndexed { index, card ->
            card.setOnClickListener {
                selectedPurpose = purposeNames[index]
                purposeCards.forEach { c ->
                    c.strokeColor = requireContext().getColor(R.color.mypage_fennel)
                    c.strokeWidth = 1
                    c.setCardBackgroundColor(requireContext().getColor(R.color.mypage_warm_white))
                }
                card.strokeColor = requireContext().getColor(R.color.mypage_fern)
                card.strokeWidth = 4
                card.setCardBackgroundColor(requireContext().getColor(R.color.mypage_secondary_soft))

                updateButtons()
            }
        }

        // [핵심] 기존 AI봇 상담 기능 복구 & 신체 정보/알레르기 연동
        binding.btnAiConsult.setOnClickListener {
            val gender = if (binding.rbMale.isChecked) "M" else "F"
            val ageStr = binding.etRegAge.text.toString().trim()
            val age = if (ageStr.isNotEmpty()) ageStr.toInt() else 0

            val heightStr = binding.etRegHeight.text.toString().trim()
            val height = if (heightStr.isNotEmpty()) heightStr.toDouble() else 0.0

            val weightStr = binding.etRegWeight.text.toString().trim()
            val weight = if (weightStr.isNotEmpty()) weightStr.toDouble() else 0.0

            val activityStr = binding.etRegActivityLevel.text.toString().trim()
            val activityLevel = if (activityStr.isNotEmpty()) activityStr.toInt() else 0

            val newUser = User(
                userId = binding.etRegUserId.text.toString().trim(),
                password = binding.etRegPassword.text.toString().trim(),
                name = binding.etRegName.text.toString().trim(),
                phone = binding.etRegPhone.text.toString().trim(),
                gender = gender,
                age = age,
                height = height,
                weight = weight,
                activityLevel = activityLevel,
                purpose = "",
                allergies = getSelectedAllergies() // 🌟 알레르기 목록 담기!
            )

            val fragment = ConsultationChatFragment.newInstance(newUser)

            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun checkCurrentStepValidation(): Boolean {
        return when (binding.viewFlipper.displayedChild) {
            0 -> { // 1단계: 기본 정보
                val name = binding.etRegName.text.toString().trim()
                val phone = binding.etRegPhone.text.toString().trim()
                val age = binding.etRegAge.text.toString().trim()
                val isGenderSelected = binding.rgGender.checkedRadioButtonId != -1
                val isPhoneValid = phone.matches(Regex("^010-\\d{4}-\\d{4}$"))

                if (phone.isNotEmpty() && !isPhoneValid) {
                    binding.etRegPhone.error = "010-xxxx-xxxx 형식으로 입력해주세요"
                } else {
                    binding.etRegPhone.error = null
                }
                name.isNotEmpty() && phone.isNotEmpty() && isPhoneValid && age.isNotEmpty() && isGenderSelected
            }
            1 -> { // 2단계: 신체 정보 (알레르기는 선택사항이므로 유효성 검사에서 통과시킴)
                val height = binding.etRegHeight.text.toString().trim()
                val weight = binding.etRegWeight.text.toString().trim()
                val activity = binding.etRegActivityLevel.text.toString().trim()
                height.isNotEmpty() && weight.isNotEmpty() && activity.isNotEmpty()
            }
            2 -> { // 3단계: 계정 정보
                if (isKakaoRegistration) return true

                val userId = binding.etRegUserId.text.toString().trim()
                val password = binding.etRegPassword.text.toString().trim()
                val passwordConfirm = binding.etRegPasswordConfirm.text.toString().trim()

                val passwordRegex = Regex("^(?=.*[A-Za-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*#?&])[A-Za-z\\d@\$!%*#?&]{8,16}$")
                val isPasswordValid = password.matches(passwordRegex)

                if (password.isNotEmpty() && !isPasswordValid) {
                    binding.tilRegPassword.error = "비밀번호는 8~16자리 영문(대문자 포함), 숫자, 특수문자 조합이어야 합니다."
                } else {
                    binding.tilRegPassword.error = null
                }

                if (passwordConfirm.isNotEmpty() && password != passwordConfirm) {
                    binding.tilRegPasswordConfirm.error = "비밀번호가 일치하지 않습니다"
                } else {
                    binding.tilRegPasswordConfirm.error = null
                }

                isIdChecked && userId.isNotEmpty() && isPasswordValid && passwordConfirm.isNotEmpty() && (password == passwordConfirm)
            }
            3 -> true // 4단계: 식사 목적 (건너뛰기 가능하므로 항상 true)
            else -> false
        }
    }

    private fun updateNextButtonState() {
        binding.btnNext.isEnabled = !isSubmitting && checkCurrentStepValidation()
    }

    private fun updateButtons() {
        binding.btnPrevious.visibility = View.VISIBLE
        val currentStep = binding.viewFlipper.displayedChild
        val stepLabels = if (isKakaoRegistration) {
            listOf("기본 정보", "신체 정보", "식사 목적")
        } else {
            listOf("기본 정보", "신체 정보", "계정 정보", "식사 목적")
        }
        val visibleStepIndex = if (isKakaoRegistration && currentStep == STEP_PURPOSE) {
            2
        } else {
            currentStep
        }
        val totalStepCount = if (isKakaoRegistration) KAKAO_TOTAL_STEPS else binding.viewFlipper.childCount

        binding.tvRegStep.text =
            "${visibleStepIndex + 1} / $totalStepCount · ${stepLabels[visibleStepIndex]}"
        binding.progressRegistration.progress = visibleStepIndex + 1

        if (currentStep == binding.viewFlipper.childCount - 1) {
            binding.btnNext.text = if (selectedPurpose.isEmpty()) "건너뛰기" else "가입 완료"
        } else {
            binding.btnNext.text = "다음"
        }
        updateNextButtonState()
    }

    private fun completeRegistration() {
        if (isSubmitting) return

        val kakaoUser = if (isKakaoRegistration) sessionManager.getUser() else null
        val name = binding.etRegName.text.toString().trim()
        val phone = binding.etRegPhone.text.toString().trim()
        val userId = kakaoUser?.userId ?: binding.etRegUserId.text.toString().trim()
        val password = kakaoUser?.password ?: binding.etRegPassword.text.toString().trim()
        val purpose = if (selectedPurpose.isEmpty()) "미지정" else selectedPurpose
        val gender = if (binding.rbMale.isChecked) "M" else "F"

        val ageStr = binding.etRegAge.text.toString().trim()
        val age = if (ageStr.isNotEmpty()) ageStr.toInt() else 0

        val heightStr = binding.etRegHeight.text.toString().trim()
        val height = if (heightStr.isNotEmpty()) heightStr.toDouble() else 0.0

        val weightStr = binding.etRegWeight.text.toString().trim()
        val weight = if (weightStr.isNotEmpty()) weightStr.toDouble() else 0.0

        val activityStr = binding.etRegActivityLevel.text.toString().trim()
        val activityLevel = if (activityStr.isNotEmpty()) activityStr.toInt() else 0

        val newUser = User(
            id = kakaoUser?.id ?: 0,
            userId = userId,
            password = password,
            name = name,
            phone = phone,
            gender = gender,
            age = age,
            height = height,
            weight = weight,
            activityLevel = activityLevel,
            purpose = purpose,
            allergies = getSelectedAllergies() // 🌟 최종 가입 완료 시에도 알레르기 목록 담기!
        )

        setRegistrationLoading(true)
        lifecycleScope.launch {
            val result = if (isKakaoRegistration) {
                if (newUser.id > 0) {
                    userRepository.updateUser(newUser)
                } else {
                    userRepository.register(newUser)
                }
            } else {
                userRepository.register(newUser)
            }

            result.onSuccess { message ->
                Log.d("Register", "가입/수정 성공: $message")
                // 💡 참고: 백엔드의 register 로직에서 넘어온 userId 값을 이용해
                // 여기서 UserRepository를 통해 아까 만든 POST /users/{userId}/allergies API를 호출하도록
                // UserRepository 로직을 향후 업데이트 하시면 완벽합니다!

                if (isKakaoRegistration) {
                    val savedUser = if (newUser.id > 0) {
                        newUser
                    } else {
                        userRepository.getUserByUserId(newUser.userId).getOrElse { newUser }
                    }

                    if (savedUser.id > 0) {
                        userRepository.updateUserAllergies(savedUser.id, newUser.allergies)
                    }
                    sessionManager.createSession(savedUser.copy(allergies = newUser.allergies))
                    FcmTokenRegistrar.registerCurrentUser(requireContext().applicationContext)
                    Toast.makeText(requireContext(), "${newUser.name}님 환영합니다.", Toast.LENGTH_SHORT).show()
                    (requireActivity() as? MainActivity)?.onKakaoLoginSuccessGoHome()
                } else {
                    setRegistrationLoading(false)
                    Toast.makeText(requireContext(), "${newUser.name}님, FitBox 가입을 환영합니다!", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.main_container, WelcomeFragment())
                        .commit()
                }

            }.onFailure { error ->
                setRegistrationLoading(false)
                Log.e("Register", "가입/통신 에러: ${error.message}")
                Toast.makeText(requireContext(), "가입 실패: 중복된 아이디이거나 서버 오류입니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setRegistrationLoading(isLoading: Boolean) {
        isSubmitting = isLoading
        binding.progressRegistrationSubmit.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnNext.text = if (isLoading) "" else {
            if (binding.viewFlipper.displayedChild == binding.viewFlipper.childCount - 1) {
                if (selectedPurpose.isEmpty()) "건너뛰기" else "가입 완료"
            } else {
                "다음"
            }
        }
        binding.btnPrevious.isEnabled = !isLoading
        binding.btnNext.isEnabled = !isLoading && checkCurrentStepValidation()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun formatDecimal(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format("%.1f", value)
        }
    }

    companion object {
        private const val ARG_FROM_KAKAO_LOGIN = "fromKakaoLogin"
        private const val STEP_BASIC = 0
        private const val STEP_BODY = 1
        private const val STEP_ACCOUNT = 2
        private const val STEP_PURPOSE = 3
        private const val KAKAO_TOTAL_STEPS = 3

        fun newKakaoLoginInstance(): RegisterFragment {
            return RegisterFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_FROM_KAKAO_LOGIN, true)
                }
            }
        }
    }
}
