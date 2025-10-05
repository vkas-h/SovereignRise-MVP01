package com.sovereign_rise.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sovereign_rise.app.domain.model.User
import com.sovereign_rise.app.domain.repository.AuthRepository
import com.sovereign_rise.app.domain.usecase.auth.GetCurrentUserUseCase
import com.sovereign_rise.app.domain.usecase.auth.LogoutUseCase
import com.sovereign_rise.app.domain.usecase.user.UpdateProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for user profile screen.
 * Manages user profile data, profile editing, and logout functionality.
 * 
 * @param getCurrentUserUseCase Use case for retrieving current user
 * @param logoutUseCase Use case for logging out
 * @param updateProfileUseCase Use case for updating profile
 * @param authRepository Repository for checking guest status
 */
class ProfileViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()
    
    init {
        loadProfile()
    }
    
    /**
     * Loads the current user's profile and guest status.
     */
    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            
            val result = getCurrentUserUseCase(Unit)
            
            _uiState.value = result.fold(
                onSuccess = { user ->
                    if (user != null) {
                        val isGuest = authRepository.isGuest()
                        ProfileUiState.Success(user, isGuest)
                    } else {
                        // User is null - check if we have a valid local session
                        // If no valid session, trigger logout
                        if (!authRepository.hasValidLocalSession()) {
                            ProfileUiState.LoggedOut
                        } else {
                            // Still have valid session but profile fetch failed (network issue, etc.)
                            ProfileUiState.Error("Unable to load profile. Check your connection and try again.")
                        }
                    }
                },
                onFailure = { error ->
                    ProfileUiState.Error(error.message ?: "Failed to load profile. Please try again.")
                }
            )
        }
    }
    
    /**
     * Logs out the current user.
     */
    fun logout() {
        viewModelScope.launch {
            val result = logoutUseCase(Unit)
            
            result.fold(
                onSuccess = {
                    _uiState.value = ProfileUiState.LoggedOut
                },
                onFailure = { error ->
                    _uiState.value = ProfileUiState.Error(error.message ?: "Logout failed")
                }
            )
        }
    }
    
    /**
     * Updates the user's profile (username and/or photo URL).
     * @param username New username (null to keep existing)
     * @param photoUrl New photo URL (null to keep existing)
     */
    fun updateProfile(username: String? = null, photoUrl: String? = null) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            
            val result = updateProfileUseCase(
                UpdateProfileUseCase.Params(
                    username = username,
                    photoUrl = photoUrl
                )
            )
            
            _uiState.value = result.fold(
                onSuccess = { user ->
                    val isGuest = authRepository.isGuest()
                    ProfileUiState.Success(user, isGuest)
                },
                onFailure = { error ->
                    ProfileUiState.Error(error.message ?: "Failed to update profile")
                }
            )
        }
    }
}

/**
 * Sealed class representing profile UI states.
 */
sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data class Success(val user: User, val isGuest: Boolean = false) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
    data object LoggedOut : ProfileUiState()
}
