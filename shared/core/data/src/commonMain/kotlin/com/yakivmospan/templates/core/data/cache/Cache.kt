package com.yakivmospan.templates.core.data.cache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlin.time.Duration

/**
 * A generic cache for a single value of type [T].
 *
 * Implementations are responsible for any invalidation strategy
 * (e.g. expiry, persistence, capacity) behind [get] — callers only
 * observe presence or absence via nullability.
 */
interface Cache<T> {

    /**
     * Returns the cached value, or `null` if absent or invalidated.
     */
    suspend fun get(): T?

    /**
     * Emits the current cached value and any subsequent updates.
     * Emits `null` if the cache is absent or invalidated.
     */
    fun getFlow(): Flow<T?>

    /**
     * Stores [data] in the cache, replacing any existing value.
     */
    suspend fun put(data: T)

    /**
     * Removes the cached value. Subsequent calls to [get] will return `null`.
     */
    suspend fun clear()
}

/**
 * An in-memory implementation of [Cache] backed by a [MutableStateFlow].
 *
 * Supports optional time-based expiration via [ExpirationValidator].
 * When expired, [get] and [getFlow] treat the value as absent and auto-clear the cache.
 *
 * @param T The type of value stored in the cache.
 * @param expirationValidator Strategy that determines whether the cached value has expired.
 * Defaults to [TimestampExpirationValidator] with infinite duration (never expires).
 */
class InMemoryCache<T>(
    private val expirationValidator: ExpirationValidator = TimestampExpirationValidator(Duration.INFINITE),
) : Cache<T> {

    private val state = MutableStateFlow<T?>(null)

    override suspend fun get(): T? = currentValue()

    override fun getFlow(): Flow<T?> = state.asStateFlow()
        .map { if (expirationValidator.isExpired()) null else it }

    override suspend fun put(data: T) {
        expirationValidator.refresh()
        state.value = data
    }

    override suspend fun clear() {
        state.value = null
    }

    private fun currentValue(): T? {
        if (expirationValidator.isExpired()) {
            state.value = null
            return null
        }
        return state.value
    }
}