package com.sovereign_rise.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sovereign_rise.app.domain.model.SyncStatus
import com.sovereign_rise.app.domain.model.Task
import com.sovereign_rise.app.domain.model.TaskDifficulty
import com.sovereign_rise.app.domain.model.TaskStatus

/**
 * Room entity for Task with sync metadata.
 */
@Entity(
    tableName = "tasks",
    indices = [
        Index("userId"),
        Index("status"),
        Index("createdAt")
    ]
)
data class TaskEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val description: String?,
    val status: String,
    val difficulty: String,
    val reminderTime: Long?,
    val createdAt: Long,
    val completedAt: Long?,
    val isMissed: Boolean,
    // Sync metadata
    val syncStatus: String,
    val lastSyncedAt: Long?,
    val serverUpdatedAt: Long?,
    val localUpdatedAt: Long
)

/**
 * Extension function to convert TaskEntity to Task domain model.
 */
fun TaskEntity.toTask(): Task {
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
 * Extension function to convert Task domain model to TaskEntity.
 */
fun Task.toTaskEntity(
    syncStatus: SyncStatus,
    lastSyncedAt: Long?,
    serverUpdatedAt: Long?,
    localUpdatedAt: Long
): TaskEntity {
    return TaskEntity(
        id = id,
        userId = userId,
        title = title,
        description = description,
        status = status.name,
        difficulty = difficulty.name,
        reminderTime = reminderTime,
        createdAt = createdAt,
        completedAt = completedAt,
        isMissed = isMissed,
        syncStatus = syncStatus.name,
        lastSyncedAt = lastSyncedAt,
        serverUpdatedAt = serverUpdatedAt,
        localUpdatedAt = localUpdatedAt
    )
}

