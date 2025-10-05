package com.sovereign_rise.app.domain.usecase.habit

import com.sovereign_rise.app.domain.repository.HabitRepository
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for deleting a habit.
 */
class DeleteHabitUseCase(
    private val habitRepository: HabitRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<DeleteHabitUseCase.Params, Unit>(dispatcher) {
    
    override suspend fun execute(params: Params) {
        // Validate habitId
        if (params.habitId.isBlank()) {
            throw IllegalArgumentException("Habit ID cannot be blank")
        }
        
        habitRepository.deleteHabit(params.habitId)
    }
    
    data class Params(
        val habitId: String
    )
}

