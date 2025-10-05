package com.sovereign_rise.app.domain.usecase.habit

import com.sovereign_rise.app.domain.model.Habit
import com.sovereign_rise.app.domain.model.HabitType
import com.sovereign_rise.app.domain.repository.HabitRepository
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import com.sovereign_rise.app.util.Constants
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for creating a new habit with validation.
 */
class CreateHabitUseCase(
    private val habitRepository: HabitRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<CreateHabitUseCase.Params, Habit>(dispatcher) {
    
    override suspend fun execute(params: Params): Habit {
        // Validate inputs
        if (params.name.isBlank()) {
            throw IllegalArgumentException("Habit name cannot be blank")
        }
        if (params.name.length < Constants.MIN_HABIT_NAME_LENGTH) {
            throw IllegalArgumentException(Constants.ERROR_HABIT_NAME_TOO_SHORT)
        }
        if (params.name.length > Constants.MAX_HABIT_NAME_LENGTH) {
            throw IllegalArgumentException(Constants.ERROR_HABIT_NAME_TOO_LONG)
        }
        if (params.description != null && params.description.length > Constants.MAX_HABIT_DESCRIPTION_LENGTH) {
            throw IllegalArgumentException(Constants.ERROR_HABIT_DESCRIPTION_TOO_LONG)
        }
        if (params.type == HabitType.CUSTOM_INTERVAL && params.intervalDays < Constants.MIN_CUSTOM_INTERVAL_DAYS) {
            throw IllegalArgumentException(Constants.ERROR_INVALID_INTERVAL)
        }
        
        // Check MAX_ACTIVE_HABITS limit
        val existingHabits = habitRepository.getHabits()
        val activeHabitsCount = existingHabits.count { it.isActive }
        if (activeHabitsCount >= Constants.MAX_ACTIVE_HABITS) {
            throw IllegalArgumentException(Constants.ERROR_MAX_HABITS_REACHED)
        }
        
        // Create habit
        val habit = Habit(
            id = "", // Backend generates
            userId = "", // Backend sets from token
            name = params.name,
            description = params.description,
            type = params.type,
            intervalDays = params.intervalDays,
            streakDays = 0,
            longestStreak = 0,
            lastCheckedAt = null,
            createdAt = System.currentTimeMillis(),
            isActive = true,
            totalCompletions = 0,
            milestonesAchieved = emptyList()
        )
        
        return habitRepository.createHabit(habit)
    }
    
    data class Params(
        val name: String,
        val description: String?,
        val type: HabitType,
        val intervalDays: Int = 1
    )
}

