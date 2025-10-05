package com.sovereign_rise.app.data.remote.api

import com.sovereign_rise.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API service interface for AI features endpoints.
 */
interface AIApiService {
    
    /**
     * Generate AI affirmation using Gemini
     */
    @POST("/api/ai/affirmations")
    suspend fun generateAffirmation(@Body request: GenerateAffirmationRequest): Response<AffirmationResponse>
    
    /**
     * Generate burnout insights
     */
    @POST("/api/ai/burnout-insights")
    suspend fun generateBurnoutInsights(@Body request: BurnoutInsightsRequest): Response<BurnoutInsightsResponse>
    
    /**
     * Generate smart reminder suggestion
     */
    @POST("/api/ai/smart-reminder")
    suspend fun generateSmartReminder(@Body request: SmartReminderRequest): Response<SmartReminderResponse>
}

