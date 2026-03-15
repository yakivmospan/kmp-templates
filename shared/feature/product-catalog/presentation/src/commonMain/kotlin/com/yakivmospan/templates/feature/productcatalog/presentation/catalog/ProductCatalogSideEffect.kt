package com.yakivmospan.templates.feature.productcatalog.presentation.catalog

import dev.icerock.moko.resources.desc.StringDesc

sealed class ProductCatalogSideEffect {
    data class ShowError(val message: StringDesc) : ProductCatalogSideEffect()
}

