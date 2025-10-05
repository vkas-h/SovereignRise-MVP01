package com.sovereign_rise.app.data.remote.api

import com.sovereign_rise.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API service interface for habit endpoints.
 */
interface HabitApiService {
    
    /**
     * Get all habits for the authenticated user.
     */
    @GET("/api/habits")
    suspend fun getHabits(): Response<List<HabitDto>>
    
    /**
     * Get a specific habit by ID.
     */
    @GET("/api/habits/{habitId}")
    suspend fun getHabitById(
        @Path("habitId") habitId: String
    ): Response<HabitDto>
    
    /**
     * Create a new habit.
     */
    @POST("/api/habits")
    suspend fun createHabit(
        @Body request: CreateHabitRequest
    ): Response<HabitDto>
    
    /**
     * Update an existing habit.
     */
    @PUT("/api/habits/{habitId}")
    suspend fun updateHabit(
        @Path("habitId") habitId: String,
        @Body request: UpdateHabitRequest
    ): Response<HabitDto>
    
    /**
     * Delete a habit.
     */
    @DELETE("/api/habits/{habitId}")
    suspend fun deleteHabit(
        @Path("habitId") habitId: String
    ): Response<Unit>
    
    /**
     * Mark habit as completed for today.
     */
    @POST("/api/habits/{habitId}/tick")
    suspend fun tickHabit(
        @Path("habitId") habitId: String
    ): Response<TickHabitResponse>
    
    /**
     * Check for missed habits and apply penalties.
     */
    @POST("/api/habits/check-breaks")
    suspend fun checkStreakBreaks(): Response<StreakBreakResponse>
}

