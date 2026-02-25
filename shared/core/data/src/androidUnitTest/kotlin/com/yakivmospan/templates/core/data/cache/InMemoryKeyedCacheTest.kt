package com.yakivmospan.templates.core.data.cache

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryKeyedCacheTest {

    // -------------------------------------------------------------------------
    // Infrastructure
    // -------------------------------------------------------------------------

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private val expirationValidator = mockk<ExpirationValidator>()

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

    private fun cacheWithMockedValidator(): InMemoryKeyedCache<String, String> =
        InMemoryKeyedCache(validatorFactory = { expirationValidator })

    private fun mockExpired() {
        every { expirationValidator.isExpired() } returns true
        justRun { expirationValidator.refresh() }
    }

    private fun mockNotExpired() {
        every { expirationValidator.isExpired() } returns false
        justRun { expirationValidator.refresh() }
    }

    // -------------------------------------------------------------------------
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    fun `when cache is created then get returns null for any key`() = testScope.runTest {
        // Given
        mockNotExpired()
        val cache = cacheWithMockedValidator()

        // When
        val result1 = cache.get("key1")
        val result2 = cache.get("key2")

        // Then
        assertNull(result1)
        assertNull(result2)
    }

    @Test
    fun `when cache is created then getFlow emits null for any key`() = testScope.runTest {
        // Given
        mockNotExpired()
        val cache = cacheWithMockedValidator()

        // When
        val flowResult1 = cache.getFlow("key1").take(1).toList()
        val flowResult2 = cache.getFlow("key2").take(1).toList()

        // Then
        assertEquals(listOf(null), flowResult1)
        assertEquals(listOf(null), flowResult2)
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    fun `when put is called then get returns stored value for that key`() = testScope.runTest {
        // Given
        mockNotExpired()
        val cache = cacheWithMockedValidator()
        cache.put("key1", "value1")

        // When
        val result = cache.get("key1")

        // Then
        assertEquals("value1", result)
    }

    @Test
    fun `when put is called then getFlow emits stored value for that key`() = testScope.runTest {
        // Given
        mockNotExpired()
        val cache = cacheWithMockedValidator()
        val results = mutableListOf<String?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            cache.getFlow("key1").take(2).toList(results)
        }

        // When
        cache.put("key1", "value1")

        // Then
        assertEquals(listOf(null, "value1"), results)
    }

    @Test
    fun `when put is called for multiple keys then get returns correct value per key`() = testScope.runTest {
        // Given
        mockNotExpired()
        val cache = cacheWithMockedValidator()
        cache.put("key1", "value1")
        cache.put("key2", "value2")

        // When
        val result1 = cache.get("key1")
        val result2 = cache.get("key2")

        // Then
        assertEquals("value1", result1)
        assertEquals("value2", result2)
    }

    @Test
    fun `when put is called for multiple keys then getFlow emits correct value per key`() = testScope.runTest {
        // Given
        mockNotExpired()
        val cache = cacheWithMockedValidator()
        val results1 = mutableListOf<String?>()
        val results2 = mutableListOf<String?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            cache.getFlow("key1").take(2).toList(results1)
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            cache.getFlow("key2").take(2).toList(results2)
        }

        // When
        cache.put("key1", "value1")
        cache.put("key2", "value2")

        // Then
        assertEquals(listOf(null, "value1"), results1)
        assertEquals(listOf(null, "value2"), results2)
    }

    @Test
    fun `when put is called multiple times for same key then get returns the last value`() = testScope.runTest {
        // Given
        mockNotExpired()
        val cache = cacheWithMockedValidator()
        cache.put("key1", "value1")
        cache.put("key1", "value2")

        // When
        val result = cache.get("key1")

        // Then
        assertEquals("value2", result)
    }

    @Test
    fun `when put is called multiple times for same key then getFlow emits each update`() = testScope.runTest {
        // Given
        mockNotExpired()
        val cache = cacheWithMockedValidator()
        val results = mutableListOf<String?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            cache.getFlow("key1").take(3).toList(results)
        }

        // When
        cache.put("key1", "value1")
        cache.put("key1", "value2")

        // Then
        assertEquals(listOf(null, "value1", "value2"), results)
    }

    // -------------------------------------------------------------------------
    // Clear (per-key)
    // -------------------------------------------------------------------------

    @Test
    fun `when clear is called for a key then get returns null for that key`() = testScope.runTest {
        // Given
        mockNotExpired()
        val cache = cacheWithMockedValidator()
        cache.put("key1", "value1")
        cache.clear("key1")

        // When
        val result = cache.get("key1")

        // Then
        assertNull(result)
    }

    @Test
    fun `when clear is called for a key then getFlow emits null for that key`() = testScope.runTest {
        // Given
        mockNotExpired()
        val cache = cacheWithMockedValidator()
        val results = mutableListOf<String?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            cache.getFlow("key1").take(3).toList(results)
        }

        // When
        cache.put("key1", "value1")
        cache.clear("key1")

        // Then
        assertEquals(listOf(null, "value1", null), results)
    }

    @Test
    fun `when clear is called for a key then other keys are not affected`() = testScope.runTest {
        // Given
        mockNotExpired()
        val cache = cacheWithMockedValidator()
        cache.put("key1", "value1")
        cache.put("key2", "value2")
        cache.clear("key1")

        // When
        val result1 = cache.get("key1")
        val result2 = cache.get("key2")

        // Then
        assertNull(result1)
        assertEquals("value2", result2)
    }

    @Test
    fun `when clear is called on empty key then put still stores value`() = testScope.runTest {
        // Given
        mockNotExpired()
        val cache = cacheWithMockedValidator()
        cache.clear("key1")
        cache.put("key1", "value1")

        // When
        val result = cache.get("key1")

        // Then
        assertEquals("value1", result)
    }

    // -------------------------------------------------------------------------
    // ClearAll
    // -------------------------------------------------------------------------

    @Test
    fun `when clearAll is called then get returns null for all keys`() = testScope.runTest {
        // Given
        mockNotExpired()
        val cache = cacheWithMockedValidator()
        cache.put("key1", "value1")
        cache.put("key2", "value2")
        cache.clearAll()

        // When
        val result1 = cache.get("key1")
        val result2 = cache.get("key2")

        // Then
        assertNull(result1)
        assertNull(result2)
    }

    @Test
    fun `when clearAll is called then getFlow emits null for all keys`() = testScope.runTest {
        // Given
        mockNotExpired()
        val cache = cacheWithMockedValidator()
        val results1 = mutableListOf<String?>()
        val results2 = mutableListOf<String?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            cache.getFlow("key1").take(3).toList(results1)
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            cache.getFlow("key2").take(3).toList(results2)
        }

        // When
        cache.put("key1", "value1")
        cache.put("key2", "value2")
        cache.clearAll()

        // Then
        assertEquals(listOf(null, "value1", null), results1)
        assertEquals(listOf(null, "value2", null), results2)
    }

    // -------------------------------------------------------------------------
    // Expiration
    // -------------------------------------------------------------------------


    @Test
    fun `when data is expired then get returns null`() = testScope.runTest {
        // Given
        mockExpired()
        val cache = cacheWithMockedValidator()
        cache.put("key1", "value1")

        // When
        val result = cache.get("key1")

        // Then
        assertNull(result)
    }

    @Test
    fun `when data is expired then getFlow emits null`() = testScope.runTest {
        // Given
        mockExpired()
        val cache = cacheWithMockedValidator()
        val results = mutableListOf<String?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            cache.getFlow("key1").take(2).toList(results)
        }

        // When
        cache.put("key1", "value1")

        // Then
        assertEquals(listOf<String?>(null, null), results)
    }

    @Test
    fun `when cache is expired and put is called with new data then get returns the new value`() = testScope.runTest {
        // Given
        mockExpired()
        val cache = cacheWithMockedValidator()
        cache.put("key1", "value1")

        // When
        mockNotExpired()
        cache.put("key1", "value2")
        val result = cache.get("key1")

        // Then
        assertEquals("value2", result)
    }

    @Test
    fun `when put is called then refresh is invoked on expiration validator`() = testScope.runTest {
        // Given
        mockNotExpired()
        val cache = cacheWithMockedValidator()

        // When
        cache.put("key1", "value1")

        // Then
        verify { expirationValidator.refresh() }
    }

    @Test
    fun `when different keys have independent expiration then each key expires independently`() = testScope.runTest {
        // Given
        mockNotExpired()
        val cache = cacheWithMockedValidator()
        cache.put("key1", "value1")
        cache.put("key2", "value2")

        // When
        mockExpired()
        val result1BeforeExpiration = cache.get("key1")
        val result2BeforeExpiration = cache.get("key2")
        mockNotExpired()
        cache.put("key1", "value1-updated")
        val result1AfterExpiration = cache.get("key1")
        val result2AfterExpiration = cache.get("key2")

        // Then
        assertNull(result1BeforeExpiration)
        assertNull(result2BeforeExpiration)
        assertEquals("value1-updated", result1AfterExpiration)
        assertNull(result2AfterExpiration)
    }
}