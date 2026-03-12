package com.yakivmospan.templates.feature.productcatalog.presentation

sealed class ProductCatalogHomeEvent {
    data class SelectTab(val tabIndex: Int) : ProductCatalogHomeEvent()
}