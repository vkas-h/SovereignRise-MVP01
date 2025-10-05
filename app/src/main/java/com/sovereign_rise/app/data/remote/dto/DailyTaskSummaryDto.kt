package com.sovereign_rise.app.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.sovereign_rise.app.domain.model.DailyTaskSummary
import com.sovereign_rise.app.domain.model.TaskSummaryItem
import com.sovereign_rise.app.domain.model.TaskStatus
import com.sovereign_rise.app.domain.model.TaskDifficulty

/**
 * DTO for daily task summary from API.
 */
data class DailyTaskSummaryDto(
    @SerializedName("hasSummary")
    val hasSummary: Boolean,
    
    @SerializedName("date")
    val date: Long? = null,
    
    @SerializedName("dateString")
    val dateString: String? = null,
    
    @SerializedName("totalTasks")
    val totalTasks: Int? = null,
    
    @SerializedName("completedTasks")
    val completedTasks: Int? = null,
    
    @SerializedName("failedTasks")
    val failedTasks: Int? = null,
    
    @SerializedName("completionRate")
    val completionRate: Float? = null,
    
    @SerializedName("tasks")
    val tasks: List<TaskSummaryItemDto>? = null,
    
    @SerializedName("message")
    val message: String? = null
)

/**
 * DTO for individual task in summary.
 */
data class TaskSummaryItemDto(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("status")
    val status: String,
    
    @SerializedName("difficulty")
    val difficulty: String,
    
    @SerializedName("createdAt")
    val createdAt: Long,
    
    @SerializedName("completedAt")
    val completedAt: Long?
)

/**
 * Extension function to convert DTO to domain model.
 */
fun DailyTaskSummaryDto.toDomain(): DailyTaskSummary {
    return DailyTaskSummary(
        hasSummary = hasSummary,
        date = date ?: 0L,
        dateString = dateString ?: "",
        totalTasks = totalTasks ?: 0,
        completedTasks = completedTasks ?: 0,
        failedTasks = failedTasks ?: 0,
        completionRate = completionRate ?: 0f,
        tasks = tasks?.map { it.toDomain() } ?: emptyList()
    )
}

/**
 * Extension function to convert task summary item DTO to domain model.
 */
fun TaskSummaryItemDto.toDomain(): TaskSummaryItem {
    return TaskSummaryItem(
        id = id,
        title = title,
        description = description,
        status = try {
            TaskStatus.valueOf(status)
        } catch (e: Exception) {
            TaskStatus.PENDING
        },
        difficulty = try {
            TaskDifficulty.valueOf(difficulty)
        } catch (e: Exception) {
            TaskDifficulty.EASY
        },
        createdAt = createdAt,
        completedAt = completedAt
    )
}

