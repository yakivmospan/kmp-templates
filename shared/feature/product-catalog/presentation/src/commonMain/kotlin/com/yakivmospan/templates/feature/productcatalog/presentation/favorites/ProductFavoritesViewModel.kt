package com.yakivmospan.templates.feature.productcatalog.presentation.favorites

import androidx.lifecycle.viewModelScope
import com.yakivmospan.templates.core.navigation.Navigator
import com.yakivmospan.templates.core.presentation.ViewModel
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.ObserveFavoritesUseCase
import com.yakivmospan.templates.feature.productcatalog.presentation.MR
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogConfig
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogNavigationTargets
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductToViewDataMapper
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductViewData
import dev.icerock.moko.resources.desc.desc
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class ProductFavoritesViewModel(
    private val navigator: Navigator,
    private val observeFavoritesUseCase: ObserveFavoritesUseCase,
    private val viewDataMapper: ProductToViewDataMapper
) : ViewModel<ProductFavoritesEvent, ProductFavoritesState, ProductFavoritesSideEffect>(ProductFavoritesState()) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Store all favorites for filtering
    private var allFavorites: List<ProductViewData> = emptyList()
    private var observeFavoritesJob: Job? = null

    init {
        observeFavorites()
        observeSearchQuery()
    }

    override fun onEvent(event: ProductFavoritesEvent) {
        when (event) {
            is ProductFavoritesEvent.SelectProduct -> onSelectProduct(event.productId)
            is ProductFavoritesEvent.Retry -> observeFavorites()
            is ProductFavoritesEvent.SearchFavorites -> onSearchEvent(event.query)
            is ProductFavoritesEvent.ClearSearch -> onClearSearchEvent()
        }
    }

    private fun observeFavorites() {
        observeFavoritesJob?.cancel()
        observeFavoritesJob = viewModelScope.launch {
            updateState { it.copy(isLoading = true) }
            observeFavoritesUseCase()
                .catch { onFavoritesLoadError(it) }
                .collect { onFavoritesChanged(it) }
        }
    }

    private fun onFavoritesChanged(favoriteProducts: List<Product>) {
        allFavorites = favoriteProducts.map { product ->
            viewDataMapper.map(product)
        }

        val filteredFavorites = filterFavorites(_searchQuery.value)

        updateState {
            it.copy(
                favorites = allFavorites.toImmutableList(),
                searchResult = filteredFavorites.toImmutableList(),
                isLoading = false
            )
        }
    }

    private fun onFavoritesLoadError(exception: Throwable) {
        updateState { it.copy(isLoading = false) }
        emitSideEffect(
            ProductFavoritesSideEffect.ShowError(
                exception.message?.desc() ?: MR.strings.pd_catalog_feature_generic_error_message.desc()
            )
        )
    }

    // Search functionality
    private fun observeSearchQuery() = viewModelScope.launch {
        _searchQuery
            .drop(1)
            // Show loading before debounce to provide immediate feedback
            .onEach { updateState { it.copy(isSearching = true) } }
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
        updateState { it.copy(isSearching = true) }

        val filteredResults = filterFavorites(query)

        updateState {
            it.copy(
                searchResult = filteredResults.toImmutableList(),
                isSearching = false
            )
        }
    }

    private fun filterFavorites(query: String): List<ProductViewData> {
        if (query.isBlank()) return allFavorites

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