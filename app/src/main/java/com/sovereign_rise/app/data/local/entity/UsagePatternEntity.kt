package com.sovereign_rise.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usage_patterns",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["date"])
    ]
)
data class UsagePatternEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val date: Long,
    val totalScreenTimeMinutes: Int,
    val unlockCount: Int,
    val distractingAppTimeMinutes: Int,
    val productiveAppTimeMinutes: Int,
    val lateNightActivityMinutes: Int,
    val peakUsageHour: Int
)

