package com.sovereign_rise.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sovereign_rise.app.domain.model.*

/**
 * Room entity for the sync queue.
 */
@Entity(
    tableName = "sync_queue",
    indices = [
        Index("priority"),
        Index("createdAt"),
        Index("status")
    ]
)
data class SyncQueueEntity(
    @PrimaryKey val id: String,
    val actionType: String,
    val entityType: String,
    val entityId: String,
    val payload: String,
    val priority: Int,
    val createdAt: Long,
    val retryCount: Int = 0,
    val lastAttemptAt: Long? = null,
    val status: String,
    val errorMessage: String? = null
)

/**
 * Extension function to convert SyncQueueEntity to SyncAction domain model.
 */
fun SyncQueueEntity.toSyncAction(): SyncAction {
    return SyncAction(
        id = id,
        actionType = SyncActionType.valueOf(actionType),
        entityType = EntityType.valueOf(entityType),
        entityId = entityId,
        payload = payload,
        priority = priority,
        createdAt = createdAt,
        retryCount = retryCount,
        lastAttemptAt = lastAttemptAt,
        status = SyncStatus.valueOf(status),
        errorMessage = errorMessage
    )
}

/**
 * Extension function to convert SyncAction domain model to SyncQueueEntity.
 */
fun SyncAction.toSyncQueueEntity(): SyncQueueEntity {
    return SyncQueueEntity(
        id = id,
        actionType = actionType.name,
        entityType = entityType.name,
        entityId = entityId,
        payload = payload,
        priority = priority,
        createdAt = createdAt,
        retryCount = retryCount,
        lastAttemptAt = lastAttemptAt,
        status = status.name,
        errorMessage = errorMessage
    )
}

