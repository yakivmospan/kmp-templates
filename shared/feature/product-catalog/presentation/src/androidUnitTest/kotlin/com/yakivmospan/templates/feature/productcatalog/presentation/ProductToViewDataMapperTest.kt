package com.yakivmospan.templates.feature.productcatalog.presentation

import com.yakivmospan.templates.feature.productcatalog.presentation.ProductTestFixtures.createProduct
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for ProductToViewDataMapper.
 * Verifies correct mapping from domain Product to presentation ProductViewData.
 */
class ProductToViewDataMapperTest {

    private val mapper = ProductToViewDataMapper()

    @Test
    fun `maps all Product fields correctly to ProductViewData`() {
        // Given
        val product = createProduct(
            id = 42,
            title = "Gaming Console",
            description = "Next-gen gaming console",
            price = 499.99,
            imageUrl = "https://example.com/console.jpg",
            isFavorite = true
        )

        // When
        val result = mapper.map(product)

        // Then
        assertEquals(42, result.id)
        assertEquals("Gaming Console", result.title)
        assertEquals("Next-gen gaming console", result.description)
        assertEquals(499.99, result.price)
        assertEquals("https://example.com/console.jpg", result.imageUrl)
        assertEquals(true, result.isFavorite)
        assertEquals("$499.99", result.formattedPrice)
    }

    @Test
    fun `formats price with two decimal places`() {
        // Given
        val product = createProduct(price = 19.99)

        // When
        val result = mapper.map(product)

        // Then
        assertEquals("$19.99", result.formattedPrice)
    }

    @Test
    fun `formats zero price correctly`() {
        // Given
        val product = createProduct(price = 0.0)

        // When
        val result = mapper.map(product)

        // Then
        assertEquals("$0.00", result.formattedPrice)
    }

    @Test
    fun `formats large price values correctly`() {
        // Given
        val product = createProduct(price = 9999.99)

        // When
        val result = mapper.map(product)

        // Then
        assertEquals("$9999.99", result.formattedPrice)
    }

    @Test
    fun `formats price with multiple decimal places by rounding down`() {
        // Given
        val product = createProduct(price = 19.999)

        // When
        val result = mapper.map(product)

        // Then
        assertEquals("$19.99", result.formattedPrice)
    }

    @Test
    fun `formats price with single decimal place by padding`() {
        // Given
        val product = createProduct(price = 10.5)

        // When
        val result = mapper.map(product)

        // Then
        assertEquals("$10.50", result.formattedPrice)
    }

    @Test
    fun `formats whole number price with two decimal zeros`() {
        // Given
        val product = createProduct(price = 100.0)

        // When
        val result = mapper.map(product)

        // Then
        assertEquals("$100.00", result.formattedPrice)
    }

    @Test
    fun `maps isFavorite true correctly`() {
        // Given
        val product = createProduct(isFavorite = true)

        // When
        val result = mapper.map(product)

        // Then
        assertEquals(true, result.isFavorite)
    }

    @Test
    fun `maps isFavorite false correctly`() {
        // Given
        val product = createProduct(isFavorite = false)

        // When
        val result = mapper.map(product)

        // Then
        assertEquals(false, result.isFavorite)
    }
}