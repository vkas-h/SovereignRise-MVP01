package com.sovereign_rise.app.domain.usecase.base

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Base interface for use cases that execute business logic.
 * @param P Parameter type (use Unit if no parameters needed)
 * @param R Return type
 */
interface UseCase<in P, out R> {
    suspend operator fun invoke(params: P): Result<R>
}

/**
 * Abstract base class for use cases with dispatcher support.
 */
abstract class BaseUseCase<in P, R>(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : UseCase<P, R> {
    
    override suspend fun invoke(params: P): Result<R> = withContext(dispatcher) {
        try {
            Result.success(execute(params))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    @Throws(Exception::class)
    protected abstract suspend fun execute(params: P): R
}
