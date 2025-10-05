package com.sovereign_rise.app.domain.usecase.ai

import com.sovereign_rise.app.domain.model.Affirmation
import com.sovereign_rise.app.domain.model.AffirmationContext
import com.sovereign_rise.app.domain.repository.AffirmationRepository
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for generating affirmations
 */
class GenerateAffirmationUseCase(
    private val affirmationRepository: AffirmationRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<GenerateAffirmationUseCase.Params, Affirmation?>(dispatcher) {
    
    data class Params(
        val userId: String,
        val context: AffirmationContext,
        val variables: Map<String, String>
    )
    
    override suspend fun execute(params: Params): Affirmation? {
        require(params.userId.isNotBlank()) { "User ID cannot be blank" }
        
        if (!affirmationRepository.canShowAffirmation(params.userId)) {
            return null
        }
        
        val affirmation = affirmationRepository.generateAffirmation(params.context, params.variables)
        
        if (affirmation != null) {
            affirmationRepository.recordAffirmationShown(params.userId, affirmation)
        }
        
        return affirmation
    }
}

