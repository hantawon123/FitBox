package com.ssafy.fitbox.fragment

import android.graphics.Color
import android.app.Dialog
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.button.MaterialButton
import com.ssafy.fitbox.R
import com.ssafy.fitbox.activity.MainActivity
import com.ssafy.fitbox.databinding.FragmentMyPageBinding
import com.ssafy.fitbox.dto.DietReport
import com.ssafy.fitbox.dto.User
import com.ssafy.fitbox.util.DietReportCache
import com.ssafy.fitbox.util.FavoriteMealStore
import com.ssafy.fitbox.util.SessionManager
import com.ssafy.fitbox.viewmodel.MyPageViewModel

class MyPageFragment : Fragment() {

    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!

    // [핵심 수정] by viewModels() 에서 by activityViewModels() 로 변경!
    // 이제 이 뷰모델은 다른 탭 화면에 갔다 와도 파괴되지 않고 데이터를 온전히 기억합니다.
    private val myPageViewModel: MyPageViewModel by activityViewModels()

    // AI 추천 조합 박스의 확장/축소 상태를 제어하는 플래그 변수
    private var isAiReportExpanded = false
    private var currentAiReport: DietReport? = null
    private var currentUserId: Int? = null

    private val openAiApiToken: String by lazy {
        val key = getString(R.string.gms_key)
        if (key.startsWith("Bearer ")) key else "Bearer $key"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        DietReportCache.initialize(requireContext())
        FavoriteMealStore.initialize(requireContext())

        parentFragmentManager.setFragmentResultListener(
            UserProfileEditFragment.REQUEST_KEY,
            viewLifecycleOwner
        ) { _, _ ->
            myPageViewModel.resetAnalysis()
            parentFragmentManager.beginTransaction()
                .replace(R.id.main_container, MyPageFragment())
                .commit()
        }

        val sessionManager = SessionManager(requireContext())
        val savedUser = sessionManager.getUser()
        val currentUser = savedUser?.toSafeUser()
        currentUserId = currentUser?.id
        configureMenuRows()

        if (sessionManager.isLoggedIn() && currentUser != null) {
            binding.btnGoLogin.visibility = View.GONE
            binding.btnLogout.visibility = View.VISIBLE
            binding.cardMyPageMenus.visibility = View.VISIBLE
            binding.btnEditProfile.visibility = View.VISIBLE

            binding.tvProfileName.text = "${currentUser.name} 님"
            binding.tvUserPurpose.text = "현재 목표: ${currentUser.purpose}"
            binding.tvUserPurpose.visibility = View.VISIBLE

            binding.layoutBodyInfo.visibility = View.VISIBLE
            bindProfileSummary(currentUser)
            binding.btnEditProfile.setOnClickListener {
                openMyPageMenu(UserProfileEditFragment())
            }

            binding.tvMyPageAiTitle.text = "AI가 분석한 ${currentUser.purpose} 맞춤 조합"
            binding.cardMyPageAiReport.visibility = View.VISIBLE

            // 접었다 폈다 토글 버튼의 클릭 이벤트 리스너 세팅
            binding.tvAiExpandToggle.setOnClickListener {
                isAiReportExpanded = !isAiReportExpanded
                if (isAiReportExpanded) {
                    binding.layoutAiResultContainer.visibility = View.VISIBLE
                    binding.tvAiExpandToggle.setImageResource(R.drawable.ic_expand_less)
                    binding.tvAiExpandToggle.contentDescription = "식단 분석 접기"
                } else {
                    binding.layoutAiResultContainer.visibility = View.GONE
                    binding.tvAiExpandToggle.setImageResource(R.drawable.ic_expand_more)
                    binding.tvAiExpandToggle.contentDescription = "식단 분석 펼치기"
                }
            }

            // AI 시동 제어 및 연산 유도 호출
            checkEngineAndAnalyze(currentUser)

            // 뷰모델 데이터 관찰 (Result<DietReport> 수신 및 동적 UI 그리기)
            myPageViewModel.aiReport.observe(viewLifecycleOwner) { result ->
                // 데이터 혹은 에러가 도착했으므로 로딩바 해제
                showAiLoading(false)

                result.onSuccess { report ->
                    // 데이터 수신 성공 시 토글 버튼 활성화
                    binding.tvAiExpandToggle.visibility = View.VISIBLE
                    currentAiReport = report.takeIf { it.items.isNotEmpty() }
                    binding.btnAddAiFavorite.visibility =
                        if (report.items.isNotEmpty()) View.VISIBLE else View.GONE

                    // 기존 UI 조건 유지: 처음 로드되거나 보존 시 기본 접힘 상태 세팅
                    if (!isAiReportExpanded) {
                        binding.tvAiExpandToggle.setImageResource(R.drawable.ic_expand_more)
                        binding.tvAiExpandToggle.contentDescription = "식단 분석 펼치기"
                        binding.layoutAiResultContainer.visibility = View.GONE
                    } else {
                        binding.tvAiExpandToggle.setImageResource(R.drawable.ic_expand_less)
                        binding.tvAiExpandToggle.contentDescription = "식단 분석 접기"
                        binding.layoutAiResultContainer.visibility = View.VISIBLE
                    }

                    // 구조화된 텍스트뷰 데이터 바인딩
                    binding.tvTotalCalories.text = "${String.format("%.1f", report.totalCalories)} kcal"
                    binding.tvCarb.text = "탄수화물\n${String.format("%.1f", report.totalCarbs)}g"
                    binding.tvProtein.text = "단백질\n${String.format("%.1f", report.totalProtein)}g"
                    binding.tvFat.text = "지방\n${String.format("%.1f", report.totalFat)}g"
                    binding.tvTotalPrice.text = "가격: ${report.totalPrice}원"
                    binding.tvAiReason.text = report.reason

                    // 동적 재료 리스트 초기화 후 추가 생성 루프
                    binding.layoutIngredientsList.removeAllViews()
                    for (item in report.items) {
                        val verticalPadding = resources.getDimensionPixelSize(R.dimen.fit_space_xs)
                        val ingredientBottomMargin = resources.getDimensionPixelSize(R.dimen.fit_space_xs)
                        val itemTextView = TextView(requireContext()).apply {
                            text = "· ${item.name} ${item.amount}g  ${String.format("%.1f", item.calories)}kcal / ${String.format("%,d", item.price)}원"
                            textSize = 13f
                            setTextColor(requireContext().getColor(R.color.mypage_text))
                            setPadding(0, verticalPadding, 0, verticalPadding)
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            ).apply {
                                bottomMargin = ingredientBottomMargin
                            }
                        }
                        binding.layoutIngredientsList.addView(itemTextView)
                    }
                }.onFailure { error ->
                    currentAiReport = null
                    binding.tvAiExpandToggle.visibility = View.GONE
                    binding.layoutAiResultContainer.visibility = View.GONE
                    Toast.makeText(requireContext(), "식단 분석 실패: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }

            // 신체 정보 수정 로직
            /*
            binding.btnSaveBodyInfo.setOnClickListener {
                val heightStr = binding.etMyHeight.text.toString().trim()
                val weightStr = binding.etMyWeight.text.toString().trim()

                if (heightStr.isEmpty() || weightStr.isEmpty()) {
                    Toast.makeText(requireContext(), "키와 몸무게를 정확히 입력해주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val updatedUser = currentUser.copy(
                    height = heightStr.toDouble(),
                    weight = weightStr.toDouble()
                )

                viewLifecycleOwner.lifecycleScope.launch {
                    val result = userRepository.updateUser(updatedUser)

                    result.onSuccess {
                        sessionManager.createSession(updatedUser)
                        Toast.makeText(requireContext(), "신체 정보가 안전하게 저장되었습니다! 💪", Toast.LENGTH_SHORT).show()

                        // 사용자가 신체 스펙을 "직접 바꿨을 때만" 예외적으로 식단을 새로 고침해야 하므로 리셋 호출
                        myPageViewModel.resetAnalysis()
                        checkEngineAndAnalyze(updatedUser)
                    }.onFailure { error ->
                        Toast.makeText(requireContext(), "정보 수정 실패: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            */

            binding.btnLogout.setOnClickListener {
                // 로그아웃 시엔 메모리 보존을 털어내야 하므로 분석 초기화 후 전환
                myPageViewModel.resetAnalysis()
                (requireActivity() as? MainActivity)?.onLogoutSuccess()
            }

        } else {
            binding.layoutBodyInfo.visibility = View.GONE
            binding.cardMyPageAiReport.visibility = View.GONE
            binding.cardMyPageMenus.visibility = View.GONE
            binding.btnGoLogin.visibility = View.VISIBLE
            binding.btnLogout.visibility = View.GONE
            binding.btnEditProfile.visibility = View.GONE
            binding.tvProfileName.text = "로그인을 해주세요"
            binding.tvUserPurpose.visibility = View.GONE

            binding.btnGoLogin.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, LoginFragment())
                    .addToBackStack(null)
                    .commit()
            }
        }

        binding.layoutManageAddress.root.setOnClickListener {
            openMyPageMenu(AddressManagementFragment())
        }
        binding.layoutManageSubscription.root.setOnClickListener {
            openMyPageMenu(SubscriptionHistoryFragment())
        }
        binding.layoutOrderHistory.root.setOnClickListener {
            openMyPageMenu(OrderListFragment.newSingleOrderInstance())
        }
        binding.layoutFavoriteMeals.root.setOnClickListener {
            openMyPageMenu(FavoriteMealsFragment())
        }
        binding.btnAddAiFavorite.setOnClickListener {
            addAiReportToFavorite()
        }
    }

