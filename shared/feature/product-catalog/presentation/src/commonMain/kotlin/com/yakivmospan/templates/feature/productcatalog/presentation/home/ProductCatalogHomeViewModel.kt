package com.yakivmospan.templates.feature.productcatalog.presentation.home

import com.yakivmospan.templates.core.presentation.ViewModel
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogHomeEvent
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogHomeState

class ProductCatalogHomeViewModel : ViewModel<ProductCatalogHomeEvent, ProductCatalogHomeState, Unit>(ProductCatalogHomeState()) {
    override fun onEvent(event: ProductCatalogHomeEvent) {
        when (event) {
            is ProductCatalogHomeEvent.SelectTab -> onSelectTab(event.tabIndex)
        }
    }

    private fun onSelectTab(tabIndex: Int) = updateState { it.copy(selectedTabIndex = tabIndex) }
}