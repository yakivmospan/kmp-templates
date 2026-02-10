package com.yakivmospan.templates.feature.productcatalog.presentation

data class ProductDetailsState(
    val product: ProductDetailsViewData? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)