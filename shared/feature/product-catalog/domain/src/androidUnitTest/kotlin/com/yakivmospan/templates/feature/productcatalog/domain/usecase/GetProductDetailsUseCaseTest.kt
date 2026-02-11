package com.yakivmospan.templates.feature.productcatalog.domain.usecase

import com.yakivmospan.templates.core.common.Result
import com.yakivmospan.templates.feature.productcatalog.domain.repository.ProductRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetProductDetailsUseCaseTest : UseCaseTest() {

    private lateinit var repository: ProductRepository
    private lateinit var useCase: GetProductDetailsUseCase

    override fun setupTest() {
        repository = mockk()
        useCase = GetProductDetailsUseCase(repository)
    }

    @Test
    fun `invoke should call repository with correct product id and return result`() = runTest {
        // Given
        val productId = 1
        val expectedResult = Result.Success(ProductTestFixtures.sampleProduct1)
        coEvery { repository.getProductById(productId) } returns expectedResult

        // When
        val result = useCase(productId)

        // Then
        assertEquals(expectedResult, result)
        coVerify(exactly = 1) { repository.getProductById(productId) }
    }

    @Test
    fun `invoke should return error when repository returns error`() = runTest {
        // Given
        val productId = 999
        val expectedResult = Result.Error(Exception("Product not found"))
        coEvery { repository.getProductById(productId) } returns expectedResult

        // When
        val result = useCase(productId)

        // Then
        assertEquals(expectedResult, result)
        coVerify(exactly = 1) { repository.getProductById(productId) }
    }
}