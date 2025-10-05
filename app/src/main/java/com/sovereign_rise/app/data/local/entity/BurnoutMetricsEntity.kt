package com.sovereign_rise.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "burnout_metrics",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["date"])
    ]
)
data class BurnoutMetricsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val date: Long,
    val completionRate: Float,
    val snoozeCount: Int,
    val missedTaskCount: Int,
    val lateNightActivityMinutes: Int,
    val streakBreaks: Int,
    val burnoutScore: Float
)

