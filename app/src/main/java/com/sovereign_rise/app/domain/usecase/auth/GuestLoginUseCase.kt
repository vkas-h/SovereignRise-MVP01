package com.sovereign_rise.app.domain.usecase.auth

import com.sovereign_rise.app.domain.model.User
import com.sovereign_rise.app.domain.repository.AuthRepository
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import com.sovereign_rise.app.util.ConnectivityObserver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for guest mode login.
 * Allows users to access the app anonymously with limited features.
 * Supports both online (Firebase + backend) and offline (local-only) guest modes.
 * 
 * @param authRepository Repository for authentication operations
 * @param connectivityObserver Observer for network connectivity
 * @param dispatcher Coroutine dispatcher for execution (defaults to IO)
 */
class GuestLoginUseCase(
    private val authRepository: AuthRepository,
    private val connectivityObserver: ConnectivityObserver,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<Unit, User>(dispatcher) {
    
    override suspend fun execute(params: Unit): User {
        // Check network connectivity
        val isOnline = connectivityObserver.isOnline()
        
        return try {
            if (isOnline) {
                // If online: try Firebase + backend
                try {
                    authRepository.loginAsGuest()
                } catch (e: Exception) {
                    // If online method fails (e.g., no actual internet), fallback to offline
                    android.util.Log.w("GuestLoginUseCase", "Online guest login failed, falling back to offline: ${e.message}")
                    authRepository.loginAsGuestOffline()
                }
            } else {
                // If offline: create local-only guest session
                authRepository.loginAsGuestOffline()
            }
        } catch (e: Exception) {
            // Last resort: ensure we always try offline mode if everything else fails
            android.util.Log.e("GuestLoginUseCase", "Guest login failed, attempting offline as last resort: ${e.message}")
            try {
                authRepository.loginAsGuestOffline()
            } catch (offlineError: Exception) {
                // If even offline fails, throw the original error with context
                throw Exception("Guest login failed (offline mode also unavailable): ${e.message}")
            }
        }
    }
}
