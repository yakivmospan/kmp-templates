package com.yakivmospan.templates.feature.productcatalog.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ProductCatalogHomeViewModel : ViewModel() {

    private val _state = MutableStateFlow(ProductCatalogHomeState())
    val state: StateFlow<ProductCatalogHomeState> = _state.asStateFlow()

    fun onEvent(event: ProductCatalogHomeEvent) {
        when (event) {
            is ProductCatalogHomeEvent.SelectTab -> onSelectTab(event.tabIndex)
        }
    }

    private fun onSelectTab(tabIndex: Int) {
        _state.update { it.copy(selectedTabIndex = tabIndex) }
    }
}