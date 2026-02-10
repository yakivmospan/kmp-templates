package com.yakivmospan.templates.feature.productcatalog.presentation

import com.yakivmospan.templates.feature.productcatalog.domain.model.Product

data class ProductCatalogState(
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val hasNextPage: Boolean = false,
    val hasPreviousPage: Boolean = false,
    val isSearching: Boolean = false
)