package com.ssafy.fitbox.fragment

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.ssafy.fitbox.R
import com.ssafy.fitbox.dto.ChatMessage
import com.ssafy.fitbox.dto.ChatRequest
import com.ssafy.fitbox.dto.User
import com.ssafy.fitbox.network.RetrofitClient
import com.ssafy.fitbox.repository.UserRepository
import kotlinx.coroutines.launch
import org.json.JSONObject

class ConsultationChatFragment : ChatFragment() {

    private var userData: User? = null
    private val userRepository = UserRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userData = arguments?.getSerializable("user_data") as? User
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnCompleteRegistration.visibility = View.GONE
    }

    override fun requiresLogin(): Boolean = false

    override fun getSystemPrompt(): String {
        val userAllergies = userData?.allergies?.joinToString(", ") ?: "없음"

        return """
            너는 FitBox 앱의 식사 목적 선택 가이드 AI야.
            사용자는 회원가입 과정에서 본인에게 가장 적합한 식사 목적(다이어트, 벌크업, 유지어터, 운동 후 식사 중 1개)을 고르려고 상담하고 있어.

            [사용자의 사전 신체 정보 및 알레르기]
            - 나이: ${userData?.age ?: "미입력"}
            - 키: ${userData?.height ?: "미입력"}cm
            - 몸무게: ${userData?.weight ?: "미입력"}kg
            - 운동 횟수: 일주일에 ${userData?.activityLevel ?: "미입력"}회
            - 알레르기 성분: $userAllergies

            [상담 원칙]
            1. 사용자의 운동 종목, 생활 패턴, 선호 음식, 싫어하는 음식, 구체 목표를 자연스럽게 물어봐.
            2. 사용자가 "배드민턴에 맞는 몸", "바디프로필", "러닝 기록 향상", "축구 후 회복"처럼 구체 목표를 말하면 그 맥락을 반영해 목적을 추천해.
            3. 목적이 충분히 정해졌다고 판단되면 마지막 줄에 아래 태그 중 하나만 정확히 출력해.
               - [GOAL:다이어트]
               - [GOAL:벌크업]
               - [GOAL:유지어터]
               - [GOAL:운동 후 식사]
        """.trimIndent()
    }

    override fun getInitialGreeting(): String {
        val allergyWarning = if (!userData?.allergies.isNullOrEmpty()) {
            "\n등록하신 알레르기 성분 [${userData?.allergies?.joinToString(", ")}]은 제외해서 상담할게요."
        } else ""

        return "안녕하세요! 회원가입 마지막 단계의 식사 목적 상담입니다. 건강 목표, 자주 하는 운동, 좋아하거나 싫어하는 음식, 생활 패턴을 알려주시면 가장 잘 맞는 목적을 추천해드릴게요.$allergyWarning"
    }

    override fun onAiResponseReceived(reply: String) {
        val matchResult = Regex("\\[GOAL:(다이어트|벌크업|유지어터|운동 후 식사)\\]").find(reply)

        if (matchResult != null) {
            val goal = matchResult.groupValues[1].trim()
            val activity = activity ?: return
            activity.runOnUiThread {
                if (!isAdded || _binding == null) return@runOnUiThread
                binding.btnCompleteRegistration.visibility = View.VISIBLE
                binding.btnCompleteRegistration.text = "'$goal' 선택하고 가입 완료하기"
                binding.btnCompleteRegistration.setOnClickListener {
                    completeRegistrationWithGoal(goal)
                }
            }
        }
    }

    private fun completeRegistrationWithGoal(goal: String) {
        val user = userData ?: return
        val userWithGoal = user.copy(purpose = goal)

        lifecycleScope.launch {
            val result = userRepository.register(userWithGoal)
            result.onSuccess {
                saveConsultationPreference(userWithGoal, goal)
                Toast.makeText(requireContext(), "FitBox 회원가입을 축하합니다!", Toast.LENGTH_SHORT).show()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.main_container, WelcomeFragment())
                    .commit()
            }.onFailure { error ->
                Toast.makeText(requireContext(), "가입 실패: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun saveConsultationPreference(user: User, goal: String) {
        val conversationLog = getConversationLogForAnalysis()
        if (conversationLog.isBlank()) return

        runCatching {
            val createdUser = userRepository.getUserByUserId(user.userId).getOrNull() ?: return
            val prompt = """
                너는 회원가입 중 식사 목적 상담 대화를 분석해서 user_preference_table에 저장할 개인 취향 요약만 추출하는 데이터 정리기야.
                반드시 JSON만 출력해. Markdown 코드블록은 쓰지 마.

                [사용자 기본 정보]
                - 최종 식사 목적: $goal
                - 키: ${user.height}cm
                - 몸무게: ${user.weight}kg
                - 활동레벨: ${user.activityLevel}
                - 알레르기: ${user.allergies.joinToString(", ").ifBlank { "없음" }}

                [회원가입 목적 상담 대화]
                $conversationLog

                [저장 규칙]
                1. preference에는 좋아하는 음식, 싫어하는 음식, 선호/비선호 식감과 조리법, 자주 하는 운동 종목, 운동 강도/빈도/시간, 생활 패턴, 식단을 지키기 어려운 상황, 식사 목적의 구체적 이유와 최종 목표를 최대한 디테일하게 저장해.
                2. 키, 몸무게, 알레르기, 활동레벨 숫자처럼 DB에 별도 저장된 정보는 preference에 저장하지 마.
                3. 단, "배드민턴을 즐김", "축구 후 회복식이 필요함", "러닝을 자주 함"처럼 구체적인 운동/생활 성향은 preference에 저장해.
                4. 식사 목적은 단순히 "$goal"만 쓰지 말고, 대화에서 드러난 디테일을 포함해. 예: "배드민턴에 맞는 가벼운 몸을 만들기 위해 다이어트하려 함", "바디프로필 촬영을 목표로 체지방 감량에 관심이 있음".
                5. 알레르기로 언급된 음식은 preference의 좋아하는 음식에 넣지 마.
                6. 최종 문장은 분류별로 정리된 2~5문장으로 써.

                [응답 형식]
                {"preference":"..."}
            """.trimIndent()

            val request = ChatRequest(
                messages = listOf(ChatMessage(role = "user", content = prompt)),
                temperature = 0.1
            )
            val response = RetrofitClient.chatApi.sendMessage(OPENAI_API_KEY, request)
            val body = response.body()
            if (!response.isSuccessful || body == null) return

            val aiReply = body
                .choices
                .firstOrNull()
                ?.message
                ?.content
                ?.replace("```json", "")
                ?.replace("```", "")
                ?.trim()
                .orEmpty()
            val preference = JSONObject(aiReply).optString("preference", "").trim()
            if (preference.isNotBlank()) {
                userRepository.saveUserPreference(createdUser.id, preference)
            }
        }
    }

    companion object {
        fun newInstance(user: User): ConsultationChatFragment {
            return ConsultationChatFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("user_data", user)
                }
            }
        }
    }
}
