package com.example.boardexamreviewer.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "quizzes")
public class QuizEntity {
    @PrimaryKey(autoGenerate = true)
    public int id = 0;
    public int userId = 0;
    public int documentId;
    public String title;
    public String questionsJson;
    public int score = 0;
    public long timestamp = System.currentTimeMillis();

    @Ignore
    public QuizEntity() {}

    public QuizEntity(int userId, int documentId, String title, String questionsJson) {
        this.userId = userId;
        this.documentId = documentId;
        this.title = title;
        this.questionsJson = questionsJson;
        this.timestamp = System.currentTimeMillis();
    }
}
