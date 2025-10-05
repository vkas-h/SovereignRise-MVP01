package com.sovereign_rise.app.domain.model

/**
 * Metrics used to detect burnout
 */
data class BurnoutMetrics(
    val userId: String,
    val date: Long,
    val completionRate: Float,
    val snoozeCount: Int,
    val missedTaskCount: Int,
    val lateNightActivityMinutes: Int,
    val averageTaskCompletionTime: Long?,
    val streakBreaks: Int,
    val burnoutScore: Float
)

/**
 * Burnout severity levels
 */
enum class BurnoutLevel {
    HEALTHY,
    MILD,
    MODERATE,
    SEVERE
}

/**
 * Alert triggered when burnout is detected
 */
data class BurnoutAlert(
    val userId: String,
    val level: BurnoutLevel,
    val detectedAt: Long,
    val metrics: BurnoutMetrics,
    val recommendations: List<String>,
    val isRecoveryModeActive: Boolean
)

/**
 * Recovery mode configuration
 */
data class RecoveryMode(
    val userId: String,
    val startedAt: Long,
    val endsAt: Long,
    val penaltyMultiplier: Float,
    val taskReductionSuggested: Int,
    val message: String
)

/**
 * Types of interventions for burnout
 */
enum class BurnoutIntervention {
    REDUCE_TASKS,
    ACTIVATE_RECOVERY_MODE,
    SUGGEST_BREAK,
    ADJUST_REMINDERS,
    ENCOURAGE_HABITS
}

