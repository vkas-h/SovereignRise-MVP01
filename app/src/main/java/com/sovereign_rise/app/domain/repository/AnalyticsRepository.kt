package com.sovereign_rise.app.domain.repository

import com.sovereign_rise.app.domain.model.AnalyticsData
import com.sovereign_rise.app.domain.model.WeeklyInsights

/**
 * Repository interface for analytics data
 * Supports both online (with AI insights) and offline (local data only) modes
 */
interface AnalyticsRepository {
    
    /**
     * Get comprehensive analytics data including phone usage, tasks, habits, and AI insights
     * Returns local data even when offline, with AI insights as optional enhancement
     */
    suspend fun getComprehensiveAnalytics(userId: String, periodDays: Int): AnalyticsData
    
    /**
     * Get local analytics from Room database only (usage stats, task completion, habits)
     * Works completely offline without backend calls
     */
    suspend fun getLocalAnalytics(userId: String, periodDays: Int): AnalyticsData
    
    /**
     * Get AI-generated insights from backend (requires internet)
     * Returns null if offline or if AI fetch fails
     */
    suspend fun getAIInsights(userId: String, periodDays: Int): String?
    
    /**
     * Get weekly insights (optional, may return null)
     */
    suspend fun getWeeklyInsights(userId: String): WeeklyInsights?
}
