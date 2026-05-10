package com.example.boardexamreviewer.data

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

// --- Request Models ---

data class ChatRequest(
    val model: String = "deepseek-chat",
    val messages: List<Message>,
    val stream: Boolean = false
)

data class Message(
    val role: String,
    val content: String
)

// --- Response Models ---

data class ChatResponse(
    val id: String,
    @SerializedName("choices") val choices: List<Choice>
)

data class Choice(
    @SerializedName("message") val message: Message
)

// --- Retrofit Interface ---

interface DeepSeekApiService {
    @POST("v1/chat/completions")
    suspend fun getChatCompletion(
        @Header("Authorization") apiKey: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>
}
