package com.yakivmospan.templates.feature.productcatalog.presentation.favorites

import com.yakivmospan.templates.feature.productcatalog.presentation.ProductViewData

data class ProductFavoritesState(
    val favorites: List<ProductViewData> = emptyList(),
    val searchResult: List<ProductViewData> = emptyList(),
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
)