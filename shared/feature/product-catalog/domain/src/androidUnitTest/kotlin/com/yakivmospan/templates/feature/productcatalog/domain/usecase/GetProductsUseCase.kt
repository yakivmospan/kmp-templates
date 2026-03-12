package com.yakivmospan.templates.feature.productcatalog.domain.usecase

import com.yakivmospan.templates.core.common.Result
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
class GetProductsUseCaseTest {

    private lateinit var repository: ProductRepository
    private lateinit var useCase: GetProductsUseCase

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        useCase = GetProductsUseCase(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `invoke should call repository with correct page request and return result`() = runTest {
        // Given
        val pageRequest = ProductTestFixtures.firstPageRequest
        val expectedResult = Result.Success(ProductTestFixtures.firstPagePaginatedData)
        coEvery { repository.getProducts(pageRequest) } returns expectedResult

        // When
        val result = useCase(pageRequest)

        // Then
        assertEquals(expectedResult, result)
        coVerify(exactly = 1) { repository.getProducts(pageRequest) }
    }

    @Test
    fun `invoke should return error when repository returns error`() = runTest {
        // Given
        val pageRequest = ProductTestFixtures.firstPageRequest
        val expectedResult = Result.Error(Exception("Network error"))
        coEvery { repository.getProducts(pageRequest) } returns expectedResult

        // When
        val result = useCase(pageRequest)

        // Then
        assertEquals(expectedResult, result)
        coVerify(exactly = 1) { repository.getProducts(pageRequest) }
    }
}