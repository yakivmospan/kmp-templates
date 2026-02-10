package com.yakivmospan.templates.feature.productcatalog.domain.usecase

import com.yakivmospan.templates.core.common.Result
import com.yakivmospan.templates.core.domain.UseCase
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product
import com.yakivmospan.templates.feature.productcatalog.domain.repository.ProductRepository

data class ToggleFavoriteParams(
    val product: Product
)

class ToggleFavoriteUseCase(
    private val repository: ProductRepository
) : UseCase<ToggleFavoriteParams, Result<Unit>>() {

    override suspend fun invoke(params: ToggleFavoriteParams): Result<Unit> {
        val product = params.product

        return if (product.isFavorite) {
            repository.removeFromFavorites(product.id)
        } else {
            repository.addToFavorites(product)
        }
    }
}