package com.sovereign_rise.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sovereign_rise.app.data.sync.SyncManager
import com.sovereign_rise.app.domain.model.Affirmation
import com.sovereign_rise.app.domain.model.AffirmationContext
import com.sovereign_rise.app.domain.model.Habit
import com.sovereign_rise.app.domain.model.HabitType
import com.sovereign_rise.app.domain.repository.TickHabitResult
import com.sovereign_rise.app.domain.repository.StreakBreakResult
import com.sovereign_rise.app.domain.usecase.ai.GenerateAffirmationUseCase
import com.sovereign_rise.app.domain.usecase.habit.*
import com.sovereign_rise.app.presentation.components.SyncState
import com.sovereign_rise.app.util.ConnectivityObserver
import com.sovereign_rise.app.util.ConnectivityStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for habits screen.
 * Manages habit list UI state and coordinates habit-related use cases.
 */
class HabitViewModel(
    private val getHabitsUseCase: GetHabitsUseCase,
    private val createHabitUseCase: CreateHabitUseCase,
    private val updateHabitUseCase: UpdateHabitUseCase,
    private val deleteHabitUseCase: DeleteHabitUseCase,
    private val tickHabitUseCase: TickHabitUseCase,
    private val checkStreakBreaksUseCase: CheckStreakBreaksUseCase,
    private val getHabitByIdUseCase: GetHabitByIdUseCase,
    private val generateAffirmationUseCase: GenerateAffirmationUseCase,
    private val syncManager: SyncManager,
    private val connectivityObserver: ConnectivityObserver,
    private val habitRepository: com.sovereign_rise.app.data.repository.HabitRepositoryImpl
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow<HabitUiState>(HabitUiState.Loading)
    val uiState: StateFlow<HabitUiState> = _uiState.asStateFlow()
    
    // Selected habit for edit screen
    private val _selectedHabit = MutableStateFlow<Habit?>(null)
    val selectedHabit: StateFlow<Habit?> = _selectedHabit.asStateFlow()
    
    // Tick result for display
    private val _tickResult = MutableStateFlow<TickHabitResult?>(null)
    val tickResult: StateFlow<TickHabitResult?> = _tickResult.asStateFlow()
    
    // Streak break result
    private val _streakBreakResult = MutableStateFlow<StreakBreakResult?>(null)
    val streakBreakResult: StateFlow<StreakBreakResult?> = _streakBreakResult.asStateFlow()
    
    // Affirmation for motivational messages
    private val _affirmation = MutableStateFlow<Affirmation?>(null)
    val affirmation: StateFlow<Affirmation?> = _affirmation.asStateFlow()
    
    // Sync state for offline mode
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Synced)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    init {
        // Monitor sync status
        monitorSyncStatus()
        // Check for streak breaks before loading habits
        viewModelScope.launch {
            try {
                checkStreakBreaks()
            } catch (e: Exception) {
                // Log error but continue to load habits
                e.printStackTrace()
            }
            // Load habits after streak check
            loadHabits()
        }
    }
    
    /**
     * Loads habits from the repository via use case.
     */
    fun loadHabits() {
        viewModelScope.launch {
            _uiState.value = HabitUiState.Loading
            
            try {
                // Fetch habits with sync status
                val habitsWithStatus = habitRepository.getHabitsWithSyncStatus()
                val habitViewModels = habitsWithStatus.map { (habit, syncStatus) ->
                    HabitWithSyncStatus(habit, syncStatus)
                }
                
                _uiState.value = if (habitViewModels.isEmpty()) {
                    HabitUiState.Empty
                } else {
                    HabitUiState.Success(habitViewModels)
                }
            } catch (e: Exception) {
                _uiState.value = HabitUiState.Error(e.message ?: "Failed to load habits")
            }
        }
    }
    
    /**
     * Checks for streak breaks and applies penalties/protections.
     */
    fun checkStreakBreaks() {
        viewModelScope.launch {
            try {
                val result = checkStreakBreaksUseCase(Unit)
                result.fold(
                    onSuccess = { streakResult ->
                        // Only update if there are actually broken or protected habits
                        if (streakResult.brokenHabits.isNotEmpty()) {
                            _streakBreakResult.value = streakResult
                        }
                    },
                    onFailure = { error ->
                        // Log error but don't show to user
                        error.printStackTrace()
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Creates a new habit.
     */
    fun createHabit(
        name: String,
        description: String?,
        type: HabitType,
        intervalDays: Int = 1
    ) {
        viewModelScope.launch {
            _uiState.value = HabitUiState.Loading
            
            val result = createHabitUseCase(
                CreateHabitUseCase.Params(
                    name = name,
                    description = description,
                    type = type,
                    intervalDays = intervalDays
                )
            )
            
            result.fold(
                onSuccess = {
                    _uiState.value = HabitUiState.Success(emptyList())
                    // Don't load habits here - will load when returning to habit list
                },
                onFailure = { error ->
                    _uiState.value = HabitUiState.Error(error.message ?: "Failed to create habit")
                }
            )
        }
    }
    
    /**
     * Updates an existing habit.
     */
    fun updateHabit(
        habitId: String,
        name: String? = null,
        description: String? = null,
        type: HabitType? = null,
        intervalDays: Int? = null,
        isActive: Boolean? = null
    ) {
        viewModelScope.launch {
            _uiState.value = HabitUiState.Loading
            
            val result = updateHabitUseCase(
                UpdateHabitUseCase.Params(
                    habitId = habitId,
                    name = name,
                    description = description,
                    type = type,
                    intervalDays = intervalDays,
                    isActive = isActive
                )
            )
            
            result.fold(
                onSuccess = {
                    _uiState.value = HabitUiState.Success(emptyList())
                    // Don't load habits here - will load when returning to habit list
                },
                onFailure = { error ->
                    _uiState.value = HabitUiState.Error(error.message ?: "Failed to update habit")
                }
            )
        }
    }
    
    /**
     * Deletes a habit.
     */
    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            val result = deleteHabitUseCase(DeleteHabitUseCase.Params(habitId))
            
            result.fold(
                onSuccess = {
                    loadHabits()
                },
                onFailure = { error ->
                    _uiState.value = HabitUiState.Error(error.message ?: "Failed to delete habit")
                }
            )
        }
    }
    
    /**
     * Marks a habit as completed for today (tick).
     */
    fun tickHabit(habitId: String) {
        viewModelScope.launch {
            val result = tickHabitUseCase(TickHabitUseCase.Params(habitId))
            
            result.fold(
                onSuccess = { tickResult ->
                    // Check for level-up and set it first (higher priority)
                    _tickResult.value = tickResult
                    loadHabits()
                    
                    // Generate affirmation after habit tick
                    val context = if (tickResult.milestoneAchieved != null) {
                        AffirmationContext.STREAK_MILESTONE
                    } else {
                        AffirmationContext.HABIT_CHECKED
                    }
                    generateAffirmation(
                        context = context,
                        variables = mapOf(
                            "streak_days" to tickResult.newStreakDays.toString()
                        )
                    )
                },
                onFailure = { error ->
                    _uiState.value = HabitUiState.Error(error.message ?: "Failed to tick habit")
                }
            )
        }
    }
    
    /**
     * Generates an affirmation message.
     */
    private fun generateAffirmation(context: AffirmationContext, variables: Map<String, String>) {
        viewModelScope.launch {
            try {
                val params = GenerateAffirmationUseCase.Params(
                    userId = "current_user", // TODO: Replace with actual user ID
                    context = context,
                    variables = variables
                )
                val result = generateAffirmationUseCase(params)
                result.fold(
                    onSuccess = { affirmation ->
                        _affirmation.value = affirmation
                    },
                    onFailure = { 
                        // Silently fail - affirmations are optional
                    }
                )
            } catch (e: Exception) {
                // Silently fail - affirmations are optional
            }
        }
    }
    
    /**
     * Selects a habit for editing.
     */
    fun selectHabit(habit: Habit) {
        _selectedHabit.value = habit
    }
    
    /**
     * Selects a habit by ID for editing (loads from repository).
     * Use this when opening edit screen via deep link or config change.
     */
    suspend fun selectHabit(habitId: String) {
        viewModelScope.launch {
            try {
                val result = getHabitByIdUseCase(GetHabitByIdUseCase.Params(habitId))
                result.fold(
                    onSuccess = { habit ->
                        _selectedHabit.value = habit
                    },
                    onFailure = { error ->
                        _uiState.value = HabitUiState.Error(error.message ?: "Failed to load habit")
                    }
                )
            } catch (e: Exception) {
                _uiState.value = HabitUiState.Error(e.message ?: "Failed to load habit")
            }
        }
    }
    
    /**
     * Clears the selected habit.
     */
    fun clearSelectedHabit() {
        _selectedHabit.value = null
    }
    
    /**
     * Clears the tick result.
     */
    fun clearTickResult() {
        _tickResult.value = null
    }
    
    /**
     * Clears the level-up result.
     */
    fun clearLevelUpResult() {
    }
    
    /**
     * Clears the streak break result.
     */
    fun clearStreakBreakResult() {
        _streakBreakResult.value = null
    }
    
    /**
     * Clears the affirmation.
     */
    fun clearAffirmation() {
        _affirmation.value = null
    }
    
    /**
     * Resets the UI state and reloads habits.
     */
    fun resetState() {
        loadHabits()
    }
    
    /**
     * Refreshes habits by checking streak breaks and reloading.
     */
    fun refresh() {
        viewModelScope.launch {
            checkStreakBreaks()
            loadHabits()
        }
    }
    
    /**
     * Monitors sync status and connectivity.
     * Automatically triggers sync when connectivity is available.
     */
    private fun monitorSyncStatus() {
        viewModelScope.launch {
            // Monitor connectivity
            connectivityObserver.observe().collect { status ->
                when (status) {
                    ConnectivityStatus.AVAILABLE -> {
                        val pendingCount = syncManager.getPendingSyncCount()
                        if (pendingCount > 0) {
                            // Automatically trigger sync when connected
                            _syncState.value = SyncState.Syncing(0, pendingCount)
                            try {
                                val result = syncManager.syncPendingActions()
                                _syncState.value = if (result.success) {
                                    SyncState.Synced
                                } else {
                                    SyncState.Failed(result.failedCount, result.errors)
                                }
                                // Reload habits after sync
                                if (result.success) {
                                    loadHabits()
                                }
                            } catch (e: Exception) {
                                _syncState.value = SyncState.Failed(pendingCount, listOf(e.message ?: "Unknown error"))
                            }
                        } else {
                            _syncState.value = SyncState.Synced
                        }
                    }
                    ConnectivityStatus.UNAVAILABLE -> {
                        val pendingCount = syncManager.getPendingSyncCount()
                        _syncState.value = if (pendingCount > 0) {
                            SyncState.Offline
                        } else {
                            SyncState.Synced
                        }
                    }
                    else -> {}
                }
            }
        }
    }
    
    /**
     * Manually triggers sync.
     */
    fun syncNow() {
        viewModelScope.launch {
            val pendingCount = syncManager.getPendingSyncCount()
            if (pendingCount > 0) {
                _syncState.value = SyncState.Syncing(0, pendingCount)
                try {
                    val result = syncManager.syncPendingActions()
                    _syncState.value = if (result.success) {
                        SyncState.Synced
                    } else {
                        SyncState.Failed(result.failedCount, result.errors)
                    }
                } catch (e: Exception) {
                    _syncState.value = SyncState.Failed(pendingCount, listOf(e.message ?: "Unknown error"))
                }
            }
        }
    }
    
    /**
     * Retries failed sync actions.
     */
    fun retryFailedSync() {
        syncNow()
    }
}

/**
 * Data class combining Habit with its sync status for UI display.
 */
data class HabitWithSyncStatus(
    val habit: Habit,
    val syncStatus: com.sovereign_rise.app.domain.model.SyncStatus
)

/**
 * Sealed class representing different UI states for the habits screen.
 */
sealed class HabitUiState {
    object Loading : HabitUiState()
    object Empty : HabitUiState()
    data class Success(val habits: List<HabitWithSyncStatus>) : HabitUiState()
    data class Error(val message: String) : HabitUiState()
}

