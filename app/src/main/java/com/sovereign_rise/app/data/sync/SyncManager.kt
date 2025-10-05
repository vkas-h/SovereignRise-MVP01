package com.sovereign_rise.app.data.sync

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.sovereign_rise.app.data.local.dao.*
import com.sovereign_rise.app.data.local.entity.*
import com.sovereign_rise.app.data.remote.api.HabitApiService
import com.sovereign_rise.app.data.remote.api.TaskApiService
import com.sovereign_rise.app.data.remote.dto.*
import com.sovereign_rise.app.domain.model.*
import com.sovereign_rise.app.util.*
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Central sync engine that processes the queue and syncs with backend.
 */
class SyncManager(
    private val taskDao: TaskDao,
    private val habitDao: HabitDao,
    private val userDao: UserDao,
    private val syncQueueDao: SyncQueueDao,
    private val taskApiService: TaskApiService,
    private val habitApiService: HabitApiService,
    private val context: Context
) {
    
    companion object {
        private const val TAG = "SyncManager"
    }
    
    private val gson = Gson()
    private val connectivityObserver = ConnectivityObserver(context)
    
    /**
     * Main sync orchestrator that processes pending actions.
     */
    suspend fun syncPendingActions(): SyncResult {
        // Check network connectivity
        if (!isOnline()) {
            Log.d(TAG, "Offline - skipping sync")
            return SyncResult(success = false, errors = listOf("No network connection"))
        }
        
        var syncedCount = 0
        var failedCount = 0
        var conflictCount = 0
        val errors = mutableListOf<String>()
        
        try {
            // Get pending actions from queue
            val pendingActions = syncQueueDao.getPendingActions(limit = Constants.SYNC_BATCH_SIZE)
            
            Log.d(TAG, "Processing ${pendingActions.size} pending actions")
            
            for (action in pendingActions) {
                try {
                    // Mark action as syncing
                    syncQueueDao.markActionSyncing(action.id)
                    
                    // Process action based on type
                    val success = when (action.entityType) {
                        EntityType.TASK.name -> processTaskAction(action)
                        EntityType.HABIT.name -> processHabitAction(action)
                        EntityType.USER.name -> processUserAction(action)
                        else -> {
                            Log.w(TAG, "Unknown entity type: ${action.entityType}")
                            false
                        }
                    }
                    
                    if (success) {
                        // Delete from queue and mark entity as synced
                        syncQueueDao.deleteAction(action.id)
                        syncedCount++
                        Log.d(TAG, "Successfully synced action ${action.id}")
                    } else {
                        // Increment retry count and mark as failed if max retries reached
                        val newRetryCount = action.retryCount + 1
                        if (newRetryCount >= Constants.SYNC_RETRY_ATTEMPTS) {
                            syncQueueDao.markActionFailed(
                                action.id,
                                SyncStatus.FAILED.name,
                                newRetryCount,
                                System.currentTimeMillis(),
                                "Max retry attempts exceeded"
                            )
                            failedCount++
                            errors.add("Failed to sync ${action.entityType} ${action.entityId}")
                        } else {
                            // Keep as PENDING for retry
                            syncQueueDao.updateActionStatus(
                                action.id,
                                SyncStatus.PENDING.name,
                                System.currentTimeMillis()
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing action ${action.id}", e)
                    val newRetryCount = action.retryCount + 1
                    if (newRetryCount >= Constants.SYNC_RETRY_ATTEMPTS) {
                        syncQueueDao.markActionFailed(
                            action.id,
                            SyncStatus.FAILED.name,
                            newRetryCount,
                            System.currentTimeMillis(),
                            e.message ?: "Unknown error"
                        )
                        failedCount++
                    } else {
                        syncQueueDao.updateActionStatus(
                            action.id,
                            SyncStatus.PENDING.name,
                            System.currentTimeMillis()
                        )
                    }
                    errors.add("Error syncing ${action.entityType} ${action.entityId}: ${e.message}")
                }
            }
            
            Log.d(TAG, "Sync complete: $syncedCount synced, $failedCount failed, $conflictCount conflicts")
            
            return SyncResult(
                success = failedCount == 0,
                syncedCount = syncedCount,
                failedCount = failedCount,
                conflictCount = conflictCount,
                errors = errors
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error during sync", e)
            return SyncResult(
                success = false,
                errors = listOf("Sync error: ${e.message}")
            )
        }
    }
    
    /**
     * Processes task-related sync actions.
     */
    private suspend fun processTaskAction(action: SyncQueueEntity): Boolean {
        return try {
            when (SyncActionType.valueOf(action.actionType)) {
                SyncActionType.CREATE_TASK -> {
                    val request = gson.fromJson(action.payload, CreateTaskRequest::class.java)
                    val response = taskApiService.createTask(request)
                    if (response.isSuccessful) {
                        val task = response.body()?.toTask() ?: return false
                        // Delete the temporary local entity with client UUID
                        taskDao.deleteTaskById(action.entityId)
                        // Insert the server-created entity with real ID
                        val entity = task.toTaskEntity(
                            SyncStatus.SYNCED,
                            System.currentTimeMillis(),
                            System.currentTimeMillis(),
                            System.currentTimeMillis()
                        )
                        taskDao.insertTask(entity)
                        true
                    } else {
                        false
                    }
                }
                SyncActionType.UPDATE_TASK -> {
                    // Fetch local entity to check timestamps for conflict resolution
                    val localEntity = taskDao.getTaskById(action.entityId)
                    if (localEntity == null) {
                        Log.w(TAG, "Local task not found for update: ${action.entityId}")
                        return false
                    }
                    
                    // Check if server data is newer (conflict resolution)
                    val serverUpdatedAt = localEntity.serverUpdatedAt ?: 0L
                    val localUpdatedAt = localEntity.localUpdatedAt
                    
                    if (serverUpdatedAt > localUpdatedAt) {
                        // Server is newer - fetch and update local, skip push
                        Log.d(TAG, "Server data is newer, fetching latest for task ${action.entityId}")
                        try {
                            val fetchResponse = taskApiService.getTaskById(action.entityId)
                            if (fetchResponse.isSuccessful) {
                                val serverTask = fetchResponse.body()?.toTask()
                                if (serverTask != null) {
                                    val syncedEntity = serverTask.toTaskEntity(
                                        SyncStatus.SYNCED,
                                        System.currentTimeMillis(),
                                        fetchResponse.body()!!.serverUpdatedAt ?: System.currentTimeMillis(),
                                        System.currentTimeMillis()
                                    )
                                    taskDao.updateTask(syncedEntity)
                                    return true
                                }
                            } else if (fetchResponse.code() == 404) {
                                // Task was deleted on server (e.g., by cleanup system)
                                Log.i(TAG, "Task ${action.entityId} was deleted on server, removing from local DB")
                                taskDao.deleteTaskById(action.entityId)
                                return true // Mark as successfully synced (deletion)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to fetch server data", e)
                        }
                    }
                    
                    // Local is newer or equal - proceed with update
                    val request = gson.fromJson(action.payload, UpdateTaskRequest::class.java)
                    val requestWithTimestamp = request.copy(clientUpdatedAt = localUpdatedAt)
                    val response = taskApiService.updateTask(action.entityId, requestWithTimestamp)
                    
                    when {
                        response.isSuccessful -> {
                            val task = response.body()?.toTask() ?: return false
                            val entity = task.toTaskEntity(
                                SyncStatus.SYNCED,
                                System.currentTimeMillis(),
                                response.body()!!.serverUpdatedAt ?: System.currentTimeMillis(),
                                System.currentTimeMillis()
                            )
                            taskDao.updateTask(entity)
                            true
                        }
                        response.code() == 404 -> {
                            // Task was deleted on server (e.g., by cleanup system)
                            Log.i(TAG, "Task ${action.entityId} was deleted on server, removing from local DB")
                            taskDao.deleteTaskById(action.entityId)
                            true // Mark as successfully synced (deletion)
                        }
                        response.code() == 409 -> {
                            // Conflict detected - mark as CONFLICT for manual resolution
                            Log.w(TAG, "Conflict detected for task ${action.entityId}")
                            taskDao.updateSyncStatus(action.entityId, SyncStatus.CONFLICT.name, System.currentTimeMillis())
                            false
                        }
                        else -> false
                    }
                }
                SyncActionType.COMPLETE_TASK -> {
                    val response = taskApiService.completeTask(action.entityId)
                    when {
                        response.isSuccessful -> {
                            val task = response.body()?.task?.toTask() ?: return false
                            val entity = task.toTaskEntity(
                                SyncStatus.SYNCED,
                                System.currentTimeMillis(),
                                System.currentTimeMillis(),
                                System.currentTimeMillis()
                            )
                            taskDao.updateTask(entity)
                            true
                        }
                        response.code() == 404 -> {
                            // Task was deleted on server (e.g., by cleanup system)
                            Log.i(TAG, "Task ${action.entityId} was deleted on server, removing from local DB")
                            taskDao.deleteTaskById(action.entityId)
                            true // Mark as successfully synced (deletion)
                        }
                        else -> false
                    }
                }
                SyncActionType.DELETE_TASK -> {
                    val response = taskApiService.deleteTask(action.entityId)
                    when {
                        response.isSuccessful -> {
                            taskDao.deleteTaskById(action.entityId)
                            true
                        }
                        response.code() == 404 -> {
                            // Task already deleted on server (e.g., by cleanup system)
                            Log.i(TAG, "Task ${action.entityId} already deleted on server")
                            taskDao.deleteTaskById(action.entityId)
                            true // Mark as successfully synced
                        }
                        else -> false
                    }
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing task action", e)
            false
        }
    }
    
    /**
     * Processes habit-related sync actions.
     */
    private suspend fun processHabitAction(action: SyncQueueEntity): Boolean {
        return try {
            when (SyncActionType.valueOf(action.actionType)) {
                SyncActionType.CREATE_HABIT -> {
                    val request = gson.fromJson(action.payload, CreateHabitRequest::class.java)
                    val response = habitApiService.createHabit(request)
                    if (response.isSuccessful) {
                        val habit = response.body()?.toHabit() ?: return false
                        // Delete the temporary local entity with client UUID
                        habitDao.deleteHabitById(action.entityId)
                        // Insert the server-created entity with real ID
                        val entity = habit.toHabitEntity(
                            SyncStatus.SYNCED,
                            System.currentTimeMillis(),
                            System.currentTimeMillis(),
                            System.currentTimeMillis()
                        )
                        habitDao.insertHabit(entity)
                        true
                    } else {
                        false
                    }
                }
                SyncActionType.UPDATE_HABIT -> {
                    // Fetch local entity to check timestamps for conflict resolution
                    val localEntity = habitDao.getHabitById(action.entityId)
                    if (localEntity == null) {
                        Log.w(TAG, "Local habit not found for update: ${action.entityId}")
                        return false
                    }
                    
                    // Check if server data is newer (conflict resolution)
                    val serverUpdatedAt = localEntity.serverUpdatedAt ?: 0L
                    val localUpdatedAt = localEntity.localUpdatedAt
                    
                    if (serverUpdatedAt > localUpdatedAt) {
                        // Server is newer - fetch and update local, skip push
                        Log.d(TAG, "Server data is newer, fetching latest for habit ${action.entityId}")
                        try {
                            val fetchResponse = habitApiService.getHabitById(action.entityId)
                            if (fetchResponse.isSuccessful) {
                                val serverHabit = fetchResponse.body()?.toHabit()
                                if (serverHabit != null) {
                                    val syncedEntity = serverHabit.toHabitEntity(
                                        SyncStatus.SYNCED,
                                        System.currentTimeMillis(),
                                        fetchResponse.body()!!.serverUpdatedAt ?: System.currentTimeMillis(),
                                        System.currentTimeMillis()
                                    )
                                    habitDao.updateHabit(syncedEntity)
                                    return true
                                }
                            } else if (fetchResponse.code() == 404) {
                                // Habit was deleted on server (e.g., by cleanup system)
                                Log.i(TAG, "Habit ${action.entityId} was deleted on server, removing from local DB")
                                habitDao.deleteHabitById(action.entityId)
                                return true // Mark as successfully synced (deletion)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to fetch server data", e)
                        }
                    }
                    
                    // Local is newer or equal - proceed with update
                    val request = gson.fromJson(action.payload, UpdateHabitRequest::class.java)
                    val requestWithTimestamp = request.copy(clientUpdatedAt = localUpdatedAt)
                    val response = habitApiService.updateHabit(action.entityId, requestWithTimestamp)
                    
                    when {
                        response.isSuccessful -> {
                            val habit = response.body()?.toHabit() ?: return false
                            val entity = habit.toHabitEntity(
                                SyncStatus.SYNCED,
                                System.currentTimeMillis(),
                                response.body()!!.serverUpdatedAt ?: System.currentTimeMillis(),
                                System.currentTimeMillis()
                            )
                            habitDao.updateHabit(entity)
                            true
                        }
                        response.code() == 404 -> {
                            // Habit was deleted on server (e.g., by cleanup system)
                            Log.i(TAG, "Habit ${action.entityId} was deleted on server, removing from local DB")
                            habitDao.deleteHabitById(action.entityId)
                            true // Mark as successfully synced (deletion)
                        }
                        response.code() == 409 -> {
                            // Conflict detected - mark as CONFLICT for manual resolution
                            Log.w(TAG, "Conflict detected for habit ${action.entityId}")
                            habitDao.updateSyncStatus(action.entityId, SyncStatus.CONFLICT.name, System.currentTimeMillis())
                            false
                        }
                        else -> false
                    }
                }
                SyncActionType.TICK_HABIT -> {
                    val response = habitApiService.tickHabit(action.entityId)
                    when {
                        response.isSuccessful -> {
                            val habit = response.body()?.habit?.toHabit() ?: return false
                            val entity = habit.toHabitEntity(
                                SyncStatus.SYNCED,
                                System.currentTimeMillis(),
                                System.currentTimeMillis(),
                                System.currentTimeMillis()
                            )
                            habitDao.updateHabit(entity)
                            true
                        }
                        response.code() == 404 -> {
                            // Habit was deleted on server (e.g., by cleanup system)
                            Log.i(TAG, "Habit ${action.entityId} was deleted on server, removing from local DB")
                            habitDao.deleteHabitById(action.entityId)
                            true // Mark as successfully synced (deletion)
                        }
                        else -> false
                    }
                }
                SyncActionType.DELETE_HABIT -> {
                    val response = habitApiService.deleteHabit(action.entityId)
                    when {
                        response.isSuccessful -> {
                            habitDao.deleteHabitById(action.entityId)
                            true
                        }
                        response.code() == 404 -> {
                            // Habit already deleted on server (e.g., by cleanup system)
                            Log.i(TAG, "Habit ${action.entityId} already deleted on server")
                            habitDao.deleteHabitById(action.entityId)
                            true // Mark as successfully synced
                        }
                        else -> false
                    }
                }
                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing habit action", e)
            false
        }
    }
    
    /**
     * Processes user-related sync actions.
     */
    private suspend fun processUserAction(@Suppress("UNUSED_PARAMETER") action: SyncQueueEntity): Boolean {
        // User profile updates can be implemented here
        Log.d(TAG, "User action processing not yet implemented")
        return false
    }
    
    /**
     * Enqueues a sync action to the queue.
     */
    suspend fun enqueueAction(
        actionType: SyncActionType,
        entityType: EntityType,
        entityId: String,
        payload: String,
        priority: Int
    ) {
        try {
            // Check queue size
            val queueSize = syncQueueDao.getQueueSize()
            if (queueSize >= Constants.MAX_QUEUED_ACTIONS) {
                // Trim queue (remove lowest priority actions)
                val trimCount = queueSize - Constants.MAX_QUEUED_ACTIONS + 1
                syncQueueDao.trimQueue(trimCount)
                Log.w(TAG, "Queue full - trimmed $trimCount actions")
            }
            
            // Create sync action
            val action = SyncAction(
                id = UUID.randomUUID().toString(),
                actionType = actionType,
                entityType = entityType,
                entityId = entityId,
                payload = payload,
                priority = priority,
                createdAt = System.currentTimeMillis(),
                retryCount = 0,
                lastAttemptAt = null,
                status = SyncStatus.PENDING,
                errorMessage = null
            )
            
            // Insert into queue
            syncQueueDao.enqueueAction(action.toSyncQueueEntity())
            Log.d(TAG, "Enqueued action: ${actionType.name} for ${entityType.name} $entityId")
        } catch (e: Exception) {
            Log.e(TAG, "Error enqueueing action", e)
        }
    }
    
    /**
     * Observes connectivity changes.
     */
    fun observeConnectivity(): Flow<ConnectivityStatus> {
        return connectivityObserver.observe()
    }
    
    /**
     * Checks if device is online.
     */
    fun isOnline(): Boolean {
        return connectivityObserver.isOnline()
    }
    
    /**
     * Gets the count of pending sync actions.
     */
    suspend fun getPendingSyncCount(): Int {
        return try {
            syncQueueDao.getPendingCount()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting pending sync count", e)
            0
        }
    }
    
    /**
     * Clears all failed actions from the queue.
     */
    suspend fun clearFailedActions() {
        try {
            val cutoffTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 hours ago
            syncQueueDao.deleteOldFailedActions(cutoffTime)
            Log.d(TAG, "Cleared old failed actions")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing failed actions", e)
        }
    }
}