    private fun configureMenuRows() {
        binding.layoutManageAddress.tvMenuTitle.text = "배송지 관리"
        binding.layoutManageAddress.ivMenuIcon.setImageResource(R.drawable.ic_location_line)

        binding.layoutManageSubscription.tvMenuTitle.text = "정기구독 내역"
        binding.layoutManageSubscription.ivMenuIcon.setImageResource(R.drawable.ic_calendar_line)

        binding.layoutOrderHistory.tvMenuTitle.text = "일반 주문 내역"
        binding.layoutOrderHistory.ivMenuIcon.setImageResource(R.drawable.ic_receipt_line)

        binding.layoutFavoriteMeals.tvMenuTitle.text = "즐겨찾기 식단"
        binding.layoutFavoriteMeals.ivMenuIcon.setImageResource(R.drawable.ic_heart_line)
    }

    private fun bindProfileSummary(user: User) {
        val allergyText = if (user.allergies.isEmpty()) {
            "알러지 없음"
        } else {
            "알러지: ${user.allergies.joinToString(", ")}"
        }
        binding.tvBodySummary.text =
            "키 ${formatDecimal(user.height)}cm · 몸무게 ${formatDecimal(user.weight)}kg · 활동량 주 ${user.activityLevel}회"
        binding.tvActivitySummary.visibility = View.GONE
        binding.tvAllergySummary.text = allergyText
    }

