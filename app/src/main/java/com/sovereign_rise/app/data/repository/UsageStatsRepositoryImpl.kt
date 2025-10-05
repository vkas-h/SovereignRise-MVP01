package com.sovereign_rise.app.data.repository

import android.content.Context
import com.sovereign_rise.app.data.local.AppUsageData
import com.sovereign_rise.app.data.local.DailySummary
import com.sovereign_rise.app.data.local.UsageStatsManager
import com.sovereign_rise.app.data.remote.api.UsageStatsApiService
import com.sovereign_rise.app.data.remote.api.UsageSyncRequest
import com.sovereign_rise.app.data.remote.api.toDto
import com.sovereign_rise.app.domain.repository.UsageStatsRepository
import java.util.Calendar

/**
 * Implementation of UsageStatsRepository
 */
class UsageStatsRepositoryImpl(
    private val context: Context,
    private val usageStatsApi: UsageStatsApiService
) : UsageStatsRepository {

    private val usageStatsManager = UsageStatsManager(context)

    override fun hasPermission(): Boolean {
        return usageStatsManager.hasUsageStatsPermission()
    }

    override fun getTodayUsageData(): List<AppUsageData> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        
        val rawData = usageStatsManager.getAppUsageStats(startTime, endTime)
        
        // Deduplicate by package name (in case of any duplicates)
        val deduplicatedData = rawData
            .groupBy { it.packageName }
            .map { (_, apps) ->
                // Take the entry with the highest usage time (most recent/accurate)
                apps.maxByOrNull { it.usageTimeMinutes } ?: apps.first()
            }
        
        android.util.Log.d("UsageStatsRepo", "Today's usage: ${deduplicatedData.size} apps, " +
            "total time: ${deduplicatedData.sumOf { it.usageTimeMinutes }} min")
        
        return deduplicatedData
    }

    override fun getDailySummary(timestamp: Long): DailySummary {
        val summary = usageStatsManager.getDailySummary(timestamp)
        
        // Validate summary data
        if (summary.totalScreenTimeMinutes < 0 || summary.totalScreenTimeMinutes > 1440) {
            android.util.Log.w("UsageStatsRepo", "Invalid screen time: ${summary.totalScreenTimeMinutes}, resetting to 0")
            return summary.copy(totalScreenTimeMinutes = 0)
        }
        
        if (summary.unlockCount < 0 || summary.unlockCount > 500) {
            android.util.Log.w("UsageStatsRepo", "Invalid unlock count: ${summary.unlockCount}, capping")
            return summary.copy(unlockCount = summary.unlockCount.coerceIn(0, 500))
        }
        
        return summary
    }

    override suspend fun syncUsageData(
        appUsageData: List<AppUsageData>,
        dailySummary: DailySummary?
    ): Result<String> {
        return try {
            val request = UsageSyncRequest(
                appUsageData = appUsageData.map { it.toDto() },
                dailySummary = dailySummary?.toDto()
            )
            
            android.util.Log.d("UsageStatsRepo", "Sending sync request to API...")
            val response = usageStatsApi.syncUsageData(request)
            
            if (response.success) {
                android.util.Log.d("UsageStatsRepo", "Sync successful: ${response.message}")
                Result.success(response.message)
            } else {
                android.util.Log.e("UsageStatsRepo", "Sync failed: ${response.message}")
                Result.failure(Exception(response.message))
            }
        } catch (e: Exception) {
            android.util.Log.e("UsageStatsRepo", "Sync error: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun syncTodayUsageData(): Result<String> {
        if (!hasPermission()) {
            android.util.Log.e("UsageStatsRepo", "Permission not granted")
            return Result.failure(Exception("Usage stats permission not granted"))
        }
        
        val todayUsage = getTodayUsageData()
        val todaySummary = getDailySummary(System.currentTimeMillis())
        
        // Validate data before syncing
        if (todaySummary.totalScreenTimeMinutes == 0 && todayUsage.isEmpty()) {
            android.util.Log.w("UsageStatsRepo", "No usage data collected - device might have just started or permission just granted")
            return Result.success("No usage data available yet")
        }
        
        // Validate that app usage sums match summary (within reasonable margin)
        val appUsageSum = todayUsage.sumOf { it.usageTimeMinutes }
        val summaryTotal = todaySummary.totalScreenTimeMinutes
        
        if (appUsageSum > 0 && summaryTotal > 0) {
            val difference = kotlin.math.abs(appUsageSum - summaryTotal)
            val percentDiff = (difference.toFloat() / summaryTotal) * 100
            
            if (percentDiff > 20) {
                android.util.Log.w("UsageStatsRepo", "Data mismatch: app sum=$appUsageSum, summary=$summaryTotal (${percentDiff.toInt()}% diff)")
            }
        }
        
        android.util.Log.d("UsageStatsRepo", "Syncing ${todayUsage.size} apps, " +
                "Screen time: ${todaySummary.totalScreenTimeMinutes} min, " +
                "Unlocks: ${todaySummary.unlockCount}, " +
                "Timestamp: ${java.util.Date(todaySummary.timestamp)}")
        
        if (todayUsage.isEmpty()) {
            android.util.Log.w("UsageStatsRepo", "No app usage data to sync")
            return Result.success("No app usage data available")
        }
        
        return syncUsageData(todayUsage, todaySummary)
    }
}
