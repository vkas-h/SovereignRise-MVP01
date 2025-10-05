package com.sovereign_rise.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sovereign_rise.app.domain.model.OnboardingScreen
import com.sovereign_rise.app.domain.model.TutorialDay
import com.sovereign_rise.app.domain.model.TutorialState
import com.sovereign_rise.app.domain.model.TutorialTask
import com.sovereign_rise.app.domain.repository.OnboardingRepository
import com.sovereign_rise.app.util.ErrorLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for onboarding and tutorial.
 * Manages onboarding flow and progressive tutorial system.
 */
class OnboardingViewModel(
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<OnboardingUiState>(
        OnboardingUiState.OnboardingInProgress(OnboardingScreen.WELCOME)
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    // Current onboarding page
    private val _currentPage = MutableStateFlow(0)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    // Tutorial tasks for current day
    private val _tutorialTasks = MutableStateFlow<List<TutorialTask>>(emptyList())
    val tutorialTasks: StateFlow<List<TutorialTask>> = _tutorialTasks.asStateFlow()

    init {
        checkOnboardingStatus()
    }

    private fun checkOnboardingStatus() {
        viewModelScope.launch {
            try {
                val isCompleted = onboardingRepository.isOnboardingCompleted()
                if (isCompleted) {
                    loadTutorialState()
                } else {
                    _uiState.value = OnboardingUiState.OnboardingInProgress(OnboardingScreen.WELCOME)
                }
            } catch (e: Exception) {
                ErrorLogger.logException(e, "OnboardingViewModel.checkOnboardingStatus")
            }
        }
    }

    private suspend fun loadTutorialState() {
        try {
            val tutorialState = onboardingRepository.getTutorialState()
            if (tutorialState.isCompleted) {
                _uiState.value = OnboardingUiState.Completed
            } else {
                _uiState.value = OnboardingUiState.TutorialActive(tutorialState)
                _tutorialTasks.value = onboardingRepository.getTutorialTasks(tutorialState.currentDay)
            }
        } catch (e: Exception) {
            ErrorLogger.logException(e, "OnboardingViewModel.loadTutorialState")
        }
    }

    fun nextScreen() {
        val currentScreen = (_uiState.value as? OnboardingUiState.OnboardingInProgress)?.currentScreen
        val nextScreen = when (currentScreen) {
            OnboardingScreen.WELCOME -> OnboardingScreen.PROGRESSION
            OnboardingScreen.PROGRESSION -> OnboardingScreen.TRUTH
            OnboardingScreen.TRUTH -> OnboardingScreen.SOCIAL
            OnboardingScreen.SOCIAL -> OnboardingScreen.BEGIN
            OnboardingScreen.BEGIN -> {
                completeOnboarding()
                return
            }
            null -> return
        }
        
        _uiState.value = OnboardingUiState.OnboardingInProgress(nextScreen)
        _currentPage.value = nextScreen.ordinal
    }

    fun previousScreen() {
        val currentScreen = (_uiState.value as? OnboardingUiState.OnboardingInProgress)?.currentScreen
        val prevScreen = when (currentScreen) {
            OnboardingScreen.PROGRESSION -> OnboardingScreen.WELCOME
            OnboardingScreen.TRUTH -> OnboardingScreen.PROGRESSION
            OnboardingScreen.SOCIAL -> OnboardingScreen.TRUTH
            OnboardingScreen.BEGIN -> OnboardingScreen.SOCIAL
            else -> return
        }
        
        _uiState.value = OnboardingUiState.OnboardingInProgress(prevScreen)
        _currentPage.value = prevScreen.ordinal
    }

    fun skipOnboarding() {
        completeOnboarding()
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            try {
                onboardingRepository.markOnboardingCompleted()
                loadTutorialState()
            } catch (e: Exception) {
                ErrorLogger.logException(e, "OnboardingViewModel.completeOnboarding")
            }
        }
    }

    fun getTutorialTasks() {
        viewModelScope.launch {
            try {
                val currentDay = onboardingRepository.getCurrentTutorialDay()
                _tutorialTasks.value = onboardingRepository.getTutorialTasks(currentDay)
            } catch (e: Exception) {
                ErrorLogger.logException(e, "OnboardingViewModel.getTutorialTasks")
            }
        }
    }

    fun completeTutorialTask(taskId: String) {
        viewModelScope.launch {
            try {
                onboardingRepository.completeTutorialTask(taskId)
                loadTutorialState()
                getTutorialTasks()
            } catch (e: Exception) {
                ErrorLogger.logException(e, "OnboardingViewModel.completeTutorialTask")
            }
        }
    }

    fun checkTutorialProgress() {
        viewModelScope.launch {
            try {
                val tutorialState = onboardingRepository.getTutorialState()
                val tasks = onboardingRepository.getTutorialTasks(tutorialState.currentDay)
                
                if (tasks.all { it.isCompleted }) {
                    onboardingRepository.advanceTutorialDay()
                    loadTutorialState()
                }
            } catch (e: Exception) {
                ErrorLogger.logException(e, "OnboardingViewModel.checkTutorialProgress")
            }
        }
    }

    fun canCreateTask(): Boolean {
        val state = (_uiState.value as? OnboardingUiState.TutorialActive)?.tutorialState
        return state != null && state.maxTasksAllowed > 0
    }

    fun getMaxTasks(): Int {
        val state = (_uiState.value as? OnboardingUiState.TutorialActive)?.tutorialState
        return state?.maxTasksAllowed ?: 0
    }

    fun skipTutorial() {
        viewModelScope.launch {
            try {
                onboardingRepository.advanceTutorialDay()
                onboardingRepository.advanceTutorialDay()
                onboardingRepository.advanceTutorialDay()
                onboardingRepository.advanceTutorialDay()
                _uiState.value = OnboardingUiState.Completed
            } catch (e: Exception) {
                ErrorLogger.logException(e, "OnboardingViewModel.skipTutorial")
            }
        }
    }
}

/**
 * UI State for onboarding
 */
sealed class OnboardingUiState {
    data class OnboardingInProgress(val currentScreen: OnboardingScreen) : OnboardingUiState()
    data class TutorialActive(val tutorialState: TutorialState) : OnboardingUiState()
    data object Completed : OnboardingUiState()
}

