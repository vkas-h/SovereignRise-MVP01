package com.sovereign_rise.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "completion_patterns",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["taskType"])
    ]
)
data class CompletionPatternEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val taskType: String?,
    val completionTimesJson: String,
    val averageCompletionHour: Int,
    val averageCompletionMinute: Int,
    val standardDeviation: Float,
    val sampleSize: Int
)

