package com.sovereign_rise.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Request to generate AI affirmation
 */
data class GenerateAffirmationRequest(
    @SerializedName("context")
    val context: String,
    
    @SerializedName("tone")
    val tone: String,
    
    @SerializedName("variables")
    val variables: Map<String, String>? = null
)

/**
 * Response from AI affirmation generation
 */
data class AffirmationResponse(
    @SerializedName("id")
    val id: String,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("tone")
    val tone: String,
    
    @SerializedName("context")
    val context: String
)

/**
 * Request for burnout insights
 */
data class BurnoutInsightsRequest(
    @SerializedName("metrics")
    val metrics: BurnoutMetrics
)

data class BurnoutMetrics(
    @SerializedName("completionRate")
    val completionRate: Double,
    
    @SerializedName("missedTaskCount")
    val missedTaskCount: Int,
    
    @SerializedName("lateNightActivityMinutes")
    val lateNightActivityMinutes: Int,
    
    @SerializedName("streakBreaks")
    val streakBreaks: Int,
    
    @SerializedName("snoozeCount")
    val snoozeCount: Int
)

/**
 * Response from burnout insights
 */
data class BurnoutInsightsResponse(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("recommendations")
    val recommendations: List<String>
)

/**
 * Request for smart reminder suggestion
 */
data class SmartReminderRequest(
    @SerializedName("taskTitle")
    val taskTitle: String,
    
    @SerializedName("taskDescription")
    val taskDescription: String?,
    
    @SerializedName("completionPatterns")
    val completionPatterns: CompletionPatterns
)

data class CompletionPatterns(
    @SerializedName("averageHour")
    val averageHour: Int,
    
    @SerializedName("averageMinute")
    val averageMinute: Int,
    
    @SerializedName("sampleSize")
    val sampleSize: Int
)

/**
 * Response from smart reminder
 */
data class SmartReminderResponse(
    @SerializedName("suggestedTime")
    val suggestedTime: String,
    
    @SerializedName("reasoning")
    val reasoning: String
)