    private fun formatDecimal(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.1f", value)
    }

    private fun addAiReportToFavorite() {
        val userId = currentUserId
        val report = currentAiReport
        if (userId == null) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }
        if (report == null || report.items.isEmpty()) {
            Toast.makeText(requireContext(), "저장할 추천 식단이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_favorite_meal_name, null)
        val nameLayout = dialogView.findViewById<TextInputLayout>(R.id.layoutMealName)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.etMealName)
        val dialog = Dialog(requireContext()).apply {
            setContentView(dialogView)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.9f).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }
        dialogView.findViewById<MaterialButton>(R.id.btnSave).setOnClickListener {
            val mealName = nameInput.text?.toString()?.trim().orEmpty()
            if (mealName.isBlank()) {
                nameLayout.error = "식단 이름을 입력해주세요."
                return@setOnClickListener
            }
            nameLayout.error = null
            val isAdded = FavoriteMealStore.addFromDietReport(userId, report, mealName)
            Toast.makeText(
                requireContext(),
                if (isAdded) "즐겨찾기에 추가했습니다." else "즐겨찾기에 추가할 수 없습니다.",
                Toast.LENGTH_SHORT
            ).show()
            dialog.dismiss()
        }
        dialog.setOnShowListener {
            dialog.window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.9f).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            nameInput.selectAll()
            nameInput.requestFocus()
        }
        dialog.show()
    }

    private fun openMyPageMenu(fragment: Fragment) {
        if (!SessionManager(requireContext()).isLoggedIn()) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showAiLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.layoutAiLoading.visibility = View.VISIBLE
            binding.layoutAiResultContainer.visibility = View.GONE
            binding.tvAiExpandToggle.visibility = View.GONE
        } else {
            binding.layoutAiLoading.visibility = View.GONE
        }
    }

    private fun User.toSafeUser(): User {
        return User(
            id = id,
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
            allergies = allergies ?: emptyList()
        )
    }

    private fun checkEngineAndAnalyze(user: User) {
        // 뷰모델이 소멸되지 않으므로 이미 계산이 끝난 상태(isAnalyzed == true)라면
        // 로딩바를 띄우지 않고 기존 데이터를 그대로 화면 갱신만 처리
        if (!myPageViewModel.isAnalyzed) {
            showAiLoading(true)
        }

        myPageViewModel.analyzeDiet(user, openAiApiToken)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_OPEN_EDIT_GUIDE = "openEditGuide"

        fun newInstance(openEditGuide: Boolean = false): MyPageFragment {
            return MyPageFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_OPEN_EDIT_GUIDE, openEditGuide)
                }
            }
        }
    }
}
