package com.example.boardexamreviewer.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "categories")
public class CategoryEntity {
    @PrimaryKey(autoGenerate = true)
    public int id = 0;
    public String name;

    @Ignore
    public CategoryEntity() {}

    public CategoryEntity(String name) {
        this.name = name;
    }
}
