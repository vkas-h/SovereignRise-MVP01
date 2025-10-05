package com.sovereign_rise.app.domain.model

/**
 * Represents a smart reminder suggestion for a task
 */
data class SmartReminder(
    val taskId: String,
    val suggestedTime: Long,
    val confidence: Float,
    val reason: String,
    val basedOn: ReminderBasis
)

/**
 * The basis for a smart reminder suggestion
 */
enum class ReminderBasis {
    HISTORICAL_COMPLETION_TIME,
    LOCATION_PATTERN,
    CALENDAR_EVENT,
    OPTIMAL_ENERGY
}

/**
 * Historical task completion pattern
 */
data class CompletionPattern(
    val userId: String,
    val taskType: String?,
    val completionTimes: List<Long>,
    val averageCompletionHour: Int,
    val averageCompletionMinute: Int,
    val standardDeviation: Float,
    val sampleSize: Int
)

/**
 * A smart reminder suggestion with alternatives
 */
data class SmartReminderSuggestion(
    val suggestedTime: Long,
    val confidence: Float,
    val explanation: String,
    val alternativeTimes: List<Long>
)

