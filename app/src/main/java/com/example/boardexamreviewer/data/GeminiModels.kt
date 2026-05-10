package com.example.boardexamreviewer.data

/**
 * [SUB-MODULE: GEMINI MODELS]
 * Data classes for Google Gemini API communication.
 */

data class GeminiRequest(
    val contents: List<Content>
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)

data class GeminiResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: Content?
)
