package com.sovereign_rise.app.domain.model

/**
 * Domain model representing a daily task summary.
 */
data class DailyTaskSummary(
    val hasSummary: Boolean,
    val date: Long,
    val dateString: String,
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val failedTasks: Int = 0,
    val completionRate: Float = 0f,
    val tasks: List<TaskSummaryItem> = emptyList()
)

/**
 * Individual task in the summary.
 */
data class TaskSummaryItem(
    val id: String,
    val title: String,
    val description: String?,
    val status: TaskStatus,
    val difficulty: TaskDifficulty,
    val createdAt: Long,
    val completedAt: Long?
)

