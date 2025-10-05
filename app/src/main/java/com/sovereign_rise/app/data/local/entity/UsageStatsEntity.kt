package com.sovereign_rise.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usage_stats",
    indices = [Index(value = ["timestamp"])]
)
data class UsageStatsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,
    val totalScreenTimeMinutes: Int,
    val unlockCount: Int,
    val distractingAppTimeMinutes: Int,
    val productiveAppTimeMinutes: Int,
    val peakUsageHour: Int
)

