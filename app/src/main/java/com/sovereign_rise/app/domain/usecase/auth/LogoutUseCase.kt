package com.sovereign_rise.app.domain.usecase.auth

import com.sovereign_rise.app.domain.repository.AuthRepository
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for logging out the current user.
 * Clears authentication state and tokens from the device.
 * 
 * @param authRepository Repository for authentication operations
 * @param dispatcher Coroutine dispatcher for execution (defaults to IO)
 */
class LogoutUseCase(
    private val authRepository: AuthRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<Unit, Unit>(dispatcher) {
    
    override suspend fun execute(params: Unit) {
        // No validation needed for logout
        authRepository.logout()
    }
}
