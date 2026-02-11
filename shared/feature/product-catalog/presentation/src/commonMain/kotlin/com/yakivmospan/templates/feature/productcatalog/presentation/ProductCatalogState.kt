package com.yakivmospan.templates.feature.productcatalog.presentation

import dev.icerock.moko.resources.desc.StringDesc

data class ProductCatalogState(
    val searchResult: List<ProductViewData> = emptyList(),
    val products: List<ProductViewData> = emptyList(),
    val isLoading: Boolean = false,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val hasNextPage: Boolean = false,
    val hasPreviousPage: Boolean = false,
    val isSearching: Boolean = false
)