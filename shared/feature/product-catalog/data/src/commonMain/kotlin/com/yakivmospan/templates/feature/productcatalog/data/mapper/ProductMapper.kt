package com.yakivmospan.templates.feature.productcatalog.data.mapper

import com.yakivmospan.templates.core.common.Mapper
import com.yakivmospan.templates.feature.productcatalog.data.remote.dto.ProductResponse
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product

class ProductMapper : Mapper<ProductResponse, Product> {
    override fun map(input: ProductResponse): Product {
        return Product(
            id = input.id,
            title = input.title,
            description = input.description,
            price = input.price,
            imageUrl = input.thumbnail,
        )
    }
}