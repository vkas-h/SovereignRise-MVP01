package com.sovereign_rise.app.domain.usecase.habit

import com.sovereign_rise.app.domain.model.Habit
import com.sovereign_rise.app.domain.model.HabitType
import com.sovereign_rise.app.domain.repository.HabitRepository
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import com.sovereign_rise.app.util.Constants
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for updating an existing habit with validation.
 */
class UpdateHabitUseCase(
    private val habitRepository: HabitRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<UpdateHabitUseCase.Params, Habit>(dispatcher) {
    
    override suspend fun execute(params: Params): Habit {
        // Validate habitId
        if (params.habitId.isBlank()) {
            throw IllegalArgumentException("Habit ID cannot be blank")
        }
        
        // Validate name if provided
        if (params.name != null) {
            if (params.name.length < Constants.MIN_HABIT_NAME_LENGTH) {
                throw IllegalArgumentException(Constants.ERROR_HABIT_NAME_TOO_SHORT)
            }
            if (params.name.length > Constants.MAX_HABIT_NAME_LENGTH) {
                throw IllegalArgumentException(Constants.ERROR_HABIT_NAME_TOO_LONG)
            }
        }
        
        // Validate description if provided
        if (params.description != null && params.description.length > Constants.MAX_HABIT_DESCRIPTION_LENGTH) {
            throw IllegalArgumentException(Constants.ERROR_HABIT_DESCRIPTION_TOO_LONG)
        }
        
        // Validate intervalDays if provided
        if (params.intervalDays != null && params.intervalDays < Constants.MIN_CUSTOM_INTERVAL_DAYS) {
            throw IllegalArgumentException(Constants.ERROR_INVALID_INTERVAL)
        }
        
        // Get existing habit
        val existingHabit = habitRepository.getHabitById(params.habitId)
        
        // Create updated habit (keep streak-related fields unchanged)
        val updatedHabit = existingHabit.copy(
            name = params.name ?: existingHabit.name,
            description = params.description ?: existingHabit.description,
            type = params.type ?: existingHabit.type,
            intervalDays = params.intervalDays ?: existingHabit.intervalDays,
            isActive = params.isActive ?: existingHabit.isActive
        )
        
        return habitRepository.updateHabit(updatedHabit)
    }
    
    data class Params(
        val habitId: String,
        val name: String? = null,
        val description: String? = null,
        val type: HabitType? = null,
        val intervalDays: Int? = null,
        val isActive: Boolean? = null
    )
}

