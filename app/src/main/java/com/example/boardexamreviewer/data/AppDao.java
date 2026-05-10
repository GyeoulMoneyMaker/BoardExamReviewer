package com.example.boardexamreviewer.data;

import androidx.room.*;
import java.util.List;

@Dao
public interface AppDao {
    // --- Documents ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertDocument(DocumentEntity document);

    @Query("SELECT * FROM documents WHERE userId = :userId ORDER BY timestamp DESC")
    List<DocumentEntity> getAllDocumentsByUser(int userId);

    @Query("SELECT * FROM documents WHERE userId = :userId ORDER BY timestamp DESC LIMIT 1")
    DocumentEntity getLastDocumentByUser(int userId);

    @Delete
    void deleteDocument(DocumentEntity document);

    // --- Reviewers ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertReviewer(ReviewerEntity reviewer);

    @Query("SELECT * FROM reviewers WHERE userId = :userId ORDER BY timestamp DESC")
    List<ReviewerEntity> getAllReviewersByUser(int userId);

    // --- Quizzes ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertQuiz(QuizEntity quiz);

    @Query("SELECT * FROM quizzes WHERE userId = :userId ORDER BY timestamp DESC")
    List<QuizEntity> getAllQuizzesByUser(int userId);

    // --- Users (Profile Module) ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertUser(UserEntity user);

    @Query("SELECT * FROM users ORDER BY name ASC")
    List<UserEntity> getAllUsers();

    @Query("SELECT * FROM users WHERE id = :userId")
    UserEntity getUserById(int userId);

    // --- Categories ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertCategory(CategoryEntity category);

    @Query("SELECT * FROM categories ORDER BY name ASC")
    List<CategoryEntity> getAllCategories();

    // --- Progress Tracking ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertStudySession(StudySessionEntity session);

    @Query("SELECT * FROM study_sessions WHERE userId = :userId ORDER BY timestamp DESC")
    List<StudySessionEntity> getStudySessionsByUser(int userId);

    @Query("SELECT SUM(durationMinutes) FROM study_sessions WHERE userId = :userId AND sessionType = 'Work'")
    Integer getTotalStudyTime(int userId);
}
