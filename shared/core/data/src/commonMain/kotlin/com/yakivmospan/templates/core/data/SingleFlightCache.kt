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
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

// TODO Separate into SingleFlightCommand and MemoryCache classes, and compose them in repositories as needed.
/**
 * A coroutine-based cache that deduplicates concurrent requests for the same key
 * (the "single flight" pattern) and optionally retains results for a TTL.
 *
 * ### Single flight
 * If multiple coroutines call [get] or [getFlow] for the same key concurrently,
 * only one [worker] invocation is launched. All callers share the same [MutableSharedFlow]
 * and receive the result (or error) when the worker completes.
 *
 * ### Caching
 * When [keepFor] is non-zero and [keepIf] returns `true` for a result, the shared flow
 * is retained in memory with `replay = 1`. Subsequent callers within the TTL window
 * receive the cached value immediately without invoking [worker] again. Once the TTL
 * expires the entry is evicted on the next access.
 *
 * ### Invalidation
 * [invalidate] removes entries from [cache] and [timestamps] while intentionally
 * leaving any in-flight workers untouched. Callers already subscribed to an
 * in-flight flow still receive their result. However, because the cache entry is
 * cleared before [doWork] promotes the result, the completed value will not be
 * retained — the next caller after the worker finishes will trigger a fresh
 * worker invocation.
 *
 * ### Error handling
 * If [worker] throws, the entry is removed from the cache before the error is
 * broadcast, so the next caller will trigger a fresh [worker] invocation. All
 * current subscribers receive the exception via [Flow] termination.
 *
 * ### Thread safety
 * All mutations to internal state are guarded by a [Mutex]. Note that [worker] and
 * [MutableSharedFlow.emit] are intentionally called *outside* the lock to avoid
 * suspending while holding it.
 *
 * @param K The key type used to identify distinct cache entries.
 * @param V The value type produced by [worker].
 * @param scope The [CoroutineScope] in which worker coroutines are launched.
 * @param worker The suspending function invoked to produce a value for a given key.
 * @param keepFor How long a successful result should be cached. Defaults to [Duration.ZERO]
 *   (no caching — the entry is evicted as soon as all current subscribers have received it).
 * @param keepIf Predicate evaluated on each successful result. Return `false` to skip
 *   caching even when [keepFor] is non-zero (e.g. to avoid caching empty or partial data).
 *   Defaults to always `true`.
 * @param timeSource The [TimeSource] used for TTL measurement. Defaults to
 *   [TimeSource.Monotonic]. Override in tests to control time.
 */
class SingleFlightCache<K, V>(
    private val scope: CoroutineScope,
    private val worker: suspend (K) -> V,
    private val keepFor: Duration = Duration.ZERO,
    private val keepIf: (V) -> Boolean = { true },
    private val timeSource: TimeSource = TimeSource.Monotonic
) {
    private val mutex = Mutex()

    private val inFlight = HashMap<K, MutableSharedFlow<Result<V>>>()
    private val cache = HashMap<K, MutableSharedFlow<Result<V>>>()

    private val timestamps = HashMap<K, TimeMark>()

    /**
     * Returns the value for [key], suspending until the worker completes.
     * Throws if the worker throws.
     *
     * Shorthand for `getFlow(key).first()`.
     */
    suspend fun get(key: K): V = getFlow(key).first()

    /**
     * Returns a cold [Flow] that emits a single value for [key] and completes,
     * or throws if the worker throws.
     *
     * Collecting this flow will either join an in-flight worker, return a cached
     * result immediately, or launch a new worker — depending on cache state at the
     * time of collection.
     */
    fun getFlow(key: K): Flow<V> = flow {
        val sharedFlow = mutex.withLock { getOrPut(key) }
        emitAll(sharedFlow.map { it.getOrThrow() })
    }

    /**
     * Invalidates cached entries without disturbing in-flight workers.
     *
     * When [key] is provided, only that entry is removed from [cache] and
     * [timestamps]. When [key] is `null` (the default), all entries are removed.
     *
     * In-flight workers are left running intentionally:
     * - Callers already subscribed to an in-flight flow still receive their result.
     * - Because the cache entry is gone when [doWork] tries to promote the result,
     *   the value will not be stored and the next caller will trigger a new worker.
     *
     * @param key The specific key to invalidate, or `null` to invalidate all entries.
     */
    suspend fun invalidate(key: K? = null) {
        mutex.withLock {
            if (key == null) {
                cache.clear()
                timestamps.clear()
            } else {
                cache.remove(key)
                timestamps.remove(key)
            }
        }
    }

    /**
     * Returns the existing [MutableSharedFlow] for [key] if one is in-flight or
     * still within its TTL, otherwise creates a new one and launches a worker.
     *
     * Lookup order:
     * 1. [inFlight] — a worker is already running for this key.
     * 2. [cache] — a previously completed result is still within its TTL.
     * 3. Neither — create a new [MutableSharedFlow], register it in [inFlight],
     *    and launch a worker.
     *
     * Must be called with [mutex] held.
     */
    private fun getOrPut(key: K): MutableSharedFlow<Result<V>> {
        // Evict stale cache entry so we fall through to launching a fresh worker.
        if (keepFor > Duration.ZERO) {
            val cachedAt = timestamps[key]
            if (cachedAt != null && cachedAt.elapsedNow() > keepFor) {
                cache.remove(key)
                timestamps.remove(key)
            }
        }

        return inFlight[key]
            ?: cache[key]
            ?: run {
                val sharedFlow = MutableSharedFlow<Result<V>>(
                    replay = 1,
                    extraBufferCapacity = 0,
                    onBufferOverflow = BufferOverflow.SUSPEND
                )
                inFlight[key] = sharedFlow
                scope.launch { doWork(key, sharedFlow) }
                sharedFlow
            }
    }

    /**
     * Invokes [worker] for [key] and emits the outcome into [sharedFlow].
     *
     * On success:
     * - emits `Result.success(value)`
     * - moves the entry from [inFlight] to [cache] (with a fresh timestamp) if
     *   [keepFor] is non-zero and [keepIf] approves the result, and the entry
     *   has not been invalidated in the meantime
     * - otherwise removes the entry from [inFlight] with no caching
     *
     * On error:
     * - removes the entry from [inFlight] *before* emitting, so new callers
     *   trigger a fresh worker rather than receiving the replayed failure
     * - emits `Result.failure(exception)` to notify current subscribers
     */
    private suspend fun doWork(key: K, sharedFlow: MutableSharedFlow<Result<V>>) {
        var shouldCache = false
        var isError = false
        try {
            val result = worker(key)
            shouldCache = keepFor > Duration.ZERO && keepIf(result)

            mutex.withLock {
                inFlight.remove(key)
                if (shouldCache) {
                    cache[key] = sharedFlow
                    timestamps[key] = timeSource.markNow()
                }
            }
            sharedFlow.emit(Result.success(result))
        } catch (e: Exception) {
            isError = true
            mutex.withLock {
                inFlight.remove(key)
                timestamps.remove(key)
            }
            sharedFlow.emit(Result.failure(e))
        } finally {
            if (!isError && !shouldCache) {
                mutex.withLock {
                    inFlight.remove(key)
                }
            }
        }
    }
}