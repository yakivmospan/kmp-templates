package com.yakivmospan.templates.feature.productcatalog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yakivmospan.templates.core.navigation.Navigator
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.ObserveFavoritesUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class ProductFavoritesViewModel(
    private val navigator: Navigator,
    private val observeFavoritesUseCase: ObserveFavoritesUseCase,
    private val viewDataMapper: ProductToViewDataMapper
) : ViewModel() {

    private val _state = MutableStateFlow(ProductFavoritesState())
    val state: StateFlow<ProductFavoritesState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Store all favorites for filtering
    private var allFavorites: List<ProductViewData> = emptyList()

    init {
        observeFavorites()
        observeSearchQuery()
    }

    fun onEvent(event: ProductFavoritesEvent) {
        when (event) {
            is ProductFavoritesEvent.SelectProduct -> onSelectProduct(event.productId)
            is ProductFavoritesEvent.Retry -> observeFavorites()
            is ProductFavoritesEvent.SearchFavorites -> onSearchEvent(event.query)
            is ProductFavoritesEvent.ClearSearch -> onClearSearchEvent()
        }
    }

    private fun observeFavorites() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }
        observeFavoritesUseCase()
            .catch { onFavoritesLoadError(it) }
            .collect { onFavoritesChanged(it) }
    }

    private fun onFavoritesChanged(favoriteProducts: List<Product>) {
        allFavorites = favoriteProducts.map { product ->
            viewDataMapper.map(product)
        }

        val filteredFavorites = filterFavorites(_searchQuery.value)

        _state.update {
            it.copy(
                favorites = allFavorites,
                searchResult = filteredFavorites,
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

    // Search functionality
    private fun observeSearchQuery() = viewModelScope.launch {
        _searchQuery
            .debounce(ProductCatalogConfig.SEARCH_DEBOUNCE_MS)
            .distinctUntilChanged()
            .collectLatest { query ->
                performSearch(query)
            }
    }

    private fun onSearchEvent(query: String) {
        _searchQuery.value = query
    }

    private fun onClearSearchEvent() {
        _searchQuery.value = ""
    }

    private fun performSearch(query: String) {
        _state.update { it.copy(isSearching = true) }

        val filteredResults = filterFavorites(query)

        _state.update {
            it.copy(
                searchResult = filteredResults,
                isSearching = false
            )
        }
    }

    private fun filterFavorites(query: String): List<ProductViewData> {
        if (query.isBlank()) return emptyList()

        val lowercaseQuery = query.lowercase()
        return allFavorites.filter { product ->
            product.title.lowercase().contains(lowercaseQuery) ||
                    product.description.lowercase().contains(lowercaseQuery)
        }
    }

    private fun onSelectProduct(productId: Int) {
        navigator.navigate(ProductCatalogNavigationTargets.ToProductDetails(productId))
    }
}