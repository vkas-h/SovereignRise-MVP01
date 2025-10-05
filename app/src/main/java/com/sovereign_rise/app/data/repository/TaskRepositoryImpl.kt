package com.sovereign_rise.app.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.sovereign_rise.app.data.local.TokenDataStore
import com.sovereign_rise.app.data.local.dao.TaskDao
import com.sovereign_rise.app.data.local.entity.toTask
import com.sovereign_rise.app.data.local.entity.toTaskEntity
import com.sovereign_rise.app.data.remote.api.TaskApiService
import com.sovereign_rise.app.data.remote.dto.*
import com.sovereign_rise.app.data.sync.SyncManager
import com.sovereign_rise.app.domain.model.*
import com.sovereign_rise.app.domain.repository.TaskRepository
import com.sovereign_rise.app.util.Constants
import com.sovereign_rise.app.util.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Implementation of TaskRepository with local-first offline support.
 * 
 * @param context Application context
 * @param taskDao Local database DAO
 * @param taskApiService API service for task endpoints
 * @param syncManager Sync manager for handling offline actions
 * @param tokenDataStore DataStore for retrieving current user ID
 */
class TaskRepositoryImpl(
    private val context: Context,
    private val taskDao: TaskDao,
    private val taskApiService: TaskApiService,
    private val syncManager: SyncManager,
    private val tokenDataStore: TokenDataStore
) : TaskRepository {
    
    companion object {
        private const val TAG = "TaskRepositoryImpl"
    }
    
    private val gson = Gson()
    
    override suspend fun getTasks(): List<Task> = withContext(Dispatchers.IO) {
        try {
            // Get current user ID
            val userId = tokenDataStore.getUserId() ?: return@withContext emptyList()
            
            // First, read from local database (fast UI)
            val localTasks = taskDao.getTasksByUserId(userId).map { it.toTask() }
            
            // Try to fetch from API in background if online
            if (syncManager.isOnline()) {
                try {
                    val response = taskApiService.getTasks()
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            // Update local database with server data
                            val serverTasks = body.tasks.map { dto ->
                                dto.toTask().toTaskEntity(
                                    syncStatus = SyncStatus.SYNCED,
                                    lastSyncedAt = System.currentTimeMillis(),
                                    serverUpdatedAt = dto.serverUpdatedAt ?: dto.updatedAt ?: System.currentTimeMillis(),
                                    localUpdatedAt = System.currentTimeMillis()
                                )
                            }
                            taskDao.insertAll(serverTasks)
                            return@withContext serverTasks.map { it.toTask() }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching tasks from API, using local data", e)
                }
            }
            
            // Return local data (graceful degradation)
            localTasks
        } catch (e: Exception) {
            Log.e(TAG, "Error getting tasks", e)
            emptyList()
        }
    }
    
    /**
     * Gets tasks with their sync status for UI display.
     */
    suspend fun getTasksWithSyncStatus(): List<Pair<Task, SyncStatus>> = withContext(Dispatchers.IO) {
        try {
            // Get current user ID
            val userId = tokenDataStore.getUserId() ?: return@withContext emptyList()
            
            val taskEntities = taskDao.getTasksByUserId(userId)
            
            // Try to fetch from API in background if online
            if (syncManager.isOnline()) {
                try {
                    val response = taskApiService.getTasks()
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            val serverTasks = body.tasks.map { dto ->
                                dto.toTask().toTaskEntity(
                                    syncStatus = SyncStatus.SYNCED,
                                    lastSyncedAt = System.currentTimeMillis(),
                                    serverUpdatedAt = dto.serverUpdatedAt ?: dto.updatedAt ?: System.currentTimeMillis(),
                                    localUpdatedAt = System.currentTimeMillis()
                                )
                            }
                            taskDao.insertAll(serverTasks)
                            return@withContext serverTasks.map { 
                                it.toTask() to SyncStatus.valueOf(it.syncStatus)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching tasks from API, using local data", e)
                }
            }
            
            // Return local data with sync status
            taskEntities.map { entity ->
                entity.toTask() to SyncStatus.valueOf(entity.syncStatus)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting tasks with sync status", e)
            emptyList()
        }
    }
    
    override suspend fun getTaskById(taskId: String): Task = withContext(Dispatchers.IO) {
        try {
            // Get from local database first
            val taskEntity = taskDao.getTaskById(taskId)
            if (taskEntity != null) {
                return@withContext taskEntity.toTask()
            }
            
            // If not found locally and online, try API
            if (syncManager.isOnline()) {
                try {
                    val response = taskApiService.getTaskById(taskId)
                    if (response.isSuccessful) {
                        val dto = response.body()
                        if (dto != null) {
                            val task = dto.toTask()
                            // Cache it locally
                            val entity = task.toTaskEntity(
                                syncStatus = SyncStatus.SYNCED,
                                lastSyncedAt = System.currentTimeMillis(),
                                serverUpdatedAt = dto.serverUpdatedAt ?: System.currentTimeMillis(),
                                localUpdatedAt = System.currentTimeMillis()
                            )
                            taskDao.insertTask(entity)
                            return@withContext task
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching task from API", e)
                }
            }
            
            throw Exception("Task not found")
        } catch (e: Exception) {
            Log.e(TAG, "Error getting task by ID", e)
            throw e
        }
    }
    
    override suspend fun createTask(task: Task): Task = withContext(Dispatchers.IO) {
        try {
            // Get current user ID
            val userId = tokenDataStore.getUserId() 
                ?: throw Exception("User ID not found. Please log in again.")
            
            // Generate temporary ID if not present
            val taskId = task.id.ifEmpty { UUID.randomUUID().toString() }
            val newTask = task.copy(id = taskId, userId = userId)
            
            // Insert into local database immediately
            val entity = newTask.toTaskEntity(
                syncStatus = SyncStatus.PENDING,
                lastSyncedAt = null,
                serverUpdatedAt = null,
                localUpdatedAt = System.currentTimeMillis()
            )
            taskDao.insertTask(entity)
            
            // Schedule reminder if set
            newTask.reminderTime?.let { reminderTime ->
                ReminderScheduler.scheduleTaskReminder(
                    context = context,
                    taskId = newTask.id,
                    taskTitle = newTask.title,
                    taskDescription = newTask.description,
                    reminderTime = reminderTime
                )
            }
            
            // Create sync action payload
            val request = CreateTaskRequest(
                title = newTask.title,
                description = newTask.description,
                difficulty = newTask.difficulty.name,
                reminderTime = newTask.reminderTime
            )
            val payload = gson.toJson(request)
            
            // Enqueue sync action
            syncManager.enqueueAction(
                actionType = SyncActionType.CREATE_TASK,
                entityType = EntityType.TASK,
                entityId = taskId,
                payload = payload,
                priority = Constants.PRIORITY_TASK_CREATE
            )
            
            // If online, trigger immediate sync
            if (syncManager.isOnline()) {
                syncManager.syncPendingActions()
            }
            
            newTask
        } catch (e: Exception) {
            Log.e(TAG, "Error creating task", e)
            throw e
        }
    }
    
    override suspend fun updateTask(task: Task): Task = withContext(Dispatchers.IO) {
        try {
            // Update local entity
            val localUpdatedAt = System.currentTimeMillis()
            val entity = task.toTaskEntity(
                syncStatus = SyncStatus.PENDING,
                lastSyncedAt = null,
                serverUpdatedAt = null,
                localUpdatedAt = localUpdatedAt
            )
            taskDao.updateTask(entity)
            
            // Update reminder: cancel old one and schedule new one if present
            ReminderScheduler.cancelTaskReminder(context, task.id)
            task.reminderTime?.let { reminderTime ->
                ReminderScheduler.scheduleTaskReminder(
                    context = context,
                    taskId = task.id,
                    taskTitle = task.title,
                    taskDescription = task.description,
                    reminderTime = reminderTime
                )
            }
            
            // Create sync action payload with timestamp for conflict resolution
            val request = UpdateTaskRequest(
                title = task.title,
                description = task.description,
                difficulty = task.difficulty.name,
                reminderTime = task.reminderTime,
                status = task.status.name,
                clientUpdatedAt = localUpdatedAt
            )
            val payload = gson.toJson(request)
            
            // Enqueue sync action
            syncManager.enqueueAction(
                actionType = SyncActionType.UPDATE_TASK,
                entityType = EntityType.TASK,
                entityId = task.id,
                payload = payload,
                priority = Constants.PRIORITY_TASK_UPDATE
            )
            
            // If online, trigger immediate sync
            if (syncManager.isOnline()) {
                syncManager.syncPendingActions()
            }
            
            task
        } catch (e: Exception) {
            Log.e(TAG, "Error updating task", e)
            throw e
        }
    }
    
    override suspend fun deleteTask(taskId: String) = withContext(Dispatchers.IO) {
        try {
            // Cancel any scheduled reminder
            ReminderScheduler.cancelTaskReminder(context, taskId)
            
            // Delete from local database
            taskDao.deleteTaskById(taskId)
            
            // Enqueue sync action
            val payload = "{}"
            syncManager.enqueueAction(
                actionType = SyncActionType.DELETE_TASK,
                entityType = EntityType.TASK,
                entityId = taskId,
                payload = payload,
                priority = Constants.PRIORITY_TASK_DELETE
            )
            
            // If online, trigger immediate sync
            if (syncManager.isOnline()) {
                syncManager.syncPendingActions()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting task", e)
            throw e
        }
    }
    
    // Store the last completion response for the ViewModel to access
    var lastCompletionResponse: CompleteTaskResponse? = null
        private set
    
    override suspend fun completeTask(taskId: String): Task = withContext(Dispatchers.IO) {
        // Get task from local DB
        val taskEntity = taskDao.getTaskById(taskId)
            ?: throw Exception("Task not found")
        
        val task = taskEntity.toTask()
        
        // Update local entity: mark as completed
        val completedTask = task.copy(
            status = TaskStatus.COMPLETED,
            completedAt = System.currentTimeMillis()
        )
        
        val updatedEntity = completedTask.toTaskEntity(
            syncStatus = SyncStatus.PENDING,
            lastSyncedAt = null,
            serverUpdatedAt = null,
            localUpdatedAt = System.currentTimeMillis()
        )
        
        try {
            taskDao.updateTask(updatedEntity)
            
            // Enqueue sync action (highest priority)
            val payload = "{}" // Empty payload, taskId is in entityId
            syncManager.enqueueAction(
                actionType = SyncActionType.COMPLETE_TASK,
                entityType = EntityType.TASK,
                entityId = taskId,
                payload = payload,
                priority = Constants.PRIORITY_TASK_COMPLETION
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error updating local task or enqueuing sync", e)
            throw Exception("Failed to complete task locally: ${e.message}")
        }
        
        // If online, try immediate sync to get rewards (but don't fail if this fails)
        var serverTask: Task? = null
        if (syncManager.isOnline()) {
            try {
                // Use withTimeout to prevent long hangs on poor connections
                kotlinx.coroutines.withTimeout(3000L) { // 3 second timeout for optimistic sync
                    val response = taskApiService.completeTask(taskId)
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            lastCompletionResponse = body
                            // Update with server response
                            val task = body.task.toTask()
                            val syncedEntity = task.toTaskEntity(
                                syncStatus = SyncStatus.SYNCED,
                                lastSyncedAt = System.currentTimeMillis(),
                                serverUpdatedAt = body.task.serverUpdatedAt ?: System.currentTimeMillis(),
                                localUpdatedAt = System.currentTimeMillis()
                            )
                            taskDao.updateTask(syncedEntity)
                            serverTask = task
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.w(TAG, "Task completion sync timed out - will retry via sync queue")
                // Continue with offline completion
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing task completion, will retry later via sync queue", e)
                // Continue with offline completion - sync will happen later
            }
        }
        
        // Return server task if available, otherwise return local completed task
        serverTask?.let { return@withContext it }
        
        // Return optimistic result (local completion succeeded)
        completedTask
    }
    
    override suspend fun triggerDailyReset(): List<Task> = withContext(Dispatchers.IO) {
        try {
            if (syncManager.isOnline()) {
                // Try API call
                try {
                    val response = taskApiService.triggerDailyReset()
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null) {
                            // Update local database
                            val tasks = body.tasks.map { dto ->
                                dto.toTask().toTaskEntity(
                                    syncStatus = SyncStatus.SYNCED,
                                    lastSyncedAt = System.currentTimeMillis(),
                                    serverUpdatedAt = dto.serverUpdatedAt ?: System.currentTimeMillis(),
                                    localUpdatedAt = System.currentTimeMillis()
                                )
                            }
                            taskDao.insertAll(tasks)
                            return@withContext tasks.map { it.toTask() }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error triggering daily reset via API", e)
                }
            }
            
            // Offline: Perform local reset
            // Get current user ID
            val userId = tokenDataStore.getUserId() ?: return@withContext emptyList()
            val yesterdayMidnight = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
            val pendingTasks = taskDao.getTasksCreatedAfter(userId, yesterdayMidnight)
            
            // Mark pending tasks as FAILED
            val failedTasks = pendingTasks.filter { it.status == "PENDING" }.map { entity ->
                entity.copy(
                    status = TaskStatus.FAILED.name,
                    isMissed = true,
                    syncStatus = SyncStatus.PENDING.name,
                    localUpdatedAt = System.currentTimeMillis()
                )
            }
            
            failedTasks.forEach { taskDao.updateTask(it) }
            
            // Return all tasks
            val userId2 = "current_user"
            taskDao.getTasksByUserId(userId2).map { it.toTask() }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering daily reset", e)
            throw e
        }
    }
}
