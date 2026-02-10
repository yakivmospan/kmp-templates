package com.yakivmospan.templates.feature.productcatalog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yakivmospan.templates.core.common.PageRequest
import com.yakivmospan.templates.core.common.Result
import com.yakivmospan.templates.core.domain.DomainException
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.GetProductsUseCase
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.SearchProductsParams
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.SearchProductsUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class ProductCatalogViewModel(
    private val getProductsUseCase: GetProductsUseCase,
    private val searchProductsUseCase: SearchProductsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProductCatalogState())
    val state: StateFlow<ProductCatalogState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadProducts()
        observeSearchQuery()
    }

    private fun observeSearchQuery() = viewModelScope.launch {
        _searchQuery
            .debounce(300)
            .distinctUntilChanged()
            .collect { query ->
                if (query.isBlank()) {
                    loadProducts()
                } else {
                    searchProducts(query)
                }
            }
    }

    fun onEvent(event: ProductCatalogEvent) {
        when (event) {
            is ProductCatalogEvent.LoadProducts -> loadProducts()
            is ProductCatalogEvent.LoadPage -> loadProducts(page = event.page)
            is ProductCatalogEvent.LoadNextPage -> loadNextPage()
            is ProductCatalogEvent.LoadPreviousPage -> loadPreviousPage()
            is ProductCatalogEvent.SelectProduct -> {
                // Handle product selection - navigate to detail screen
            }

            is ProductCatalogEvent.SearchProducts -> {
                _searchQuery.value = event.query
            }

            is ProductCatalogEvent.ClearSearch -> clearSearch()
            is ProductCatalogEvent.Retry -> retry()
        }
    }

    private fun loadProducts(page: Int = 1) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val pageRequest = PageRequest(page = page, pageSize = 20)

            when (val result = getProductsUseCase(pageRequest)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            products = result.data.items,
                            isLoading = false,
                            currentPage = result.data.currentPage,
                            totalPages = result.data.totalPages,
                            hasNextPage = result.data.hasNextPage,
                            hasPreviousPage = result.data.hasPreviousPage
                        )
                    }
                }

                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = mapErrorMessage(result.exception)
                        )
                    }
                }
            }
        }
    }

    private fun searchProducts(query: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSearching = true, error = null) }

            val params = SearchProductsParams(
                query = query,
                pageRequest = PageRequest(page = 1, pageSize = 20)
            )

            when (val result = searchProductsUseCase(params)) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            products = result.data.items,
                            isSearching = false,
                            currentPage = result.data.currentPage,
                            totalPages = result.data.totalPages,
                            hasNextPage = result.data.hasNextPage,
                            hasPreviousPage = result.data.hasPreviousPage
                        )
                    }
                }

                is Result.Error -> {
                    _state.update {
                        it.copy(
                            isSearching = false,
                            error = mapErrorMessage(result.exception)
                        )
                    }
                }
            }
        }
    }

    private fun clearSearch() {
        _searchQuery.value = ""
    }

    private fun loadNextPage() {
        val currentState = _state.value
        if (!currentState.hasNextPage || currentState.isLoading) return

        if (_searchQuery.value.isNotBlank()) {
            searchProducts(_searchQuery.value)
        } else {
            loadProducts(page = currentState.currentPage + 1)
        }
    }

    private fun loadPreviousPage() {
        val currentState = _state.value
        if (!currentState.hasPreviousPage || currentState.isLoading) return

        if (_searchQuery.value.isNotBlank()) {
            searchProducts(_searchQuery.value)
        } else {
            loadProducts(page = currentState.currentPage - 1)
        }
    }

    private fun retry() {
        if (_searchQuery.value.isNotBlank()) {
            searchProducts(_searchQuery.value)
        } else {
            loadProducts(page = _state.value.currentPage)
        }
    }


    // Simple error mapping function, in reality we could be updating all sort of state properties based on error type
    private fun mapErrorMessage(exception: Throwable): String {
        return when (exception) {
            is DomainException.NetworkError -> "Network error. Please check your connection."
            is DomainException.ServerError -> mapServerErrorMessage(exception)
            is DomainException.Unauthorized -> "Unauthorized. Please log in."
            is DomainException.NotFound -> "Products not found."
            is DomainException.Timeout -> "Request timed out. Please try again."
            is DomainException.Unknown -> exception.errorMessage
            else -> exception.message ?: "An error occurred"
        }
    }

    private fun mapServerErrorMessage(
        exception: DomainException.ServerError
    ) = buildString {
        append(exception.errorMessage)

        exception.errorCode?.let {
            append(" (Error: $it)")
        }

        exception.errorDetails?.entries?.firstOrNull()?.let { (_, value) ->
            append(" - $value")
        }
    }
}