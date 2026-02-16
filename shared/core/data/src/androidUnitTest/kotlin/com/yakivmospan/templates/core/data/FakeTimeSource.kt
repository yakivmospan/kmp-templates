package com.yakivmospan.templates.core.data

import kotlin.time.AbstractLongTimeSource
import kotlin.time.DurationUnit

/**
 * A controllable TimeSource for tests.
 * Call [advanceBy] to move time forward deterministically.
 */
class FakeTimeSource : AbstractLongTimeSource(unit = DurationUnit.MILLISECONDS) {
    private var nowMs: Long = 0L

    override fun read(): Long = nowMs

    fun advanceBy(ms: Long) {
        nowMs += ms
    }
}