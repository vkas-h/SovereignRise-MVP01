package com.sovereign_rise.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sovereign_rise.app.data.local.entity.UsageStatsEntity

@Dao
interface UsageStatsDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsageStats(stats: UsageStatsEntity)
    
    @Insert
    suspend fun insertAll(stats: List<UsageStatsEntity>)
    
    @Query("SELECT * FROM usage_stats WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    suspend fun getUsageStatsInRange(startTime: Long, endTime: Long): List<UsageStatsEntity>
    
    @Query("SELECT * FROM usage_stats ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentUsageStats(limit: Int): List<UsageStatsEntity>
    
    @Query("SELECT * FROM usage_stats WHERE timestamp >= :startTime")
    suspend fun getUsageStatsSince(startTime: Long): List<UsageStatsEntity>
    
    @Query("SELECT SUM(totalScreenTimeMinutes) FROM usage_stats WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getTotalScreenTime(startTime: Long, endTime: Long): Int?
    
    @Query("SELECT SUM(unlockCount) FROM usage_stats WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getTotalUnlocks(startTime: Long, endTime: Long): Int?
    
    @Query("SELECT AVG(distractingAppTimeMinutes) FROM usage_stats WHERE timestamp >= :startTime")
    suspend fun getAverageDistractingTime(startTime: Long): Float?
    
    @Query("DELETE FROM usage_stats WHERE timestamp < :cutoffTime")
    suspend fun deleteOldStats(cutoffTime: Long)
    
    @Query("DELETE FROM usage_stats")
    suspend fun deleteAll()
}

