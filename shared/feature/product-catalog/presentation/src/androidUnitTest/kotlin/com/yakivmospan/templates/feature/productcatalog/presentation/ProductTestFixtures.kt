package com.yakivmospan.templates.feature.productcatalog.presentation

import com.yakivmospan.templates.feature.productcatalog.domain.model.Product

/**
 * Test fixtures for Product domain models and view data.
 * Provides consistent test data across all test files.
 */
object ProductTestFixtures {

    fun createProduct(
        id: Int = 1,
        title: String = "Test Product",
        description: String = "Test Description",
        price: Double = 99.99,
        imageUrl: String = "https://example.com/image.jpg",
        isFavorite: Boolean = false
    ) = Product(
        id = id,
        title = title,
        description = description,
        price = price,
        imageUrl = imageUrl,
        isFavorite = isFavorite
    )

    fun createProductViewData(
        id: Int = 1,
        title: String = "Test Product",
        description: String = "Test Description",
        price: Double = 99.99,
        formattedPrice: String = "99.99",
        imageUrl: String = "https://example.com/image.jpg",
        isFavorite: Boolean = false
    ) = ProductViewData(
        id = id,
        title = title,
        description = description,
        price = price,
        formattedPrice = formattedPrice,
        imageUrl = imageUrl,
        isFavorite = isFavorite
    )

    // Common test products
    val sampleProduct1 = createProduct(
        id = 1,
        title = "Smartphone",
        description = "Latest smartphone with amazing features",
        price = 699.99,
        isFavorite = true
    )

    val sampleProduct2 = createProduct(
        id = 2,
        title = "Laptop",
        description = "High-performance laptop for professionals",
        price = 1299.50,
        isFavorite = true
    )

    val sampleProduct3 = createProduct(
        id = 3,
        title = "Headphones",
        description = "Noise-cancelling wireless headphones",
        price = 249.99,
        isFavorite = true
    )

    val sampleProducts = listOf(sampleProduct1, sampleProduct2, sampleProduct3)
}