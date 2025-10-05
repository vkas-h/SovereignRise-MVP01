package com.sovereign_rise.app.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.sovereign_rise.app.domain.model.*

data class AnalyticsResponse(
    val period: Int,
    val summary: AnalyticsSummaryDto,
    val streakHistory: List<StreakDataDto>,
    val taskCompletionRate: List<TaskCompletionDataDto>,
    val phoneUsage: List<PhoneUsageDataDto>,
    val topApps: List<AppUsageDataDto>,
    val habitCompletions: List<HabitCompletionDataDto>,
    val aiInsights: String?,
    val generatedAt: Long
)

data class AnalyticsSummaryDto(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalTasksCompleted: Int,
    val totalHabitCompletions: Int,
    val overallCompletionRate: Double,
    val avgDailyScreenTime: Int,
    val avgDailyUnlocks: Int,
    val totalDistractingTime: Int,
    val totalProductiveTime: Int,
    val todayScreenTime: Int,
    val todayUnlocks: Int,
    val weekScreenTime: Int,
    val weekUnlocks: Int
)

data class StreakDataDto(
    val date: Long,
    val streakDays: Int
)

data class TaskCompletionDataDto(
    val date: Long,
    val tasksCompleted: Int,
    val tasksPending: Int,
    val tasksFailed: Int,
    val completionRate: Double,
    val avgCompletionHours: Double
)

data class PhoneUsageDataDto(
    val date: Long,
    val screenTimeMinutes: Int,
    val unlockCount: Int,
    val distractingTime: Int,
    val productiveTime: Int,
    val peakHour: Int?
)

data class AppUsageDataDto(
    val packageName: String,
    val appName: String,
    val totalMinutes: Int,
    val category: String?,
    val isProductive: Boolean,
    val usageCount: Int
)

data class HabitCompletionDataDto(
    val date: Long,
    val completions: Int,
    val avgStreak: Double
)

// Extension functions to convert DTOs to domain models
fun AnalyticsResponse.toDomain(): AnalyticsData {
    return AnalyticsData(
        period = period,
        summary = summary.toDomain(),
        streakHistory = streakHistory.map { it.toDomain() },
        taskCompletionRate = taskCompletionRate.map { it.toDomain() },
        phoneUsage = phoneUsage.map { it.toDomain() },
        topApps = topApps.map { it.toDomain() },
        habitCompletions = habitCompletions.map { it.toDomain() },
        aiInsights = aiInsights,
        generatedAt = generatedAt
    )
}

fun AnalyticsSummaryDto.toDomain(): AnalyticsSummary {
    return AnalyticsSummary(
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        totalTasksCompleted = totalTasksCompleted,
        totalHabitCompletions = totalHabitCompletions,
        overallCompletionRate = overallCompletionRate,
        avgDailyScreenTime = avgDailyScreenTime,
        avgDailyUnlocks = avgDailyUnlocks,
        totalDistractingTime = totalDistractingTime,
        totalProductiveTime = totalProductiveTime,
        todayScreenTime = todayScreenTime,
        todayUnlocks = todayUnlocks,
        weekScreenTime = weekScreenTime,
        weekUnlocks = weekUnlocks
    )
}

fun StreakDataDto.toDomain(): StreakData {
    return StreakData(date = date, streakDays = streakDays)
}

fun TaskCompletionDataDto.toDomain(): TaskCompletionData {
    return TaskCompletionData(
        date = date,
        tasksCompleted = tasksCompleted,
        tasksPending = tasksPending,
        tasksFailed = tasksFailed,
        completionRate = completionRate,
        avgCompletionHours = avgCompletionHours
    )
}

fun PhoneUsageDataDto.toDomain(): PhoneUsageData {
    return PhoneUsageData(
        date = date,
        screenTimeMinutes = screenTimeMinutes,
        unlockCount = unlockCount,
        distractingTime = distractingTime,
        productiveTime = productiveTime,
        peakHour = peakHour
    )
}

fun AppUsageDataDto.toDomain(): AppUsageData {
    return AppUsageData(
        packageName = packageName,
        appName = appName,
        totalMinutes = totalMinutes,
        category = category,
        isProductive = isProductive,
        usageCount = usageCount
    )
}

fun HabitCompletionDataDto.toDomain(): HabitCompletionData {
    return HabitCompletionData(
        date = date,
        completions = completions,
        avgStreak = avgStreak
    )
}
