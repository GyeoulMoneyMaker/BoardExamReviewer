package com.example.boardexamreviewer.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class QuizQuestion(
    val question: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String // A, B, C, or D
)

object QuizParser {
    fun fromJson(json: String): List<QuizQuestion> {
        val type = object : TypeToken<List<QuizQuestion>>() {}.type
        return Gson().fromJson(json, type) ?: emptyList()
    }

    fun toJson(questions: List<QuizQuestion>): String {
        return Gson().toJson(questions)
    }
}
