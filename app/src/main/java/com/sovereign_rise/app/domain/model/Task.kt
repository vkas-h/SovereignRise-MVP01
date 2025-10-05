package com.sovereign_rise.app.domain.model

/**
 * Domain model representing a task in the app.
 */
data class Task(
    val id: String,
    val userId: String,
    val title: String,
    val description: String?,
    val status: TaskStatus,
    val difficulty: TaskDifficulty,
    val reminderTime: Long?,
    val createdAt: Long,
    val completedAt: Long?,
    val isMissed: Boolean
)

/**
 * Task status enum.
 */
enum class TaskStatus {
    PENDING,    // Task is active and not yet completed
    COMPLETED,  // Task was fully completed
    PARTIAL,    // Task was partially completed (50%+ progress)
    FAILED      // Task was missed or failed
}

/**
 * Task difficulty enum.
 */
enum class TaskDifficulty {
    EASY,
    MEDIUM,
    HARD,
    VERY_HARD
}
