package com.yakivmospan.templates.core.data.cache

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

class FakeClock(startTime: Instant = Instant.fromEpochMilliseconds(0)) : Clock {
    var now: Instant = startTime
        private set

    override fun now(): Instant = now

    fun advance(duration: Duration) {
        now += duration
    }
}

