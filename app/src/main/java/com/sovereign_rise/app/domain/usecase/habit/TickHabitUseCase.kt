package com.sovereign_rise.app.domain.usecase.habit

import com.sovereign_rise.app.domain.repository.HabitRepository
import com.sovereign_rise.app.domain.repository.TickHabitResult
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for marking a habit as completed (ticking).
 */
class TickHabitUseCase(
    private val habitRepository: HabitRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<TickHabitUseCase.Params, TickHabitResult>(dispatcher) {
    
    override suspend fun execute(params: Params): TickHabitResult {
        // Validate habitId
        if (params.habitId.isBlank()) {
            throw IllegalArgumentException("Habit ID cannot be blank")
        }
        
        return habitRepository.tickHabit(params.habitId)
    }
    
    data class Params(
        val habitId: String
    )
}

