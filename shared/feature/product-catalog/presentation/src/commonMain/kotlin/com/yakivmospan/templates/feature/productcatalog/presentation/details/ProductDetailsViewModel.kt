package com.yakivmospan.templates.feature.productcatalog.presentation.details

import androidx.lifecycle.viewModelScope
import com.yakivmospan.templates.core.common.Result
import com.yakivmospan.templates.core.domain.DomainException
import com.yakivmospan.templates.core.navigation.Navigator
import com.yakivmospan.templates.core.presentation.ViewModel
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.GetProductDetailsParams
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.GetProductDetailsUseCase
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.ToggleFavoriteParams
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.ToggleFavoriteUseCase
import com.yakivmospan.templates.feature.productcatalog.presentation.MR
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductToViewDataMapper
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductViewDataToEntityMapper
import dev.icerock.moko.resources.desc.StringDesc
import dev.icerock.moko.resources.desc.desc
import kotlinx.coroutines.launch

class ProductDetailsViewModel(
    private val productId: Int,
    private val navigator: Navigator,
    private val getProductDetailsUseCase: GetProductDetailsUseCase,
    private val toggleFavoriteUseCase: ToggleFavoriteUseCase,
    private val productToViewDataMapper: ProductToViewDataMapper,
    private val viewDataToProductMapper: ProductViewDataToEntityMapper
) : ViewModel<ProductDetailsEvent, ProductDetailsState, ProductDetailsSideEffect>(ProductDetailsState()) {

    init {
        loadProduct()
    }

    override fun onEvent(event: ProductDetailsEvent) {
        when (event) {
            is ProductDetailsEvent.LoadProduct -> loadProduct(event.productId)
            is ProductDetailsEvent.ToggleFavorite -> onToggleFavoriteEvent()
            is ProductDetailsEvent.NavigateBack -> onNavigateBackEvent()
            is ProductDetailsEvent.Retry -> onRetryEvent()
        }
    }

    // Product Loading
    private fun loadProduct(id: Int = productId) = viewModelScope.launch {
        updateState { it.copy(isLoading = true) }

        when (val result = getProductDetailsUseCase(GetProductDetailsParams(id, useCache = true))) {
            is Result.Success -> onLoadProductSuccess(result)
            is Result.Error -> onLoadProductError(result)
        }
    }

    private fun onLoadProductSuccess(result: Result.Success<Product>) = updateState {
        it.copy(
            product = productToViewDataMapper.map(result.data),
            isLoading = false,
            errorMessage = null
        )
    }

    private fun onLoadProductError(result: Result.Error) {
        val errorMessage = mapErrorMessage(result.exception)
        if (state.value.product != null) {
            // Refresh failure: product already displayed, show snackbar.
            emitSideEffect(ProductDetailsSideEffect.ShowError(errorMessage))
            updateState { it.copy(isLoading = false) }
        } else {
            // Initial-load failure: surface error in the full-screen error state.
            updateState { it.copy(isLoading = false, errorMessage = errorMessage) }
        }
    }

    // Toggle Favorite
    private fun onToggleFavoriteEvent() = viewModelScope.launch {
        val currentProduct = state.value.product ?: return@launch

        // Optimistically update UI
        val updatedProduct = currentProduct.copy(isFavorite = !currentProduct.isFavorite)
        updateState { it.copy(product = updatedProduct) }

        val domainProduct = viewDataToProductMapper.map(currentProduct)
        val params = ToggleFavoriteParams(product = domainProduct)

        when (val result = toggleFavoriteUseCase(params)) {
            is Result.Success -> {
                // Success — optimistic update already applied
            }
            is Result.Error -> {
                // Revert optimistic update on error
                updateState { it.copy(product = currentProduct) }
                emitSideEffect(ProductDetailsSideEffect.ShowError(mapErrorMessage(result.exception)))
            }
        }
    }

    // Navigation
    private fun onNavigateBackEvent() {
        navigator.back()
    }

    private fun onRetryEvent() {
        loadProduct()
    }

    // Utils
    private fun mapErrorMessage(exception: Throwable): StringDesc = when (exception) {
        is DomainException.NetworkError -> MR.strings.pd_catalog_feature_network_error_message.desc()
        is DomainException.ServerError -> exception.errorMessage.desc()
        is DomainException.Unauthorized -> MR.strings.pd_catalog_feature_unauthorized_error_message.desc()
        is DomainException.NotFound -> MR.strings.pd_catalog_feature_product_not_found_error.desc()
        is DomainException.Timeout -> MR.strings.pd_catalog_feature_timeout_error_message.desc()
        is DomainException.Unknown -> exception.errorMessage.desc()
        else -> exception.message?.desc() ?: MR.strings.pd_catalog_feature_generic_error_message.desc()
    }
}