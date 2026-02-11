package com.yakivmospan.templates.feature.productcatalog.domain.usecase

import com.yakivmospan.templates.feature.productcatalog.domain.model.Product
import com.yakivmospan.templates.feature.productcatalog.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow

class ObserveFavoritesUseCase(
    private val repository: ProductRepository
) {
    operator fun invoke(): Flow<List<Product>> {
        return repository.observeFavorites()
    }
}