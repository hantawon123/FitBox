package com.ssafy.fitbox.fragment

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ssafy.fitbox.R
import com.ssafy.fitbox.adapter.ChatAdapter
import com.ssafy.fitbox.databinding.FragmentChatBinding
import com.ssafy.fitbox.dto.ChatMessage
import com.ssafy.fitbox.dto.ChatRequest
import com.ssafy.fitbox.dto.User
import com.ssafy.fitbox.network.RetrofitClient
import com.ssafy.fitbox.repository.UserRepository
import com.ssafy.fitbox.util.LoginRequiredDialog
import com.ssafy.fitbox.util.SessionManager
import com.ssafy.fitbox.viewmodel.MyPageViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

open class ChatFragment : Fragment() {

    protected var _binding: FragmentChatBinding? = null
    protected val binding get() = _binding!!

    private val apiChatHistory = mutableListOf<ChatMessage>()
    private val uiChatList = mutableListOf<ChatMessage>()

    private lateinit var chatAdapter: ChatAdapter

    private val userRepository = UserRepository()
    private val myPageViewModel: MyPageViewModel by activityViewModels()
    protected lateinit var sessionManager: SessionManager
    private lateinit var backPressedCallback: OnBackPressedCallback

    // Logcat 필터링용 고유 태그
    private val DEBUG_TAG = "ChatFragmentDebug"

    private data class ProfileChangeOption(
        val key: String,
        val title: String,
        val description: String
    )

    protected val OPENAI_API_KEY: String by lazy {
        val key = getString(R.string.gms_key)
        if (key.startsWith("Bearer ")) key else "Bearer $key"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        if (requiresLogin() && !sessionManager.isLoggedIn()) {
            LoginRequiredDialog.show(this)
            parentFragmentManager.popBackStack()
            return
        }

        apiChatHistory.add(ChatMessage("system", getSystemPrompt()))

        chatAdapter = ChatAdapter(uiChatList)
        binding.rvChat.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChat.adapter = chatAdapter

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val imeVisible = windowInsets.isVisible(WindowInsetsCompat.Type.ime())
            val imeHeight = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom

            v.setPadding(0, 0, 0, if (imeVisible) imeHeight else 0)

            _binding?.layoutInput?.post {
                val binding = _binding ?: return@post
                val density = binding.root.resources.displayMetrics.density

                if (imeVisible) {
                    val paddingBottomPx = (10 * density).toInt()
                    binding.layoutInput.setPadding(
                        binding.layoutInput.paddingLeft,
                        binding.layoutInput.paddingTop,
                        binding.layoutInput.paddingRight,
                        paddingBottomPx
                    )

                    if (uiChatList.isNotEmpty()) {
                        binding.rvChat.scrollToPosition(uiChatList.size - 1)
                    }
                } else {
                    val paddingBottomPx = (52 * density).toInt()
                    binding.layoutInput.setPadding(
                        binding.layoutInput.paddingLeft,
                        binding.layoutInput.paddingTop,
                        binding.layoutInput.paddingRight,
                        paddingBottomPx
                    )
                }
            }
            windowInsets
        }

        getInitialGreeting()?.let { greeting ->
            val aiMsg = ChatMessage("assistant", greeting)
            apiChatHistory.add(aiMsg)
            uiChatList.add(aiMsg)
            chatAdapter.notifyItemInserted(uiChatList.size - 1)
        }

