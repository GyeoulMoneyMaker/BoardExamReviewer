package com.example.boardexamreviewer.data;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * [SUB-MODULE: GEMINI API SERVICE]
 */
public interface GeminiApiService {
    @POST("v1/models/{model}:generateContent")
    Call<GeminiResponse> generateContent(
        @Path("model") String model,
        @Query("key") String apiKey,
        @Body GeminiRequest request
    );
}
