package com.sovereign_rise.app.data.repository

import com.sovereign_rise.app.data.local.dao.TaskDao
import com.sovereign_rise.app.data.local.dao.HabitDao
import com.sovereign_rise.app.data.local.UsageStatsDatabase
import com.sovereign_rise.app.data.remote.api.AnalyticsApiService
import com.sovereign_rise.app.data.remote.dto.toDomain
import com.sovereign_rise.app.domain.model.*
import com.sovereign_rise.app.domain.repository.AnalyticsRepository
import com.sovereign_rise.app.domain.repository.UsageStatsRepository
import com.sovereign_rise.app.util.ConnectivityObserver
import java.util.concurrent.TimeUnit

/**
 * Implementation of Analytics Repository with offline-first support
 */
class AnalyticsRepositoryImpl(
    private val analyticsApi: AnalyticsApiService,
    private val taskDao: TaskDao,
    private val habitDao: HabitDao,
    private val database: UsageStatsDatabase,
    private val usageStatsRepository: UsageStatsRepository,
    private val connectivityObserver: ConnectivityObserver
) : AnalyticsRepository {
    
    override suspend fun getComprehensiveAnalytics(userId: String, periodDays: Int): AnalyticsData {
        return try {
            // First, always get local analytics
            val localData = getLocalAnalytics(userId, periodDays)
            
            // Then, try to get AI insights if online
            val aiInsights = getAIInsights(userId, periodDays)
            
            // Merge both: local data + AI insights (if available)
            localData.copy(
                aiInsights = aiInsights,
                hasAIInsights = aiInsights != null
            )
        } catch (e: Exception) {
            android.util.Log.e("AnalyticsRepo", "Error in comprehensive analytics: ${e.message}")
            // Always return at least local data
            getLocalAnalytics(userId, periodDays)
        }
    }
    
    override suspend fun getLocalAnalytics(userId: String, periodDays: Int): AnalyticsData {
        try {
            val endTime = System.currentTimeMillis()
            val startTime = endTime - TimeUnit.DAYS.toMillis(periodDays.toLong())
            
            // Get usage stats from Room database
            val usageStatsDao = database.usageStatsDao()
            
            // Calculate today's midnight timestamp for accurate "today" filtering
            val calendar = java.util.Calendar.getInstance()
            calendar.timeInMillis = endTime
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
            calendar.set(java.util.Calendar.MINUTE, 0)
            calendar.set(java.util.Calendar.SECOND, 0)
            calendar.set(java.util.Calendar.MILLISECOND, 0)
            val todayStartTime = calendar.timeInMillis
            
            val todayUsage = usageStatsDao.getUsageStatsInRange(todayStartTime, endTime)
            
            // Get real-time usage data from UsageStatsRepository as primary source
            val realTimeSummary = try {
                if (usageStatsRepository.hasPermission()) {
                    usageStatsRepository.getDailySummary(System.currentTimeMillis())
                } else null
            } catch (e: Exception) {
                android.util.Log.e("AnalyticsRepo", "Error getting real-time summary: ${e.message}")
                null
            }
            
            // Use real-time data if available, otherwise fall back to database
            val todayScreenTime = realTimeSummary?.totalScreenTimeMinutes?.toLong() 
                ?: todayUsage.sumOf { it.totalScreenTimeMinutes.toLong() }
            val todayUnlocks = realTimeSummary?.unlockCount 
                ?: todayUsage.sumOf { it.unlockCount }
            
            val weekStartTime = endTime - TimeUnit.DAYS.toMillis(7)
            val weekUsage = usageStatsDao.getUsageStatsInRange(weekStartTime, endTime)
            val weekScreenTime = weekUsage.sumOf { it.totalScreenTimeMinutes.toLong() }
            val weekUnlocks = weekUsage.sumOf { it.unlockCount }
            
            // Get task data
            val tasks = taskDao.getTasksByUserId(userId)
            val todayTasks = tasks.filter { it.createdAt >= todayStartTime }
            val completedTasks = todayTasks.count { it.status == "COMPLETED" }
            
            // Get habit data
            val habits = habitDao.getAllHabitsByUserId(userId)
            
            // Calculate aggregate times from usage stats
            val todayDistractingTime = todayUsage.sumOf { it.distractingAppTimeMinutes }
            val todayProductiveTime = todayUsage.sumOf { it.productiveAppTimeMinutes }
            
            // Get real-time per-app usage data from UsageStatsRepository
            val topApps = try {
                if (usageStatsRepository.hasPermission()) {
                    val appUsage = usageStatsRepository.getTodayUsageData()
                    appUsage.map { appData ->
                        AppUsageData(
                            packageName = appData.packageName,
                            appName = appData.appName,
                            totalMinutes = appData.usageTimeMinutes,
                            category = appData.category,
                            isProductive = appData.isProductive,
                            usageCount = 0, // Not tracked per-app in current implementation
                            daysUsed = 1
                        )
                    }.sortedByDescending { it.totalMinutes }.take(10)
                } else {
                    android.util.Log.w("AnalyticsRepo", "Usage stats permission not granted")
                    emptyList()
                }
            } catch (e: Exception) {
                android.util.Log.e("AnalyticsRepo", "Error getting app usage data: ${e.message}")
                emptyList()
            }
            
            val distractingTime = if (topApps.isNotEmpty()) {
                topApps.filter { !it.isProductive }.sumOf { it.totalMinutes }
            } else todayDistractingTime
            
            val productiveTime = if (topApps.isNotEmpty()) {
                topApps.filter { it.isProductive }.sumOf { it.totalMinutes }
            } else todayProductiveTime
            
            // Build analytics data
            return AnalyticsData(
                period = periodDays,
                summary = AnalyticsSummary(
                    currentStreak = 0, // Calculate from task completion
                    longestStreak = 0,
                    totalTasksCompleted = completedTasks,
                    totalHabitCompletions = 0, // Calculate from habits
                    overallCompletionRate = if (todayTasks.isNotEmpty()) {
                        completedTasks.toDouble() / todayTasks.size
                    } else 0.0,
                    avgDailyScreenTime = if (weekUsage.isNotEmpty()) (weekScreenTime / weekUsage.size).toInt() else 0,
                    avgDailyUnlocks = if (weekUsage.isNotEmpty()) (weekUnlocks / weekUsage.size).toInt() else 0,
                    totalDistractingTime = distractingTime,
                    totalProductiveTime = productiveTime,
                    todayScreenTime = todayScreenTime.toInt(),
                    todayUnlocks = todayUnlocks,
                    weekScreenTime = weekScreenTime.toInt(),
                    weekUnlocks = weekUnlocks.toInt()
                ),
                streakHistory = emptyList(),
                taskCompletionRate = listOf(
                    TaskCompletionData(
                        date = System.currentTimeMillis(),
                        tasksCompleted = completedTasks,
                        tasksPending = todayTasks.size - completedTasks,
                        tasksFailed = 0,
                        completionRate = if (todayTasks.isNotEmpty()) {
                            completedTasks.toDouble() / todayTasks.size
                        } else 0.0,
                        avgCompletionHours = 0.0
                    )
                ),
                phoneUsage = listOf(
                    PhoneUsageData(
                        date = System.currentTimeMillis(),
                        screenTimeMinutes = todayScreenTime.toInt(),
                        unlockCount = todayUnlocks.toInt(),
                        distractingTime = distractingTime,
                        productiveTime = productiveTime,
                        peakHour = null
                    )
                ),
                topApps = topApps,
                habitCompletions = emptyList(),
                aiInsights = null,
                hasAIInsights = false,
                generatedAt = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            android.util.Log.e("AnalyticsRepo", "Error getting local analytics: ${e.message}")
            // Return empty analytics on error
            return createEmptyAnalytics(userId, periodDays)
        }
    }
    
    override suspend fun getAIInsights(userId: String, periodDays: Int): String? {
        return try {
            // Check connectivity
            val isOnline = connectivityObserver.isOnline()
            if (!isOnline) {
                android.util.Log.d("AnalyticsRepo", "Offline - skipping AI insights")
                return null
            }
            
            // Call backend API for AI insights
            val response = analyticsApi.getAnalytics(periodDays)
            val analyticsData = response.toDomain()
            analyticsData.aiInsights
        } catch (e: Exception) {
            android.util.Log.e("AnalyticsRepo", "Error fetching AI insights: ${e.message}")
            null
        }
    }
    
    override suspend fun getWeeklyInsights(userId: String): WeeklyInsights? {
        // Weekly insights are included in the main analytics via AI
        return null
    }
    
    private fun isProductiveApp(packageName: String): Boolean {
        // Simple heuristic - can be improved with a proper categorization system
        val productiveKeywords = listOf(
            "docs", "sheets", "slides", "calendar", "gmail", "drive",
            "notion", "evernote", "todoist", "trello", "slack", "teams",
            "zoom", "meet", "office", "word", "excel", "powerpoint"
        )
        return productiveKeywords.any { packageName.contains(it, ignoreCase = true) }
    }
    
    private fun createEmptyAnalytics(userId: String, periodDays: Int): AnalyticsData {
        return AnalyticsData(
            period = periodDays,
            summary = AnalyticsSummary(
                currentStreak = 0,
                longestStreak = 0,
                totalTasksCompleted = 0,
                totalHabitCompletions = 0,
                overallCompletionRate = 0.0,
                avgDailyScreenTime = 0,
                avgDailyUnlocks = 0,
                totalDistractingTime = 0,
                totalProductiveTime = 0,
                todayScreenTime = 0,
                todayUnlocks = 0,
                weekScreenTime = 0,
                weekUnlocks = 0
            ),
            streakHistory = emptyList(),
            taskCompletionRate = emptyList(),
            phoneUsage = emptyList(),
            topApps = emptyList(),
            habitCompletions = emptyList(),
            aiInsights = null,
            hasAIInsights = false,
            generatedAt = System.currentTimeMillis()
        )
    }
}
