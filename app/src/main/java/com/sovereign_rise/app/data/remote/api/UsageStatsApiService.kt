package com.sovereign_rise.app.data.remote.api

import com.sovereign_rise.app.data.local.AppUsageData
import com.sovereign_rise.app.data.local.DailySummary
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * API service for syncing usage stats
 */
interface UsageStatsApiService {
    
    @POST("api/app-usage/sync")
    suspend fun syncUsageData(
        @Body request: UsageSyncRequest
    ): UsageSyncResponse
}

data class UsageSyncRequest(
    val appUsageData: List<AppUsageDataDto>,
    val dailySummary: DailySummaryDto?
)

data class AppUsageDataDto(
    val packageName: String,
    val appName: String,
    val usageTimeMinutes: Int,
    val category: String,
    val isProductive: Boolean,
    val timestamp: Long
)

data class DailySummaryDto(
    val timestamp: Long,
    val totalScreenTimeMinutes: Int,
    val unlockCount: Int,
    val distractingAppTimeMinutes: Int,
    val productiveAppTimeMinutes: Int,
    val peakUsageHour: Int
)

data class UsageSyncResponse(
    val success: Boolean,
    val syncedApps: Int,
    val message: String
)

// Extension functions to convert domain models to DTOs
fun AppUsageData.toDto(): AppUsageDataDto {
    return AppUsageDataDto(
        packageName = packageName,
        appName = appName,
        usageTimeMinutes = usageTimeMinutes,
        category = category,
        isProductive = isProductive,
        timestamp = timestamp
    )
}

fun DailySummary.toDto(): DailySummaryDto {
    return DailySummaryDto(
        timestamp = timestamp,
        totalScreenTimeMinutes = totalScreenTimeMinutes,
        unlockCount = unlockCount,
        distractingAppTimeMinutes = distractingAppTimeMinutes,
        productiveAppTimeMinutes = productiveAppTimeMinutes,
        peakUsageHour = peakUsageHour
    )
}

