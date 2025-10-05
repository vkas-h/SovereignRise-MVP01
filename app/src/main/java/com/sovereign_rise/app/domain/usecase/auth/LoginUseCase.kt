package com.sovereign_rise.app.domain.usecase.auth

import com.sovereign_rise.app.domain.model.User
import com.sovereign_rise.app.domain.repository.AuthRepository
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import com.sovereign_rise.app.util.ConnectivityObserver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for logging in a user.
 * Encapsulates the business logic for user authentication.
 * First-time login requires internet connection.
 * 
 * @param authRepository Repository for authentication operations
 * @param connectivityObserver Observer for network connectivity
 * @param dispatcher Coroutine dispatcher for execution (defaults to IO)
 */
class LoginUseCase(
    private val authRepository: AuthRepository,
    private val connectivityObserver: ConnectivityObserver,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<LoginUseCase.Params, User>(dispatcher) {
    
    override suspend fun execute(params: Params): User {
        // Business logic: validate inputs before calling repository
        require(params.email.isNotBlank()) { "Email cannot be blank" }
        require(params.password.isNotBlank()) { "Password cannot be blank" }
        require(params.email.contains("@")) { "Invalid email format" }
        
        // Check network connectivity
        val isOnline = connectivityObserver.isOnline()
        if (!isOnline) {
            throw Exception("Internet connection required for first-time login")
        }
        
        // Delegate to repository for actual authentication
        return authRepository.login(params.email, params.password)
    }
    
    /**
     * Parameters for login use case.
     */
    data class Params(
        val email: String,
        val password: String
    )
}
