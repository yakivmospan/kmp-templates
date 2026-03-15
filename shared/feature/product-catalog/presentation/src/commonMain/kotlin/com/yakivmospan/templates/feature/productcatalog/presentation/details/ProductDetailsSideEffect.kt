package com.yakivmospan.templates.feature.productcatalog.presentation.details

import dev.icerock.moko.resources.desc.StringDesc

sealed class ProductDetailsSideEffect {
    data class ShowError(val message: StringDesc) : ProductDetailsSideEffect()
}

