package com.sovereign_rise.app.domain.repository

import com.sovereign_rise.app.domain.model.CompletionPattern
import com.sovereign_rise.app.domain.model.SmartReminder
import com.sovereign_rise.app.domain.model.SmartReminderSuggestion

/**
 * Repository interface for smart reminder suggestions
 */
interface SmartReminderRepository {
    
    // Suggestion generation
    suspend fun generateSmartReminder(taskId: String): SmartReminder?
    suspend fun getCompletionPatterns(userId: String): List<CompletionPattern>
    suspend fun analyzeCompletionTimes(taskType: String?): CompletionPattern?
    
    // Pattern storage
    suspend fun saveCompletionPattern(pattern: CompletionPattern)
    suspend fun updateCompletionPattern(taskId: String, completionTime: Long)
    
    // Suggestion retrieval
    suspend fun getSuggestionsForTask(taskId: String): List<SmartReminderSuggestion>
    suspend fun getOptimalReminderTime(taskId: String): Long?
    
    // Backend sync
    suspend fun syncPatternsToBackend(patterns: List<CompletionPattern>)
    suspend fun fetchSuggestionsFromBackend(taskId: String): List<SmartReminder>
}

