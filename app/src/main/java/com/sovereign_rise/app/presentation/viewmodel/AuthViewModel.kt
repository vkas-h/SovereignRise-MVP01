package com.sovereign_rise.app.presentation.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sovereign_rise.app.domain.model.User
import com.sovereign_rise.app.domain.usecase.auth.GetCurrentUserUseCase
import com.sovereign_rise.app.domain.usecase.auth.GoogleSignInUseCase
import com.sovereign_rise.app.domain.usecase.auth.GuestLoginUseCase
import com.sovereign_rise.app.domain.usecase.auth.LoginUseCase
import com.sovereign_rise.app.domain.usecase.auth.LogoutUseCase
import com.sovereign_rise.app.domain.usecase.auth.RegisterUseCase
import com.sovereign_rise.app.domain.usecase.auth.UpgradeAccountUseCase
import com.sovereign_rise.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for authentication screens.
 * Manages UI state and coordinates all authentication use cases.
 * 
 * @param loginUseCase Use case for user login
 * @param registerUseCase Use case for user registration
 * @param googleSignInUseCase Use case for Google Sign-In
 * @param guestLoginUseCase Use case for guest mode login
 * @param upgradeAccountUseCase Use case for upgrading guest accounts
 * @param logoutUseCase Use case for logging out
 * @param getCurrentUserUseCase Use case for retrieving current user
 * @param authRepository Repository for authentication operations
 */
class AuthViewModel(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val googleSignInUseCase: GoogleSignInUseCase,
    private val guestLoginUseCase: GuestLoginUseCase,
    private val upgradeAccountUseCase: UpgradeAccountUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    init {
        // Check if user is already logged in on app startup
        checkAuthStatus()
    }
    
    /**
     * Checks if a user is already authenticated and restores the session.
     * Emits GuestMode state for guest users to show upgrade path.
     */
    fun checkAuthStatus() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            val result = getCurrentUserUseCase(Unit)
            
            _uiState.value = result.fold(
                onSuccess = { user ->
                    if (user != null) {
                        // Check if user is a guest and set appropriate state
                        val isGuest = authRepository.isGuest()
                        if (isGuest) {
                            AuthUiState.GuestMode(user)
                        } else {
                            AuthUiState.Success(user)
                        }
                    } else {
                        AuthUiState.Idle
                    }
                },
                onFailure = { AuthUiState.Idle }
            )
        }
    }
    
    /**
     * Attempts to log in the user with email and password.
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            val result = loginUseCase(LoginUseCase.Params(email, password))
            
            _uiState.value = result.fold(
                onSuccess = { user -> AuthUiState.Success(user) },
                onFailure = { error -> AuthUiState.Error(error.message ?: "Login failed") }
            )
        }
    }
    
    /**
     * Attempts to register a new user.
     */
    fun register(username: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            val result = registerUseCase(RegisterUseCase.Params(username, email, password))
            
            _uiState.value = result.fold(
                onSuccess = { user -> AuthUiState.Success(user) },
                onFailure = { error -> AuthUiState.Error(error.message ?: "Registration failed") }
            )
        }
    }
    
    /**
     * Attempts to log in with Google Sign-In.
     */
    fun loginWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            val result = googleSignInUseCase(GoogleSignInUseCase.Params(activity))
            
            _uiState.value = result.fold(
                onSuccess = { user -> AuthUiState.Success(user) },
                onFailure = { error -> 
                    AuthUiState.Error(error.message ?: "Google Sign-In failed")
                }
            )
        }
    }
    
    /**
     * Logs in as a guest (anonymous user).
     */
    fun loginAsGuest() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            val result = guestLoginUseCase(Unit)
            
            _uiState.value = result.fold(
                onSuccess = { user -> AuthUiState.GuestMode(user) },
                onFailure = { error -> 
                    AuthUiState.Error(error.message ?: "Guest login failed")
                }
            )
        }
    }
    
    /**
     * Upgrades a guest account to a full account.
     */
    fun upgradeAccount(username: String, email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            val result = upgradeAccountUseCase(
                UpgradeAccountUseCase.Params(username, email, password)
            )
            
            _uiState.value = result.fold(
                onSuccess = { user -> AuthUiState.Success(user) },
                onFailure = { error -> 
                    AuthUiState.Error(error.message ?: "Account upgrade failed")
                }
            )
        }
    }
    
    /**
     * Logs out the current user.
     */
    fun logout() {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            val result = logoutUseCase(Unit)
            
            _uiState.value = result.fold(
                onSuccess = { AuthUiState.Idle },
                onFailure = { error -> 
                    // Even if logout fails, reset to Idle state
                    AuthUiState.Error(error.message ?: "Logout failed")
                }
            )
        }
    }
    
    /**
     * Resets the UI state to idle.
     */
    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}

/**
 * Sealed class representing authentication UI states.
 */
sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class GuestMode(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}