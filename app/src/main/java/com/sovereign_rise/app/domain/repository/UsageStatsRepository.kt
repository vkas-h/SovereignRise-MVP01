package com.sovereign_rise.app.domain.repository

import com.sovereign_rise.app.data.local.AppUsageData
import com.sovereign_rise.app.data.local.DailySummary

/**
 * Repository interface for usage stats
 */
interface UsageStatsRepository {
    
    /**
     * Check if usage stats permission is granted
     */
    fun hasPermission(): Boolean
    
    /**
     * Check if usage stats permission is granted (legacy method)
     */
    fun hasUsageStatsPermission(): Boolean = hasPermission()
    
    /**
     * Get app usage data for today
     */
    fun getTodayUsageData(): List<AppUsageData>
    
    /**
     * Get daily summary for a specific day
     */
    fun getDailySummary(timestamp: Long): DailySummary
    
    /**
     * Sync usage data to backend
     */
    suspend fun syncUsageData(
        appUsageData: List<AppUsageData>,
        dailySummary: DailySummary?
    ): Result<String>
    
    /**
     * Sync today's usage data
     */
    suspend fun syncTodayUsageData(): Result<String>
    
    // Legacy methods for backward compatibility with AI features
    suspend fun collectCurrentUsageStats() {}
    suspend fun saveUsageStats(stats: Any) {}
    suspend fun getUsageStats(startTime: Long, endTime: Long): List<Any> = emptyList()
    suspend fun getDistractingAppUsage(hours: Int = 1): Pair<String, Int>? = null
    suspend fun shouldTriggerNudge(): Boolean = false
    suspend fun recordNudgeSent() {}
    suspend fun requestUsageStatsPermission() {}
}
