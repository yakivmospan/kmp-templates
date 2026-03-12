package com.yakivmospan.templates.feature.productcatalog.presentation.details

sealed class ProductDetailsEvent {
    data class LoadProduct(val productId: Int) : ProductDetailsEvent()
    data object NavigateBack : ProductDetailsEvent()
    data object Retry : ProductDetailsEvent()
    data object ToggleFavorite : ProductDetailsEvent()
}