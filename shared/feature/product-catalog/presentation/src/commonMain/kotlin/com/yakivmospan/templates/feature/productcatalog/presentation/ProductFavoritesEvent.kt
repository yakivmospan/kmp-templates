package com.yakivmospan.templates.feature.productcatalog.presentation

sealed class ProductFavoritesEvent {
    data class SelectProduct(val productId: Int) : ProductFavoritesEvent()
    data object Retry : ProductFavoritesEvent()
}