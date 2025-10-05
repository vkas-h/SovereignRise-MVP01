package com.sovereign_rise.app.domain.usecase.habit

import com.sovereign_rise.app.domain.model.Habit
import com.sovereign_rise.app.domain.repository.HabitRepository
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for fetching a specific habit by ID.
 */
class GetHabitByIdUseCase(
    private val habitRepository: HabitRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<GetHabitByIdUseCase.Params, Habit>(dispatcher) {
    
    override suspend fun execute(params: Params): Habit {
        if (params.habitId.isBlank()) {
            throw IllegalArgumentException("Habit ID cannot be blank")
        }
        return habitRepository.getHabitById(params.habitId)
    }
    
    data class Params(val habitId: String)
}

