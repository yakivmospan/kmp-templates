package com.yakivmospan.templates.core.data.cache

/**
 * Determines whether something (like cached data for e.g.) has expired.
 */
interface ExpirationValidator {

    /**
     * Returns `true` if the cached data should be considered expired and discarded.
     */
    fun isExpired(): Boolean

    /**
     * Resets the expiration state, e.g. after new data is stored in the cache.
     */
    fun refresh()
}

