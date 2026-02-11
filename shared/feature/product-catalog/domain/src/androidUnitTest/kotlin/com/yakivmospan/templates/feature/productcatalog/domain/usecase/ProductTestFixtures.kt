package com.yakivmospan.templates.feature.productcatalog.domain.usecase

import com.yakivmospan.templates.core.common.PageRequest
import com.yakivmospan.templates.core.common.PaginatedData
import com.yakivmospan.templates.core.common.Result
import com.yakivmospan.templates.core.common.SortDirection
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product

/**
 * Test fixtures for Product domain models and related test data.
 * Provides consistent test data across all use case test files.
 */
object ProductTestFixtures {

    // Product Factory
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

    // Additional test products for various scenarios
    val nonFavoriteProduct = createProduct(
        id = 4,
        title = "Smart Watch",
        description = "Feature-rich smartwatch with health tracking",
        price = 399.99,
        isFavorite = false
    )

    val tabletProduct = createProduct(
        id = 5,
        title = "Tablet Ultra",
        description = "Powerful tablet for work and entertainment",
        price = 799.99,
        isFavorite = false
    )

    val allProducts = listOf(
        sampleProduct1,
        sampleProduct2,
        sampleProduct3,
        nonFavoriteProduct,
        tabletProduct
    )

    val favoriteProducts = sampleProducts.filter { it.isFavorite }

    // Single product for detail tests
    val singleProduct = sampleProduct1

    // Page Requests
    val firstPageRequest = PageRequest(
        page = 1,
        pageSize = 10,
        sortBy = "title",
        sortDirection = SortDirection.ASC
    )

    val secondPageRequest = PageRequest(
        page = 2,
        pageSize = 10,
        sortBy = "title",
        sortDirection = SortDirection.ASC
    )

    val largePageRequest = PageRequest(
        page = 1,
        pageSize = 100,
        sortBy = null,
        sortDirection = SortDirection.ASC
    )

    val descendingSortRequest = PageRequest(
        page = 1,
        pageSize = 10,
        sortBy = "price",
        sortDirection = SortDirection.DESC
    )

    // Paginated Data Factory
    fun createPaginatedData(
        items: List<Product> = sampleProducts,
        currentPage: Int = 1,
        pageSize: Int = 10,
        totalItems: Int = items.size
    ): PaginatedData<Product> {
        val totalPages = if (totalItems == 0) 0 else (totalItems + pageSize - 1) / pageSize
        return PaginatedData(
            items = items,
            currentPage = currentPage,
            totalPages = totalPages,
            totalItems = totalItems,
            hasNextPage = currentPage < totalPages,
            hasPreviousPage = currentPage > 1
        )
    }

    val emptyPaginatedData = PaginatedData<Product>(
        items = emptyList(),
        currentPage = 1,
        totalPages = 0,
        totalItems = 0,
        hasNextPage = false,
        hasPreviousPage = false
    )

    val firstPagePaginatedData = createPaginatedData(
        items = sampleProducts,
        currentPage = 1,
        pageSize = 10,
        totalItems = 25
    )

    val secondPagePaginatedData = createPaginatedData(
        items = listOf(nonFavoriteProduct, tabletProduct),
        currentPage = 2,
        pageSize = 10,
        totalItems = 25
    )

    // Result Helpers
    fun <T> successResult(data: T): Result<T> = Result.Success(data)

    fun <T> errorResult(message: String = "Test error"): Result<T> =
        Result.Error(Exception(message))

    fun networkErrorResult(): Result<Nothing> =
        Result.Error(Exception("Network connection failed"))

    fun notFoundErrorResult(): Result<Nothing> =
        Result.Error(Exception("Product not found"))

    fun timeoutErrorResult(): Result<Nothing> =
        Result.Error(Exception("Request timed out"))

    // Search Query Samples
    const val SEARCH_QUERY_SMARTPHONE = "smartphone"
    const val SEARCH_QUERY_LAPTOP = "laptop"
    const val SEARCH_QUERY_HEADPHONES = "headphones"
    const val SEARCH_QUERY_NO_RESULTS = "nonexistent product xyz"
    const val SEARCH_QUERY_EMPTY = ""
    const val SEARCH_QUERY_BLANK = "   "
}