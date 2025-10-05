package com.sovereign_rise.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sovereign_rise.app.domain.model.User
import com.sovereign_rise.app.domain.model.TaskStatus
import com.sovereign_rise.app.domain.model.DailyTaskSummary
import com.sovereign_rise.app.domain.usecase.auth.GetCurrentUserUseCase
import com.sovereign_rise.app.domain.usecase.task.GetTasksUseCase
import com.sovereign_rise.app.domain.usecase.task.GetYesterdayTaskSummaryUseCase
import com.sovereign_rise.app.domain.usecase.habit.GetHabitsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Home screen.
 * Manages dashboard state showing user stats and quick access.
 */
class HomeViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getTasksUseCase: GetTasksUseCase,
    private val getHabitsUseCase: GetHabitsUseCase,
    private val getYesterdayTaskSummaryUseCase: GetYesterdayTaskSummaryUseCase
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    init {
        loadHomeData()
    }
    
    /**
     * Loads all home screen data.
     */
    fun loadHomeData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            
            try {
                // Get current user
                val userResult = getCurrentUserUseCase(Unit)
                val user = userResult.getOrNull() 
                    ?: throw Exception("Failed to get user data")
                
                // Get tasks
                val tasksResult = getTasksUseCase(Unit)
                val tasks = tasksResult.getOrNull() ?: emptyList()
                
                val tasksCompletedToday = tasks.count { it.status == TaskStatus.COMPLETED }
                val tasksPendingToday = tasks.count { it.status == TaskStatus.PENDING }
                
                // Get habits (handle errors gracefully)
                val habitsResult = getHabitsUseCase(Unit)
                val habits = habitsResult.getOrNull() ?: emptyList()
                
                // Count habits checked today (lastCheckedAt is today)
                val today = System.currentTimeMillis()
                val todayStart = today - (today % 86400000) // Start of day in UTC
                
                val habitsCheckedToday = try {
                    habits.count { habit ->
                        habit.lastCheckedAt != null && habit.lastCheckedAt >= todayStart
                    }
                } catch (e: Exception) {
                    0 // If habits fail, show 0 instead of crashing
                }
                val habitsPendingToday = habits.size - habitsCheckedToday
                
                // Get yesterday's task summary
                val yesterdaySummary = try {
                    android.util.Log.d("HomeViewModel", "Fetching yesterday's summary...")
                    val result = getYesterdayTaskSummaryUseCase(Unit)
                    val summary = result.getOrNull()
                    
                    if (summary != null) {
                        android.util.Log.d("HomeViewModel", "Yesterday summary: ${summary.totalTasks} tasks, ${summary.completionRate}% completion")
                    } else {
                        android.util.Log.d("HomeViewModel", "No yesterday summary available")
                    }
                    
                    summary
                } catch (e: Exception) {
                    android.util.Log.e("HomeViewModel", "Failed to fetch yesterday summary", e)
                    null
                }
                
                // Create home data
                val homeData = HomeData(
                    user = user,
                    tasksCompletedToday = tasksCompletedToday,
                    tasksPendingToday = tasksPendingToday,
                    habitsCheckedToday = habitsCheckedToday,
                    habitsPendingToday = habitsPendingToday,
                    currentStreak = user.streakDays,
                    yesterdaySummary = yesterdaySummary
                )
                
                _uiState.value = HomeUiState.Success(homeData)
                
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Failed to load home data")
            }
        }
    }
    
    /**
     * Refreshes home data (pull-to-refresh).
     */
    fun refresh() {
        loadHomeData()
    }
    
    /**
     * Resets state to idle.
     */
    fun resetState() {
        loadHomeData()
    }
}

/**
 * Sealed class representing home screen UI states.
 */
sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(val data: HomeData) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

/**
 * Data class containing all information displayed on the home screen.
 */
data class HomeData(
    val user: User,
    val tasksCompletedToday: Int,
    val tasksPendingToday: Int,
    val habitsCheckedToday: Int,
    val habitsPendingToday: Int,
    val currentStreak: Int,
    val yesterdaySummary: DailyTaskSummary? = null
)

