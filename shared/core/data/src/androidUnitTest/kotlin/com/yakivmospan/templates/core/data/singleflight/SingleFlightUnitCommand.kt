package com.yakivmospan.templates.core.data.singleflight

import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SingleFlightUnitCommandTest {

    // -------------------------------------------------------------------------
    // Infrastructure
    // -------------------------------------------------------------------------

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val executor: suspend () -> String = mockk()
    private val onSuccess: suspend (String) -> Unit = mockk(relaxed = true)
    private val onError: suspend (Exception) -> Unit = mockk(relaxed = true)

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        clearAllMocks()
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun buildCommand(
        useOnSuccess: Boolean = false,
        useOnError: Boolean = false,
    ) = SingleFlightUnitCommand(
        scope = testScope,
        executor = executor,
        onSuccess = if (useOnSuccess) onSuccess else null,
        onError = if (useOnError) onError else null,
    )

    private val successValue = "result"
    private val testException = RuntimeException("boom")

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    fun `when execute is called then returns value from executor`() = runTest {
        // Given
        coEvery { executor() } returns successValue
        val command = buildCommand()

        // When
        val result = command.execute()

        // Then
        kotlin.test.assertEquals(successValue, result)
    }

    @Test
    fun `when executionFlow is collected then emits single value and completes`() = runTest {
        // Given
        coEvery { executor() } returns successValue
        val command = buildCommand()
        val emissions = mutableListOf<String>()

        // When
        val job = launch { command.executionFlow().collect { emissions.add(it) } }
        advanceUntilIdle()
        job.join()

        // Then
        kotlin.test.assertEquals(listOf(successValue), emissions)
    }

    @Test
    fun `when onSuccess callback is provided then it is invoked with result`() = runTest {
        // Given
        coEvery { executor() } returns successValue
        val command = buildCommand(useOnSuccess = true)

        // When
        command.execute()

        // Then
        coVerify(exactly = 1) { onSuccess(successValue) }
    }

    // -------------------------------------------------------------------------
    // Error conditions
    // -------------------------------------------------------------------------

    @Test
    fun `when executor throws then execute rethrows exception`() = runTest {
        // Given
        coEvery { executor() } throws testException
        val command = buildCommand()

        // When
        val result = runCatching { command.execute() }

        // Then
        kotlin.test.assertEquals(testException, result.exceptionOrNull())
    }

    @Test
    fun `when executor throws then executionFlow collector receives exception`() = runTest {
        // Given
        coEvery { executor() } throws testException
        val command = buildCommand()
        var caughtError: Throwable? = null

        // When
        val job = launch {
            runCatching { command.executionFlow().collect {} }
                .exceptionOrNull()
                ?.let { caughtError = it }
        }
        advanceUntilIdle()
        job.join()

        // Then
        kotlin.test.assertEquals(testException, caughtError)
    }

    @Test
    fun `when executor throws then next executionFlow collection triggers fresh executor invocation`() = runTest {
        // Given
        coEvery { executor() } throws testException andThen successValue
        val command = buildCommand()

        // When
        val job1 = launch { runCatching { command.executionFlow().collect {} } }
        advanceUntilIdle()
        job1.join()
        val emissions = mutableListOf<String>()
        val job2 = launch { command.executionFlow().collect { emissions.add(it) } }
        advanceUntilIdle()
        job2.join()

        // Then
        coVerify(exactly = 2) { executor() }
        kotlin.test.assertEquals(listOf(successValue), emissions)
    }

    @Test
    fun `when executor throws then all concurrent callers receive exception`() = runTest {
        // Given
        coEvery { executor() } throws testException
        val command = buildCommand()

        // When
        val result1 = async { runCatching { command.execute() } }
        val result2 = async { runCatching { command.execute() } }
        val result3 = async { runCatching { command.execute() } }

        // Then
        kotlin.test.assertEquals(testException, result1.await().exceptionOrNull())
        kotlin.test.assertEquals(testException, result2.await().exceptionOrNull())
        kotlin.test.assertEquals(testException, result3.await().exceptionOrNull())
        coVerify(exactly = 1) { executor() }
    }

    @Test
    fun `when executor throws then onError callback is invoked`() = runTest {
        // Given
        coEvery { executor() } throws testException
        val command = buildCommand(useOnError = true)

        // When
        runCatching { command.execute() }

        // Then
        coVerify(exactly = 1) { onError(testException) }
    }

    @Test
    fun `when executor throws then next execute triggers fresh executor invocation`() = runTest {
        // Given
        coEvery { executor() } throws testException andThen successValue
        val command = buildCommand()

        // When
        runCatching { command.execute() }
        val result = command.execute()

        // Then
        coVerify(exactly = 2) { executor() }
        kotlin.test.assertEquals(successValue, result)
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    fun `when execute is called concurrently then executor is invoked only once`() = runTest {
        // Given
        coEvery { executor() } returns successValue
        val command = buildCommand()

        // When
        val job1 = launch { command.execute() }
        val job2 = launch { command.execute() }
        val job3 = launch { command.execute() }
        advanceUntilIdle()
        job1.join(); job2.join(); job3.join()

        // Then
        coVerify(exactly = 1) { executor() }
    }

    @Test
    fun `when executionFlow is collected concurrently then all collectors receive same value`() = runTest {
        // Given
        coEvery { executor() } returns successValue
        val command = buildCommand()
        val results = mutableListOf<String>()

        // When
        val job1 = launch { command.executionFlow().collect { results.add(it) } }
        val job2 = launch { command.executionFlow().collect { results.add(it) } }
        val job3 = launch { command.executionFlow().collect { results.add(it) } }
        advanceUntilIdle()
        job1.join(); job2.join(); job3.join()

        // Then
        kotlin.test.assertEquals(listOf(successValue, successValue, successValue), results)
        coVerify(exactly = 1) { executor() }
    }

    @Test
    fun `when first call fails and second call follows then second call succeeds independently`() = runTest {
        // Given
        coEvery { executor() } throws testException andThen successValue
        val command = buildCommand()

        // When
        val job = launch { runCatching { command.execute() } }
        advanceUntilIdle()
        job.join()
        val result = command.execute()

        // Then
        kotlin.test.assertEquals(successValue, result)
        coVerify(exactly = 2) { executor() }
    }

    @Test
    fun `when first executionFlow collection fails and second follows then second collection succeeds independently`() = runTest {
        // Given
        coEvery { executor() } throws testException andThen successValue
        val command = buildCommand()

        // When
        val job1 = launch { runCatching { command.executionFlow().collect {} } }
        advanceUntilIdle()
        job1.join()
        val emissions = mutableListOf<String>()
        val job2 = launch { command.executionFlow().collect { emissions.add(it) } }
        advanceUntilIdle()
        job2.join()

        // Then
        kotlin.test.assertEquals(listOf(successValue), emissions)
        coVerify(exactly = 2) { executor() }
    }

    @Test
    fun `when onSuccess throws then exception is propagated to caller`() = runTest {
        // Given
        coEvery { executor() } returns successValue
        coEvery { onSuccess(any()) } throws RuntimeException("callback error")
        val command = buildCommand(useOnSuccess = true)

        // When
        val result = runCatching { command.execute() }

        // Then
        kotlin.test.assertTrue(result.isFailure)
        coVerify(exactly = 1) { onSuccess(successValue) }
    }

    // -------------------------------------------------------------------------
    // Scope and cancellation
    // -------------------------------------------------------------------------

    @Test
    fun `when caller scope is cancelled then executor continues on app scope`() = runTest {
        // Given
        var executorCompleted = false
        val command = SingleFlightUnitCommand(
            scope = backgroundScope,
            executor = {
                delay(1000)
                executorCompleted = true
                successValue
            }
        )

        // When
        val job = async { runCatching { command.execute() } }
        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(100)
        job.cancel()
        testScheduler.advanceTimeBy(900)
        testScheduler.runCurrent()

        // Then
        kotlin.test.assertTrue(executorCompleted)
    }

    @Test
    fun `when caller scope is cancelled then onSuccess is still invoked`() = runTest {
        // Given
        var onSuccessInvoked = false
        val command = SingleFlightUnitCommand(
            scope = backgroundScope,
            executor = {
                delay(1000)
                successValue
            },
            onSuccess = { onSuccessInvoked = true }
        )

        // When
        val job = async { runCatching { command.execute() } }
        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(100)
        job.cancel()
        testScheduler.advanceTimeBy(900)
        testScheduler.runCurrent()

        // Then
        kotlin.test.assertTrue(onSuccessInvoked)
    }

    @Test
    fun `when caller scope is cancelled then entry is removed from in-flight after completion`() = runTest {
        // Given
        var executorCallCount = 0
        val command = SingleFlightUnitCommand(
            scope = backgroundScope,
            executor = {
                delay(1000)
                executorCallCount++
                successValue
            }
        )

        // When
        val job = async { runCatching { command.execute() } }
        testScheduler.runCurrent()
        testScheduler.advanceTimeBy(100)
        job.cancel()
        testScheduler.advanceTimeBy(900)
        testScheduler.runCurrent()
        val result = command.execute()

        // Then
        kotlin.test.assertEquals(successValue, result)
        kotlin.test.assertEquals(2, executorCallCount)
    }

    @Test
    fun `when app scope is cancelled then executor is cancelled`() = runTest {
        // Given
        var executorCompleted = false
        val appScope = TestScope(testDispatcher)
        val command = SingleFlightUnitCommand(
            scope = appScope,
            executor = {
                delay(1000)
                executorCompleted = true
                successValue
            }
        )

        // When
        val job = async { runCatching { command.execute() } }
        testScheduler.advanceTimeBy(500)
        appScope.cancel()
        testScheduler.advanceTimeBy(500)
        testScheduler.runCurrent()

        // Then
        kotlin.test.assertFalse(executorCompleted)
        kotlin.test.assertTrue(job.await().isFailure)
    }
}