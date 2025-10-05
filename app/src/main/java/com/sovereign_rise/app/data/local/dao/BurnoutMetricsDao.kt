package com.sovereign_rise.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.sovereign_rise.app.data.local.entity.BurnoutMetricsEntity

@Dao
interface BurnoutMetricsDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBurnoutMetrics(metrics: BurnoutMetricsEntity)
    
    @Query("SELECT * FROM burnout_metrics WHERE userId = :userId AND date = :date")
    suspend fun getBurnoutMetrics(userId: String, date: Long): BurnoutMetricsEntity?
    
    @Query("SELECT * FROM burnout_metrics WHERE userId = :userId AND date >= :startDate ORDER BY date DESC LIMIT :limit")
    suspend fun getBurnoutTrend(userId: String, startDate: Long, limit: Int): List<BurnoutMetricsEntity>
    
    @Query("DELETE FROM burnout_metrics WHERE date < :cutoffDate")
    suspend fun deleteOldMetrics(cutoffDate: Long)
}

