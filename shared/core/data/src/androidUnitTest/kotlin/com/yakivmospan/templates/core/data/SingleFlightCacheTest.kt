package com.yakivmospan.templates.core.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class SingleFlightCacheTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var fakeTimeSource: FakeTimeSource

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeTimeSource = FakeTimeSource()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // 1. Single call invokes worker once
    // -------------------------------------------------------------------------

    @Test
    fun `get - single call invokes worker once and returns value`() = testScope.runTest {
        // Given
        var callCount = 0
        val cache = SingleFlightCache<String, Int>(
            scope = testScope,
            worker = { _ -> callCount++; 42 }
        )

        // When
        val result = cache.get("key")
        advanceUntilIdle()

        // Then
        assertEquals(42, result)
        assertEquals(1, callCount)
    }

    // -------------------------------------------------------------------------
    // 2. Concurrent calls for the same key invoke worker exactly once
    // -------------------------------------------------------------------------

    @Test
    fun `get - concurrent calls for same key invoke worker exactly once`() = testScope.runTest {
        // Given
        var callCount = 0
        val cache = SingleFlightCache<String, Int>(
            scope = testScope,
            worker = { _ -> callCount++; 99 }
        )

        // When
        val d1 = async { cache.get("key") }
        val d2 = async { cache.get("key") }
        val d3 = async { cache.get("key") }
        advanceUntilIdle()

        // Then
        assertEquals(99, d1.await())
        assertEquals(99, d2.await())
        assertEquals(99, d3.await())
        assertEquals(1, callCount)
    }

    // -------------------------------------------------------------------------
    // 3. Sequential calls with no TTL invoke worker each time
    // -------------------------------------------------------------------------

    @Test
    fun `get - sequential calls with keepFor=ZERO invoke worker each time`() = testScope.runTest {
        // Given
        var callCount = 0
        val cache = SingleFlightCache<String, Int>(
            scope = testScope,
            worker = { _ -> callCount++ }
        )

        // When
        cache.get("key")
        advanceUntilIdle()
        cache.get("key")
        advanceUntilIdle()

        // Then
        assertEquals(2, callCount)
    }

    // -------------------------------------------------------------------------
    // 4. Result served from cache within TTL — worker called only once
    // -------------------------------------------------------------------------

    @Test
    fun `get - second call within TTL returns cached result without calling worker again`() = testScope.runTest {
        // Given
        var callCount = 0
        val cache = SingleFlightCache<String, Int>(
            scope = testScope,
            worker = { _ -> callCount++; 7 },
            keepFor = 1000.milliseconds,
            timeSource = fakeTimeSource
        )

        // When
        val first = cache.get("key")
        advanceUntilIdle()
        fakeTimeSource.advanceBy(500) // still within TTL
        val second = cache.get("key")
        advanceUntilIdle()

        // Then
        assertEquals(7, first)
        assertEquals(7, second)
        assertEquals(1, callCount)
    }

    // -------------------------------------------------------------------------
    // 5. Result re-fetched after TTL expires
    // -------------------------------------------------------------------------

    @Test
    fun `get - call after TTL expires re-invokes worker`() = testScope.runTest {
        // Given
        var callCount = 0
        val cache = SingleFlightCache<String, Int>(
            scope = testScope,
            worker = { _ -> callCount++; callCount },
            keepFor = 1000.milliseconds,
            timeSource = fakeTimeSource
        )

        // When
        val first = cache.get("key")
        advanceUntilIdle()
        fakeTimeSource.advanceBy(1500) // past TTL
        val second = cache.get("key")
        advanceUntilIdle()

        // Then
        assertEquals(1, first)
        assertEquals(2, second)
        assertEquals(2, callCount)
    }

    // -------------------------------------------------------------------------
    // 6. keepFor=ZERO never caches — always calls worker
    // -------------------------------------------------------------------------

    @Test
    fun `get - keepFor=ZERO never caches result`() = testScope.runTest {
        // Given
        var callCount = 0
        val cache = SingleFlightCache<String, Int>(
            scope = testScope,
            worker = { _ -> callCount++ },
            keepFor = kotlin.time.Duration.ZERO
        )

        // When
        repeat(3) {
            cache.get("key")
            advanceUntilIdle()
        }

        // Then
        assertEquals(3, callCount)
    }

    // -------------------------------------------------------------------------
    // 7. keepIf returns false — result not cached, worker called again
    // -------------------------------------------------------------------------

    @Test
    fun `get - keepIf returning false does not cache result`() = testScope.runTest {
        // Given
        var callCount = 0
        val cache = SingleFlightCache<String, Int>(
            scope = testScope,
            worker = { _ -> callCount++; -1 },
            keepFor = 1000.milliseconds,
            keepIf = { value -> value > 0 },
            timeSource = fakeTimeSource
        )

        // When
        val first = cache.get("key")
        advanceUntilIdle()
        val second = cache.get("key")
        advanceUntilIdle()

        // Then
        assertEquals(-1, first)
        assertEquals(-1, second)
        assertEquals(2, callCount)
    }

    // -------------------------------------------------------------------------
    // 8. keepIf returns true — result is cached normally
    // -------------------------------------------------------------------------

    @Test
    fun `get - keepIf returning true caches result`() = testScope.runTest {
        // Given
        var callCount = 0
        val cache = SingleFlightCache<String, Int>(
            scope = testScope,
            worker = { _ -> callCount++; 5 },
            keepFor = 1000.milliseconds,
            keepIf = { value -> value > 0 },
            timeSource = fakeTimeSource
        )

        // When
        val first = cache.get("key")
        advanceUntilIdle()
        val second = cache.get("key")
        advanceUntilIdle()

        // Then
        assertEquals(5, first)
        assertEquals(5, second)
        assertEquals(1, callCount)
    }

    // -------------------------------------------------------------------------
    // 9. Exception from worker propagates to caller
    // -------------------------------------------------------------------------

    @Test
    fun `get - exception thrown by worker propagates to caller`() = testScope.runTest {
        // Given
        val cache = SingleFlightCache<String, Int>(
            scope = backgroundScope,
            worker = { _ -> throw RuntimeException("worker failed") }
        )

        // When / Then
        assertFailsWith<RuntimeException> {
            cache.get("key")
        }
    }

    // -------------------------------------------------------------------------
    // 10. getFlow — multiple collectors all receive the value
    // -------------------------------------------------------------------------

    @Test
    fun `getFlow - multiple collectors on same key all receive the emitted value`() = testScope.runTest {
        // Given
        var callCount = 0
        val cache = SingleFlightCache<String, Int>(
            scope = backgroundScope,
            worker = { _ -> callCount++; 100 }
        )

        // When
        val d1 = async { cache.getFlow("key").first() }
        val d2 = async { cache.getFlow("key").first() }
        val d3 = async { cache.getFlow("key").first() }
        advanceUntilIdle()

        // Then
        assertEquals(100, d1.await())
        assertEquals(100, d2.await())
        assertEquals(100, d3.await())
        assertEquals(1, callCount)
    }

    // -------------------------------------------------------------------------
    // 11. Caller scope cancelled — worker still completes
    // -------------------------------------------------------------------------

    @Test
    fun `get - cancelling caller scope does not cancel the worker`() = testScope.runTest {
        // Given
        val callerScope = CoroutineScope(testDispatcher + Job())
        var callCount = 0
        val cache = SingleFlightCache<String, Int>(
            scope = backgroundScope,
            worker = { _ ->
                delay(500)
                callCount++
                5
            },
            keepFor = 1000.milliseconds,
            keepIf = { value -> value > 0 },
            timeSource = fakeTimeSource
        )

        // When — first call, cancel caller mid-flight
        val job1 = callerScope.launch { cache.get("key") }
        advanceTimeBy(100)             // worker started, inside delay(500), not yet complete
        callerScope.cancel()                          // cancel caller — worker keeps running on backgroundScope
        advanceTimeBy(500)            // worker completes, result cached

        assertEquals(false, job1.isActive)
        assertEquals(true, job1.isCancelled)
        assertEquals(true, job1.isCompleted)
        assertEquals(1, callCount)     // worker ran once

        // second call hits cache — no new worker invocation
        val second = cache.get("key")
        advanceUntilIdle()

        assertEquals(5, second)
        assertEquals(1, callCount)     // still 1 — served from cache
    }

