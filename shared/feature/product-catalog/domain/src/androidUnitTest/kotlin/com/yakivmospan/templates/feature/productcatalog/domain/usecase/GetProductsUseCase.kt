package com.yakivmospan.templates.feature.productcatalog.domain.usecase

import com.yakivmospan.templates.core.common.Result
import com.yakivmospan.templates.feature.productcatalog.domain.repository.ProductRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetProductsUseCaseTest : UseCaseTest() {

    private lateinit var repository: ProductRepository
    private lateinit var useCase: GetProductsUseCase

    override fun setupTest() {
        repository = mockk()
        useCase = GetProductsUseCase(repository)
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