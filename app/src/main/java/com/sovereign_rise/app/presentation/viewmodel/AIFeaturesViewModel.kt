package com.sovereign_rise.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sovereign_rise.app.domain.model.Affirmation
import com.sovereign_rise.app.domain.model.AffirmationContext
import com.sovereign_rise.app.domain.model.AffirmationTone
import com.sovereign_rise.app.domain.model.BurnoutLevel
import com.sovereign_rise.app.domain.model.UsageStats
import com.sovereign_rise.app.domain.repository.AffirmationRepository
import com.sovereign_rise.app.domain.repository.BurnoutRepository
import com.sovereign_rise.app.domain.repository.SmartReminderRepository
import com.sovereign_rise.app.domain.repository.UsageStatsRepository
import com.sovereign_rise.app.domain.usecase.ai.CollectUsageStatsUseCase
import com.sovereign_rise.app.domain.usecase.ai.DetectBurnoutUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for AI features settings and status
 */
class AIFeaturesViewModel(
    private val usageStatsRepository: UsageStatsRepository,
    private val smartReminderRepository: SmartReminderRepository,
    private val burnoutRepository: BurnoutRepository,
    private val affirmationRepository: AffirmationRepository,
    private val collectUsageStatsUseCase: CollectUsageStatsUseCase,
    private val detectBurnoutUseCase: DetectBurnoutUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AIFeaturesUiState>(AIFeaturesUiState.Loading)
    val uiState: StateFlow<AIFeaturesUiState> = _uiState.asStateFlow()
    
    // Preview affirmation state
    private val _previewAffirmation = MutableStateFlow<Affirmation?>(null)
    val previewAffirmation: StateFlow<Affirmation?> = _previewAffirmation.asStateFlow()
    
    init {
        loadAIFeaturesData()
    }
    
    fun loadAIFeaturesData() {
        viewModelScope.launch {
            try {
                val data = AIFeaturesData(
                    usageStatsEnabled = true, // TODO: Load from preferences
                    hasUsageStatsPermission = usageStatsRepository.hasUsageStatsPermission(),
                    smartRemindersEnabled = true, // TODO: Load from preferences
                    burnoutDetectionEnabled = true, // TODO: Load from preferences
                    aiNudgesEnabled = true, // TODO: Load from preferences
                    affirmationTone = affirmationRepository.getUserPreferredTone("current_user"),
                    affirmationsShownToday = affirmationRepository.getAffirmationsShownToday("current_user"),
                    isRecoveryModeActive = burnoutRepository.isRecoveryModeActive("current_user"),
                    recoveryModeEndsAt = burnoutRepository.getRecoveryMode("current_user")?.endsAt,
                    currentBurnoutLevel = null, // Will be loaded on demand
                    usageStatsToday = null
                )
                _uiState.value = AIFeaturesUiState.Success(data)
            } catch (e: Exception) {
                _uiState.value = AIFeaturesUiState.Error(e.message ?: "Failed to load AI features")
            }
        }
    }
    
    fun requestUsageStatsPermission() {
        viewModelScope.launch {
            usageStatsRepository.requestUsageStatsPermission()
        }
    }
    
    fun toggleUsageStats(enabled: Boolean) {
        // TODO: Save to preferences
        loadAIFeaturesData()
    }
    
    fun toggleSmartReminders(enabled: Boolean) {
        // TODO: Save to preferences
        loadAIFeaturesData()
    }
    
    fun toggleBurnoutDetection(enabled: Boolean) {
        // TODO: Save to preferences
        loadAIFeaturesData()
    }
    
    fun toggleAINudges(enabled: Boolean) {
        // TODO: Save to preferences
        loadAIFeaturesData()
    }
    
    fun setAffirmationTone(tone: AffirmationTone) {
        viewModelScope.launch {
            affirmationRepository.setUserPreferredTone("current_user", tone)
            loadAIFeaturesData()
        }
    }
    
    fun activateRecoveryMode() {
        viewModelScope.launch {
            burnoutRepository.activateRecoveryMode("current_user")
            loadAIFeaturesData()
        }
    }
    
    fun deactivateRecoveryMode() {
        viewModelScope.launch {
            burnoutRepository.deactivateRecoveryMode("current_user")
            loadAIFeaturesData()
        }
    }
    
    fun checkBurnoutNow() {
        viewModelScope.launch {
            val result = detectBurnoutUseCase.invoke(DetectBurnoutUseCase.Params("current_user"))
            // TODO: Handle result
            loadAIFeaturesData()
        }
    }
    
    fun collectUsageStatsNow() {
        viewModelScope.launch {
            collectUsageStatsUseCase.invoke(Unit)
            loadAIFeaturesData()
        }
    }
    
    fun previewAffirmation(tone: AffirmationTone = AffirmationTone.MOTIVATIONAL) {
        viewModelScope.launch {
            // Generate a preview affirmation with sample context
            val affirmation = affirmationRepository.selectAffirmation(
                tone = tone,
                context = AffirmationContext.TASK_COMPLETED,
                variables = mapOf("xp" to "10")
            )
            _previewAffirmation.value = affirmation
        }
    }
    
    fun clearPreviewAffirmation() {
        _previewAffirmation.value = null
    }
}

sealed class AIFeaturesUiState {
    object Loading : AIFeaturesUiState()
    data class Success(val data: AIFeaturesData) : AIFeaturesUiState()
    data class Error(val message: String) : AIFeaturesUiState()
}

data class AIFeaturesData(
    val usageStatsEnabled: Boolean,
    val hasUsageStatsPermission: Boolean,
    val smartRemindersEnabled: Boolean,
    val burnoutDetectionEnabled: Boolean,
    val aiNudgesEnabled: Boolean,
    val affirmationTone: AffirmationTone,
    val affirmationsShownToday: Int,
    val isRecoveryModeActive: Boolean,
    val recoveryModeEndsAt: Long?,
    val currentBurnoutLevel: BurnoutLevel?,
    val usageStatsToday: UsageStats?
)

