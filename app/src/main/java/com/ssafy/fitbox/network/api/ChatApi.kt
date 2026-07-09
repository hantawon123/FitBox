package com.ssafy.fitbox.network.api

import com.ssafy.fitbox.dto.ChatRequest
import com.ssafy.fitbox.dto.ChatResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ChatApi {
    @POST("chat/completions")
    suspend fun sendMessage(
        // 통행증(GMS 키)을 헤더에 담아서 보냅니다.
        @Header("Authorization") token: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>
}