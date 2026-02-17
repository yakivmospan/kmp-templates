package com.yakivmospan.templates.core.data

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
 * A coroutine-based command that deduplicates concurrent requests for the same key
 * (the "single flight" pattern).
 *
 * ### Single flight
 * If multiple coroutines call [execute] or [executeFlow] for the same key concurrently,
 * only one [executor] invocation is launched. All callers share the same [MutableSharedFlow]
 * and receive the result (or error) when the executor completes.
 *
 * ### Error handling
 * If [executor] throws, the entry is removed before the error is broadcast, so the
 * next caller will trigger a fresh [executor] invocation. All current subscribers
 * receive the exception via [Flow] termination.
 *
 * ### Thread safety
 * All mutations to internal state are guarded by a [Mutex]. Note that [executor] and
 * [MutableSharedFlow.emit] are intentionally called *outside* the lock to avoid
 * suspending while holding it.
 *
 * @param K The key type used to identify distinct in-flight entries.
 * @param V The value type produced by [executor].
 * @param scope The [CoroutineScope] in which executor coroutines are launched.
 * @param executor The suspending function invoked to produce a value for a given key.
 * @param onSuccess Optional callback invoked with the result after a successful [executor] invocation, but before broadcasting to subscribers.
 *   Runs on [scope], so it survives cancellation of any individual caller's scope.
 * @param onError Optional callback invoked with the exception after a failed [executor] invocation, but before broadcasting to subscribers.
 *   Runs on [scope], so it survives cancellation of any individual caller's scope.
 */
class SingleFlightCommand<K, V>(
    private val scope: CoroutineScope,
    private val executor: suspend (K) -> V,
    private val onSuccess: (suspend (V) -> Unit)? = null,
    private val onError: (suspend (Exception) -> Unit)? = null,
) {
    private val mutex = Mutex()

    private val inFlight = HashMap<K, MutableSharedFlow<Result<V>>>()

    /**
     * Returns the value for [key], suspending until the executor completes.
     * Throws if the executor throws.
     *
     * Shorthand for `executeFlow(key).first()`.
     */
    suspend fun execute(key: K): V = executionFlow(key).first()

    /**
     * Returns a cold [Flow] that emits a single value for [key] and completes,
     * or throws if the executor throws.
     *
     * Collecting this flow will either join an in-flight executor or launch a new
     * one — depending on cache state at the time of collection.
     */
    fun executionFlow(key: K): Flow<V> = flow {
        val sharedFlow = mutex.withLock { launchIfAbsent(key) }
        emitAll(sharedFlow.map { it.getOrThrow() })
    }

    /**
     * Returns the existing [MutableSharedFlow] for [key] if one is in-flight,
     * otherwise creates a new one and launches an executor.
     *
     * Synchronized - is called with [mutex] held.
     */
    private fun launchIfAbsent(key: K): MutableSharedFlow<Result<V>> {
        return inFlight[key] ?: run {
            val sharedFlow = MutableSharedFlow<Result<V>>(
                replay = 1,
                extraBufferCapacity = 0,
                onBufferOverflow = BufferOverflow.SUSPEND
            )
            inFlight[key] = sharedFlow
            scope.launch { doExecute(key, sharedFlow) }
            sharedFlow
        }
    }

    /**
     * Invokes [executor] for [key] and emits the outcome into [sharedFlow].
     *
     * On success:
     * - invokes [onSuccess] if provided
     * - emits `Result.success(value)`
     * - removes the entry from [inFlight]
     *
     * On error:
     * - invokes [onError] if provided
     * - removes the entry from [inFlight] *before* emitting, so new callers
     *   trigger a fresh executor rather than receiving the replayed failure
     * - emits `Result.failure(exception)` to notify current subscribers
     */
    private suspend fun doExecute(key: K, sharedFlow: MutableSharedFlow<Result<V>>) {
        // 0. Launched only once for new added key.

        try {
            // 1. Execute the outside work
            val result = executor(key)

            // 2. Notify success listener before broadcasting
            onSuccess?.invoke(result)

            // 3. Notify all subscribers about job done
            sharedFlow.emit(Result.success(result))
        } catch (e: Exception) {
            // 2. Notify error listener before broadcasting
            onError?.invoke(e)

            // Remove before emitting so new callers get a fresh executor
            mutex.withLock { inFlight.remove(key) }
            sharedFlow.emit(Result.failure(e))
        } finally {
            mutex.withLock { inFlight.remove(key) }
        }
    }
}