package com.yakivmospan.templates.feature.productcatalog.domain.usecase

import com.yakivmospan.templates.core.common.PageRequest
import com.yakivmospan.templates.core.common.PaginatedData
import com.yakivmospan.templates.core.common.Result
import com.yakivmospan.templates.core.domain.UseCase
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product
import com.yakivmospan.templates.feature.productcatalog.domain.repository.ProductRepository

data class SearchProductsParams(
    val query: String,
    val pageRequest: PageRequest = PageRequest()
)

class SearchProductsUseCase(
    private val repository: ProductRepository
) : UseCase<SearchProductsParams, Result<PaginatedData<Product>>>() {

    override suspend fun invoke(params: SearchProductsParams): Result<PaginatedData<Product>> {
        return repository.searchProducts(params.query, params.pageRequest)
    }
}