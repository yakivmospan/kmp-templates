package com.yakivmospan.templates.feature.productcatalog.presentation

data class ProductCatalogState(
    val searchResult: List<ProductCatalogViewData> = emptyList(),
    val products: List<ProductCatalogViewData> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val hasNextPage: Boolean = false,
    val hasPreviousPage: Boolean = false,
    val isSearching: Boolean = false
)