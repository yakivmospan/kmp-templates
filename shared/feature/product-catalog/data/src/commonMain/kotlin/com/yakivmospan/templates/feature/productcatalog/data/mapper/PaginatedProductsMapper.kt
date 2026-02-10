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
        // Calculate current page from skip and limit
        val currentPage = (input.skip / input.limit) + 1
        val totalPages = (input.total + input.limit - 1) / input.limit

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