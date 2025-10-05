package com.sovereign_rise.app.data.repository

import com.google.gson.Gson
import com.sovereign_rise.app.data.local.dao.CompletionPatternDao
import com.sovereign_rise.app.data.local.entity.CompletionPatternEntity
import com.sovereign_rise.app.domain.model.CompletionPattern
import com.sovereign_rise.app.domain.model.ReminderBasis
import com.sovereign_rise.app.domain.model.SmartReminder
import com.sovereign_rise.app.domain.model.SmartReminderSuggestion
import com.sovereign_rise.app.domain.model.Task
import com.sovereign_rise.app.domain.repository.SmartReminderRepository
import com.sovereign_rise.app.domain.repository.TaskRepository
import com.sovereign_rise.app.util.Constants
import kotlinx.coroutines.flow.first
import java.util.Calendar
import kotlin.math.pow
import kotlin.math.sqrt

class SmartReminderRepositoryImpl(
    private val taskRepository: TaskRepository,
    private val completionPatternDao: CompletionPatternDao
) : SmartReminderRepository {
    
    private val gson = Gson()
    
    override suspend fun generateSmartReminder(taskId: String): SmartReminder? {
        val patterns = getCompletionPatterns("current_user") // Will be replaced with actual user ID
        if (patterns.isEmpty()) return null
        
        val pattern = patterns.firstOrNull { it.sampleSize >= Constants.MIN_COMPLETION_HISTORY_FOR_SUGGESTIONS }
            ?: return null
        
        if (pattern.sampleSize < Constants.MIN_COMPLETION_HISTORY_FOR_SUGGESTIONS) {
            return null
        }
        
        val confidence = calculateConfidence(pattern.standardDeviation, pattern.sampleSize)
        if (confidence < Constants.SMART_REMINDER_CONFIDENCE_THRESHOLD) {
            return null
        }
        
        val suggestedTime = calculateSuggestedTime(pattern)
        val reason = "You usually complete tasks around ${formatTime(pattern.averageCompletionHour, pattern.averageCompletionMinute)}"
        
        return SmartReminder(
            taskId = taskId,
            suggestedTime = suggestedTime,
            confidence = confidence,
            reason = reason,
            basedOn = ReminderBasis.HISTORICAL_COMPLETION_TIME
        )
    }
    
    override suspend fun getCompletionPatterns(userId: String): List<CompletionPattern> {
        val entities = completionPatternDao.getCompletionPatterns(userId)
        return entities.map { it.toCompletionPattern() }
    }
    
    override suspend fun analyzeCompletionTimes(taskType: String?): CompletionPattern? {
        val tasks = taskRepository.getTasks()
        val completedTasks = tasks.filter { it.completedAt != null }
        
        if (completedTasks.size < Constants.MIN_COMPLETION_HISTORY_FOR_SUGGESTIONS) {
            return null
        }
        
        val completionTimes = completedTasks.mapNotNull { it.completedAt }
        val (avgHour, avgMinute) = calculateAverageTime(completionTimes)
        val stdDev = calculateStandardDeviation(completionTimes, avgHour, avgMinute)
        
        return CompletionPattern(
            userId = "current_user",
            taskType = taskType,
            completionTimes = completionTimes,
            averageCompletionHour = avgHour,
            averageCompletionMinute = avgMinute,
            standardDeviation = stdDev,
            sampleSize = completionTimes.size
        )
    }
    
    override suspend fun saveCompletionPattern(pattern: CompletionPattern) {
        val entity = CompletionPatternEntity(
            userId = pattern.userId,
            taskType = pattern.taskType,
            completionTimesJson = gson.toJson(pattern.completionTimes),
            averageCompletionHour = pattern.averageCompletionHour,
            averageCompletionMinute = pattern.averageCompletionMinute,
            standardDeviation = pattern.standardDeviation,
            sampleSize = pattern.sampleSize
        )
        completionPatternDao.insertCompletionPattern(entity)
    }
    
    override suspend fun updateCompletionPattern(taskId: String, completionTime: Long) {
        val userId = "current_user"
        val existingPattern = completionPatternDao.getCompletionPatternByType(userId, null)
        
        val updatedTimes = if (existingPattern != null) {
            val times = gson.fromJson(existingPattern.completionTimesJson, Array<Long>::class.java).toMutableList()
            times.add(completionTime)
            times
        } else {
            listOf(completionTime)
        }
        
        val (avgHour, avgMinute) = calculateAverageTime(updatedTimes)
        val stdDev = calculateStandardDeviation(updatedTimes, avgHour, avgMinute)
        
        val pattern = CompletionPattern(
            userId = userId,
            taskType = null,
            completionTimes = updatedTimes,
            averageCompletionHour = avgHour,
            averageCompletionMinute = avgMinute,
            standardDeviation = stdDev,
            sampleSize = updatedTimes.size
        )
        
        saveCompletionPattern(pattern)
    }
    
    override suspend fun getSuggestionsForTask(taskId: String): List<SmartReminderSuggestion> {
        val reminder = generateSmartReminder(taskId) ?: return emptyList()
        
        val alternatives = listOf(
            reminder.suggestedTime - (30 * 60 * 1000),
            reminder.suggestedTime + (30 * 60 * 1000)
        )
        
        return listOf(
            SmartReminderSuggestion(
                suggestedTime = reminder.suggestedTime,
                confidence = reminder.confidence,
                explanation = reminder.reason,
                alternativeTimes = alternatives
            )
        )
    }
    
    override suspend fun getOptimalReminderTime(taskId: String): Long? {
        return generateSmartReminder(taskId)?.suggestedTime
    }
    
    override suspend fun syncPatternsToBackend(patterns: List<CompletionPattern>) {
        // TODO: Implement backend sync when API is ready
    }
    
    override suspend fun fetchSuggestionsFromBackend(taskId: String): List<SmartReminder> {
        // TODO: Implement backend fetch when API is ready
        return emptyList()
    }
    
    // Helper functions
    
    private fun calculateAverageTime(timestamps: List<Long>): Pair<Int, Int> {
        var totalHours = 0
        var totalMinutes = 0
        
        timestamps.forEach { timestamp ->
            val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
            totalHours += calendar.get(Calendar.HOUR_OF_DAY)
            totalMinutes += calendar.get(Calendar.MINUTE)
        }
        
        val avgHour = totalHours / timestamps.size
        val avgMinute = totalMinutes / timestamps.size
        
        return Pair(avgHour, avgMinute)
    }
    
    private fun calculateStandardDeviation(timestamps: List<Long>, avgHour: Int, avgMinute: Int): Float {
        if (timestamps.size < 2) return 0f
        
        val avgTimeInMinutes = avgHour * 60 + avgMinute
        val sumSquaredDiffs = timestamps.sumOf { timestamp ->
            val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
            val timeInMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
            val diff = timeInMinutes - avgTimeInMinutes
            (diff * diff).toDouble()
        }
        
        return sqrt(sumSquaredDiffs / timestamps.size).toFloat()
    }
    
    private fun calculateConfidence(standardDeviation: Float, sampleSize: Int): Float {
        // Lower standard deviation = higher confidence
        // More samples = higher confidence
        val consistencyFactor = 1.0f / (1.0f + (standardDeviation / 60.0f))
        val sampleFactor = minOf(1.0f, sampleSize / 10.0f)
        return (consistencyFactor + sampleFactor) / 2.0f
    }
    
    private fun calculateSuggestedTime(pattern: CompletionPattern): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, pattern.averageCompletionHour)
            set(Calendar.MINUTE, pattern.averageCompletionMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            
            // If the time has passed today, suggest for tomorrow
            if (timeInMillis < System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }
        return calendar.timeInMillis
    }
    
    private fun formatTime(hour: Int, minute: Int): String {
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        return String.format("%d:%02d %s", displayHour, minute, amPm)
    }
    
    private fun CompletionPatternEntity.toCompletionPattern(): CompletionPattern {
        val times = gson.fromJson(completionTimesJson, Array<Long>::class.java).toList()
        return CompletionPattern(
            userId = userId,
            taskType = taskType,
            completionTimes = times,
            averageCompletionHour = averageCompletionHour,
            averageCompletionMinute = averageCompletionMinute,
            standardDeviation = standardDeviation,
            sampleSize = sampleSize
        )
    }
}

