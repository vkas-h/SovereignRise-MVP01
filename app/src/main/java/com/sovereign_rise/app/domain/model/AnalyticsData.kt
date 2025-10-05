package com.sovereign_rise.app.domain.model

/**
 * Comprehensive analytics data including phone usage, tasks, habits, and AI insights
 */
data class AnalyticsData(
    val period: Int,
    val summary: AnalyticsSummary,
    val streakHistory: List<StreakData>,
    val taskCompletionRate: List<TaskCompletionData>,
    val phoneUsage: List<PhoneUsageData>,
    val topApps: List<AppUsageData>,
    val habitCompletions: List<HabitCompletionData>,
    val aiInsights: String?,
    val hasAIInsights: Boolean = false, // Indicates whether AI insights were successfully fetched
    val generatedAt: Long
)

data class AnalyticsSummary(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalTasksCompleted: Int,
    val totalHabitCompletions: Int,
    val overallCompletionRate: Double,
    val avgDailyScreenTime: Int, // in minutes
    val avgDailyUnlocks: Int,
    val totalDistractingTime: Int, // in minutes
    val totalProductiveTime: Int, // in minutes
    val todayScreenTime: Int, // Today's screen time in minutes
    val todayUnlocks: Int, // Today's unlocks
    val weekScreenTime: Int, // Last 7 days screen time in minutes
    val weekUnlocks: Int // Last 7 days unlocks
) {
    // Validated versions
    val validatedTodayScreenTime: Int
        get() = todayScreenTime.coerceIn(0, 1440)
    
    val validatedTodayUnlocks: Int
        get() = todayUnlocks.coerceIn(0, 500)
    
    val validatedWeekScreenTime: Int
        get() = weekScreenTime.coerceIn(0, 10080) // Max 7 * 24 hours
    
    val validatedWeekUnlocks: Int
        get() = weekUnlocks.coerceIn(0, 3500) // Max 500 * 7 days
}

data class StreakData(
    val date: Long,
    val streakDays: Int
)

data class TaskCompletionData(
    val date: Long,
    val tasksCompleted: Int,
    val tasksPending: Int,
    val tasksFailed: Int,
    val completionRate: Double,
    val avgCompletionHours: Double
)

data class PhoneUsageData(
    val date: Long,
    val screenTimeMinutes: Int,
    val unlockCount: Int,
    val distractingTime: Int,
    val productiveTime: Int,
    val peakHour: Int?
)

data class AppUsageData(
    val packageName: String,
    val appName: String,
    val totalMinutes: Int,
    val category: String?,
    val isProductive: Boolean,
    val usageCount: Int,
    val daysUsed: Int = 1
) {
    // Validate that usage time is reasonable
    val validatedMinutes: Int
        get() = totalMinutes.coerceIn(0, 1440) // Max 24 hours per day
    
    val hours: Int
        get() = validatedMinutes / 60
    
    val minutes: Int
        get() = validatedMinutes % 60
}

data class HabitCompletionData(
    val date: Long,
    val completions: Int,
    val avgStreak: Double
)

/**
 * Weekly insights from AI analysis
 */
data class WeeklyInsights(
    val summary: String,
    val keyAchievements: List<String>,
    val suggestions: List<String>,
    val streakTrend: String,
    val productivityScore: Double,
    val focusScore: Double,
    val generatedAt: Long
)
