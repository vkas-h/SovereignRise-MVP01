package com.sovereign_rise.app.domain.usecase.habit

import com.sovereign_rise.app.domain.repository.HabitRepository
import com.sovereign_rise.app.domain.repository.StreakBreakResult
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for checking and handling streak breaks with protection.
 */
class CheckStreakBreaksUseCase(
    private val habitRepository: HabitRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<Unit, StreakBreakResult>(dispatcher) {
    
    override suspend fun execute(params: Unit): StreakBreakResult {
        return habitRepository.checkStreakBreaks()
    }
}

