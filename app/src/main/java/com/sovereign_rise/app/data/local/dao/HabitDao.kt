package com.sovereign_rise.app.data.local.dao

import androidx.room.*
import com.sovereign_rise.app.data.local.entity.HabitEntity

/**
 * DAO for Habit operations.
 */
@Dao
interface HabitDao {
    
    // Insert/Update operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(habits: List<HabitEntity>)
    
    @Update
    suspend fun updateHabit(habit: HabitEntity)
    
    @Transaction
    suspend fun upsertHabit(habit: HabitEntity) {
        insertHabit(habit)
    }
    
    // Query operations
    @Query("""
        SELECT * FROM habits 
        WHERE userId = :userId AND isActive = 1 
        ORDER BY streakDays DESC, name ASC
    """)
    suspend fun getActiveHabitsByUserId(userId: String): List<HabitEntity>
    
    @Query("""
        SELECT * FROM habits 
        WHERE userId = :userId 
        ORDER BY isActive DESC, streakDays DESC
    """)
    suspend fun getAllHabitsByUserId(userId: String): List<HabitEntity>
    
    @Query("SELECT * FROM habits WHERE id = :habitId")
    suspend fun getHabitById(habitId: String): HabitEntity?
    
    @Query("SELECT * FROM habits WHERE syncStatus = :syncStatus")
    suspend fun getHabitsBySyncStatus(syncStatus: String): List<HabitEntity>
    
    // Sync status updates
    @Query("UPDATE habits SET syncStatus = :status, lastSyncedAt = :timestamp WHERE id = :habitId")
    suspend fun updateSyncStatus(habitId: String, status: String, timestamp: Long)
    
    @Query("""
        UPDATE habits 
        SET syncStatus = :status, lastSyncedAt = :timestamp, serverUpdatedAt = :serverTimestamp 
        WHERE id = :habitId
    """)
    suspend fun markAsSynced(habitId: String, status: String, timestamp: Long, serverTimestamp: Long)
    
    // Streak updates (for offline tick)
    @Query("""
        UPDATE habits 
        SET streakDays = :streakDays, 
            longestStreak = :longestStreak, 
            lastCheckedAt = :lastCheckedAt, 
            totalCompletions = :totalCompletions, 
            localUpdatedAt = :timestamp, 
            syncStatus = 'PENDING' 
        WHERE id = :habitId
    """)
    suspend fun updateStreakOffline(
        habitId: String, 
        streakDays: Int, 
        longestStreak: Int, 
        lastCheckedAt: Long, 
        totalCompletions: Int, 
        timestamp: Long
    )
    
    // Delete operations
    @Delete
    suspend fun deleteHabit(habit: HabitEntity)
    
    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabitById(habitId: String)
    
    @Query("DELETE FROM habits WHERE userId = :userId AND createdAt < :cutoffTime")
    suspend fun deleteOldHabits(userId: String, cutoffTime: Long)
    
    // Aggregate queries
    @Query("SELECT COUNT(*) FROM habits WHERE userId = :userId AND isActive = 1")
    suspend fun getActiveHabitCount(userId: String): Int
    
    @Query("SELECT COUNT(*) FROM habits WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncCount(): Int
}

