package com.example.boardexamreviewer.data

import androidx.room.*

@Dao
interface AppDao {
    // --- Documents ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentEntity): Long

    @Query("SELECT * FROM documents WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllDocumentsByUser(userId: Int): List<DocumentEntity>

    @Query("SELECT * FROM documents WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastDocumentByUser(userId: Int): DocumentEntity?

    @Delete
    suspend fun deleteDocument(document: DocumentEntity)

    // --- Reviewers ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviewer(reviewer: ReviewerEntity)

    @Query("SELECT * FROM reviewers WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllReviewersByUser(userId: Int): List<ReviewerEntity>

    // --- Quizzes ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuiz(quiz: QuizEntity)

    @Query("SELECT * FROM quizzes WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getAllQuizzesByUser(userId: Int): List<QuizEntity>

    // --- Users (Profile Module) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users ORDER BY name ASC")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: Int): UserEntity?

    // --- Categories ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategories(): List<CategoryEntity>

    // --- Progress Tracking ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySession(session: StudySessionEntity)

    @Query("SELECT * FROM study_sessions WHERE userId = :userId ORDER BY timestamp DESC")
    suspend fun getStudySessionsByUser(userId: Int): List<StudySessionEntity>

    @Query("SELECT SUM(durationMinutes) FROM study_sessions WHERE userId = :userId AND sessionType = 'Work'")
    suspend fun getTotalStudyTime(userId: Int): Int?
}
