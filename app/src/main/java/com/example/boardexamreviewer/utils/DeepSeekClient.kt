package com.example.boardexamreviewer.utils

import com.example.boardexamreviewer.data.DeepSeekApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * DeepSeekClient: The official "Brain" that connects to the AI.
 */
object DeepSeekClient {
    private const val BASE_URL = "https://api.deepseek.com/"

    val apiService: DeepSeekApiService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.MINUTES)
            .readTimeout(30, TimeUnit.MINUTES)
            .writeTimeout(30, TimeUnit.MINUTES)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()
            .create(DeepSeekApiService::class.java)
    }
}
