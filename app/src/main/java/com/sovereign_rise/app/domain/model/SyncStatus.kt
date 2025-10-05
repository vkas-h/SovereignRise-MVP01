package com.sovereign_rise.app.domain.model

/**
 * Enum representing sync status for entities.
 */
enum class SyncStatus {
    SYNCED,     // Data is synchronized with server
    PENDING,    // Waiting to be synced
    SYNCING,    // Currently being synced
    FAILED,     // Sync failed after retries
    CONFLICT    // Conflict detected, needs resolution
}

/**
 * Data class representing a sync action in the queue.
 */
data class SyncAction(
    val id: String,
    val actionType: SyncActionType,
    val entityType: EntityType,
    val entityId: String,
    val payload: String,
    val priority: Int,
    val createdAt: Long,
    val retryCount: Int = 0,
    val lastAttemptAt: Long? = null,
    val status: SyncStatus = SyncStatus.PENDING,
    val errorMessage: String? = null
)

/**
 * Enum representing types of sync actions.
 */
enum class SyncActionType {
    // Task actions
    CREATE_TASK,
    UPDATE_TASK,
    DELETE_TASK,
    COMPLETE_TASK,
    
    // Habit actions
    CREATE_HABIT,
    UPDATE_HABIT,
    DELETE_HABIT,
    TICK_HABIT,
    
    // User actions
    UPDATE_USER_PROFILE
}

/**
 * Enum representing types of entities that can be synced.
 */
enum class EntityType {
    TASK,
    HABIT,
    USER,
    ITEM
}

/**
 * Data class representing the result of a sync operation.
 */
data class SyncResult(
    val success: Boolean,
    val syncedCount: Int = 0,
    val failedCount: Int = 0,
    val conflictCount: Int = 0,
    val errors: List<String> = emptyList()
)

