package com.sovereign_rise.app.data.remote.api

import com.sovereign_rise.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API service interface for task endpoints.
 */
interface TaskApiService {
    
    /**
     * Get all tasks for current user.
     */
    @GET("/api/tasks")
    suspend fun getTasks(): Response<TaskListResponse>
    
    /**
     * Get specific task by ID.
     */
    @GET("/api/tasks/{taskId}")
    suspend fun getTaskById(@Path("taskId") taskId: String): Response<TaskDto>
    
    /**
     * Create new task.
     */
    @POST("/api/tasks")
    suspend fun createTask(@Body request: CreateTaskRequest): Response<TaskDto>
    
    /**
     * Update existing task.
     */
    @PUT("/api/tasks/{taskId}")
    suspend fun updateTask(
        @Path("taskId") taskId: String,
        @Body request: UpdateTaskRequest
    ): Response<TaskDto>
    
    /**
     * Delete task.
     */
    @DELETE("/api/tasks/{taskId}")
    suspend fun deleteTask(@Path("taskId") taskId: String): Response<Unit>
    
    /**
     * Mark task as completed and award XP/Aether.
     */
    @POST("/api/tasks/{taskId}/complete")
    suspend fun completeTask(@Path("taskId") taskId: String): Response<CompleteTaskResponse>
    
    /**
     * Trigger daily reset (mark uncompleted tasks as failed, apply penalties).
     */
    @POST("/api/tasks/reset")
    suspend fun triggerDailyReset(): Response<TaskListResponse>
    
    /**
     * Get yesterday's task summary.
     */
    @GET("/api/tasks/summary/yesterday")
    suspend fun getYesterdaySummary(): Response<DailyTaskSummaryDto>
}

