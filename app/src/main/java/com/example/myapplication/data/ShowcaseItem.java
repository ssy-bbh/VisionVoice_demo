package com.example.myapplication.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "showcase_items")
public class ShowcaseItem {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String targetWord;
    public String category;
    public boolean isUnlocked;
    public long unlockTime;
    public String bestImagePath;
    public int highestScore;
    public long lastReviewedTime;

    public ShowcaseItem(String targetWord, String category, boolean isUnlocked) {
        this.targetWord = targetWord;
        this.category = category;
        this.isUnlocked = isUnlocked;
        this.highestScore = 0;
    }
}