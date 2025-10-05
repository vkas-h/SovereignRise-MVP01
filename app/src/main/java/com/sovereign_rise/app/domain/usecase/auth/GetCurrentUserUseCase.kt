package com.sovereign_rise.app.domain.usecase.auth

import com.sovereign_rise.app.domain.model.User
import com.sovereign_rise.app.domain.repository.AuthRepository
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for retrieving the current authenticated user.
 * Used on app startup to check if a user is already logged in and restore their session.
 * 
 * @param authRepository Repository for authentication operations
 * @param dispatcher Coroutine dispatcher for execution (defaults to IO)
 */
class GetCurrentUserUseCase(
    private val authRepository: AuthRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<Unit, User?>(dispatcher) {
    
    override suspend fun execute(params: Unit): User? {
        // No validation needed
        return authRepository.getCurrentUser()
    }
}
