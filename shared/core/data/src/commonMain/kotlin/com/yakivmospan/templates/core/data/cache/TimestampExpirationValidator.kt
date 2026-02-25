package com.yakivmospan.templates.core.data.cache

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * Validates expiration based on a timestamp, using [kotlin.time.Clock.System] for KMP compatibility.
 *
 * @param expirationInterval Duration after which the timestamp is considered expired.
 * Use [kotlin.time.Duration.Companion.INFINITE] for no expiration.
 */
class TimestampExpirationValidator(
    private val expirationInterval: Duration = DEFAULT_EXPIRATION_INTERVAL,
    private val clock: Clock = Clock.System,
) : ExpirationValidator {
    private var timestamp: Instant = clock.now()

    override fun isExpired(): Boolean {
        if (expirationInterval.isInfinite()) return false
        return clock.now() - timestamp > expirationInterval
    }

    override fun refresh() {
        timestamp = clock.now()
    }

    companion object {
        val DEFAULT_EXPIRATION_INTERVAL: Duration = 5.minutes
    }
}