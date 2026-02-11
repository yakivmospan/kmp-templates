package com.yakivmospan.templates.feature.productcatalog.presentation

data class ProductFavoritesState(
    val favorites: List<ProductViewData> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
