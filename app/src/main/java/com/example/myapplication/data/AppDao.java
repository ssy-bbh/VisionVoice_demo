package com.example.myapplication.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface AppDao {
    @Insert
    void insertRecord(PracticeRecord record);

    @Query("SELECT COUNT(*) FROM practice_records WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay")
    int getPracticeCountForDay(long startOfDay, long endOfDay);

    @Insert
    void insertShowcaseItems(List<ShowcaseItem> items);

    @Query("SELECT * FROM showcase_items")
    List<ShowcaseItem> getAllShowcaseItems();

    @Query("SELECT * FROM showcase_items WHERE targetWord = :word LIMIT 1")
    ShowcaseItem getShowcaseItemByWord(String word);

    @Update
    void updateShowcaseItem(ShowcaseItem item);
}