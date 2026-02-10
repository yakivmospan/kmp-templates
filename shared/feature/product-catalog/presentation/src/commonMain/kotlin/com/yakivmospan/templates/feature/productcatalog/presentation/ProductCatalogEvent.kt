package com.yakivmospan.templates.feature.productcatalog.presentation

sealed class ProductCatalogEvent {
    data object LoadProducts : ProductCatalogEvent()
    data class LoadPage(val page: Int) : ProductCatalogEvent()
    data object LoadNextPage : ProductCatalogEvent()
    data object LoadPreviousPage : ProductCatalogEvent()
    data class SelectProduct(val productId: String) : ProductCatalogEvent()
    data class SearchProducts(val query: String) : ProductCatalogEvent()
    data object ClearSearch : ProductCatalogEvent()
    data object Retry : ProductCatalogEvent()
}