// -------------------------------------------------------------------------
// 12. Different keys invoke worker independently and return different results
// -------------------------------------------------------------------------

    @Test
    fun `get - different keys invoke worker independently and return different results`() = testScope.runTest {
        // Given
        var callCount = 0
        val cache = SingleFlightCache<String, Int>(
            scope = testScope,
            worker = { key ->
                callCount++
                when (key) {
                    "keyA" -> 1
                    "keyB" -> 2
                    "keyC" -> 3
                    else -> -1
                }
            },
            keepFor = 1000.milliseconds,
            timeSource = fakeTimeSource
        )

        // When
        val dA = async { cache.get("keyA") }
        val dB = async { cache.get("keyB") }
        val dC = async { cache.get("keyC") }
        advanceUntilIdle()

        // Then — each key returns its own result
        assertEquals(1, dA.await())
        assertEquals(2, dB.await())
        assertEquals(3, dC.await())
        // worker invoked once per key, not shared across keys
        assertEquals(3, callCount)
    }

// -------------------------------------------------------------------------
// 13. Cancelling caller of one key does not affect in-flight worker of another key
// -------------------------------------------------------------------------

    @Test
    fun `get - cancelling caller of one key does not affect worker of another key`() = testScope.runTest {
        // Given
        val callerScopeA = CoroutineScope(testDispatcher + Job())
        var callCountA = 0
        var callCountB = 0

        val cache = SingleFlightCache<String, Int>(
            scope = backgroundScope,
            worker = { key ->
                delay(500)
                when (key) {
                    "keyA" -> {
                        callCountA++; 10
                    }

                    "keyB" -> {
                        callCountB++; 20
                    }

                    else -> -1
                }
            },
            keepFor = 1000.milliseconds,
            timeSource = fakeTimeSource
        )

        // When — launch both keys concurrently, cancel keyA's caller mid-flight
        val jobA = callerScopeA.launch { cache.get("keyA") }
        val dB = async { cache.get("keyB") }

        advanceTimeBy(100)       // both workers started, inside delay(500), not yet complete
        callerScopeA.cancel()    // cancel keyA's caller — keyB's worker must keep running
        advanceTimeBy(500)       // both workers complete (keyA's worker runs on backgroundScope)
        advanceUntilIdle()

        // Then — keyA caller was cancelled
        assertEquals(false, jobA.isActive)
        assertEquals(true, jobA.isCancelled)

        // keyB completed normally and independently
        assertEquals(20, dB.await())
        assertEquals(1, callCountB)  // keyB worker ran exactly once

        // keyA worker also completed on backgroundScope (caller cancel ≠ worker cancel)
        assertEquals(1, callCountA)

        // keyA result is now cached — a new caller gets it without re-invoking the worker
        val secondA = cache.get("keyA")
        advanceUntilIdle()

        assertEquals(10, secondA)
        assertEquals(1, callCountA)  // still 1 — served from cache, not re-fetched
    }
}