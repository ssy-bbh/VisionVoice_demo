package com.example.myapplication.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "practice_records")
public class PracticeRecord {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String word;
    public int score;
    public long timestamp;
    public String imagePath;

    public PracticeRecord(String word, int score, long timestamp, String imagePath) {
        this.word = word;
        this.score = score;
        this.timestamp = timestamp;
        this.imagePath = imagePath;
    }
}