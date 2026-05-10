package com.example.boardexamreviewer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * [SUB-MODULE: PROFILE]
 * Entity representing a user profile for mock login.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val password: String = "", // [NEW: SECURITY]
    val avatarResId: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

/**
 * Entity representing an uploaded document.
 */
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int = 0,
    val categoryId: Int = 0,
    val fileName: String,
    val filePath: String,
    val extractedText: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Entity representing a generated reviewer summary.
 */
@Entity(tableName = "reviewers")
data class ReviewerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int = 0, // [NEW: ISOLATION]
    val documentId: Int,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Entity representing a generated quiz.
 */
@Entity(tableName = "quizzes")
data class QuizEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int = 0, // [NEW: ISOLATION]
    val documentId: Int,
    val title: String,
    val questionsJson: String,
    val score: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * [SUB-MODULE: PROGRESS TRACKING]
 */
@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val durationMinutes: Int,
    val sessionType: String,
    val timestamp: Long = System.currentTimeMillis()
)
