package com.yakivmospan.templates.feature.productcatalog.data.mapper

import com.yakivmospan.templates.core.common.Mapper
import com.yakivmospan.templates.core.common.PaginatedData
import com.yakivmospan.templates.feature.productcatalog.data.remote.dto.PaginatedProductResponse
import com.yakivmospan.templates.feature.productcatalog.data.remote.dto.ProductResponse
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product

class PaginatedProductsMapper(
    private val productMapper: ProductMapper
) : Mapper<PaginatedProductResponse<ProductResponse>, PaginatedData<Product>> {

    override fun map(input: PaginatedProductResponse<ProductResponse>): PaginatedData<Product> {
        // Ensure limit is positive to avoid division by zero
        val safeLimit = if (input.limit > 0) input.limit else 1

        // Calculate current page from skip and limit
        val currentPage = (input.skip / safeLimit) + 1
        val totalPages = (input.total + safeLimit - 1) / safeLimit

        return PaginatedData(
            items = input.products.map { productMapper.map(it) },
            currentPage = currentPage,
            totalPages = totalPages,
            totalItems = input.total,
            hasNextPage = input.skip + input.limit < input.total,
            hasPreviousPage = input.skip > 0
        )
    }
}