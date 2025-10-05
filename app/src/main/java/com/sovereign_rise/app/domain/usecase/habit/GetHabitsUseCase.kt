package com.sovereign_rise.app.domain.usecase.habit

import com.sovereign_rise.app.domain.model.Habit
import com.sovereign_rise.app.domain.repository.HabitRepository
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for fetching and sorting user's habits.
 */
class GetHabitsUseCase(
    private val habitRepository: HabitRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<Unit, List<Habit>>(dispatcher) {
    
    override suspend fun execute(params: Unit): List<Habit> {
        val habits = habitRepository.getHabits()
        
        // Sort habits: active first (by streak desc), then inactive (by name)
        return habits.sortedWith(
            compareByDescending<Habit> { it.isActive }
                .thenByDescending { it.streakDays }
                .thenBy { it.name }
        )
    }
}

