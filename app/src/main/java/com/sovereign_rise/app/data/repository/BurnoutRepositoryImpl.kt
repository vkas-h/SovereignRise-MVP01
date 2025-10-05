package com.sovereign_rise.app.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.sovereign_rise.app.data.local.dao.BurnoutMetricsDao
import com.sovereign_rise.app.data.local.entity.BurnoutMetricsEntity
import com.sovereign_rise.app.domain.model.*
import com.sovereign_rise.app.domain.repository.BurnoutRepository
import com.sovereign_rise.app.domain.repository.HabitRepository
import com.sovereign_rise.app.domain.repository.TaskRepository
import com.sovereign_rise.app.domain.repository.UsageStatsRepository
import com.sovereign_rise.app.util.Constants
import kotlinx.coroutines.flow.first
import java.util.Calendar

class BurnoutRepositoryImpl(
    private val taskRepository: TaskRepository,
    private val habitRepository: HabitRepository,
    private val usageStatsRepository: UsageStatsRepository,
    private val burnoutMetricsDao: BurnoutMetricsDao,
    private val dataStore: DataStore<Preferences>
) : BurnoutRepository {
    
    companion object {
        private val RECOVERY_MODE_ACTIVE_KEY = booleanPreferencesKey(Constants.PREF_RECOVERY_MODE_ACTIVE)
        private val RECOVERY_MODE_START_TIME_KEY = longPreferencesKey(Constants.PREF_RECOVERY_MODE_START_TIME)
    }
    
    override suspend fun collectBurnoutMetrics(userId: String, date: Long): BurnoutMetrics {
        val startOfDay = getDayStart(date)
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000)
        
        val tasks = taskRepository.getTasks()
        val completedTasks = tasks.count { it.completedAt != null && it.completedAt!! in startOfDay..endOfDay }
        val totalTasks = tasks.size
        val completionRate = if (totalTasks > 0) completedTasks.toFloat() / totalTasks else 1.0f
        
        val missedTasks = tasks.count { task ->
            task.reminderTime?.let { reminderTime ->
                reminderTime < System.currentTimeMillis() && task.completedAt == null
            } ?: false
        }
        
        // Temporarily disabled - usage stats integration pending
        val lateNightActivity = 0 // TODO: Implement with new UsageStatsManager
        
        val habits = habitRepository.getHabits()
        val streakBreaks = habits.count { it.streakDays == 0 }
        
        val metrics = BurnoutMetrics(
            userId = userId,
            date = date,
            completionRate = completionRate,
            snoozeCount = 0,
            missedTaskCount = missedTasks,
            lateNightActivityMinutes = lateNightActivity,
            averageTaskCompletionTime = null,
            streakBreaks = streakBreaks,
            burnoutScore = 0.0f // Will be calculated next
        )
        
        val burnoutScore = calculateBurnoutScore(metrics)
        
        return metrics.copy(burnoutScore = burnoutScore)
    }
    
    override suspend fun calculateBurnoutScore(metrics: BurnoutMetrics): Float {
        var score = 0.0f
        
        // Low completion rate (<50%)
        if (metrics.completionRate < Constants.BURNOUT_COMPLETION_RATE_THRESHOLD) {
            score += 0.3f
        }
        
        // High snooze count
        if (metrics.snoozeCount > Constants.BURNOUT_SNOOZE_THRESHOLD) {
            score += 0.2f
        }
        
        // Late-night activity (>60 min after 11 PM)
        if (metrics.lateNightActivityMinutes > 60) {
            score += 0.2f
        }
        
        // Streak breaks
        if (metrics.streakBreaks > 0) {
            score += 0.15f
        }
        
        // Missed tasks
        if (metrics.missedTaskCount > 3) {
            score += 0.15f
        }
        
        return score.coerceIn(0.0f, 1.0f)
    }
    
    override suspend fun determineBurnoutLevel(score: Float): BurnoutLevel {
        return when {
            score < 0.3f -> BurnoutLevel.HEALTHY
            score < 0.5f -> BurnoutLevel.MILD
            score < 0.7f -> BurnoutLevel.MODERATE
            else -> BurnoutLevel.SEVERE
        }
    }
    
    override suspend fun detectBurnout(userId: String): BurnoutAlert? {
        val metrics = collectBurnoutMetrics(userId, System.currentTimeMillis())
        val level = determineBurnoutLevel(metrics.burnoutScore)
        
        if (level == BurnoutLevel.HEALTHY || level == BurnoutLevel.MILD) {
            return null
        }
        
        val recommendations = getRecommendedInterventions(metrics).map { it.toString() }
        val isRecoveryActive = isRecoveryModeActive(userId)
        
        return BurnoutAlert(
            userId = userId,
            level = level,
            detectedAt = System.currentTimeMillis(),
            metrics = metrics,
            recommendations = recommendations,
            isRecoveryModeActive = isRecoveryActive
        )
    }
    
    override suspend fun getBurnoutTrend(userId: String, days: Int): List<BurnoutMetrics> {
        val startDate = getDayStart(System.currentTimeMillis() - (days * 24 * 60 * 60 * 1000))
        val entities = burnoutMetricsDao.getBurnoutTrend(userId, startDate, days)
        
        return entities.map { entity ->
            BurnoutMetrics(
                userId = entity.userId,
                date = entity.date,
                completionRate = entity.completionRate,
                snoozeCount = entity.snoozeCount,
                missedTaskCount = entity.missedTaskCount,
                lateNightActivityMinutes = entity.lateNightActivityMinutes,
                averageTaskCompletionTime = null,
                streakBreaks = entity.streakBreaks,
                burnoutScore = entity.burnoutScore
            )
        }
    }
    
    override suspend fun shouldTriggerIntervention(metrics: BurnoutMetrics): Boolean {
        val level = determineBurnoutLevel(metrics.burnoutScore)
        return level == BurnoutLevel.MODERATE || level == BurnoutLevel.SEVERE
    }
    
    override suspend fun activateRecoveryMode(userId: String): RecoveryMode {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + (Constants.BURNOUT_RECOVERY_MODE_DAYS * 24 * 60 * 60 * 1000)
        
        dataStore.edit { preferences ->
            preferences[RECOVERY_MODE_ACTIVE_KEY] = true
            preferences[RECOVERY_MODE_START_TIME_KEY] = startTime
        }
        
        val taskReduction = suggestTaskReduction(userId)
        
        return RecoveryMode(
            userId = userId,
            startedAt = startTime,
            endsAt = endTime,
            penaltyMultiplier = Constants.BURNOUT_RECOVERY_PENALTY_MULTIPLIER,
            taskReductionSuggested = taskReduction,
            message = "Recovery Mode activated. Take it easy for the next 3 days."
        )
    }
    
    override suspend fun getRecoveryMode(userId: String): RecoveryMode? {
        val preferences = dataStore.data.first()
        val isActive = preferences[RECOVERY_MODE_ACTIVE_KEY] ?: false
        
        if (!isActive) return null
        
        val startTime = preferences[RECOVERY_MODE_START_TIME_KEY] ?: return null
        val endTime = startTime + (Constants.BURNOUT_RECOVERY_MODE_DAYS * 24 * 60 * 60 * 1000)
        
        // Check if recovery mode has expired
        if (System.currentTimeMillis() > endTime) {
            deactivateRecoveryMode(userId)
            return null
        }
        
        val taskReduction = suggestTaskReduction(userId)
        
        return RecoveryMode(
            userId = userId,
            startedAt = startTime,
            endsAt = endTime,
            penaltyMultiplier = Constants.BURNOUT_RECOVERY_PENALTY_MULTIPLIER,
            taskReductionSuggested = taskReduction,
            message = "Recovery Mode active until ${formatDate(endTime)}"
        )
    }
    
    override suspend fun isRecoveryModeActive(userId: String): Boolean {
        val recoveryMode = getRecoveryMode(userId)
        return recoveryMode != null
    }
    
    override suspend fun deactivateRecoveryMode(userId: String) {
        dataStore.edit { preferences ->
            preferences[RECOVERY_MODE_ACTIVE_KEY] = false
            preferences.remove(RECOVERY_MODE_START_TIME_KEY)
        }
    }
    
    override suspend fun getRecommendedInterventions(metrics: BurnoutMetrics): List<BurnoutIntervention> {
        val level = determineBurnoutLevel(metrics.burnoutScore)
        
        return when (level) {
            BurnoutLevel.HEALTHY -> emptyList()
            BurnoutLevel.MILD -> listOf(BurnoutIntervention.ADJUST_REMINDERS)
            BurnoutLevel.MODERATE -> listOf(
                BurnoutIntervention.REDUCE_TASKS,
                BurnoutIntervention.ADJUST_REMINDERS
            )
            BurnoutLevel.SEVERE -> listOf(
                BurnoutIntervention.ACTIVATE_RECOVERY_MODE,
                BurnoutIntervention.REDUCE_TASKS,
                BurnoutIntervention.SUGGEST_BREAK
            )
        }
    }
    
    override suspend fun applyIntervention(userId: String, intervention: BurnoutIntervention) {
        when (intervention) {
            BurnoutIntervention.ACTIVATE_RECOVERY_MODE -> activateRecoveryMode(userId)
            BurnoutIntervention.REDUCE_TASKS -> {
                // TODO: Implement task reduction logic
            }
            BurnoutIntervention.SUGGEST_BREAK -> {
                // TODO: Show break suggestion dialog
            }
            BurnoutIntervention.ADJUST_REMINDERS -> {
                // TODO: Reduce reminder frequency
            }
            BurnoutIntervention.ENCOURAGE_HABITS -> {
                // TODO: Suggest focusing on habits over tasks
            }
        }
    }
    
    override suspend fun suggestTaskReduction(userId: String): Int {
        val currentTaskCount = taskRepository.getTasks().size
        val metrics = collectBurnoutMetrics(userId, System.currentTimeMillis())
        return (currentTaskCount * (1 - metrics.burnoutScore)).toInt()
    }
    
    override suspend fun syncMetricsToBackend(metrics: BurnoutMetrics) {
        // TODO: Implement backend sync when API is ready
    }
    
    override suspend fun reportBurnoutAlert(alert: BurnoutAlert) {
        // TODO: Implement backend reporting when API is ready
    }
    
    override suspend fun syncRecoveryMode(recoveryMode: RecoveryMode) {
        // TODO: Implement backend sync when API is ready
    }
    
    // Helper functions
    
    private fun getDayStart(timestamp: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
    
    private fun formatDate(timestamp: Long): String {
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        return "${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.DAY_OF_MONTH)}"
    }
}

