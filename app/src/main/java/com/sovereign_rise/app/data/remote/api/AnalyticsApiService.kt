package com.sovereign_rise.app.data.remote.api

import com.sovereign_rise.app.data.remote.dto.AnalyticsResponse
import retrofit2.http.*

/**
 * Retrofit API service interface for analytics endpoints.
 */
interface AnalyticsApiService {
    
    /**
     * Get comprehensive analytics data with AI insights
     */
    @GET("/api/analytics")
    suspend fun getAnalytics(
        @Query("period") period: Int = 30
    ): AnalyticsResponse
}

