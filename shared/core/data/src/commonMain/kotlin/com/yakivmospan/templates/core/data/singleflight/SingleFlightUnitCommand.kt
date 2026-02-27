package com.yakivmospan.templates.core.data.singleflight

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A coroutine-based command that deduplicates concurrent requests
 * (the "single flight" pattern) for a single operation without parameters.
 *
 * ### Single flight
 * If multiple coroutines call [execute] or [executionFlow] concurrently,
 * only one [executor] invocation is launched. All callers share the same
 * [MutableSharedFlow] and receive the result (or error) when the executor completes.
 *
 * ### Flow contract
 * The returned flow is a single-shot stream that emits one value and completes.
 * It is primarily intended for structured concurrency integration rather than
 * continuous observation.
 *
 * ### Error handling
 * If [executor] throws, the in-flight entry is cleared before the error is broadcast,
 * so the next caller will trigger a fresh [executor] invocation. All current
 * subscribers receive the exception via [Flow] termination.
 *
 * ### Thread safety
 * All mutations to internal state are guarded by a [Mutex]. Note that [executor] and
 * [MutableSharedFlow.emit] are intentionally called *outside* the lock to avoid
 * suspending while holding it.
 *
 * ### When parameters are needed
 * If you need request deduplication per key (for example, per ID or query),
 * use the keyed variant: [SingleFlightCommand] with parameters (`SingleFlightCommand<K, V>`).
 *
 * ### Usage
 *
 * Basic usage with suspending API:
 *
 * ```
 * val command = SingleFlightCommand(
 *     scope = viewModelScope,
 *     executor = { api.fetchConfig() }
 * )
 *
 * // Multiple concurrent callers will share the same request
 * val config = command.execute()
 * ```
 *
 * Using Flow API:
 *
 * ```
 * viewModelScope.launch {
 *     command.executionFlow().collect { config ->
 *         render(config)
 *     }
 * }
 * ```
 *
 * Deduplicating concurrent refresh calls:
 *
 * ```
 * val refreshCommand = SingleFlightCommand(
 *     scope = appScope,
 *     executor = { repository.refreshToken() }
 * )
 *
 * suspend fun refreshIfNeeded() {
 *     refreshCommand.execute()
 * }
 * ```
 *
 * @param V The value type produced by [executor].
 * @param scope The [CoroutineScope] in which executor coroutines are launched.
 * @param executor The suspending function invoked to produce a value.
 * @param onSuccess Optional callback invoked with the result after a successful
 * [executor] invocation, but before broadcasting to subscribers.
 *   Runs on [scope], so it survives cancellation of any individual caller's scope.
 * @param onError Optional callback invoked with the exception after a failed
 * [executor] invocation, but before broadcasting to subscribers.
 *   Runs on [scope], so it survives cancellation of any individual caller's scope.
 */
class SingleFlightUnitCommand<V>(
    private val scope: CoroutineScope,
    private val executor: suspend () -> V,
    private val onSuccess: (suspend (V) -> Unit)? = null,
    private val onError: (suspend (Exception) -> Unit)? = null,
) {
    private val mutex = Mutex()

    private var inFlight: MutableSharedFlow<Result<V>>? = null

    /**
     * Returns the value, suspending until the executor completes.
     * Throws if the executor throws.
     *
     * Shorthand for `executionFlow().first()`.
     */
    suspend fun execute(): V = executionFlow().first()

    /**
     * Returns a cold [Flow] that emits a single value and completes,
     * or throws if the executor throws.
     *
     * Collecting this flow will either join an in-flight executor or launch a new
     * one — depending on state at the time of collection.
     */
    fun executionFlow(): Flow<V> = flow {
        val sharedFlow = mutex.withLock { launchIfAbsent() }
        emit(sharedFlow.map { it.getOrThrow() }.first())
    }

    /**
     * Returns the existing [MutableSharedFlow] if one is in-flight,
     * otherwise creates a new one and launches an executor.
     *
     * Synchronized — is called with [mutex] held.
     */
    private fun launchIfAbsent(): MutableSharedFlow<Result<V>> {
        return inFlight ?: run {
            val sharedFlow = MutableSharedFlow<Result<V>>(
                replay = 1,
                extraBufferCapacity = 0,
                onBufferOverflow = BufferOverflow.SUSPEND
            )
            inFlight = sharedFlow
            scope.launch { doExecute(sharedFlow) }
            sharedFlow
        }
    }

    /**
     * Invokes [executor] and emits the outcome into [sharedFlow].
     *
     * On success:
     * - invokes [onSuccess] if provided
     * - emits `Result.success(value)`
     * - clears the in-flight reference
     *
     * On error:
     * - invokes [onError] if provided
     * - clears the in-flight reference *before* emitting, so new callers
     *   trigger a fresh executor rather than receiving the replayed failure
     * - emits `Result.failure(exception)` to notify current subscribers
     */
    private suspend fun doExecute(sharedFlow: MutableSharedFlow<Result<V>>) {
        var cleared = false
        try {
            val result = executor()

            onSuccess?.invoke(result)

            sharedFlow.emit(Result.success(result))
        } catch (e: Exception) {
            onError?.invoke(e)

            mutex.withLock { inFlight = null }.also { cleared = true }

            sharedFlow.emit(Result.failure(e))
        } finally {
            if (!cleared) mutex.withLock { inFlight = null }
        }
    }
}