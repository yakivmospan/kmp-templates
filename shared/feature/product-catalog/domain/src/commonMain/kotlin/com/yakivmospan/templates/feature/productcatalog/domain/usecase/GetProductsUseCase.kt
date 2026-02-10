package com.yakivmospan.templates.feature.productcatalog.domain.usecase

import com.yakivmospan.templates.core.common.PageRequest
import com.yakivmospan.templates.core.common.PaginatedData
import com.yakivmospan.templates.core.common.Result
import com.yakivmospan.templates.core.domain.UseCase
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product
import com.yakivmospan.templates.feature.productcatalog.domain.repository.ProductRepository

class GetProductsUseCase(
    private val repository: ProductRepository
) : UseCase<PageRequest, Result<PaginatedData<Product>>>() {

    override suspend fun invoke(params: PageRequest): Result<PaginatedData<Product>> {
        return repository.getProducts(params)
    }
}