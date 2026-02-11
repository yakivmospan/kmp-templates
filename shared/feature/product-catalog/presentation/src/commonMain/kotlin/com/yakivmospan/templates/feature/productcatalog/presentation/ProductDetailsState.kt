package com.yakivmospan.templates.feature.productcatalog.presentation

data class ProductDetailsState(
    val product: ProductViewData? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)