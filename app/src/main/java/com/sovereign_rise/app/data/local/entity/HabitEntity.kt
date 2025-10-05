package com.sovereign_rise.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sovereign_rise.app.domain.model.Habit
import com.sovereign_rise.app.domain.model.HabitType
import com.sovereign_rise.app.domain.model.SyncStatus

/**
 * Room entity for Habit with sync metadata.
 */
@Entity(
    tableName = "habits",
    indices = [
        Index("userId"),
        Index("isActive"),
        Index("streakDays")
    ]
)
data class HabitEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val description: String?,
    val type: String,
    val intervalDays: Int,
    val streakDays: Int,
    val longestStreak: Int,
    val lastCheckedAt: Long?,
    val createdAt: Long,
    val isActive: Boolean,
    val totalCompletions: Int,
    val milestonesAchievedJson: String,
    // Sync metadata
    val syncStatus: String,
    val lastSyncedAt: Long?,
    val serverUpdatedAt: Long?,
    val localUpdatedAt: Long
)

private val gson = Gson()

/**
 * Extension function to convert HabitEntity to Habit domain model.
 */
fun HabitEntity.toHabit(): Habit {
    val milestonesType = object : TypeToken<List<Int>>() {}.type
    val milestones: List<Int> = try {
        gson.fromJson(milestonesAchievedJson, milestonesType) ?: emptyList()
    } catch (e: Exception) {
        emptyList()
    }
    
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
        milestonesAchieved = milestones
    )
}

/**
 * Extension function to convert Habit domain model to HabitEntity.
 */
fun Habit.toHabitEntity(
    syncStatus: SyncStatus,
    lastSyncedAt: Long?,
    serverUpdatedAt: Long?,
    localUpdatedAt: Long
): HabitEntity {
    val milestonesJson = gson.toJson(milestonesAchieved)
    
    return HabitEntity(
        id = id,
        userId = userId,
        name = name,
        description = description,
        type = type.name,
        intervalDays = intervalDays,
        streakDays = streakDays,
        longestStreak = longestStreak,
        lastCheckedAt = lastCheckedAt,
        createdAt = createdAt,
        isActive = isActive,
        totalCompletions = totalCompletions,
        milestonesAchievedJson = milestonesJson,
        syncStatus = syncStatus.name,
        lastSyncedAt = lastSyncedAt,
        serverUpdatedAt = serverUpdatedAt,
        localUpdatedAt = localUpdatedAt
    )
}

