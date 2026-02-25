package com.yakivmospan.templates.core.data.cache

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class TimestampExpirationValidatorTest {

    // -------------------------------------------------------------------------
    // Not expired
    // -------------------------------------------------------------------------

    @Test
    fun `when interval is not elapsed then isExpired returns false`() {
        // Given
        val clock = FakeClock()
        val validator = TimestampExpirationValidator(5.minutes, clock)

        // When
        clock.advance(4.minutes)

        // Then
        assertFalse(validator.isExpired())
    }

    @Test
    fun `when interval is exactly elapsed then isExpired returns false`() {
        // Given
        val clock = FakeClock()
        val validator = TimestampExpirationValidator(5.minutes, clock)

        // When
        clock.advance(5.minutes)

        // Then
        assertFalse(validator.isExpired())
    }

    @Test
    fun `when interval is infinite then isExpired always returns false`() {
        // Given
        val clock = FakeClock()
        val validator = TimestampExpirationValidator(Duration.INFINITE, clock)

        // When
        clock.advance(100.minutes)

        // Then
        assertFalse(validator.isExpired())
    }

    // -------------------------------------------------------------------------
    // Expired
    // -------------------------------------------------------------------------

    @Test
    fun `when interval is elapsed then isExpired returns true`() {
        // Given
        val clock = FakeClock()
        val validator = TimestampExpirationValidator(5.minutes, clock)

        // When
        clock.advance(6.minutes)

        // Then
        assertTrue(validator.isExpired())
    }

    // -------------------------------------------------------------------------
    // Refresh
    // -------------------------------------------------------------------------

    @Test
    fun `when refresh is called after expiry and isExpired is checked at the same tick then returns false`() {
        // Given
        val clock = FakeClock()
        val validator = TimestampExpirationValidator(5.minutes, clock)
        clock.advance(6.minutes) // expire

        // When — refresh and isExpired called at the exact same clock tick (clock does not advance)
        validator.refresh()

        // Then — 0 elapsed since refresh, 0 > 5.minutes is false
        assertFalse(validator.isExpired())
    }

    @Test
    fun `when refresh is called then interval restarts from that point`() {
        // Given
        val clock = FakeClock()
        val validator = TimestampExpirationValidator(5.minutes, clock)
        clock.advance(6.minutes) // expire
        validator.refresh()

        // When
        clock.advance(4.minutes) // within the new interval

        // Then
        assertFalse(validator.isExpired())
    }

    @Test
    fun `when refresh is called then value expires again after interval`() {
        // Given
        val clock = FakeClock()
        val validator = TimestampExpirationValidator(5.minutes, clock)
        clock.advance(6.minutes) // expire
        validator.refresh()

        // When
        clock.advance(6.minutes) // exceed the new interval

        // Then
        assertTrue(validator.isExpired())
    }
}



