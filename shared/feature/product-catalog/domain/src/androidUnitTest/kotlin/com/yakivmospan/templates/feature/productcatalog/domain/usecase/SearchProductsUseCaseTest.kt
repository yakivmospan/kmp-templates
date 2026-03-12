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
class SearchProductsUseCaseTest {

    private lateinit var repository: ProductRepository
    private lateinit var useCase: SearchProductsUseCase

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        useCase = SearchProductsUseCase(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `invoke should call repository with correct query and page request and return result`() = runTest {
        // Given
        val query = ProductTestFixtures.SEARCH_QUERY_SMARTPHONE
        val pageRequest = ProductTestFixtures.firstPageRequest
        val params = SearchProductsParams(query, pageRequest)
        val expectedResult = Result.Success(ProductTestFixtures.firstPagePaginatedData)
        coEvery { repository.searchProducts(query, pageRequest) } returns expectedResult

        // When
        val result = useCase(params)

        // Then
        assertEquals(expectedResult, result)
        coVerify(exactly = 1) { repository.searchProducts(query, pageRequest) }
    }

    @Test
    fun `invoke should return error when repository returns error`() = runTest {
        // Given
        val query = ProductTestFixtures.SEARCH_QUERY_LAPTOP
        val pageRequest = ProductTestFixtures.firstPageRequest
        val params = SearchProductsParams(query, pageRequest)
        val expectedResult = Result.Error(Exception("Search failed"))
        coEvery { repository.searchProducts(query, pageRequest) } returns expectedResult

        // When
        val result = useCase(params)

        // Then
        assertEquals(expectedResult, result)
        coVerify(exactly = 1) { repository.searchProducts(query, pageRequest) }
    }

    @Test
    fun `invoke with empty query should call repository with empty string`() = runTest {
        // Given
        val query = ProductTestFixtures.SEARCH_QUERY_EMPTY
        val pageRequest = ProductTestFixtures.firstPageRequest
        val params = SearchProductsParams(query, pageRequest)
        val expectedResult = Result.Success(ProductTestFixtures.firstPagePaginatedData)
        coEvery { repository.searchProducts(query, pageRequest) } returns expectedResult

        // When
        val result = useCase(params)

        // Then
        assertEquals(expectedResult, result)
        coVerify(exactly = 1) { repository.searchProducts(query, pageRequest) }
    }
}