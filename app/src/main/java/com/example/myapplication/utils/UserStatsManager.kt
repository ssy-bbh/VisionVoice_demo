package com.example.myapplication.utils

import android.content.Context
import com.example.myapplication.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.TimeZone

object UserStatsManager {
    private const val PREF_NAME = "VisionVoiceStats"

    // 🌟 核心兼容魔法：自己算！获取基于当前时区的“绝对天数”
    // 这个方法兼容所有老版本 Android，永不报错
    private fun getCurrentEpochDay(): Long {
        val millisInDay = 1000 * 60 * 60 * 24L
        val now = System.currentTimeMillis()
        val offset = TimeZone.getDefault().getOffset(now) // 补齐时区差（比如东八区早8个小时）
        return (now + offset) / millisInDay
    }

    // ==========================================
    // 🔥 功能 1：连续打卡火苗管理
    // ==========================================
    fun recordPractice(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val lastPracticeDay = prefs.getLong("last_practice_day", 0L)
        val currentDay = getCurrentEpochDay() // 使用我们兼容性无敌的新方法

        var streak = prefs.getInt("current_streak", 0)

        if (currentDay == lastPracticeDay) {
            return // 今天练过了
        } else if (currentDay - lastPracticeDay == 1L) {
            streak += 1 // 连续打卡，+1！
        } else {
            streak = 1 // 断签，重置为 1
        }

        prefs.edit()
            .putLong("last_practice_day", currentDay)
            .putInt("current_streak", streak)
            .apply()
    }

    fun getStreakCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val lastPracticeDay = prefs.getLong("last_practice_day", 0L)
        val currentDay = getCurrentEpochDay()

        if (currentDay - lastPracticeDay > 1L) {
            prefs.edit().putInt("current_streak", 0).apply()
            return 0
        }
        return prefs.getInt("current_streak", 0)
    }

    // ==========================================
    // 🎯 功能 2：从数据库随机抽取每日挑战
    // ==========================================
    suspend fun getDailyChallenges(context: Context): List<String> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedDay = prefs.getLong("daily_challenge_day", 0L)
        val currentDay = getCurrentEpochDay()

        if (savedDay != currentDay) {
            // 1. 去数据库里拿数据
            val dao = AppDatabase.getInstance(context).appDao()
            val allItems = dao.getAllShowcaseItems()

            // 2. 判空拦截
            if (allItems == null || allItems.isEmpty()) {
                return@withContext listOf("Apple", "Cup", "Mouse")
            }

            // 3. 🚨 核心修复：明确声明泛型类型，并将 it.word 替换为 item.targetWord！
            val newChallenges: List<String> = allItems.shuffled().take(3).map { item ->
                item.targetWord // 如果你的实体类里不叫 targetWord，请改这里！
            }

            val newChallengesStr: String = newChallenges.joinToString(",")

            // 4. 存入本地缓存
            prefs.edit()
                .putLong("daily_challenge_day", currentDay)
                .putString("daily_challenges", newChallengesStr)
                .apply()

            return@withContext newChallenges
        } else {
            // 今天已经抽过了，直接读缓存
            val savedChallenges =
                prefs.getString("daily_challenges", "Apple,Cup,Mouse") ?: "Apple,Cup,Mouse"
            return@withContext savedChallenges.split(",")
        }
    }
    interface JavaCallback {
        fun onTasksLoaded(tasks: List<String>)
    }

    fun getDailyChallengesAsync(context: Context, callback: JavaCallback) {
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            val tasks = getDailyChallenges(context)
            callback.onTasksLoaded(tasks)
        }
    }
}