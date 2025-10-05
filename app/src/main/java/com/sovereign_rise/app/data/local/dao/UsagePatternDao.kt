package com.sovereign_rise.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sovereign_rise.app.data.local.entity.UsagePatternEntity

@Dao
interface UsagePatternDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsagePattern(pattern: UsagePatternEntity)
    
    @Query("SELECT * FROM usage_patterns WHERE userId = :userId AND date = :date")
    suspend fun getUsagePattern(userId: String, date: Long): UsagePatternEntity?
    
    @Query("SELECT * FROM usage_patterns WHERE userId = :userId AND date >= :startDate ORDER BY date DESC LIMIT :limit")
    suspend fun getUsageTrend(userId: String, startDate: Long, limit: Int): List<UsagePatternEntity>
    
    @Query("DELETE FROM usage_patterns WHERE date < :cutoffDate")
    suspend fun deleteOldPatterns(cutoffDate: Long)
}

