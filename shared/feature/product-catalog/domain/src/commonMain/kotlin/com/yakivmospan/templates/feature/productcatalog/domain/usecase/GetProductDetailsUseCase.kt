package com.yakivmospan.templates.feature.productcatalog.domain.usecase

import com.yakivmospan.templates.core.common.Result
import com.yakivmospan.templates.core.domain.UpdateStrategy
import com.yakivmospan.templates.core.domain.UseCase
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product
import com.yakivmospan.templates.feature.productcatalog.domain.repository.ProductRepository

data class GetProductDetailsParams(
    val id: Int,
    val useCache: Boolean
)
class GetProductDetailsUseCase(
    private val repository: ProductRepository
) : UseCase<GetProductDetailsParams, Result<Product>>() {

    override suspend fun invoke(params: GetProductDetailsParams): Result<Product> {
        val strategy = if(params.useCache) UpdateStrategy.TRY_CACHED_ELSE_FETCH else UpdateStrategy.TRY_FETCH_ELSE_CACHED
        return repository.getProductById(params.id, strategy)
    }
}