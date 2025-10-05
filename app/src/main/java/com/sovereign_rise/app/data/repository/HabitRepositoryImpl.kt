package com.sovereign_rise.app.data.repository

import android.util.Log
import com.google.gson.Gson
import com.sovereign_rise.app.data.local.TokenDataStore
import com.sovereign_rise.app.data.local.dao.HabitDao
import com.sovereign_rise.app.data.local.entity.HabitEntity
import com.sovereign_rise.app.data.local.entity.toHabit
import com.sovereign_rise.app.data.local.entity.toHabitEntity
import com.sovereign_rise.app.data.remote.api.HabitApiService
import com.sovereign_rise.app.data.remote.dto.*
import com.sovereign_rise.app.data.sync.SyncManager
import com.sovereign_rise.app.domain.model.*
import com.sovereign_rise.app.domain.repository.*
import com.sovereign_rise.app.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Implementation of HabitRepository with local-first offline support.
 */
class HabitRepositoryImpl(
    private val habitDao: HabitDao,
    private val habitApiService: HabitApiService,
    private val syncManager: SyncManager,
    private val tokenDataStore: TokenDataStore
) : HabitRepository {
    
    companion object {
        private const val TAG = "HabitRepositoryImpl"
    }
    
    private val gson = Gson()
    
    override suspend fun getHabits(): List<Habit> = withContext(Dispatchers.IO) {
        try {
            // Get current user ID
            val userId = tokenDataStore.getUserId() ?: return@withContext emptyList()
            
            // First, read from local database (fast UI)
            val localHabits = habitDao.getAllHabitsByUserId(userId).map { it.toHabit() }
            
            // Try to fetch from API in background if online
            if (syncManager.isOnline()) {
                try {
                    val response = habitApiService.getHabits()
                    if (response.isSuccessful) {
                        val habitsDto = response.body()
                        if (habitsDto != null) {
                            // Update local database with server data
                            val serverHabits = habitsDto.map { dto ->
                                dto.toHabit().toHabitEntity(
                                    syncStatus = SyncStatus.SYNCED,
                                    lastSyncedAt = System.currentTimeMillis(),
                                    serverUpdatedAt = dto.serverUpdatedAt ?: dto.updatedAt ?: System.currentTimeMillis(),
                                    localUpdatedAt = System.currentTimeMillis()
                                )
                            }
                            habitDao.insertAll(serverHabits)
                            return@withContext serverHabits.map { it.toHabit() }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching habits from API, using local data", e)
                }
            }
            
            // Return local data (graceful degradation)
            localHabits
        } catch (e: Exception) {
            Log.e(TAG, "Error getting habits", e)
            emptyList()
        }
    }
    
    /**
     * Gets habits with their sync status for UI display.
     */
    suspend fun getHabitsWithSyncStatus(): List<Pair<Habit, SyncStatus>> = withContext(Dispatchers.IO) {
        try {
            // Get current user ID
            val userId = tokenDataStore.getUserId() ?: return@withContext emptyList()
            
            val habitEntities = habitDao.getAllHabitsByUserId(userId)
            
            // Try to fetch from API in background if online
            if (syncManager.isOnline()) {
                try {
                    val response = habitApiService.getHabits()
                    if (response.isSuccessful) {
                        val habitsDto = response.body()
                        if (habitsDto != null) {
                            // Merge server data with local data, preserving PENDING changes
                            val localHabitsMap = habitEntities.associateBy { it.id }
                            
                            val mergedHabits = habitsDto.map { dto ->
                                val localHabit = localHabitsMap[dto.id]
                                
                                // If local habit has PENDING status, keep it and don't overwrite with server data
                                if (localHabit != null && localHabit.syncStatus == SyncStatus.PENDING.name) {
                                    localHabit // Keep local PENDING changes
                                } else {
                                    // Use server data
                                    dto.toHabit().toHabitEntity(
                                        syncStatus = SyncStatus.SYNCED,
                                        lastSyncedAt = System.currentTimeMillis(),
                                        serverUpdatedAt = dto.serverUpdatedAt ?: dto.updatedAt ?: System.currentTimeMillis(),
                                        localUpdatedAt = System.currentTimeMillis()
                                    )
                                }
                            }
                            
                            habitDao.insertAll(mergedHabits)
                            return@withContext mergedHabits.map { 
                                it.toHabit() to SyncStatus.valueOf(it.syncStatus)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching habits from API, using local data", e)
                }
            }
            
            // Return local data with sync status
            habitEntities.map { entity ->
                entity.toHabit() to SyncStatus.valueOf(entity.syncStatus)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting habits with sync status", e)
            emptyList()
        }
    }
    
    override suspend fun getHabitById(habitId: String): Habit = withContext(Dispatchers.IO) {
        try {
            // Get from local database first
            val habitEntity = habitDao.getHabitById(habitId)
            if (habitEntity != null) {
                return@withContext habitEntity.toHabit()
            }
            
            // If not found locally and online, try API
            if (syncManager.isOnline()) {
                try {
                    val response = habitApiService.getHabitById(habitId)
                    if (response.isSuccessful) {
                        val dto = response.body()
                        if (dto != null) {
                            val habit = dto.toHabit()
                            // Cache it locally
                            val entity = habit.toHabitEntity(
                                syncStatus = SyncStatus.SYNCED,
                                lastSyncedAt = System.currentTimeMillis(),
                                serverUpdatedAt = dto.serverUpdatedAt ?: System.currentTimeMillis(),
                                localUpdatedAt = System.currentTimeMillis()
                            )
                            habitDao.insertHabit(entity)
                            return@withContext habit
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error fetching habit from API", e)
                }
            }
            
            throw Exception(Constants.ERROR_HABIT_NOT_FOUND)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting habit by ID", e)
            throw e
        }
    }
    
    override suspend fun createHabit(habit: Habit): Habit = withContext(Dispatchers.IO) {
        try {
            // Validate
            if (habit.name.isBlank() || habit.name.length < Constants.MIN_HABIT_NAME_LENGTH) {
                throw IllegalArgumentException(Constants.ERROR_HABIT_NAME_TOO_SHORT)
            }
            if (habit.name.length > Constants.MAX_HABIT_NAME_LENGTH) {
                throw IllegalArgumentException(Constants.ERROR_HABIT_NAME_TOO_LONG)
            }
            if (habit.description != null && habit.description.length > Constants.MAX_HABIT_DESCRIPTION_LENGTH) {
                throw IllegalArgumentException(Constants.ERROR_HABIT_DESCRIPTION_TOO_LONG)
            }
            if (habit.type == HabitType.CUSTOM_INTERVAL && habit.intervalDays < Constants.MIN_CUSTOM_INTERVAL_DAYS) {
                throw IllegalArgumentException(Constants.ERROR_INVALID_INTERVAL)
            }
            
            // Get current user ID
            val userId = tokenDataStore.getUserId() 
                ?: throw Exception("User ID not found. Please log in again.")
            
            // Generate temporary ID if not present
            val habitId = habit.id.ifEmpty { UUID.randomUUID().toString() }
            val newHabit = habit.copy(
                id = habitId,
                userId = userId,
                streakDays = 0,
                longestStreak = 0,
                totalCompletions = 0,
                milestonesAchieved = emptyList()
            )
            
            // Insert into local database immediately
            val entity = newHabit.toHabitEntity(
                syncStatus = SyncStatus.PENDING,
                lastSyncedAt = null,
                serverUpdatedAt = null,
                localUpdatedAt = System.currentTimeMillis()
            )
            habitDao.insertHabit(entity)
            
            // Create sync action payload
            val request = CreateHabitRequest(
                name = newHabit.name,
                description = newHabit.description,
                type = newHabit.type.name,
                intervalDays = newHabit.intervalDays
            )
            val payload = gson.toJson(request)
            
            // Enqueue sync action
            syncManager.enqueueAction(
                actionType = SyncActionType.CREATE_HABIT,
                entityType = EntityType.HABIT,
                entityId = habitId,
                payload = payload,
                priority = Constants.PRIORITY_HABIT_CREATE
            )
            
            // If online, trigger immediate sync
            if (syncManager.isOnline()) {
                syncManager.syncPendingActions()
            }
            
            newHabit
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Validation error creating habit", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error creating habit", e)
            throw e
        }
    }
    
    override suspend fun updateHabit(habit: Habit): Habit = withContext(Dispatchers.IO) {
        try {
            // Validate
            if (habit.name.length < Constants.MIN_HABIT_NAME_LENGTH) {
                throw IllegalArgumentException(Constants.ERROR_HABIT_NAME_TOO_SHORT)
            }
            if (habit.name.length > Constants.MAX_HABIT_NAME_LENGTH) {
                throw IllegalArgumentException(Constants.ERROR_HABIT_NAME_TOO_LONG)
            }
            if (habit.description != null && habit.description.length > Constants.MAX_HABIT_DESCRIPTION_LENGTH) {
                throw IllegalArgumentException(Constants.ERROR_HABIT_DESCRIPTION_TOO_LONG)
            }
            if (habit.type == HabitType.CUSTOM_INTERVAL && habit.intervalDays < Constants.MIN_CUSTOM_INTERVAL_DAYS) {
                throw IllegalArgumentException(Constants.ERROR_INVALID_INTERVAL)
            }
            
            // Update local entity
            val localUpdatedAt = System.currentTimeMillis()
            val entity = habit.toHabitEntity(
                syncStatus = SyncStatus.PENDING,
                lastSyncedAt = null,
                serverUpdatedAt = null,
                localUpdatedAt = localUpdatedAt
            )
            habitDao.updateHabit(entity)
            
            // Create sync action payload with timestamp for conflict resolution
            val request = UpdateHabitRequest(
                name = habit.name,
                description = habit.description,
                type = habit.type.name,
                intervalDays = habit.intervalDays,
                isActive = habit.isActive,
                clientUpdatedAt = localUpdatedAt
            )
            val payload = gson.toJson(request)
            
            // Enqueue sync action
            syncManager.enqueueAction(
                actionType = SyncActionType.UPDATE_HABIT,
                entityType = EntityType.HABIT,
                entityId = habit.id,
                payload = payload,
                priority = Constants.PRIORITY_HABIT_UPDATE
            )
            
            // If online, trigger immediate sync
            if (syncManager.isOnline()) {
                syncManager.syncPendingActions()
            }
            
            habit
        } catch (e: IllegalArgumentException) {
            Log.e(TAG, "Validation error updating habit", e)
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error updating habit ${habit.id}", e)
            throw e
        }
    }
    
    override suspend fun deleteHabit(habitId: String) = withContext(Dispatchers.IO) {
        try {
            // Delete from local database
            habitDao.deleteHabitById(habitId)
            
            // Enqueue sync action
            val payload = "{}"
            syncManager.enqueueAction(
                actionType = SyncActionType.DELETE_HABIT,
                entityType = EntityType.HABIT,
                entityId = habitId,
                payload = payload,
                priority = Constants.PRIORITY_HABIT_DELETE
            )
            
            // If online, trigger immediate sync
            if (syncManager.isOnline()) {
                syncManager.syncPendingActions()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting habit $habitId", e)
            throw e
        }
    }
    
    override suspend fun tickHabit(habitId: String): TickHabitResult = withContext(Dispatchers.IO) {
        // Get habit from local DB
        val habitEntity = habitDao.getHabitById(habitId)
            ?: throw Exception(Constants.ERROR_HABIT_NOT_FOUND)
        
        val habit = habitEntity.toHabit()
        
        // Validate can check today (not already checked)
        if (!habit.canCheckToday()) {
            throw Exception(Constants.ERROR_HABIT_ALREADY_CHECKED)
        }
        
        // Update locally: increment streak
        val newStreakDays = habit.streakDays + 1
        val newLongestStreak = maxOf(habit.longestStreak, newStreakDays)
        val newTotalCompletions = habit.totalCompletions + 1
        val currentTime = System.currentTimeMillis()
        
        try {
            // Update streak offline
            habitDao.updateStreakOffline(
                habitId = habitId,
                streakDays = newStreakDays,
                longestStreak = newLongestStreak,
                lastCheckedAt = currentTime,
                totalCompletions = newTotalCompletions,
                timestamp = currentTime
            )
            
            // Check for milestones locally
            val milestone = when (newStreakDays) {
                7, 30, 100 -> newStreakDays
                else -> null
            }
            
            // Enqueue sync action (high priority)
            val payload = "{}"
            syncManager.enqueueAction(
                actionType = SyncActionType.TICK_HABIT,
                entityType = EntityType.HABIT,
                entityId = habitId,
                payload = payload,
                priority = Constants.PRIORITY_HABIT_TICK
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error updating local habit or enqueuing sync", e)
            throw Exception("Failed to check habit locally: ${e.message}")
        }
        
        // If online, try immediate sync to get server confirmation (but don't fail if this fails)
        var serverResult: TickHabitResult? = null
        if (syncManager.isOnline()) {
            try {
                // Use withTimeout to prevent long hangs on poor connections
                kotlinx.coroutines.withTimeout(3000L) { // 3 second timeout for optimistic sync
                    val response = habitApiService.tickHabit(habitId)
                    if (response.isSuccessful) {
                        val tickResponse = response.body()
                        if (tickResponse != null) {
                            // Update with server response
                            val serverHabit = tickResponse.habit.toHabit()
                            val syncedEntity = serverHabit.toHabitEntity(
                                syncStatus = SyncStatus.SYNCED,
                                lastSyncedAt = System.currentTimeMillis(),
                                serverUpdatedAt = tickResponse.habit.serverUpdatedAt ?: System.currentTimeMillis(),
                                localUpdatedAt = System.currentTimeMillis()
                            )
                            habitDao.updateHabit(syncedEntity)
                            
                            serverResult = TickHabitResult(
                                habit = serverHabit,
                                newStreakDays = tickResponse.newStreakDays,
                                milestoneAchieved = tickResponse.milestoneAchieved?.toMilestoneAchievement()
                            )
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.w(TAG, "Habit tick sync timed out - will retry via sync queue")
                // Continue with offline result
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing habit tick, will retry later via sync queue", e)
                // Continue with offline result - sync will happen later
            }
        }
        
        // Return server result if available
        serverResult?.let { return@withContext it }
        
        // Return optimistic result (local check succeeded)
        val updatedHabit = habit.copy(
            streakDays = newStreakDays,
            longestStreak = newLongestStreak,
            lastCheckedAt = currentTime,
            totalCompletions = newTotalCompletions
        )
        
        TickHabitResult(
            habit = updatedHabit,
            newStreakDays = newStreakDays,
            milestoneAchieved = null // Will get from server when synced
        )
    }
    
    override suspend fun checkStreakBreaks(): StreakBreakResult = withContext(Dispatchers.IO) {
        try {
            if (syncManager.isOnline()) {
                // Try API call
                try {
                    val response = habitApiService.checkStreakBreaks()
                    if (response.isSuccessful) {
                        val breakResponse = response.body()
                        if (breakResponse != null) {
                            // Update local database with broken habits
                            val brokenHabits = breakResponse.brokenHabits.map { dto ->
                                dto.toHabit().toHabitEntity(
                                    syncStatus = SyncStatus.SYNCED,
                                    lastSyncedAt = System.currentTimeMillis(),
                                    serverUpdatedAt = dto.serverUpdatedAt ?: System.currentTimeMillis(),
                                    localUpdatedAt = System.currentTimeMillis()
                                )
                            }
                            brokenHabits.forEach { habitDao.updateHabit(it) }
                            
                            return@withContext StreakBreakResult(
                                brokenHabits = brokenHabits.map { it.toHabit() }
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking streak breaks via API", e)
                }
            }
            
            // Offline: Perform local check
            // Get current user ID
            val userId = tokenDataStore.getUserId() ?: return@withContext StreakBreakResult(emptyList())
            val activeHabits = habitDao.getActiveHabitsByUserId(userId)
            val now = System.currentTimeMillis()
            val brokenHabits = mutableListOf<HabitEntity>()
            
            for (habitEntity in activeHabits) {
                val habit = habitEntity.toHabit()
                if (habit.lastCheckedAt != null) {
                    val daysSinceLastCheck = (now - habit.lastCheckedAt) / (24 * 60 * 60 * 1000)
                    val isOverdue = when (habit.type) {
                        HabitType.DAILY -> daysSinceLastCheck > 1
                        HabitType.WEEKLY -> daysSinceLastCheck > 7
                        HabitType.CUSTOM_INTERVAL -> daysSinceLastCheck > habit.intervalDays
                    }
                    
                    if (isOverdue) {
                        // Mark streak as broken
                        val brokenHabit = habitEntity.copy(
                            streakDays = 0,
                            syncStatus = SyncStatus.PENDING.name,
                            localUpdatedAt = now
                        )
                        habitDao.updateHabit(brokenHabit)
                        brokenHabits.add(brokenHabit)
                    }
                }
            }
            
            StreakBreakResult(
                brokenHabits = brokenHabits.map { it.toHabit() }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking streak breaks", e)
            throw e
        }
    }
    
}

