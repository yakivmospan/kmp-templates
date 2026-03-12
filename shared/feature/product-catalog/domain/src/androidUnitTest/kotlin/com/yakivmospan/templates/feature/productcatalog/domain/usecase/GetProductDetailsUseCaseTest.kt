package com.yakivmospan.templates.feature.productcatalog.domain.usecase

import com.yakivmospan.templates.core.common.Result
import com.yakivmospan.templates.core.domain.UpdateStrategy
import com.yakivmospan.templates.feature.productcatalog.domain.repository.ProductRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class GetProductDetailsUseCaseTest() {

    private lateinit var repository: ProductRepository
    private lateinit var useCase: GetProductDetailsUseCase

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        useCase = GetProductDetailsUseCase(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------------------
    // Happy path
    // -------------------------------------------------------------------------

    @Test
    fun `when useCache is true then repository is called with TRY_CACHED_ELSE_FETCH strategy`() = runTest {
        // Given
        val params = GetProductDetailsParams(id = 1, useCache = true)
        val expectedResult = Result.Success(ProductTestFixtures.sampleProduct1)
        coEvery { repository.getProductById(params.id, UpdateStrategy.TRY_CACHED_ELSE_FETCH) } returns expectedResult

        // When
        val result = useCase(params)

        // Then
        assertEquals(expectedResult, result)
        coVerify(exactly = 1) { repository.getProductById(params.id, UpdateStrategy.TRY_CACHED_ELSE_FETCH) }
    }

    @Test
    fun `when useCache is false then repository is called with TRY_FETCH_ELSE_CACHED strategy`() = runTest {
        // Given
        val params = GetProductDetailsParams(id = 1, useCache = false)
        val expectedResult = Result.Success(ProductTestFixtures.sampleProduct1)
        coEvery { repository.getProductById(params.id, UpdateStrategy.TRY_FETCH_ELSE_CACHED) } returns expectedResult

        // When
        val result = useCase(params)

        // Then
        assertEquals(expectedResult, result)
        coVerify(exactly = 1) { repository.getProductById(params.id, UpdateStrategy.TRY_FETCH_ELSE_CACHED) }
    }

    // -------------------------------------------------------------------------
    // Error path
    // -------------------------------------------------------------------------

    @Test
    fun `when repository returns error then result is propagated`() = runTest {
        // Given
        val params = GetProductDetailsParams(id = 999, useCache = true)
        val expectedResult = Result.Error(Exception("Product not found"))
        coEvery { repository.getProductById(params.id, UpdateStrategy.TRY_CACHED_ELSE_FETCH) } returns expectedResult

        // When
        val result = useCase(params)

        // Then
        assertEquals(expectedResult, result)
        coVerify(exactly = 1) { repository.getProductById(params.id, UpdateStrategy.TRY_CACHED_ELSE_FETCH) }
    }
}