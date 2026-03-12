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
class ToggleFavoriteUseCaseTest {

    private lateinit var repository: ProductRepository
    private lateinit var useCase: ToggleFavoriteUseCase

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        useCase = ToggleFavoriteUseCase(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
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