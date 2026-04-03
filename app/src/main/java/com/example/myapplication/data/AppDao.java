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

    @Query("SELECT * FROM showcase_items ORDER BY CASE WHEN bestImagePath IS NOT NULL AND bestImagePath != '' THEN 0 ELSE 1 END, targetWord ASC")
    List<ShowcaseItem> getAllShowcaseItems();

    @Query("SELECT * FROM showcase_items WHERE targetWord = :word LIMIT 1")
    ShowcaseItem getShowcaseItemByWord(String word);

    @Update
    void updateShowcaseItem(ShowcaseItem item);

    // ===== 个人主页：数据看板聚合查询 =====

    // 1. 统计已解锁的图鉴总数
    @Query("SELECT COUNT(*) FROM showcase_items WHERE isUnlocked = 1")
    int getUnlockedCount();

    // 2. 统计完美发音（历史最高分 >= 90）的单词数量
    @Query("SELECT COUNT(*) FROM showcase_items WHERE highestScore >= 90")
    int getPerfectPronunciationCount();

    // 3. 统计历史发音练习的总次数
    @Query("SELECT COUNT(*) FROM practice_records")
    int getTotalPracticeCount();
}