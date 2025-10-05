package com.sovereign_rise.app.data.local.dao

import androidx.room.*
import com.sovereign_rise.app.data.local.entity.UserEntity

/**
 * DAO for User profile caching.
 */
@Dao
interface UserDao {
    
    // Insert/Update operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)
    
    @Update
    suspend fun updateUser(user: UserEntity)
    
    // Query operations
    @Query("SELECT * FROM user_profile WHERE id = :userId")
    suspend fun getUserById(userId: String): UserEntity?
    
    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?
    
    // Cache validation
    @Query("SELECT cachedAt FROM user_profile WHERE id = :userId")
    suspend fun getCacheTimestamp(userId: String): Long?
    
    @Query("UPDATE user_profile SET cachedAt = :timestamp WHERE id = :userId")
    suspend fun updateCacheTimestamp(userId: String, timestamp: Long)
    
    // Sync status updates
    @Query("UPDATE user_profile SET syncStatus = :status, lastSyncedAt = :timestamp WHERE id = :userId")
    suspend fun updateSyncStatus(userId: String, status: String, timestamp: Long)
    
    // Delete operations
    @Query("DELETE FROM user_profile")
    suspend fun deleteAll()
    
    @Query("DELETE FROM user_profile WHERE cachedAt < :cutoffTime")
    suspend fun deleteExpiredCache(cutoffTime: Long)
    
    @Query("DELETE FROM user_profile WHERE isOfflineGuest = 1")
    suspend fun deleteOfflineGuests()
}

