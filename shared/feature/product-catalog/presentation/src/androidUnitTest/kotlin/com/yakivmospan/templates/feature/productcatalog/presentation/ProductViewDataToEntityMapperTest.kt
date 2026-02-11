package com.yakivmospan.templates.feature.productcatalog.presentation

import com.yakivmospan.templates.feature.productcatalog.presentation.ProductTestFixtures.createProductViewData
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for ProductViewDataToEntityMapper.
 * Verifies correct mapping from presentation ProductViewData to domain Product.
 */
class ProductViewDataToEntityMapperTest {

    private val mapper = ProductViewDataToEntityMapper()

    @Test
    fun `maps all ProductViewData fields correctly to Product`() {
        // Given
        val productViewData = createProductViewData(
            id = 99,
            title = "Tablet",
            description = "Premium tablet device",
            price = 599.99,
            formattedPrice = "$599.99",
            imageUrl = "https://example.com/tablet.jpg",
            isFavorite = true
        )

        // When
        val result = mapper.map(productViewData)

        // Then
        assertEquals(99, result.id)
        assertEquals("Tablet", result.title)
        assertEquals("Premium tablet device", result.description)
        assertEquals(599.99, result.price)
        assertEquals("https://example.com/tablet.jpg", result.imageUrl)
        assertEquals(true, result.isFavorite)
    }

    @Test
    fun `preserves data through round-trip conversion`() {
        // Given
        val toViewDataMapper = ProductToViewDataMapper()
        val toEntityMapper = ProductViewDataToEntityMapper()
        val originalProduct = ProductTestFixtures.createProduct(
            id = 123,
            title = "Camera",
            description = "Professional DSLR camera",
            price = 1299.50,
            imageUrl = "https://example.com/camera.jpg",
            isFavorite = false
        )

        // When
        val viewData = toViewDataMapper.map(originalProduct)
        val result = toEntityMapper.map(viewData)

        // Then
        assertEquals(originalProduct.id, result.id)
        assertEquals(originalProduct.title, result.title)
        assertEquals(originalProduct.description, result.description)
        assertEquals(originalProduct.price, result.price)
        assertEquals(originalProduct.imageUrl, result.imageUrl)
        assertEquals(originalProduct.isFavorite, result.isFavorite)
    }

    @Test
    fun `maps isFavorite flag correctly`() {
        // Given
        val favoriteViewData = createProductViewData(isFavorite = true)
        val notFavoriteViewData = createProductViewData(isFavorite = false)

        // When
        val favoriteResult = mapper.map(favoriteViewData)
        val notFavoriteResult = mapper.map(notFavoriteViewData)

        // Then
        assertEquals(true, favoriteResult.isFavorite)
        assertEquals(false, notFavoriteResult.isFavorite)
    }
}