package com.sovereign_rise.app.domain.model

/**
 * Represents usage statistics for a specific time period
 */
data class UsageStats(
    val timestamp: Long,
    val totalScreenTimeMinutes: Int,
    val appUsageMap: Map<String, Int>,
    val unlockCount: Int,
    val mostUsedApps: List<AppUsage>
)

/**
 * Represents usage data for a specific app
 */
data class AppUsage(
    val packageName: String,
    val appName: String,
    val usageTimeMinutes: Int,
    val category: AppCategory
)

/**
 * Categories for classifying app usage
 */
enum class AppCategory {
    SOCIAL_MEDIA,
    ENTERTAINMENT,
    PRODUCTIVITY,
    COMMUNICATION,
    OTHER
}

/**
 * Aggregated usage pattern for a day
 */
data class UsagePattern(
    val userId: String,
    val date: Long,
    val totalScreenTimeMinutes: Int,
    val unlockCount: Int,
    val distractingAppTimeMinutes: Int,
    val productiveAppTimeMinutes: Int,
    val lateNightActivityMinutes: Int,
    val peakUsageHour: Int
)

