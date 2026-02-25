package com.yakivmospan.templates.core.data.cache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration

/**
 * A generic cache for multiple values of type [T], keyed by [K].
 *
 * Implementations are responsible for any invalidation strategy
 * (e.g. expiry, persistence, capacity) behind [get] — callers only
 * observe presence or absence via nullability.
 */
interface KeyedCache<K, T> {

    /**
     * Returns the cached value for [key], or `null` if absent or invalidated.
     */
    suspend fun get(key: K): T?

    /**
     * Emits the current cached value for [key] and any subsequent updates.
     * Emits `null` if the value is absent or invalidated.
     */
    fun getFlow(key: K): Flow<T?>

    /**
     * Stores [data] in the cache for [key], replacing any existing value.
     */
    suspend fun put(key: K, data: T)

    /**
     * Removes the cached value for [key].
     */
    suspend fun clear(key: K)

    /**
     * Removes all cached values.
     */
    suspend fun clearAll()
}

/**
 * An in-memory implementation of [KeyedCache] backed by a [MutableStateFlow]
 * per key, guarded by a [Mutex] for thread safety.
 *
 * Supports optional time-based expiration via [TimestampExpirationValidator] per key.
 *
 * @param K The type of key used to identify cached values.
 * @param T The type of value stored in the cache.
 * @param validatorFactory Factory that creates an [ExpirationValidator] for each key.
 * Defaults to [TimestampExpirationValidator] with [Duration.INFINITE] (never expires).
 */
class InMemoryKeyedCache<K, T>(
    private val validatorFactory: () -> ExpirationValidator = { TimestampExpirationValidator(Duration.INFINITE) },
) : KeyedCache<K, T> {

    private data class Entry<T>(
        val flow: MutableStateFlow<T?>,
        val expirationValidator: ExpirationValidator,
    )

    private val mutex = Mutex()
    private val store = mutableMapOf<K, Entry<T>>()

    private suspend fun getOrCreateEntry(key: K): Entry<T> =
        mutex.withLock {
            store.getOrPut(key) {
                Entry(
                    flow = MutableStateFlow(null),
                    expirationValidator = validatorFactory(),
                )
            }
        }

    override suspend fun get(key: K): T? {
        val entry = mutex.withLock { store[key] } ?: return null
        if (entry.expirationValidator.isExpired()) {
            entry.flow.value = null
            return null
        }
        return entry.flow.value
    }

    override fun getFlow(key: K): Flow<T?> = flow {
        val entry = getOrCreateEntry(key)
        emitAll(entry.flow.asStateFlow().map {
            if (entry.expirationValidator.isExpired()) null else it
        })
    }

    override suspend fun put(key: K, data: T) {
        val entry = getOrCreateEntry(key)
        entry.expirationValidator.refresh()
        entry.flow.value = data
    }

    override suspend fun clear(key: K) {
        mutex.withLock { store[key] }?.flow?.value = null
    }

    override suspend fun clearAll() {
        mutex.withLock { store.values.toList() }.forEach { it.flow.value = null }
    }
}