package com.example.boardexamreviewer.utils

import com.example.boardexamreviewer.data.GeminiApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * [SUB-MODULE: GEMINI CLIENT]
 * Handles the network connection to Google Gemini.
 */
object GeminiClient {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.MINUTES) // 30 mins timeout for long documents
        .readTimeout(30, TimeUnit.MINUTES)
        .writeTimeout(30, TimeUnit.MINUTES)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(AppConfig.GEMINI_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: GeminiApiService = retrofit.create(GeminiApiService::class.java)
}
