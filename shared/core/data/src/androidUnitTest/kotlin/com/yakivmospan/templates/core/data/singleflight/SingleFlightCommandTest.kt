package com.yakivmospan.templates.core.data.singleflight

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class SingleFlightCommandTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // 1. Happy path
    // -------------------------------------------------------------------------

    @Test
    fun `given single call, when executor succeeds, then result is returned`() = testScope.runTest {
        // Given
        var callCount = 0
        val executor: suspend (String) -> Int = {
            callCount++; 42
        }

        val cache = SingleFlightCommand(testScope, executor)

        // When
        val result = cache.execute("key 1")

        // Then
        assertEquals(42, result)
        assertEquals(1, callCount)
    }

    @Test
    fun `given single call, when executor succeeds, then on success is invoked`() = testScope.runTest {
        // Given
        var callCount = 0
        val cache = SingleFlightCommand<String, Int>(testScope, executor = { -1 }, onSuccess = { callCount++ })

        // When
        cache.execute("key 1")

        // Then
        assertEquals(1, callCount)
    }

    @Test
    fun `given single call, when executor succeeds, then on success is invoked before broadcast`() = testScope.runTest {
        // Given
        val events = mutableListOf<String>()
        val command = SingleFlightCommand<String, Int>(
            scope = testScope,
            executor = { 42 },
            onSuccess = { events.add("onSuccess") }
        )

        // When
        val result = async {
            command.executionFlow("key 1")
                .onEach { events.add("broadcast") }
                .first()
        }

        result.await()

        // Then
        assertEquals(listOf("onSuccess", "broadcast"), events)
    }


    // -------------------------------------------------------------------------
    // 2. Single flight deduplication
    // -------------------------------------------------------------------------


    @Test
    fun `given multiple calls registered at the same time, then executor is invoked once and returns correct result`() = testScope.runTest {
        // Given
        var executorCallCount = 0
        val command = SingleFlightCommand<String, Int>(
            scope = testScope,
            executor = {
                delay(1000)
                executorCallCount++
                42
            }
        )

        // When
        val result1 = async { command.execute("key 1") }
        val result2 = async { command.execute("key 1") }
        val result3 = async { command.execute("key 1") }

        // Then
        assertEquals(42, result1.await())
        assertEquals(42, result2.await())
        assertEquals(42, result3.await())
        assertEquals(1, executorCallCount)
    }


    @Test
    fun `given two concurrent calls registered at the same time, for different keys, then executor is invoked twice`() = testScope.runTest {
        // Given
        var executorCallCount = 0
        val command = SingleFlightCommand<String, Int>(
            scope = testScope,
            executor = {
                executorCallCount++
                if (it == "key 1") 42 else if (it == "key 2") 43 else -1
            }
        )

        // When
        val result1 = async { command.execute("key 1") }
        val result2 = async { command.execute("key 2") }

        // Then
        assertEquals(42, result1.await())
        assertEquals(43, result2.await())
        assertEquals(2, executorCallCount)
    }

    @Test
    fun `given sequential calls, when first completed, then second call launches new executor`() = testScope.runTest {
        // Given
        var executorCallCount = 0
        val command = SingleFlightCommand<String, Int>(
            scope = testScope,
            executor = {
                executorCallCount++
                42
            }
        )

        // When
        val result1 = command.execute("key 1")
        val result2 = command.execute("key 1")

        // Then
        assertEquals(42, result1)
        assertEquals(42, result2)
        assertEquals(2, executorCallCount)
    }

    // -------------------------------------------------------------------------
    // 3. Error handling
    // -------------------------------------------------------------------------


    @Test
    fun `given single call, when executor throws, then exception is propagated to caller`() = testScope.runTest {
        // Given
        val command = SingleFlightCommand<String, Int>(
            scope = testScope,
            executor = {
                throw RuntimeException("Error message")
            }
        )

        // When / Then
        val exception = assertFailsWith<RuntimeException> {
            command.execute("key 1")
        }
        assertEquals("Error message", exception.message)
    }

    @Test
    fun `given single call, when executor throws, then on error is invoked`() = testScope.runTest {
        // Given
        var capturedException: Exception? = null
        val command = SingleFlightCommand<String, Int>(
            scope = testScope,
            executor = {
                throw RuntimeException("Error message")
            },
            onError = {
                capturedException = it
            }
        )

        // When
        assertFailsWith<RuntimeException> {
            command.execute("key 1")
        }

        // Then
        assertEquals("Error message", capturedException?.message)
    }

    @Test
    fun `given single call, when executor throws, then on error is invoked before broadcast`() = testScope.runTest {
        // Given
        val events = mutableListOf<String>()
        val command = SingleFlightCommand<String, Int>(
            scope = testScope,
            executor = {
                delay(100) // Ensure async execution
                throw RuntimeException("Error message")
            },
            onError = {
                delay(50) // Simulate some work in onError
                events.add("onError")
            }
        )

        var completedTasks = 0

        // When - start two concurrent calls that will share the same execution
        val result1 = async {
            runCatching {
                command.executionFlow("key 1").onCompletion { if (it == null) completedTasks++ }.collect { }
            }.onFailure { events.add("broadcast1") }
        }
        val result2 = async {
            runCatching {
                command.executionFlow("key 1").onCompletion { if (it == null) completedTasks++ }.collect { }
            }.onFailure { events.add("broadcast2") }
        }

        result1.await()
        result2.await()

        assertEquals(listOf("onError", "broadcast1", "broadcast2"), events)
        assertEquals(0, completedTasks)
    }

    @Test
    fun `given single call, when executor throws, then entry is removed from in flight`() = testScope.runTest {
        // Given
        var executorCallCount = 0
        val command = SingleFlightCommand<String, Int>(
            scope = testScope,
            executor = {
                executorCallCount++
                throw RuntimeException("Error message")
            }
        )

        // When - first call throws
        assertFailsWith<RuntimeException> {
            command.execute("key 1")
        }

        // Then - second call should also launch new executor (not blocked by in-flight entry)
        assertFailsWith<RuntimeException> {
            command.execute("key 1")
        }

        assertEquals(2, executorCallCount)
    }

    @Test
    fun `given single call, when executor throws, then next call launches new executor`() = testScope.runTest {
        // Given
        var executorCallCount = 0
        val command = SingleFlightCommand<String, Int>(
            scope = testScope,
            executor = {
                executorCallCount++
                if (executorCallCount == 1) {
                    throw RuntimeException("First attempt failed")
                } else {
                    42
                }
            }
        )

        // When - first call throws
        assertFailsWith<RuntimeException> {
            command.execute("key 1")
        }

        // Then - second call should succeed with a fresh executor
        val result = command.execute("key 1")

        assertEquals(42, result)
        assertEquals(2, executorCallCount)
    }

    @Test
    fun `given two concurrent calls, when executor throws, then both callers receive exception`() = testScope.runTest {
        // Given
        var executorCallCount = 0
        val command = SingleFlightCommand<String, Int>(
            scope = testScope,
            executor = {
                delay(1000)
                executorCallCount++
                throw RuntimeException("Error message")
            }
        )

        // When - two concurrent calls
        val result1 = async {
            runCatching { command.execute("key 1") }
        }
        val result2 = async {
            runCatching { command.execute("key 1") }
        }

        // Then - both receive the exception
        assertEquals(true, result1.await().isFailure)
        assertEquals(true, result2.await().isFailure)
        assertEquals("Error message", result1.await().exceptionOrNull()?.message)
        assertEquals("Error message", result2.await().exceptionOrNull()?.message)
        assertEquals(1, executorCallCount)
    }

    // -------------------------------------------------------------------------
    // 4. Scope and cancellation
    // -------------------------------------------------------------------------

    @Test
    fun `given active call, when caller scope is cancelled, then executor continues on app scope`() = testScope.runTest {
        // Given
        var executorStarted = false
        var executorCompleted = false
        val command = SingleFlightCommand<String, Int>(
            scope = backgroundScope,
            executor = {
                executorStarted = true
                delay(1000)
                executorCompleted = true
                42
            }
        )

        // When - start call and let executor begin
        val job = async {
            command.execute("key 1")
        }

        // Advance time to ensure executor starts
        testScheduler.runCurrent() // Process immediate work
        testScheduler.advanceTimeBy(100) // Give executor time to start

        // Verify executor has started before cancelling
        assertEquals(true, executorStarted, "Executor should have started")

        job.cancel() // Cancel the caller job

        // Advance time to let executor complete on backgroundScope
        testScheduler.advanceTimeBy(900) // Complete the remaining delay
        testScheduler.runCurrent() // Process completion

        // Then - executor should complete despite caller cancellation
        assertEquals(true, executorCompleted)
    }

    @Test
    fun `given active call, when caller scope is cancelled, then on success is still invoked`() = testScope.runTest {
        // Given
        var executorStarted = false
        var onSuccessInvoked = false
        val command = SingleFlightCommand<String, Int>(
            scope = backgroundScope,
            executor = {
                executorStarted = true
                delay(1000)
                42
            },
            onSuccess = {
                onSuccessInvoked = true
            }
        )

        // When - start call and cancel it
        val job = async {
            command.execute("key 1")
        }

        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(100)
        assertEquals(true, executorStarted, "Executor should have started")

        job.cancel()

        testScheduler.advanceTimeBy(900)
        testScheduler.runCurrent()

        // Then - onSuccess should be invoked despite caller cancellation
        assertEquals(true, onSuccessInvoked)
    }

    @Test
    fun `given active call, when caller scope is cancelled, then entry is removed from in flight after completion`() = testScope.runTest {
        // Given
        var executorStarted = false
        var executorCallCount = 0
        val command = SingleFlightCommand<String, Int>(
            scope = backgroundScope,
            executor = {
                executorStarted = true
                delay(1000)
                executorCallCount++
                42
            }
        )

        // When - start call and cancel it
        val job = async {
            command.execute("key 1")
        }

        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(100)
        assertEquals(true, executorStarted, "Executor should have started")

        job.cancel()

        testScheduler.advanceTimeBy(900)
        testScheduler.runCurrent()

        // Then - next call should launch a new executor (entry was cleaned up)
        val result = command.execute("key 1")
        assertEquals(42, result)
        assertEquals(2, executorCallCount)
    }

    @Test
    fun `given active call, when app scope is cancelled, then executor is cancelled`() = testScope.runTest {
        // Given
        var executorCompleted = false
        val appScope = TestScope(testDispatcher)
        val command = SingleFlightCommand<String, Int>(
            scope = appScope,
            executor = {
                delay(1000)
                executorCompleted = true
                42
            }
        )

        // When - start executor and cancel the app scope
        val job = async {
            runCatching { command.execute("key 1") }
        }

        testScope.testScheduler.advanceTimeBy(500) // Let executor start
        appScope.cancel() // Cancel the app scope
        testScope.testScheduler.advanceTimeBy(500) // Try to advance

        // Then - executor should not complete (it was cancelled)
        assertEquals(false, executorCompleted)
        assertEquals(true, job.await().isFailure)
    }

    // -------------------------------------------------------------------------
    // 5. executeFlow vs execute
    // -------------------------------------------------------------------------

    @Test
    fun `given execute and executionFlow, when called, provide same success outputs`() = testScope.runTest {
        // Given
        var onExecuteCallCounts = 0
        var onSuccessCallCount = 0
        val command = SingleFlightCommand<String, Int>(
            scope = testScope,
            executor = { onExecuteCallCounts++; 42 },
            onSuccess = { onSuccessCallCount++ }
        )

        // When
        val executeResult = command.execute("key 1")
        val flowResult = command.executionFlow("key 1").first()

        // Then
        assertEquals(executeResult, flowResult)
        assertEquals(2, onExecuteCallCounts)
        assertEquals(2, onSuccessCallCount)
    }

    @Test
    fun `given execute and executionFlow, when called, provide same error outputs`() = testScope.runTest {
        // Given
        var onExecuteCallCounts = 0
        var onErrorCallCount = 0
        val command = SingleFlightCommand<String, Int>(
            scope = testScope,
            executor = {
                onExecuteCallCounts++
                throw RuntimeException("Error message")
            },
            onError = { onErrorCallCount++ }
        )

        // Then
        assertFailsWith<RuntimeException> {
            command.execute("key 1")
        }

        assertFailsWith<RuntimeException> {
            command.executionFlow("key 1").first()
        }

        assertEquals(2, onExecuteCallCounts)
        assertEquals(2, onErrorCallCount)
    }
}