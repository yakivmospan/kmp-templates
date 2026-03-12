package com.yakivmospan.templates.feature.productcatalog.presentation.details

import com.yakivmospan.templates.feature.productcatalog.presentation.ProductViewData

data class ProductDetailsState(
    val product: ProductViewData? = null,
    val isLoading: Boolean = false,
)