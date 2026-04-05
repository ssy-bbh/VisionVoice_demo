package com.example.myapplication.utils

import android.content.Context
import com.example.myapplication.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.TimeZone

object UserStatsManager {
    private const val PREF_NAME = "VisionVoiceStats"

    // 🌟 核心兼容魔法：获取基于当前时区的“绝对天数”
    private fun getCurrentEpochDay(): Long {
        val millisInDay = 1000 * 60 * 60 * 24L
        val now = System.currentTimeMillis()
        val offset = TimeZone.getDefault().getOffset(now)
        return (now + offset) / millisInDay
    }

    // ==========================================
    // 🔥 功能 1：连续打卡火苗管理
    // ==========================================
    fun recordPractice(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val lastPracticeDay = prefs.getLong("last_practice_day", 0L)
        val currentDay = getCurrentEpochDay()

        var streak = prefs.getInt("current_streak", 0)

        if (currentDay == lastPracticeDay) {
            return
        } else if (currentDay - lastPracticeDay == 1L) {
            streak += 1
        } else {
            streak = 1
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
    // 🎯 功能 2：每日挑战随机生成与核销
    // ==========================================
    suspend fun getDailyChallenges(context: Context, forceRefresh: Boolean = false): List<String> = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val savedDay = prefs.getLong("daily_challenge_day", 0L)
        val currentDay = getCurrentEpochDay()

        if (forceRefresh || savedDay != currentDay) {
            val dao = AppDatabase.getInstance(context).appDao()
            val allItems = dao.getAllShowcaseItems()

            if (allItems == null || allItems.isEmpty()) {
                return@withContext listOf("Laptop", "Cup", "Book")
            }

            val newChallenges: List<String> = allItems.shuffled().take(3).map { it.targetWord }
            val newChallengesStr: String = newChallenges.joinToString(",")

            prefs.edit()
                .putLong("daily_challenge_day", currentDay)
                .putString("daily_challenges", newChallengesStr)
                .putString("completed_challenges", "") // 🚨 新的一天（或强制刷新），清空完成记录！
                .apply()

            return@withContext newChallenges
        } else {
            val savedChallenges = prefs.getString("daily_challenges", "Laptop,Cup,Book") ?: "Laptop,Cup,Book"
            return@withContext savedChallenges.split(",")
        }
    }

    // 🎯 检查并核销挑战单词 (返回 true 表示是今天的新完成任务)
    fun markChallengeCompleted(context: Context, word: String): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val currentDay = getCurrentEpochDay()
        val savedDay = prefs.getLong("daily_challenge_day", 0L)

        // 不是今天的任务，不理会
        if (currentDay != savedDay) return false

        // 看看今天的任务列表里有没有这个词 (忽略大小写)
        val challenges = prefs.getString("daily_challenges", "")?.split(",") ?: emptyList()
        val isChallenge = challenges.any { it.equals(word, ignoreCase = true) }
        if (!isChallenge) return false

        // 看看是不是已经完成过了
        val completedStr = prefs.getString("completed_challenges", "") ?: ""
        val completedList = completedStr.split(",").filter { it.isNotEmpty() }.toMutableList()

        val isAlreadyCompleted = completedList.any { it.equals(word, ignoreCase = true) }
        if (!isAlreadyCompleted) {
            // 🎉 新完成的挑战！存起来！
            completedList.add(word)
            prefs.edit().putString("completed_challenges", completedList.joinToString(",")).apply()
            return true
        }
        return false
    }

    // 🎯 获取今天已经完成的单词列表
    fun getCompletedChallenges(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val currentDay = getCurrentEpochDay()
        val savedDay = prefs.getLong("daily_challenge_day", 0L)
        if (currentDay != savedDay) return emptyList()

        val completedStr = prefs.getString("completed_challenges", "") ?: ""
        return completedStr.split(",").filter { it.isNotEmpty() }
    }

    // ------------------------------------------
    // 🎯 给 Java 调用的专属异步接口
    // ------------------------------------------
    interface JavaCallback {
        fun onTasksLoaded(tasks: List<String>)
    }

    fun getDailyChallengesAsync(context: Context, forceRefresh: Boolean, callback: JavaCallback) {
        GlobalScope.launch(Dispatchers.Main) {
            val tasks = getDailyChallenges(context, forceRefresh)
            callback.onTasksLoaded(tasks)
        }
    }
}