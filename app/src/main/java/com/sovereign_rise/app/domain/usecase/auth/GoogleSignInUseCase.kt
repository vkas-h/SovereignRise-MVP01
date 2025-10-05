package com.sovereign_rise.app.domain.usecase.auth

import android.app.Activity
import com.sovereign_rise.app.domain.model.User
import com.sovereign_rise.app.domain.repository.AuthRepository
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for Google Sign-In.
 * Encapsulates the business logic for authenticating with Google OAuth.
 * 
 * @param authRepository Repository for authentication operations
 * @param dispatcher Coroutine dispatcher for execution (defaults to IO)
 */
class GoogleSignInUseCase(
    private val authRepository: AuthRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<GoogleSignInUseCase.Params, User>(dispatcher) {
    
    override suspend fun execute(params: Params): User {
        // No validation needed - Google handles authentication
        return authRepository.loginWithGoogle(params.activityContext)
    }
    
    /**
     * Parameters for Google Sign-In use case.
     */
    data class Params(
        val activityContext: Activity
    )
}
