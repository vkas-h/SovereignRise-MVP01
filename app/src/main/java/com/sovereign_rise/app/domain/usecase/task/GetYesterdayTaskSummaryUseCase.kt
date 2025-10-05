package com.sovereign_rise.app.domain.usecase.task

import com.sovereign_rise.app.data.remote.api.TaskApiService
import com.sovereign_rise.app.data.remote.dto.toDomain
import com.sovereign_rise.app.domain.model.DailyTaskSummary
import com.sovereign_rise.app.domain.usecase.base.BaseUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Use case for fetching yesterday's task summary.
 */
class GetYesterdayTaskSummaryUseCase(
    private val taskApiService: TaskApiService,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BaseUseCase<Unit, DailyTaskSummary>(dispatcher) {
    
    override suspend fun execute(params: Unit): DailyTaskSummary {
        val response = taskApiService.getYesterdaySummary()
        
        if (!response.isSuccessful) {
            throw Exception("Failed to fetch yesterday's summary: ${response.code()}")
        }
        
        val dto = response.body() ?: throw Exception("Empty response body")
        
        return dto.toDomain()
    }
}

