package com.example.boardexamreviewer.data

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * [SUB-MODULE: GEMINI API SERVICE]
 */
interface GeminiApiService {
    @POST("v1/models/{model}:generateContent")
    suspend fun generateContent(
        @retrofit2.http.Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): Response<GeminiResponse>
}
