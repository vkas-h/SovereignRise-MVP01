package com.sovereign_rise.app.domain.model

import java.util.concurrent.TimeUnit

/**
 * Domain model representing a habit with streak tracking.
 */
data class Habit(
    val id: String,
    val userId: String,
    val name: String,
    val description: String?,
    val type: HabitType,
    val intervalDays: Int,
    val streakDays: Int,
    val longestStreak: Int,
    val lastCheckedAt: Long?,
    val createdAt: Long,
    val isActive: Boolean,
    val totalCompletions: Int,
    val milestonesAchieved: List<Int>
) {
    /**
     * Checks if habit is due to be checked today based on type and last check time.
     */
    fun isCheckDueToday(): Boolean {
        if (lastCheckedAt == null) return true
        
        val now = System.currentTimeMillis()
        val daysSinceLastCheck = TimeUnit.MILLISECONDS.toDays(now - lastCheckedAt)
        
        return when (type) {
            HabitType.DAILY -> daysSinceLastCheck >= 1
            HabitType.WEEKLY -> daysSinceLastCheck >= 7
            HabitType.CUSTOM_INTERVAL -> daysSinceLastCheck >= intervalDays
        }
    }
    
    /**
     * Returns the next unclaimed milestone (7, 30, or 100 days), or null if all claimed.
     */
    fun getNextMilestone(): Int? {
        val milestones = listOf(7, 30, 100)
        return milestones.firstOrNull { it !in milestonesAchieved && it > streakDays }
    }
    
    /**
     * Validates if habit can be checked today (not already checked today).
     */
    fun canCheckToday(): Boolean {
        if (lastCheckedAt == null) return true
        
        val now = System.currentTimeMillis()
        val lastCheckDay = TimeUnit.MILLISECONDS.toDays(lastCheckedAt)
        val currentDay = TimeUnit.MILLISECONDS.toDays(now)
        
        return lastCheckDay < currentDay
    }
}

/**
 * Enum representing the frequency type of a habit.
 */
enum class HabitType {
    DAILY,
    WEEKLY,
    CUSTOM_INTERVAL
}

