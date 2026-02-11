package com.yakivmospan.templates.feature.productcatalog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yakivmospan.templates.core.common.PageRequest
import com.yakivmospan.templates.core.common.PaginatedData
import com.yakivmospan.templates.core.common.Result
import com.yakivmospan.templates.core.domain.DomainException
import com.yakivmospan.templates.core.navigation.Navigator
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.GetProductsUseCase
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.SearchProductsParams
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.SearchProductsUseCase
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogConfig.PAGE_PAGINATION_SIZE
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogConfig.SEARCH_DEBOUNCE_MS
import dev.icerock.moko.resources.format
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class ProductCatalogViewModel(
    private val navigator: Navigator,
    private val getProductsUseCase: GetProductsUseCase,
    private val searchProductsUseCase: SearchProductsUseCase,
    private val viewDataMapper: ProductToViewDataMapper
) : ViewModel() {

    private val _state = MutableStateFlow(ProductCatalogState())
    val state: StateFlow<ProductCatalogState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadProducts()
        observeSearchQuery()
    }

    fun onEvent(event: ProductCatalogEvent) {
        when (event) {
            is ProductCatalogEvent.LoadPage -> loadProducts(page = event.page)
            is ProductCatalogEvent.LoadNextPage -> onLoadNextPageEvent()
            is ProductCatalogEvent.SelectProduct -> onSelectProductEvent(event.productId)
            is ProductCatalogEvent.SearchProducts -> onSearchEvent(event)
            is ProductCatalogEvent.ClearSearch -> onClearSearchEvent()
            is ProductCatalogEvent.Retry -> onRetryEvent()
            is ProductCatalogEvent.ClearError -> onClearErrorEvent()
        }
    }

    // Product Loading & Pagination
    private fun loadProducts(page: Int = 1) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val pageRequest = PageRequest(page = page, pageSize = PAGE_PAGINATION_SIZE)
            when (val result = getProductsUseCase(pageRequest)) {
                is Result.Success -> onLoadProductsSuccess(result)
                is Result.Error -> onLoadProductsError(result)
            }
        }
    }

    private fun onLoadProductsSuccess(result: Result.Success<PaginatedData<Product>>) = _state.update {
        it.copy(
            products = result.data.items.map { item -> viewDataMapper.map(item) },
            isLoading = false,
            currentPage = result.data.currentPage,
            totalPages = result.data.totalPages,
            hasNextPage = result.data.hasNextPage,
            hasPreviousPage = result.data.hasPreviousPage
        )
    }

    private fun onLoadProductsError(result: Result.Error) = _state.update {
        it.copy(
            isLoading = false,
            error = mapErrorMessage(result.exception)
        )
    }

    private fun onLoadNextPageEvent() {
        val currentState = _state.value
        if (!currentState.hasNextPage || currentState.isLoading) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

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
    ) = _state.update {
        it.copy(
            products = currentState.products + result.data.items.map { item -> viewDataMapper.map(item) },
            isLoading = false,
            currentPage = result.data.currentPage,
            totalPages = result.data.totalPages,
            hasNextPage = result.data.hasNextPage,
            hasPreviousPage = result.data.hasPreviousPage
        )
    }

    private fun loadNextPageError(result: Result.Error) {
        _state.update {
            it.copy(
                isLoading = false,
                error = mapErrorMessage(result.exception)
            )
        }
    }

    private suspend fun searchProducts(query: String) {
        _state.update { it.copy(isSearching = true, error = null) }

        val params = SearchProductsParams(
            query = query,
            pageRequest = PageRequest(page = 1, pageSize = PAGE_PAGINATION_SIZE)
        )
        when (val result = searchProductsUseCase(params)) {
            is Result.Success -> onSearchSuccess(result)
            is Result.Error -> onSearchError(result)
        }
    }

    private fun onSearchSuccess(result: Result.Success<PaginatedData<Product>>) = _state.update {
        it.copy(
            searchResult = result.data.items.map { item -> viewDataMapper.map(item) },
            isSearching = false
        )
    }

    private fun onSearchError(result: Result.Error) = _state.update {
        it.copy(
            searchResult = emptyList(),
            isSearching = false,
            error = mapErrorMessage(result.exception)
        )
    }

    // Search
    private fun observeSearchQuery() = viewModelScope.launch {
        _searchQuery
            .drop(1)
            // Show loading before debounce to provide immediate feedback
            .onEach { _state.update { it.copy(isSearching = true) } }
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
    }

    private fun onClearSearchEvent() {
        _searchQuery.value = ""
    }

    private fun clearSearchResults() {
        _state.update {
            it.copy(
                searchResult = emptyList(),
                isSearching = false
            )
        }
    }


    // Other Events
    private fun onClearErrorEvent() {
        _state.update { it.copy(error = null) }
    }

    private fun onRetryEvent() {
        _state.update { it.copy(error = null) }

        if (_searchQuery.value.isNotBlank()) {
            viewModelScope.launch {
                searchProducts(_searchQuery.value)
            }
        } else {
            loadProducts(page = _state.value.currentPage)
        }
    }

    private fun onSelectProductEvent(productId: Int) {
        navigator.navigate(ProductCatalogNavigationTargets.ToProductDetails(productId))
    }

    // Utils
    private fun mapErrorMessage(exception: Throwable): String {
        return when (exception) {
            is DomainException.NetworkError -> MR.strings.pd_catalog_feature_network_error_message.toString()
            is DomainException.ServerError -> mapServerErrorMessage(exception)
            is DomainException.Unauthorized -> MR.strings.pd_catalog_feature_unauthorized_error_message.toString()
            is DomainException.NotFound -> MR.strings.pd_catalog_feature_products_not_found_error.toString()
            is DomainException.Timeout -> MR.strings.pd_catalog_feature_timeout_error_message.toString()
            is DomainException.Unknown -> exception.errorMessage
            else -> exception.message ?: MR.strings.pd_catalog_feature_generic_error_message.toString()
        }
    }

    private fun mapServerErrorMessage(
        exception: DomainException.ServerError
    ) = buildString {
        append(exception.errorMessage)

        exception.errorCode?.let {
            append(MR.strings.pd_catalog_feature_error_code_format.format(it).toString())
        }

        exception.errorDetails?.entries?.firstOrNull()?.let { (_, value) ->
            append(MR.strings.pd_catalog_feature_error_details_format.format(value).toString())
        }
    }
}