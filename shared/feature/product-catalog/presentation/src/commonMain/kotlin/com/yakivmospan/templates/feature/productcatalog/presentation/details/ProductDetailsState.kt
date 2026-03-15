package com.yakivmospan.templates.feature.productcatalog.presentation.details

import com.yakivmospan.templates.feature.productcatalog.presentation.ProductViewData
import dev.icerock.moko.resources.desc.StringDesc

data class ProductDetailsState(
    val product: ProductViewData? = null,
    val isLoading: Boolean = false,
    val errorMessage: StringDesc? = null,
)