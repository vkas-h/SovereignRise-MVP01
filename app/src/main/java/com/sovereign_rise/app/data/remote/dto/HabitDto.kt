package com.sovereign_rise.app.data.remote.dto

import com.google.gson.annotations.SerializedName
import com.sovereign_rise.app.domain.model.*
import com.sovereign_rise.app.domain.repository.MilestoneAchievement

/**
 * DTO for Habit received from backend.
 */
data class HabitDto(
    @SerializedName("id") val id: String,
    @SerializedName("userId") val userId: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("type") val type: String,
    @SerializedName("intervalDays") val intervalDays: Int,
    @SerializedName("streakDays") val streakDays: Int,
    @SerializedName("longestStreak") val longestStreak: Int,
    @SerializedName("lastCheckedAt") val lastCheckedAt: Long?,
    @SerializedName("createdAt") val createdAt: Long,
    @SerializedName("isActive") val isActive: Boolean,
    @SerializedName("totalCompletions") val totalCompletions: Int,
    @SerializedName("milestonesAchieved") val milestonesAchieved: List<Int>,
    // Sync metadata
    @SerializedName("server_updated_at") val serverUpdatedAt: Long? = null,
    @SerializedName("updated_at") val updatedAt: Long? = null
)

/**
 * Extension function to map HabitDto to domain Habit model.
 */
fun HabitDto.toHabit(): Habit {
    return Habit(
        id = id,
        userId = userId,
        name = name,
        description = description,
        type = HabitType.valueOf(type),
        intervalDays = intervalDays,
        streakDays = streakDays,
        longestStreak = longestStreak,
        lastCheckedAt = lastCheckedAt,
        createdAt = createdAt,
        isActive = isActive,
        totalCompletions = totalCompletions,
        milestonesAchieved = milestonesAchieved
    )
}

/**
 * Request DTO for creating a habit.
 */
data class CreateHabitRequest(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("type") val type: String,
    @SerializedName("intervalDays") val intervalDays: Int
)

/**
 * Request DTO for updating a habit.
 */
data class UpdateHabitRequest(
    @SerializedName("name") val name: String?,
    @SerializedName("description") val description: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("intervalDays") val intervalDays: Int?,
    @SerializedName("isActive") val isActive: Boolean?,
    @SerializedName("client_updated_at") val clientUpdatedAt: Long? = null
)

/**
 * Response DTO for ticking a habit.
 */
data class TickHabitResponse(
    @SerializedName("habit") val habit: HabitDto,
    @SerializedName("newStreakDays") val newStreakDays: Int,
    @SerializedName("milestoneAchieved") val milestoneAchieved: MilestoneDto?
)

/**
 * DTO for milestone achievement information.
 */
data class MilestoneDto(
    @SerializedName("milestoneDays") val milestoneDays: Int,
    @SerializedName("message") val message: String
)

/**
 * Extension function to map MilestoneDto to domain MilestoneAchievement.
 */
fun MilestoneDto.toMilestoneAchievement(): MilestoneAchievement {
    return MilestoneAchievement(
        milestoneDays = milestoneDays,
        message = message
    )
}

/**
 * Response DTO for streak break checking.
 */
data class StreakBreakResponse(
    @SerializedName("brokenHabits") val brokenHabits: List<HabitDto>
)


