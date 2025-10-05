package com.sovereign_rise.app.domain.repository

import com.sovereign_rise.app.domain.model.Habit

/**
 * Repository interface for habit operations.
 */
interface HabitRepository {
    
    /**
     * Get all active habits for the current user.
     */
    suspend fun getHabits(): List<Habit>
    
    /**
     * Get a specific habit by ID.
     */
    suspend fun getHabitById(habitId: String): Habit
    
    /**
     * Create a new habit.
     */
    suspend fun createHabit(habit: Habit): Habit
    
    /**
     * Update an existing habit.
     */
    suspend fun updateHabit(habit: Habit): Habit
    
    /**
     * Delete a habit.
     */
    suspend fun deleteHabit(habitId: String)
    
    /**
     * Mark habit as completed for today.
     * Returns updated habit plus rewards and milestone info.
     */
    suspend fun tickHabit(habitId: String): TickHabitResult
    
    /**
     * Check for missed habits and apply penalties.
     * Returns list of broken streaks and penalties applied.
     */
    suspend fun checkStreakBreaks(): StreakBreakResult
    
}

/**
 * Result of ticking a habit, including milestone info.
 */
data class TickHabitResult(
    val habit: Habit,
    val newStreakDays: Int,
    val milestoneAchieved: MilestoneAchievement?
)

/**
 * Information about a milestone achievement.
 */
data class MilestoneAchievement(
    val milestoneDays: Int,
    val message: String
)

/**
 * Result of checking for streak breaks.
 */
data class StreakBreakResult(
    val brokenHabits: List<Habit>
)

