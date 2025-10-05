package com.sovereign_rise.app.domain.model

/**
 * Represents a motivational affirmation message
 */
data class Affirmation(
    val id: String,
    val message: String,
    val tone: AffirmationTone,
    val context: AffirmationContext,
    val variables: Map<String, String>
)

/**
 * Tone/style of affirmation messages
 */
enum class AffirmationTone {
    MOTIVATIONAL,
    PHILOSOPHICAL,
    PRACTICAL,
    HUMOROUS
}

/**
 * Context in which affirmation is shown
 */
enum class AffirmationContext {
    TASK_COMPLETED,
    HABIT_CHECKED,
    STREAK_MILESTONE,
    PERFECT_DAY,
    COMEBACK,
    LEVEL_UP
}

/**
 * Record of an affirmation delivery
 */
data class AffirmationDelivery(
    val affirmation: Affirmation,
    val deliveredAt: Long,
    val userId: String,
    val triggerEvent: String,
    val wasShown: Boolean
)

/**
 * Template for generating affirmations
 */
data class AffirmationTemplate(
    val template: String,
    val tone: AffirmationTone,
    val context: AffirmationContext,
    val requiredVariables: List<String>
)

