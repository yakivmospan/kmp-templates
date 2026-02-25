package com.yakivmospan.templates.core.data.cache

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
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
class InMemoryCacheTest {

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
    // Initial state
    // -------------------------------------------------------------------------

    @Test
    fun `when cache is created then get returns null`() = testScope.runTest {
        // Given
        val cache = InMemoryCache<String>()

        // When
        val result = cache.get()

        // Then
        assertNull(result)
    }

    @Test
    fun `when cache is created then getFlow emits null`() = testScope.runTest {
        // Given
        val cache = InMemoryCache<String>()

        // When
        val result = cache.getFlow().first()

        // Then
        assertNull(result)
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    fun `when put is called then get returns stored value`() = testScope.runTest {
        // Given
        val cache = InMemoryCache<String>()

        // When
        cache.put("hello")
        val result = cache.get()

        // Then
        assertEquals("hello", result)
    }

    @Test
    fun `when put is called then getFlow emits stored value`() = testScope.runTest {
        // Given
        val cache = InMemoryCache<String>()

        // When
        cache.put("hello")
        val result = cache.getFlow().first()

        // Then
        assertEquals("hello", result)
    }

    @Test
    fun `when put is called multiple times then get returns the last value`() = testScope.runTest {
        // Given
        val cache = InMemoryCache<String>()

        // When
        cache.put("first")
        cache.put("second")
        cache.put("third")
        val result = cache.get()

        // Then
        assertEquals("third", result)
    }

    @Test
    fun `when put is called multiple times then getFlow emits each update`() = testScope.runTest {
        // Given
        val cache = InMemoryCache<String>()

        // When
        val results = mutableListOf<String?>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            cache.getFlow().take(4).toList(results)
        }
        cache.put("first")
        cache.put("second")
        cache.put("third")

        // Then
        assertEquals(listOf(null, "first", "second", "third"), results)
    }

    // -------------------------------------------------------------------------
    // Clear
    // -------------------------------------------------------------------------

    @Test
    fun `when clear is called then get returns null`() = testScope.runTest {
        // Given
        val cache = InMemoryCache<String>()
        cache.put("hello")

        // When
        cache.clear()
        val result = cache.get()

        // Then
        assertNull(result)
    }

    @Test
    fun `when clear is called then getFlow emits null`() = testScope.runTest {
        // Given
        val cache = InMemoryCache<String>()
        cache.put("hello")

        // When
        cache.clear()
        val result = cache.getFlow().first()

        // Then
        assertNull(result)
    }

    @Test
    fun `when clear is called before put then get returns null`() = testScope.runTest {
        // Given
        val cache = InMemoryCache<String>()

        // When
        cache.clear()
        val result = cache.get()

        // Then
        assertNull(result)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun mockExpired() {
        every { expirationValidator.isExpired() } returns true
        justRun { expirationValidator.refresh() }
    }

    private fun mockNotExpired() {
        every { expirationValidator.isExpired() } returns false
        justRun { expirationValidator.refresh() }
    }

    // -------------------------------------------------------------------------
    // Expiration
    // -------------------------------------------------------------------------

    @Test
    fun `when data is not expired then get returns the value`() = testScope.runTest {
        // Given
        mockNotExpired()
        val cache = InMemoryCache<String>(expirationValidator)

        // When
        cache.put("hello")
        val result = cache.get()

        // Then
        assertEquals("hello", result)
    }

    @Test
    fun `when data is not expired then getFlow emits the value`() = testScope.runTest {
        // Given
        mockNotExpired()
        val cache = InMemoryCache<String>(expirationValidator)

        // When
        cache.put("hello")
        val result = cache.getFlow().first()

        // Then
        assertEquals("hello", result)
    }

    @Test
    fun `when data is expired then get returns null`() = testScope.runTest {
        // Given
        mockExpired()
        val cache = InMemoryCache<String>(expirationValidator)

        // When
        cache.put("hello")
        val result = cache.get()

        // Then
        assertNull(result)
    }

    @Test
    fun `when data is expired then getFlow emits null`() = testScope.runTest {
        // Given
        mockExpired()
        val cache = InMemoryCache<String>(expirationValidator)

        // When
        cache.put("hello")
        val result = cache.getFlow().first()

        // Then
        assertNull(result)
    }

    @Test
    fun `when cache has infinite expiration then get never expires`() = testScope.runTest {
        // Given
        mockNotExpired()
        val cache = InMemoryCache<String>(expirationValidator)

        // When
        cache.put("hello")
        val first = cache.get()
        val second = cache.get()
        val third = cache.get()

        // Then
        assertEquals("hello", first)
        assertEquals("hello", second)
        assertEquals("hello", third)
    }

    @Test
    fun `when cache is expired and put is called with new data then get returns the new value`() = testScope.runTest {
        // Given
        mockExpired()
        val cache = InMemoryCache<String>(expirationValidator)
        cache.put("old")

        // When
        mockNotExpired()
        cache.put("fresh")
        val result = cache.get()

        // Then
        assertEquals("fresh", result)
    }

    @Test
    fun `when put is called then refresh is invoked on expiration validator`() = testScope.runTest {
        // Given
        mockNotExpired()
        val cache = InMemoryCache<String>(expirationValidator)

        // When
        cache.put("hello")

        // Then
        verify(exactly = 1) { expirationValidator.refresh() }
    }
}


