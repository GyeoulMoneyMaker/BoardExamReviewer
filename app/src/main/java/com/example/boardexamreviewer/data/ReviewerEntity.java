package com.example.boardexamreviewer.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "reviewers")
public class ReviewerEntity {
    @PrimaryKey(autoGenerate = true)
    public int id = 0;
    public int userId = 0;
    public int documentId;
    public String title;
    public String content;
    public long timestamp = System.currentTimeMillis();

    @Ignore
    public ReviewerEntity() {}

    public ReviewerEntity(int userId, int documentId, String title, String content) {
        this.userId = userId;
        this.documentId = documentId;
        this.title = title;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
    }
}
