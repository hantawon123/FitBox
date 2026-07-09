package com.ssafy.fitbox.dto

// 1. 우리가 AI에게 보낼 질문(요청) 데이터
data class ChatRequest(
    val model: String = "gpt-4o", // 사용할 AI 모델
    val messages: List<ChatMessage>,
    val temperature: Double? = null
)

// 2. 대화 메시지 하나의 형태 (누가, 무슨 말을 했는지)
data class ChatMessage(
    val role: String,    // "system"(설정), "user"(사용자), "assistant"(AI) 중 하나
    val content: String  // 실제 대화 내용
)

// 3. AI가 우리에게 돌려주는 대답(응답) 데이터 껍데기
data class ChatResponse(
    val choices: List<ChatChoice>
)

// 4. 응답 데이터 알맹이 (실제 AI의 대답이 들어있는 곳)
data class ChatChoice(
    val message: ChatMessage
)
