package com.yakivmospan.templates.feature.productcatalog.presentation.favorites

import dev.icerock.moko.resources.desc.StringDesc

sealed class ProductFavoritesSideEffect {
    data class ShowError(val message: StringDesc) : ProductFavoritesSideEffect()
}

