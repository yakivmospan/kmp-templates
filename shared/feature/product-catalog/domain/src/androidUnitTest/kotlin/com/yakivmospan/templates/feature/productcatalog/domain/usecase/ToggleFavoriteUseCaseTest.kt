package com.yakivmospan.templates.feature.productcatalog.domain.usecase

import com.yakivmospan.templates.core.common.Result
import com.yakivmospan.templates.feature.productcatalog.domain.repository.ProductRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ToggleFavoriteUseCaseTest : UseCaseTest() {

    private lateinit var repository: ProductRepository
    private lateinit var useCase: ToggleFavoriteUseCase

    override fun setupTest() {
        repository = mockk()
        useCase = ToggleFavoriteUseCase(repository)
    }

    @Test
    fun `invoke with non-favorite product should call addToFavorites`() = runTest {
        // Given
        val product = ProductTestFixtures.nonFavoriteProduct
        val params = ToggleFavoriteParams(product)
        val expectedResult = Result.Success(Unit)
        coEvery { repository.addToFavorites(product) } returns expectedResult

        // When
        val result = useCase(params)

        // Then
        assertEquals(expectedResult, result)
        coVerify(exactly = 1) { repository.addToFavorites(product) }
        coVerify(exactly = 0) { repository.removeFromFavorites(any()) }
    }

    @Test
    fun `invoke with favorite product should call removeFromFavorites`() = runTest {
        // Given
        val product = ProductTestFixtures.sampleProduct1
        val params = ToggleFavoriteParams(product)
        val expectedResult = Result.Success(Unit)
        coEvery { repository.removeFromFavorites(product.id) } returns expectedResult

        // When
        val result = useCase(params)

        // Then
        assertEquals(expectedResult, result)
        coVerify(exactly = 1) { repository.removeFromFavorites(product.id) }
        coVerify(exactly = 0) { repository.addToFavorites(any()) }
    }

    @Test
    fun `invoke should return error when addToFavorites fails`() = runTest {
        // Given
        val product = ProductTestFixtures.nonFavoriteProduct
        val params = ToggleFavoriteParams(product)
        val expectedResult = Result.Error(Exception("Failed to add to favorites"))
        coEvery { repository.addToFavorites(product) } returns expectedResult

        // When
        val result = useCase(params)

        // Then
        assertEquals(expectedResult, result)
        coVerify(exactly = 1) { repository.addToFavorites(product) }
    }

    @Test
    fun `invoke should return error when removeFromFavorites fails`() = runTest {
        // Given
        val product = ProductTestFixtures.sampleProduct2
        val params = ToggleFavoriteParams(product)
        val expectedResult = Result.Error(Exception("Failed to remove from favorites"))
        coEvery { repository.removeFromFavorites(product.id) } returns expectedResult

        // When
        val result = useCase(params)

        // Then
        assertEquals(expectedResult, result)
        coVerify(exactly = 1) { repository.removeFromFavorites(product.id) }
    }
}