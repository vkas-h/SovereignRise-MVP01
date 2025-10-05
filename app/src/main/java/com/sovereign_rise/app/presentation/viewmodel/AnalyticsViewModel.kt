package com.sovereign_rise.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sovereign_rise.app.data.local.TokenDataStore
import com.sovereign_rise.app.domain.model.AnalyticsData
import com.sovereign_rise.app.domain.repository.AnalyticsRepository
import com.sovereign_rise.app.util.ConnectivityObserver
import com.sovereign_rise.app.util.ConnectivityStatus
import com.sovereign_rise.app.util.ErrorLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UI State for Analytics Screen
 * Note: No Error state - repository guarantees fallback to local data
 */
sealed class AnalyticsUiState {
    object Loading : AnalyticsUiState()
    data class Success(val data: AnalyticsData) : AnalyticsUiState()
}

/**
 * ViewModel for the Analytics screen.
 * Manages analytics data fetching and state with offline-first support.
 */
class AnalyticsViewModel(
    private val analyticsRepository: AnalyticsRepository,
    private val connectivityObserver: ConnectivityObserver,
    private val tokenDataStore: TokenDataStore
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    // Selected period in days (default to 1 day for today's data)
    private val _selectedPeriod = MutableStateFlow(1)
    val selectedPeriod: StateFlow<Int> = _selectedPeriod.asStateFlow()
    
    // Connectivity state
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        // Observe connectivity changes
        viewModelScope.launch {
            connectivityObserver.observe().collect { status ->
                _isOnline.value = (status == ConnectivityStatus.AVAILABLE)
            }
        }
        
        loadAnalytics()
    }

    fun loadAnalytics() {
        viewModelScope.launch {
            _uiState.value = AnalyticsUiState.Loading
            
            val userId = getCurrentUserId() // Get from auth
            val period = _selectedPeriod.value
            
            android.util.Log.d("AnalyticsViewModel", "Loading analytics for period: $period days")
            
            // Always try to load data - repository will handle offline gracefully
            val data = analyticsRepository.getComprehensiveAnalytics(userId, period)
            
            // Validate data before showing
            val validatedData = validateAnalyticsData(data)
            
            android.util.Log.d("AnalyticsViewModel", "Analytics loaded: " +
                "today screen time=${validatedData.summary.todayScreenTime} min, " +
                "today unlocks=${validatedData.summary.todayUnlocks}, " +
                "top apps=${validatedData.topApps.size}, " +
                "has AI insights=${validatedData.hasAIInsights}")
            
            // Always emit Success state with data (never Error state)
            _uiState.value = AnalyticsUiState.Success(validatedData)
        }
    }

    fun changePeriod(days: Int) {
        _selectedPeriod.value = days
        loadAnalytics()
    }

    fun refresh() {
        loadAnalytics()
    }

    fun resetState() {
        _uiState.value = AnalyticsUiState.Loading
    }

    private fun validateAnalyticsData(data: AnalyticsData): AnalyticsData {
        // Validate summary stats
        val validatedSummary = data.summary.copy(
            todayScreenTime = data.summary.todayScreenTime.coerceIn(0, 1440),
            todayUnlocks = data.summary.todayUnlocks.coerceIn(0, 500),
            weekScreenTime = data.summary.weekScreenTime.coerceIn(0, 10080),
            weekUnlocks = data.summary.weekUnlocks.coerceIn(0, 3500),
            avgDailyScreenTime = data.summary.avgDailyScreenTime.coerceIn(0, 1440),
            avgDailyUnlocks = data.summary.avgDailyUnlocks.coerceIn(0, 500)
        )
        
        // Validate app usage data
        val validatedApps = data.topApps.map { app ->
            app.copy(totalMinutes = app.totalMinutes.coerceIn(0, 1440))
        }
        
        // Log any corrections made
        if (validatedSummary != data.summary) {
            android.util.Log.w("AnalyticsViewModel", "Data validation corrected some values")
        }
        
        return data.copy(
            summary = validatedSummary,
            topApps = validatedApps
        )
    }

    private suspend fun getCurrentUserId(): String {
        // Get userId from TokenDataStore
        return tokenDataStore.getUserId() ?: "guest"
    }

    fun forceRefresh() {
        viewModelScope.launch {
            android.util.Log.d("AnalyticsViewModel", "Force refreshing analytics...")
            
            // Clear any cached data
            _uiState.value = AnalyticsUiState.Loading
            
            // Small delay to ensure UI shows loading state
            kotlinx.coroutines.delay(300)
            
            loadAnalytics()
        }
    }
}
