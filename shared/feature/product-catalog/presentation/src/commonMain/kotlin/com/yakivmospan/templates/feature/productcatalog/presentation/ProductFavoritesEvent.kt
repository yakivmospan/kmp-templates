package com.yakivmospan.templates.feature.productcatalog.presentation

sealed class ProductFavoritesEvent {
    data class SelectProduct(val productId: Int) : ProductFavoritesEvent()
    data class SearchFavorites(val query: String) : ProductFavoritesEvent()
    data object ClearSearch : ProductFavoritesEvent()
    data object Retry : ProductFavoritesEvent()
}