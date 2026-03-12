package com.yakivmospan.templates.feature.productcatalog.domain.usecase

import com.yakivmospan.templates.feature.productcatalog.domain.model.Product
import com.yakivmospan.templates.feature.productcatalog.domain.repository.ProductRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveFavoritesUseCaseTest() {

    private lateinit var repository: ProductRepository
    private lateinit var useCase: ObserveFavoritesUseCase

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        useCase = ObserveFavoritesUseCase(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }


    @Test
    fun `invoke should return flow from repository`() = runTest {
        // Given
        val expectedFavorites = ProductTestFixtures.favoriteProducts
        val expectedFlow = flowOf(expectedFavorites)
        every { repository.observeFavorites() } returns expectedFlow

        // When
        val resultFlow = useCase()
        val result = resultFlow.first()

        // Then
        assertEquals(expectedFavorites, result)
        verify(exactly = 1) { repository.observeFavorites() }
    }

    @Test
    fun `invoke should return empty list when no favorites exist`() = runTest {
        // Given
        val expectedFlow = flowOf(emptyList<Product>())
        every { repository.observeFavorites() } returns expectedFlow

        // When
        val resultFlow = useCase()
        val result = resultFlow.first()

        // Then
        assertEquals(emptyList(), result)
        verify(exactly = 1) { repository.observeFavorites() }
    }
}