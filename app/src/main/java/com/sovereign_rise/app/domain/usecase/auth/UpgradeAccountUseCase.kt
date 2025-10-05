package com.sovereign_rise.app.domain.usecase.auth

import com.sovereign_rise.app.domain.model.User
import com.sovereign_rise.app.domain.repository.AuthRepository
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for upgrading a guest account to a full account.
 * Converts an anonymous Firebase account to a permanent account with email/password credentials.
 * 
 * @param authRepository Repository for authentication operations
 * @param dispatcher Coroutine dispatcher for execution (defaults to IO)
 */
class UpgradeAccountUseCase(
    private val authRepository: AuthRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<UpgradeAccountUseCase.Params, User>(dispatcher) {
    
    companion object {
        private const val MIN_USERNAME_LENGTH = 3
        private const val MAX_USERNAME_LENGTH = 20
        private const val MIN_PASSWORD_LENGTH = 8
    }
    
    override suspend fun execute(params: Params): User {
        // Validate username
        require(params.username.isNotBlank()) { "Username cannot be blank" }
        require(params.username.length >= MIN_USERNAME_LENGTH) { 
            "Username must be at least $MIN_USERNAME_LENGTH characters" 
        }
        require(params.username.length <= MAX_USERNAME_LENGTH) { 
            "Username must be at most $MAX_USERNAME_LENGTH characters" 
        }
        
        // Validate email
        require(params.email.isNotBlank()) { "Email cannot be blank" }
        require(params.email.contains("@")) { "Invalid email format" }
        
        // Validate password
        require(params.password.isNotBlank()) { "Password cannot be blank" }
        require(params.password.length >= MIN_PASSWORD_LENGTH) { 
            "Password must be at least $MIN_PASSWORD_LENGTH characters" 
        }
        
        // Delegate to repository to upgrade the guest account
        return authRepository.upgradeGuestAccount(params.username, params.email, params.password)
    }
    
    /**
     * Parameters for account upgrade use case.
     */
    data class Params(
        val username: String,
        val email: String,
        val password: String
    )
}
