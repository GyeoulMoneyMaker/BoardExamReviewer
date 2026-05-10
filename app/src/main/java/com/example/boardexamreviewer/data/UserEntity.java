package com.example.boardexamreviewer.data;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * Entity representing a user profile for mock login.
 */
@Entity(tableName = "users")
public class UserEntity {
    @PrimaryKey(autoGenerate = true)
    public int id = 0;
    public String name;
    public String password = "";
    public int avatarResId = 0;
    public long timestamp = System.currentTimeMillis();

    @Ignore
    public UserEntity() {}

    public UserEntity(String name, String password, int avatarResId) {
        this.name = name;
        this.password = password;
        this.avatarResId = avatarResId;
        this.timestamp = System.currentTimeMillis();
    }
}
