package com.sovereign_rise.app.domain.repository

import com.sovereign_rise.app.domain.model.BurnoutAlert
import com.sovereign_rise.app.domain.model.BurnoutIntervention
import com.sovereign_rise.app.domain.model.BurnoutLevel
import com.sovereign_rise.app.domain.model.BurnoutMetrics
import com.sovereign_rise.app.domain.model.RecoveryMode

/**
 * Repository interface for burnout detection and intervention
 */
interface BurnoutRepository {
    
    // Metrics collection
    suspend fun collectBurnoutMetrics(userId: String, date: Long): BurnoutMetrics
    suspend fun calculateBurnoutScore(metrics: BurnoutMetrics): Float
    suspend fun determineBurnoutLevel(score: Float): BurnoutLevel
    
    // Detection
    suspend fun detectBurnout(userId: String): BurnoutAlert?
    suspend fun getBurnoutTrend(userId: String, days: Int): List<BurnoutMetrics>
    suspend fun shouldTriggerIntervention(metrics: BurnoutMetrics): Boolean
    
    // Recovery mode
    suspend fun activateRecoveryMode(userId: String): RecoveryMode
    suspend fun getRecoveryMode(userId: String): RecoveryMode?
    suspend fun isRecoveryModeActive(userId: String): Boolean
    suspend fun deactivateRecoveryMode(userId: String)
    
    // Interventions
    suspend fun getRecommendedInterventions(metrics: BurnoutMetrics): List<BurnoutIntervention>
    suspend fun applyIntervention(userId: String, intervention: BurnoutIntervention)
    suspend fun suggestTaskReduction(userId: String): Int
    
    // Backend sync
    suspend fun syncMetricsToBackend(metrics: BurnoutMetrics)
    suspend fun reportBurnoutAlert(alert: BurnoutAlert)
    suspend fun syncRecoveryMode(recoveryMode: RecoveryMode)
}

