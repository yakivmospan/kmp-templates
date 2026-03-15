package com.yakivmospan.templates.feature.productcatalog.presentation.catalog

import androidx.lifecycle.viewModelScope
import com.yakivmospan.templates.core.common.PageRequest
import com.yakivmospan.templates.core.common.PaginatedData
import com.yakivmospan.templates.core.common.Result
import com.yakivmospan.templates.core.domain.DomainException
import com.yakivmospan.templates.core.navigation.Navigator
import com.yakivmospan.templates.core.presentation.ViewModel
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.GetProductsUseCase
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.SearchProductsParams
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.SearchProductsUseCase
import com.yakivmospan.templates.feature.productcatalog.presentation.MR
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogConfig.PAGE_PAGINATION_SIZE
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogConfig.SEARCH_DEBOUNCE_MS
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogNavigationTargets
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductToViewDataMapper
import dev.icerock.moko.resources.desc.desc
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class ProductCatalogViewModel(
    private val navigator: Navigator,
    private val getProductsUseCase: GetProductsUseCase,
    private val searchProductsUseCase: SearchProductsUseCase,
    private val viewDataMapper: ProductToViewDataMapper
) : ViewModel<ProductCatalogEvent, ProductCatalogState, ProductCatalogSideEffect>(ProductCatalogState()) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadProducts()
        observeSearchQuery()
    }

    override fun onEvent(event: ProductCatalogEvent) {
        when (event) {
            is ProductCatalogEvent.LoadPage -> loadProducts(page = event.page)
            is ProductCatalogEvent.LoadNextPage -> onLoadNextPageEvent()
            is ProductCatalogEvent.SelectProduct -> onSelectProductEvent(event.productId)
            is ProductCatalogEvent.SearchProducts -> onSearchEvent(event)
            is ProductCatalogEvent.ClearSearch -> onClearSearchEvent()
            is ProductCatalogEvent.Retry -> onRetryEvent()
        }
    }

    // Product Loading & Pagination
    private fun loadProducts(page: Int = 1) {
        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }

            val pageRequest = PageRequest(page = page, pageSize = PAGE_PAGINATION_SIZE)
            when (val result = getProductsUseCase(pageRequest)) {
                is Result.Success -> onLoadProductsSuccess(result)
                is Result.Error -> onLoadProductsError(result)
            }
        }
    }

    private fun onLoadProductsSuccess(result: Result.Success<PaginatedData<Product>>) = updateState {
        it.copy(
            products = result.data.items.map { item -> viewDataMapper.map(item) }.toImmutableList(),
            isLoading = false,
            currentPage = result.data.currentPage,
            totalPages = result.data.totalPages,
            hasNextPage = result.data.hasNextPage,
            hasPreviousPage = result.data.hasPreviousPage
        )
    }

    private fun onLoadProductsError(result: Result.Error) {
        updateState { it.copy(isLoading = false) }
        emitSideEffect(ProductCatalogSideEffect.ShowError(mapErrorMessage(result.exception)))
    }

    private fun onLoadNextPageEvent() {
        val currentState = state.value
        if (!currentState.hasNextPage || currentState.isLoading) return

        viewModelScope.launch {
            updateState { it.copy(isLoading = true) }

            val nextPage = currentState.currentPage + 1
            val pageRequest = PageRequest(page = nextPage, pageSize = PAGE_PAGINATION_SIZE)
            when (val result = getProductsUseCase(pageRequest)) {
                is Result.Success -> loadNextPageSuccess(currentState, result)
                is Result.Error -> loadNextPageError(result)
            }
        }
    }

    private fun loadNextPageSuccess(
        currentState: ProductCatalogState,
        result: Result.Success<PaginatedData<Product>>
    ) = updateState {
        it.copy(
            products = (currentState.products + result.data.items.map { item -> viewDataMapper.map(item) }).toImmutableList(),
            isLoading = false,
            currentPage = result.data.currentPage,
            totalPages = result.data.totalPages,
            hasNextPage = result.data.hasNextPage,
            hasPreviousPage = result.data.hasPreviousPage
        )
    }

    private fun loadNextPageError(result: Result.Error) {
        updateState { it.copy(isLoading = false) }
        emitSideEffect(ProductCatalogSideEffect.ShowError(mapErrorMessage(result.exception)))
    }

    private suspend fun searchProducts(query: String) {
        updateState { it.copy(isSearching = true) }

        val params = SearchProductsParams(
            query = query,
            pageRequest = PageRequest(page = 1, pageSize = PAGE_PAGINATION_SIZE)
        )
        when (val result = searchProductsUseCase(params)) {
            is Result.Success -> onSearchSuccess(result)
            is Result.Error -> onSearchError(result)
        }
    }

    private fun onSearchSuccess(result: Result.Success<PaginatedData<Product>>) = updateState {
        it.copy(
            searchResult = result.data.items.map { item -> viewDataMapper.map(item) }.toImmutableList(),
            isSearching = false
        )
    }

    private fun onSearchError(result: Result.Error) {
        updateState {
            it.copy(
                searchResult = persistentListOf(),
                isSearching = false,
            )
        }
        emitSideEffect(ProductCatalogSideEffect.ShowError(mapErrorMessage(result.exception)))
    }

    // Search
    private fun observeSearchQuery() = viewModelScope.launch {
        _searchQuery
            .drop(1)
            // Show loading before debounce to provide immediate feedback
            .onEach { updateState { it.copy(isSearching = true) } }
            .debounce(SEARCH_DEBOUNCE_MS)
            .distinctUntilChanged()
            .collectLatest { query ->
                if (query.isBlank()) {
                    clearSearchResults()
                } else {
                    searchProducts(query)
                }
            }
    }

    private fun onSearchEvent(event: ProductCatalogEvent.SearchProducts) {
        _searchQuery.value = event.query
        updateState { it.copy(isSearchMode = event.query.isNotBlank()) }
    }

    private fun onClearSearchEvent() {
        _searchQuery.value = ""
        updateState { it.copy(isSearchMode = false) }
    }

    private fun clearSearchResults() {
        updateState {
            it.copy(
                searchResult = persistentListOf(),
                isSearching = false,
                isSearchMode = false,
            )
        }
    }

    // Other Events
    private fun onRetryEvent() {
        if (_searchQuery.value.isNotBlank()) {
            viewModelScope.launch {
                searchProducts(_searchQuery.value)
            }
        } else {
            loadProducts(page = state.value.currentPage)
        }
    }

    private fun onSelectProductEvent(productId: Int) {
        navigator.navigate(ProductCatalogNavigationTargets.ToProductDetails(productId))
    }

    // Utils
    private fun mapErrorMessage(exception: Throwable) = when (exception) {
        is DomainException.NetworkError -> MR.strings.pd_catalog_feature_network_error_message.desc()
        is DomainException.ServerError -> exception.errorMessage.desc()
        is DomainException.Unauthorized -> MR.strings.pd_catalog_feature_unauthorized_error_message.desc()
        is DomainException.NotFound -> MR.strings.pd_catalog_feature_products_not_found_error.desc()
        is DomainException.Timeout -> MR.strings.pd_catalog_feature_timeout_error_message.desc()
        is DomainException.Unknown -> exception.errorMessage.desc()
        else -> exception.message?.desc() ?: MR.strings.pd_catalog_feature_generic_error_message.toString().desc()
    }

}