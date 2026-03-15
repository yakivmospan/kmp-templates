package com.yakivmospan.templates.feature.productcatalog.presentation.catalog

import com.yakivmospan.templates.feature.productcatalog.presentation.ProductViewData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

data class ProductCatalogState(
    val searchResult: ImmutableList<ProductViewData> = persistentListOf(),
    val products: ImmutableList<ProductViewData> = persistentListOf(),
    val isLoading: Boolean = false,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val hasNextPage: Boolean = false,
    val hasPreviousPage: Boolean = false,
    val isSearching: Boolean = false,
    val isSearchMode: Boolean = false,
) {
    val displayItems: ImmutableList<ProductViewData>
        get() = if (isSearchMode) searchResult else products
}
