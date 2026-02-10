package com.yakivmospan.templates.feature.productcatalog.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yakivmospan.templates.core.common.Result
import com.yakivmospan.templates.core.domain.DomainException
import com.yakivmospan.templates.core.navigation.Navigator
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.GetProductDetailsUseCase
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.ToggleFavoriteParams
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductDetailsViewModel(
    private val productId: Int,
    private val navigator: Navigator,
    private val getProductDetailsUseCase: GetProductDetailsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val productToViewDataMapper: ProductToDetailsViewDataMapper,
    private val viewDataToProductMapper: DetailsViewDataToProductMapper
) : ViewModel() {

    private val _state = MutableStateFlow(ProductDetailsState())
    val state: StateFlow<ProductDetailsState> = _state.asStateFlow()

    init {
        loadProduct()
    }

    fun onEvent(event: ProductDetailsEvent) {
        when (event) {
            is ProductDetailsEvent.LoadProduct -> loadProduct(event.productId)
            is ProductDetailsEvent.ToggleFavorite -> onToggleFavoriteEvent()
            is ProductDetailsEvent.NavigateBack -> onNavigateBackEvent()
            is ProductDetailsEvent.Retry -> onRetryEvent()
        }
    }

    // Product Loading
    private fun loadProduct(id: Int = productId) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, error = null) }

        when (val result = getProductDetailsUseCase(id)) {
            is Result.Success -> onLoadProductSuccess(result)
            is Result.Error -> onLoadProductError(result)
        }
    }

    private fun onLoadProductSuccess(result: Result.Success<Product>) = _state.update {
        it.copy(
            product = productToViewDataMapper.map(result.data),
            isLoading = false
        )
    }

    private fun onLoadProductError(result: Result.Error) = _state.update {
        it.copy(
            isLoading = false,
            error = mapErrorMessage(result.exception)
        )
    }

    // Toggle Favorite
    private fun onToggleFavoriteEvent() = viewModelScope.launch {
        val currentProduct = _state.value.product ?: return@launch

        // Optimistically update UI
        val updatedProduct = currentProduct.copy(isFavorite = !currentProduct.isFavorite)
        _state.update { it.copy(product = updatedProduct) }

        val domainProduct = viewDataToProductMapper.map(currentProduct)
        val params = ToggleFavoriteParams(product = domainProduct)

        when (val result = toggleFavoriteUseCase(params)) {
            is Result.Success -> {
                // Success - optimistic update already applied
            }

            is Result.Error -> {
                // Revert optimistic update on error
                _state.update { it.copy(product = currentProduct) }
                _state.update { it.copy(error = mapErrorMessage(result.exception)) }
            }
        }
    }

    // Navigation
    private fun onNavigateBackEvent() {
        navigator.back()
    }

    // Retry
    private fun onRetryEvent() {
        _state.update { it.copy(error = null) }
        loadProduct()
    }

    // Utils
    private fun mapErrorMessage(exception: Throwable) = when (exception) {
        is DomainException.NetworkError -> "Network error. Please check your connection."
        is DomainException.ServerError -> mapServerErrorMessage(exception)
        is DomainException.Unauthorized -> "Unauthorized. Please log in."
        is DomainException.NotFound -> "Product not found."
        is DomainException.Timeout -> "Request timed out. Please try again."
        is DomainException.Unknown -> exception.errorMessage
        else -> exception.message ?: "An error occurred"
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