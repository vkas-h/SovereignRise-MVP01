package com.sovereign_rise.app.domain.usecase.user

import com.sovereign_rise.app.domain.model.User
import com.sovereign_rise.app.domain.repository.UserRepository
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for updating user profile.
 * Validates inputs and delegates to repository for profile update.
 * 
 * @param userRepository Repository for user operations
 * @param dispatcher Coroutine dispatcher for execution (defaults to IO)
 */
class UpdateProfileUseCase(
    private val userRepository: UserRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<UpdateProfileUseCase.Params, User>(dispatcher) {
    
    override suspend fun execute(params: Params): User {
        // Validate username if provided
        if (params.username != null) {
            require(params.username.isNotBlank()) { "Username cannot be blank" }
            require(params.username.length in 3..20) { 
                "Username must be between 3 and 20 characters" 
            }
            require(params.username.matches(Regex("^[a-zA-Z0-9_]+$"))) { 
                "Username can only contain letters, numbers, and underscores" 
            }
        }
        
        // Delegate to repository
        return userRepository.updateProfile(
            username = params.username,
            photoUrl = params.photoUrl
        )
    }
    
    /**
     * Parameters for profile update use case.
     * At least one field must be non-null.
     */
    data class Params(
        val username: String? = null,
        val photoUrl: String? = null
    ) {
        init {
            require(username != null || photoUrl != null) {
                "At least one field must be provided for update"
            }
        }
    }
}