        binding.btnSend.setOnClickListener {
            val msg = binding.etMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                sendMessageToAI(msg)
                binding.etMessage.text?.clear()
            }
        }

        backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleExitChat()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)
    }

    override fun onResume() {
        super.onResume()
        requireActivity().findViewById<View>(R.id.bottom_navigation_view)?.visibility = View.GONE
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
    }

    override fun onDestroyView() {
        val activity = activity
        activity?.findViewById<View>(R.id.bottom_navigation_view)?.visibility = View.VISIBLE
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        super.onDestroyView()
        _binding = null
    }

    open fun sendMessageToAI(userText: String) {
        val userMsg = ChatMessage("user", userText)
        apiChatHistory.add(userMsg)
        uiChatList.add(userMsg)
        chatAdapter.notifyItemInserted(uiChatList.size - 1)

        val loadingMsg = ChatMessage("assistant", "AI 영양사가 최적의 답변을 생각하고 있습니다... 🤖💭")
        uiChatList.add(loadingMsg)
        chatAdapter.notifyItemInserted(uiChatList.size - 1)
        binding.rvChat.scrollToPosition(uiChatList.size - 1)

        lifecycleScope.launch {
            try {
                val request = ChatRequest(messages = apiChatHistory)
                val response = RetrofitClient.chatApi.sendMessage(OPENAI_API_KEY, request)

                if (uiChatList.isNotEmpty()) {
                    uiChatList.removeAt(uiChatList.size - 1)
                    chatAdapter.notifyItemRemoved(uiChatList.size)
                }

                val body = response.body()
                if (response.isSuccessful && body != null) {
                    val aiReply = body.choices.firstOrNull()?.message?.content ?: "응답을 읽을 수 없습니다."
                    onAiResponseReceived(aiReply)
                    val aiMsg = ChatMessage("assistant", aiReply)
                    apiChatHistory.add(aiMsg)
                    uiChatList.add(aiMsg)
                } else {
                    val errorMessage = "서버 응답 오류가 발생했습니다 😢\n(에러 코드: ${response.code()})"
                    uiChatList.add(ChatMessage("assistant", errorMessage))
                }
            } catch (e: Exception) {
                if (uiChatList.isNotEmpty() && uiChatList.last().content.contains("생각하고 있습니다")) {
                    uiChatList.removeAt(uiChatList.size - 1)
                    chatAdapter.notifyItemRemoved(uiChatList.size)
                }
                uiChatList.add(ChatMessage("assistant", "네트워크 통신에 실패했습니다. 와이파이 환경을 확인해주세요! 📡"))
            } finally {
                chatAdapter.notifyItemInserted(uiChatList.size - 1)
                _binding?.rvChat?.scrollToPosition(uiChatList.size - 1)
            }
        }
    }

    open fun getInitialGreeting(): String? = "안녕하세요! FitBox 전속 AI 영양사입니다. 🥗\n오늘의 식단이나 운동에 대해 궁금한 점을 편하게 물어보세요!"

    protected open fun requiresLogin(): Boolean = true

    open fun onAiResponseReceived(reply: String) { }

    protected fun getConversationLogForAnalysis(): String {
        return uiChatList
            .filter { it.role != "system" }
            .joinToString("\n") { "${it.role}: ${it.content}" }
    }

    open fun getSystemPrompt(): String {
        return """
            너는 FitBox 앱의 AI 영양사이자 운동 상담 파트너야.
            답변하기 전에 사용자의 질문 의도를 먼저 판단해.

            [대화 흐름 규칙]
            1. 개념 확인 질문이나 짧은 궁금증에는 바로 답해. 예: "유당불내증도 알러지야?", "단백질은 언제 먹어?", "저탄고지가 뭐야?" 같은 질문에는 키, 몸무게, 활동량을 먼저 요구하지 마.
            2. 개인 맞춤 식단, 칼로리, 감량/증량 계획을 사용자가 명확히 요청했을 때만 부족한 정보를 물어봐. 이때도 한 번에 1~2개만 자연스럽게 물어봐.
            3. 이미 대화에서 제공된 정보는 반복해서 묻지 마.
            4. 알러지, 불내증, 못 먹는 음식은 안전 정보로 중요하게 다뤄. 유당불내증은 엄밀히 알레르기와 다르지만 식단 제한으로 반영해야 한다고 설명해.
            5. 사용자가 좋아하는 음식, 싫어하는 음식, 운동 종목, 운동 빈도, 생활 패턴, 구체적인 목표를 말하면 개인화 정보로 기억할 만한 내용처럼 다뤄.
            6. 역질문이 필요할 때는 현재 대화 주제에 맞는 질문만 해. 질문 뒤에 무조건 키/몸무게를 요구하는 문장을 붙이지 마.
            7. 건강, 식단, 운동, 취향과 무관한 질문은 FitBox 상담 범위를 짧게 안내해.

            답변은 짧고 자연스럽게 해. 필요한 경우에만 후속 질문을 덧붙여.
        """.trimIndent()
        return """
            너는 핏박스(FitBox) 앱의 수석 전문 영양사이자 헬스 트레이너야. 
            너의 목표는 사용자에게 '두루뭉술하고 뻔한 답변'이 아닌, '초개인화된 디테일한 맞춤형 식단 및 운동 솔루션'을 제공하는 거야.

            [상담 원칙 - 반드시 지킬 것]
            1. 정보 요구 (역질문): 사용자가 식단이나 운동을 물어봤을 때, 개인 정보(키, 현재 체중, 목표 체중, 활동량, 알레르기, 식사 횟수 등)가 파악되지 않았다면 바로 정답을 주지 마. 
               대신 "가장 완벽한 맞춤 식단을 짜드리기 위해 몇 가지 여쭤봐도 될까요? 현재 체중과 목표 체중, 평소 활동량이 어떻게 되시나요?"와 같이 필요한 정보를 먼저 역으로 질문해.
            2. 디테일한 솔루션 제공: 사용자가 충분한 정보를 제공했다면, 탄수화물/단백질/지방의 비율, 추천 식재료와 구체적인 그램(g) 수, 하루 총 섭취 권장 칼로리, 추천 운동 루틴까지 아주 디테일하고 전문적으로 답변해 줘.
            3. 공감과 격려: 답변을 시작할 때 사용자의 목표에 대해 공감하고 할 수 있다는 긍정적인 에너지를 불어넣어 줘.

            [가드레일 - 주제 이탈 방지]
            만약 사용자가 코딩, 역사, 정치, 연예인, 날씨 등 '건강, 식단, 운동, 웰빙'과 전혀 관련 없는 엉뚱한 질문을 한다면, 절대 그 질문에 대한 답을 제공하지 마.
            대신 반드시 아래 문장으로만 똑같이 대답해:
            "죄송합니다. 저는 핏박스의 맞춤 식단 및 운동 상담을 위한 전용 AI 영양사입니다. 목표 달성을 위한 식단이나 운동에 대해 물어보시면 최선을 다해 도와드릴게요! 🥗💪"
        """.trimIndent()
    }

    open fun handleExitChat() {
        val currentUser = sessionManager.getUser()
        val validUserMsgCount = uiChatList.count { it.role == "user" }

        if (validUserMsgCount == 0 || currentUser == null) {
            exitFragment()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("대화 내용 저장")
            .setMessage("지금까지의 대화 내용을 분석하여 나만의 식단 취향으로 저장하시겠습니까?")
            .setPositiveButton("예") { _, _ ->
                analyzeAndSaveData(currentUser)
            }
            .setNegativeButton("아니오") { _, _ ->
                exitFragment()
            }
            .setCancelable(false)
            .show()
    }

    private fun analyzeAndSaveData(currentUser: User) {
        _binding?.layoutSummaryLoading?.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val existingPrefResult = userRepository.getUserPreference(currentUser.id)
                val existingPref = existingPrefResult.getOrNull() ?: ""

                val userAllergies = currentUser.allergies?.joinToString(", ") ?: "없음"

                val currentChatLog = uiChatList.filter { it.role != "system" }
                    .joinToString("\n") { "${it.role}: ${it.content}" } + """

                    [추가 민감 감지 규칙 - 반드시 따를 것]
                    1. 사용자가 "이제 다이어트로 바꿀래", "벌크업이 목표야", "유지어터로 관리하고 싶어", "운동 후 식사를 챙기고 싶어"처럼 말하면 식사 목적 변경으로 판단하고 changed_profile.purpose에 반드시 다이어트/벌크업/유지어터/운동 후 식사 중 하나를 넣어.
                    2. "바디프로필 찍으려고 감량", "배드민턴에 맞는 가벼운 몸", "축구 체력을 위해 체중 유지", "근육을 키우고 싶음"처럼 구체 목표가 드러나면 preference에는 그 디테일을 저장하고, 목적이 기존 목적과 다르면 changed_profile.purpose도 함께 넣어.
                    3. 알레르리는 아주 민감하게 감지해. "못 먹어", "먹으면 두드러기", "알러지/알레르기", "먹으면 붓는다", "먹으면 속이 안 좋다", "피해야 한다"가 특정 음식과 함께 나오면 changed_profile.allergies에 기존 알레르기와 합쳐서 넣어.
                    4. 알레르기 음식은 preference의 좋아하는 음식에서 제거하고, 싫어하는 음식으로도 저장하지 마. 알레르기는 changed_profile.allergies에만 저장해.
                    5. 좋아함/싫어함, 운동 종목, 운동 빈도와 시간, 생활 패턴, 식사 목적의 구체적 이유는 최대한 자세히 preference에 남겨.
                    """.trimIndent()

                val prompt = """
                    너는 사용자의 대화 내용을 분석하여 사용자 프로필 변경과 식단 취향을 정리하는 정밀 데이터 추출기야.
                    아래 [기존 유저 정보], [기존 취향 요약], [이번 대화 내용]을 바탕으로 반드시 JSON 형식으로만 응답해.
                    Markdown 코드 블록(```json) 없이 순수 JSON 텍스트만 출력해.

                    [기존 유저 정보]
                    - 키: ${currentUser.height}cm, 몸무게: ${currentUser.weight}kg, 활동레벨(1~5): ${currentUser.activityLevel}, 식사 목적: ${currentUser.purpose}, 알레르기: [$userAllergies]
                    
                    [기존 취향 요약]
                    $existingPref

                    [이번 대화 내용]
                    $currentChatLog

                    [최신 정보 우선 원칙 - 매우 중요]
                    1. [이번 대화 내용]이 [기존 취향 요약]과 충돌하면 무조건 이번 대화 내용을 최신 정보로 보고 반영해.
                    2. 예: 기존 취향에 "새우를 좋아함"이 있어도 이번 대화에서 "새우 알러지가 있음"이라고 하면 preference에서 새우 선호를 제거하고 changed_profile.allergies에 새우를 넣어.
                    3. 예: 기존 취향에 "새우를 좋아함"이 있어도 이번 대화에서 "이제 새우가 싫음", "새우는 안 먹고 싶음"이라고 하면 preference는 "새우를 싫어함"으로 수정해.
                    4. 알레르기는 취향보다 우선순위가 높아. 알레르기로 언급된 음식은 preference의 좋아하는 음식 목록에서 반드시 제거해.
                    5. 기존 취향과 새 취향을 단순히 모두 붙이지 말고, 충돌하는 항목은 최신 내용만 남겨.

                    [프로필 변경 감지 규칙 - 매우 중요]
                    1. 키, 몸무게, 활동량, 알레르기는 preference에 저장하지 말고 changed_profile에만 넣어.
                    2. 사용자가 이번 대화에서 아래와 같은 표현을 하면 반드시 프로필 변경 후보로 판단해.
                       - 키: "키가 175야", "키 175cm", "175센치", "신장 175"
                       - 몸무게: "몸무게 70kg", "70키로", "체중이 68로 줄었어", "살이 쪄서 75kg"
                       - 활동량: "주 3회 운동", "일주일에 4번", "매일 운동", "거의 운동 안 함"
                       - 알레르기: "새우 알러지", "새우 알레르기", "갑각류 못 먹어", "우유 먹으면 안 돼", "계란 알러지가 생김"
                    3. 기존 유저 정보와 값이 다르면 has_profile_change는 true로 하고 changed_profile에 해당 필드만 넣어.
                    4. 활동량은 앱의 activityLevel 숫자로 변환해.
                       - 거의 안 함/운동 안 함: 0
                       - 주 1회: 1
                       - 주 2회: 2
                       - 주 3회: 3
                       - 주 4회: 4
                       - 주 5회 이상/거의 매일/매일: 5
                    5. 알레르기는 기존 알레르기와 이번 대화에서 새로 언급된 알레르기를 합친 최종 배열로 작성해. 단, 사용자가 특정 알레르기가 없어졌다고 명확히 말한 경우에만 제거해.

                    [추출 규칙]
                    1. preference: [기존 취향 요약]을 절대 삭제하지 말고, 이번 대화에서 새로 파악된 개인적인 취향과 성향을 모두 병합한 누적 요약을 작성해.
                    2. 키, 몸무게, 알레르기, 활동량처럼 DB에 별도 저장 위치가 있는 정보는 preference에 절대 저장하지 마.
                       - 키, 몸무게, 활동량, 알레르기 변경은 changed_profile에만 넣어.
                       - 단, "배드민턴을 즐김", "아침 식사를 자주 거름", "매운 음식을 선호함"처럼 사용자의 습관/취향/구체적 활동 성향은 preference에 저장해.
                    3. 좋아하는 음식, 싫어하는 음식, 선호/비선호 조리법, 식감, 식사 패턴, 자주 하는 운동 종목, 운동 시간대, 식단을 지키기 어려운 상황 등 개인화에 도움 되는 정보는 사소해 보여도 누락하지 마.
                    4. 같은 종류의 정보는 시간순으로 덧붙이지 말고 분류별로 정리해.
                       - 싫어하는 음식은 한 문장에 묶기: "닭가슴살, 소고기, 돼지고기를 싫어함"
                       - 좋아하는 음식은 한 문장에 묶기: "새우와 연어를 좋아함"
                       - 구체적인 활동 성향은 별도 문장으로 묶기: "배드민턴을 즐기며 운동 후 회복식에 관심이 있음"
                       - 중복되거나 같은 의미의 표현은 하나로 합치기
                    5. 최종 문장은 간결하되 정보 손실이 없어야 해. 2~5문장까지 허용하고, 강한 취향만 남기지 말고 전체 대화에서 확인된 모든 개인 취향/성향을 보존해.
                    6. "A를 싫어하며 B를 좋아하고 C와 D를 싫어함"처럼 시간순으로 이어 붙이는 문장은 금지야. 대신 선호/비선호/습관/활동 성향별로 재정리해.
                    7. 알레르기 언급은 음식 취향이 아니라 건강상 제한 정보이므로 preference가 아니라 changed_profile.allergies에만 넣어.
                    8. has_profile_change: 대화 중 사용자가 자신의 키, 몸무게, 활동량, 식사 목적, 알레르기 정보를 새롭게 언급하여 [기존 유저 정보]와 달라진 점이 명백히 있다면 true, 없으면 false.
                    9. changed_profile: 만약 달라진 점이 있다면, 달라진 정보만 포함해서 작성. 단위 없이 숫자/문자열/배열로 작성해 (예: height, weight, activityLevel, purpose, allergies)
                    10. 이번 대화에서 새 취향이 없고 기존 취향도 없다면 preference는 빈 문자열로 둬. 기존 취향이 있다면 그대로 유지해.

                    [응답 JSON 포맷 예시]
                    {
                      "preference": "닭가슴살, 소고기, 돼지고기를 싫어하고 연어를 좋아함. 매운 음식과 구운 조리법을 선호하고 퍽퍽한 식감을 싫어함. 배드민턴을 즐기며 운동 후 단백질 위주의 회복식에 관심이 있음.",
                      "has_profile_change": true,
                      "changed_profile": {
                        "weight": 70.0,
                        "activityLevel": 3,
                        "purpose": "다이어트",
                        "allergies": ["계란", "우유", "새우"]
                      }
                    }
                """.trimIndent()

                val requestMsg = ChatMessage("user", prompt)
                val request = ChatRequest(
                    messages = listOf(requestMsg),
                    temperature = 0.1
                )
                val response = RetrofitClient.chatApi.sendMessage(OPENAI_API_KEY, request)

                val body = response.body()
                if (response.isSuccessful && body != null) {
                    var aiReply = body.choices.firstOrNull()?.message?.content ?: "{}"
                    aiReply = aiReply.replace("```json", "").replace("```", "").trim()

                    val jsonObject = JSONObject(aiReply)
                    val mergedPreference = jsonObject.optString("preference", "")
                    val hasProfileChange = jsonObject.optBoolean("has_profile_change", false)

                    if (mergedPreference.isNotEmpty()) {
                        userRepository.saveUserPreference(currentUser.id, mergedPreference)
                            .onSuccess {
                                myPageViewModel.invalidateAnalysis()
                            }
                    }

                    if (hasProfileChange && jsonObject.has("changed_profile")) {
                        _binding?.layoutSummaryLoading?.visibility = View.GONE
                        val changedProfileJson = jsonObject.getJSONObject("changed_profile")
                        showProfileUpdateDialog(currentUser, changedProfileJson)
                    } else {
                        Toast.makeText(requireContext(), "취향 저장 완료! 🍱", Toast.LENGTH_SHORT).show()
                        _binding?.layoutSummaryLoading?.visibility = View.GONE
                        exitFragment()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "분석 중 오류 발생", Toast.LENGTH_SHORT).show()
                _binding?.layoutSummaryLoading?.visibility = View.GONE
                exitFragment()
            }
        }
    }

    private fun showSelectableProfileUpdateDialog(currentUser: User, changedData: JSONObject) {
        val context = requireContext()
        val primaryColor = ContextCompat.getColor(context, R.color.fit_primary)
        val secondaryColor = ContextCompat.getColor(context, R.color.fit_text_secondary)

        val currentAllergies = currentUser.allergies ?: emptyList()
        var nextHeight = currentUser.height
        var nextWeight = currentUser.weight
        var nextActivityLevel = currentUser.activityLevel
        var nextPurpose = currentUser.purpose
        var nextAllergies = currentAllergies
        val options = mutableListOf<ProfileChangeOption>()

        if (changedData.has("height")) {
            nextHeight = changedData.optDouble("height", currentUser.height)
            options.add(
                ProfileChangeOption(
                    key = "height",
                    title = "키 변경",
                    description = "${formatDecimal(currentUser.height)}cm -> ${formatDecimal(nextHeight)}cm"
                )
            )
        }

        if (changedData.has("weight")) {
            nextWeight = changedData.optDouble("weight", currentUser.weight)
            options.add(
                ProfileChangeOption(
                    key = "weight",
                    title = "몸무게 변경",
                    description = "${formatDecimal(currentUser.weight)}kg -> ${formatDecimal(nextWeight)}kg"
                )
            )
        }

        if (changedData.has("activityLevel")) {
            nextActivityLevel = changedData.optInt("activityLevel", currentUser.activityLevel)
            options.add(
                ProfileChangeOption(
                    key = "activityLevel",
                    title = "활동량 변경",
                    description = "활동 레벨 ${currentUser.activityLevel} -> $nextActivityLevel"
                )
            )
        }

        if (changedData.has("purpose")) {
            nextPurpose = changedData.optString("purpose", currentUser.purpose).ifBlank { currentUser.purpose }
            options.add(
                ProfileChangeOption(
                    key = "purpose",
                    title = "식사 목적 변경",
                    description = "${currentUser.purpose} -> $nextPurpose"
                )
            )
        }

        if (changedData.has("allergies")) {
            val allergiesArray = changedData.optJSONArray("allergies")
            nextAllergies = (0 until (allergiesArray?.length() ?: 0))
                .mapNotNull { allergiesArray?.optString(it)?.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }

            val addedAllergies = nextAllergies.filterNot { next ->
                currentAllergies.any { current -> current.equals(next, ignoreCase = true) }
            }
            val removedAllergies = currentAllergies.filterNot { current ->
                nextAllergies.any { next -> next.equals(current, ignoreCase = true) }
            }

            val allergyDescription = buildList {
                if (addedAllergies.isNotEmpty()) {
                    add(addedAllergies.joinToString(", ") { "$it 알러지 추가" })
                }
                if (removedAllergies.isNotEmpty()) {
                    add(removedAllergies.joinToString(", ") { "$it 알러지 목록에서 제거" })
                }
            }.joinToString("\n").ifBlank {
                "알러지 목록을 ${if (nextAllergies.isEmpty()) "없음" else nextAllergies.joinToString(", ")}(으)로 정리"
            }

            options.add(
                ProfileChangeOption(
                    key = "allergies",
                    title = "알러지 정보 변경",
                    description = allergyDescription
                )
            )
        }

        if (options.isEmpty()) {
            Toast.makeText(context, "반영할 회원 정보 변경 사항이 없습니다.", Toast.LENGTH_SHORT).show()
            exitFragment()
            return
        }

        val contentView = ScrollView(context).apply {
            isFillViewport = false
            addView(
                LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(20.dp(), 8.dp(), 20.dp(), 4.dp())

                    addView(TextView(context).apply {
                        text = "대화에서 회원 정보 변화가 감지됐어요.\n반영할 항목만 선택해주세요."
                        textSize = 14f
                        setTextColor(secondaryColor)
                        setLineSpacing(2.dp().toFloat(), 1.0f)
                    })
                }
            )
        }
        val optionContainer = contentView.getChildAt(0) as LinearLayout
        val optionChecks = mutableMapOf<String, CheckBox>()
        options.forEach { option ->
            optionChecks[option.key] = addProfileChangeRow(optionContainer, option)
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle("회원 정보 변경 감지")
            .setView(contentView)
            .setPositiveButton("선택 항목 반영", null)
            .setNegativeButton("취향만 저장") { _, _ ->
                Toast.makeText(context, "회원 정보 변경은 제외하고 취향만 저장했습니다.", Toast.LENGTH_SHORT).show()
                exitFragment()
            }
            .setCancelable(false)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(primaryColor)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(secondaryColor)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val selectedKeys = optionChecks
                    .filterValues { it.isChecked }
                    .keys

                if (selectedKeys.isEmpty()) {
                    Toast.makeText(context, "반영할 항목을 하나 이상 선택해주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                _binding?.layoutSummaryLoading?.visibility = View.VISIBLE
                dialog.dismiss()

                val shouldUpdateAllergies = selectedKeys.contains("allergies")
                val finalAllergiesToSave = if (shouldUpdateAllergies) nextAllergies else currentAllergies
                val updatedUser = currentUser.copy(
                    height = if (selectedKeys.contains("height")) nextHeight else currentUser.height,
                    weight = if (selectedKeys.contains("weight")) nextWeight else currentUser.weight,
                    activityLevel = if (selectedKeys.contains("activityLevel")) nextActivityLevel else currentUser.activityLevel,
                    purpose = if (selectedKeys.contains("purpose")) nextPurpose else currentUser.purpose,
                    allergies = finalAllergiesToSave
                )

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val userUpdateResult = userRepository.updateUser(updatedUser)
                        val allergyUpdateResult = if (shouldUpdateAllergies) {
                            userRepository.updateUserAllergies(currentUser.id, finalAllergiesToSave)
                        } else {
                            Result.success(true)
                        }

                        withContext(Dispatchers.Main) {
                            if (userUpdateResult.isSuccess && allergyUpdateResult.isSuccess) {
                                sessionManager.createSession(updatedUser)
                                myPageViewModel.invalidateAnalysis()
                                Toast.makeText(context, "선택한 회원 정보를 반영했습니다.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "일부 정보 반영에 실패했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(DEBUG_TAG, "회원 정보 선택 반영 중 오류", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "네트워크 오류로 반영에 실패했습니다.", Toast.LENGTH_SHORT).show()
                        }
                    } finally {
                        withContext(Dispatchers.Main) {
                            _binding?.layoutSummaryLoading?.visibility = View.GONE
                            exitFragment()
                        }
                    }
                }
            }
        }

        dialog.show()
    }

    private fun addProfileChangeRow(parent: LinearLayout, option: ProfileChangeOption): CheckBox {
        val context = parent.context
        val primaryColor = ContextCompat.getColor(context, R.color.fit_primary)
        val textColor = ContextCompat.getColor(context, R.color.fit_text_primary)
        val secondaryColor = ContextCompat.getColor(context, R.color.fit_text_secondary)
        val outlineColor = ContextCompat.getColor(context, R.color.fit_outline)
        val containerColor = ContextCompat.getColor(context, R.color.fit_primary_container)

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14.dp(), 12.dp(), 14.dp(), 12.dp())
            background = GradientDrawable().apply {
                cornerRadius = 10.dp().toFloat()
                setColor(containerColor)
                setStroke(1.dp(), outlineColor)
            }
        }

        val checkBox = CheckBox(context).apply {
            text = option.title
            isChecked = true
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
            buttonTintList = ColorStateList.valueOf(primaryColor)
        }

        row.addView(checkBox)
        row.addView(TextView(context).apply {
            text = option.description
            textSize = 13f
            setTextColor(secondaryColor)
            setPadding(34.dp(), 2.dp(), 0, 0)
            setLineSpacing(2.dp().toFloat(), 1.0f)
        })
        row.setOnClickListener {
            checkBox.isChecked = !checkBox.isChecked
        }

        parent.addView(
            row,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 12.dp()
            }
        )
        return checkBox
    }

    private fun formatDecimal(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString() else String.format("%.1f", value)
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun showProfileUpdateDialog(currentUser: User, changedData: JSONObject) {
        showSelectableProfileUpdateDialog(currentUser, changedData)
        return
        val changesList = mutableListOf<String>()
        val nextAllergies = mutableListOf<String>()

        var nextHeight = currentUser.height
        var nextWeight = currentUser.weight
        var nextActivityLevel = currentUser.activityLevel
        var nextPurpose = currentUser.purpose
        val isAllergyChanged = changedData.has("allergies")

        if (changedData.has("height")) {
            nextHeight = changedData.getDouble("height")
            changesList.add("키: ${currentUser.height}cm ➔ ${nextHeight}cm")
        }
        if (changedData.has("weight")) {
            nextWeight = changedData.getDouble("weight")
            changesList.add("몸무게: ${currentUser.weight}kg ➔ ${nextWeight}kg")
        }
        if (changedData.has("activityLevel")) {
            nextActivityLevel = changedData.getInt("activityLevel")
            changesList.add("활동레벨: ${currentUser.activityLevel} ➔ $nextActivityLevel")
        }

        if (changedData.has("purpose")) {
            nextPurpose = changedData.getString("purpose")
            changesList.add("식사 목적: ${currentUser.purpose} → $nextPurpose")
        }

        if (isAllergyChanged) {
            val allergiesArray = changedData.getJSONArray("allergies")
            for (i in 0 until allergiesArray.length()) {
                nextAllergies.add(allergiesArray.getString(i))
            }
            val prevAllergiesText = currentUser.allergies?.joinToString(", ") ?: "없음"
            changesList.add("알레르기: [$prevAllergiesText] ➔ [${nextAllergies.joinToString(", ")}]")
        }

        val changeText = changesList.joinToString("\n")

        AlertDialog.Builder(requireContext())
            .setTitle("회원 정보 변경 감지 👀")
            .setMessage("대화 내용 중 회원님의 신체 정보 변화가 감지되었습니다.\n내 프로필 정보에도 반영하시겠습니까?\n\n[변경 내역]\n$changeText")
            .setPositiveButton("반영하기") { _, _ ->
                Log.d(DEBUG_TAG, "=== '반영하기' 버튼 클릭됨 ===")

                // 🌟 [핵심 방어막] 이 try-catch 블록이 어떠한 크래시도 완벽하게 흡수합니다.
                try {
                    // _binding을 안전하게 접근 (? 사용)
                    _binding?.layoutSummaryLoading?.visibility = View.VISIBLE

                    // 🌟 Gson 파싱 에러 완벽 차단! (기존 세션에 알레르기가 아예 Null일 경우를 방어)
                    val currentAllergiesSafe: List<String> = currentUser.allergies ?: emptyList()
                    val finalAllergiesToSave = if (isAllergyChanged) nextAllergies else currentAllergiesSafe

                    // 객체 복사 (여기서 죽었던 문제를 위 코드로 완벽 해결)
                    val updatedUser = currentUser.copy(
                        height = nextHeight,
                        weight = nextWeight,
                        activityLevel = nextActivityLevel,
                        purpose = nextPurpose,
                        allergies = finalAllergiesToSave
                    )
                    Log.d(DEBUG_TAG, "새로 빌드된 유저 객체 복사 성공: $updatedUser")

                    // 비동기 통신 시작
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            Log.d(DEBUG_TAG, "1. user_table 정보 동기화 요청 시작 (ID: ${updatedUser.id})")
                            val userUpdateResult = userRepository.updateUser(updatedUser)
                            Log.d(DEBUG_TAG, "user_table 동기화 결과: ${userUpdateResult.isSuccess}")

                            val allergyUpdateResult = if (isAllergyChanged) {
                                Log.d(DEBUG_TAG, "2. 알레르기 변경 감지됨 - user_allergy_table 동기화 요청 시작")
                                userRepository.updateUserAllergies(currentUser.id, nextAllergies)
                            } else {
                                Log.d(DEBUG_TAG, "2. 알레르기 변경 없음 - 스킵")
                                Result.success(true)
                            }

                            // 메인 스레드로 돌아와서 UI 업데이트
                            withContext(Dispatchers.Main) {
                                if (userUpdateResult.isSuccess && allergyUpdateResult.isSuccess) {
                                    Log.d(DEBUG_TAG, "3. 서버 반영 성공 -> 로컬 세션 동기화 시작")
                                    try {
                                        sessionManager.createSession(updatedUser)
                                        myPageViewModel.invalidateAnalysis()
                                    } catch (e: Exception) {
                                        Log.e(DEBUG_TAG, "세션 저장 실패", e)
                                    }
                                    Toast.makeText(requireContext(), "신체 스펙 및 취향이 모두 동기화되었습니다! 🧬", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(requireContext(), "통신은 성공했으나 반영에 실패했습니다.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(DEBUG_TAG, "네트워크 통신 중 에러", e)
                            withContext(Dispatchers.Main) {
                                Toast.makeText(requireContext(), "네트워크 오류로 반영에 실패했습니다.", Toast.LENGTH_SHORT).show()
                            }
                        } finally {
                            withContext(Dispatchers.Main) {
                                _binding?.layoutSummaryLoading?.visibility = View.GONE
                                exitFragment()
                            }
                        }
                    }

                } catch (e: Exception) {
                    Log.e(DEBUG_TAG, "🚨 '반영하기' 버튼 로직 수행 중 동기적 크래시 발생!", e)
                    Toast.makeText(requireContext(), "앱 내부 데이터 오류로 반영하지 못했습니다.", Toast.LENGTH_LONG).show()
                    _binding?.layoutSummaryLoading?.visibility = View.GONE
                    exitFragment()
                }
            }
            .setNegativeButton("취향만 저장") { _, _ ->
                Toast.makeText(requireContext(), "신체 변화를 제외하고, 순수 취향 정보만 저장했습니다! 🍱", Toast.LENGTH_SHORT).show()
                exitFragment()
            }
            .setCancelable(false)
            .show()
    }

    private fun exitFragment() {
        backPressedCallback.isEnabled = false
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }
}
