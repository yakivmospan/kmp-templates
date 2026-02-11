package com.yakivmospan.templates.feature.productcatalog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yakivmospan.templates.core.navigation.Navigator
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.ObserveFavoritesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductFavoritesViewModel(
    private val navigator: Navigator,
    private val observeFavoritesUseCase: ObserveFavoritesUseCase,
    private val viewDataMapper: ProductToViewDataMapper
) : ViewModel() {

    private val _state = MutableStateFlow(ProductFavoritesState())
    val state: StateFlow<ProductFavoritesState> = _state.asStateFlow()

    init {
        observeFavorites()
    }

    fun onEvent(event: ProductFavoritesEvent) {
        when (event) {
            is ProductFavoritesEvent.SelectProduct -> onSelectProduct(event.productId)
            is ProductFavoritesEvent.Retry -> observeFavorites()
        }
    }

    private fun observeFavorites() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        observeFavoritesUseCase()
            .catch { onFavoritesLoadError(it) }
            .collect { onFavoritesChanged(it) }
    }

    private fun onFavoritesChanged(favoriteProducts: List<Product>) {
        val favorites = favoriteProducts.map { product ->
            viewDataMapper.map(product)
        }
        _state.update {
            it.copy(
                favorites = favorites,
                isLoading = false,
                error = null
            )
        }
    }

    private fun onFavoritesLoadError(exception: Throwable) {
        _state.update {
            it.copy(
                isLoading = false,
                error = exception.message ?: MR.strings.pd_catalog_feature_generic_error_message.toString()
            )
        }
    }

    private fun onSelectProduct(productId: Int) {
        navigator.navigate(ProductCatalogNavigationTargets.ToProductDetails(productId))
    }
}