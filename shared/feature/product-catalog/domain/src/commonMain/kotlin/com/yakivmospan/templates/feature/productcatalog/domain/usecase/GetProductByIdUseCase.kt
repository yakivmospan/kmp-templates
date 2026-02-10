package com.yakivmospan.templates.feature.productcatalog.domain.usecase

import com.yakivmospan.templates.core.common.Result
import com.yakivmospan.templates.core.domain.UseCase
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product
import com.yakivmospan.templates.feature.productcatalog.domain.repository.ProductRepository

class GetProductByIdUseCase(
    private val repository: ProductRepository
) : UseCase<String, Result<Product>>() {

    override suspend fun invoke(params: String): Result<Product> {
        return repository.getProductById(params)
    }
}