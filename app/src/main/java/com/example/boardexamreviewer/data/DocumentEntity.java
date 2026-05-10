package com.example.boardexamreviewer.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "documents")
public class DocumentEntity {
    @PrimaryKey(autoGenerate = true)
    public int id = 0;
    public int userId = 0;
    public int categoryId = 0;
    public String fileName;
    public String filePath;
    public String extractedText;
    public long timestamp = System.currentTimeMillis();

    @Ignore
    public DocumentEntity() {}

    public DocumentEntity(int userId, int categoryId, String fileName, String filePath, String extractedText) {
        this.userId = userId;
        this.categoryId = categoryId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.extractedText = extractedText;
        this.timestamp = System.currentTimeMillis();
    }
}
