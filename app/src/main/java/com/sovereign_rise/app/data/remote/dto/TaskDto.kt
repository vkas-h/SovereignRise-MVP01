package com.sovereign_rise.app.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.sovereign_rise.app.domain.model.Task
import com.sovereign_rise.app.domain.model.TaskDifficulty
import com.sovereign_rise.app.domain.model.TaskStatus

/**
 * Data transfer object for Task API responses.
 */
data class TaskDto(
    val id: String,
    @SerializedName("user_id") val userId: String,
    val title: String,
    val description: String?,
    val status: String,
    val difficulty: String,
    @SerializedName("reminder_time") val reminderTime: Long?,
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("completed_at") val completedAt: Long?,
    @SerializedName("is_missed") val isMissed: Boolean,
    // Sync metadata
    @SerializedName("server_updated_at") val serverUpdatedAt: Long? = null,
    @SerializedName("updated_at") val updatedAt: Long? = null
)

/**
 * Maps TaskDto to domain Task model.
 */
fun TaskDto.toTask(): Task {
    return Task(
        id = id,
        userId = userId,
        title = title,
        description = description,
        status = TaskStatus.valueOf(status),
        difficulty = TaskDifficulty.valueOf(difficulty),
        reminderTime = reminderTime,
        createdAt = createdAt,
        completedAt = completedAt,
        isMissed = isMissed
    )
}

/**
 * Request DTO for creating a new task.
 */
data class CreateTaskRequest(
    val title: String,
    val description: String?,
    val difficulty: String,
    @SerializedName("reminder_time") val reminderTime: Long?
)

/**
 * Request DTO for updating an existing task.
 */
data class UpdateTaskRequest(
    val title: String?,
    val description: String?,
    val difficulty: String?,
    @SerializedName("reminder_time") val reminderTime: Long?,
    val status: String?,
    @SerializedName("client_updated_at") val clientUpdatedAt: Long? = null
)

/**
 * Response DTO for task completion.
 */
data class CompleteTaskResponse(
    val task: TaskDto
)

/**
 * Response DTO for task list.
 */
data class TaskListResponse(
    val tasks: List<TaskDto>,
    @SerializedName("total_pending") val totalPending: Int,
    @SerializedName("total_completed") val totalCompleted: Int,
    @SerializedName("total_failed") val totalFailed: Int,
    @SerializedName("next_reset_time") val nextResetTime: Long
)

