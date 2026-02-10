package com.yakivmospan.templates.feature.productcatalog.presentation

import com.yakivmospan.templates.core.common.Mapper
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product

class ProductCatalogViewData(
    val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val imageUrl: String
)

class ProductCatalogViewDataMapper : Mapper<Product, ProductCatalogViewData> {
    override fun map(input: Product): ProductCatalogViewData {

        return ProductCatalogViewData(
            id = input.id,
            title = input.title,
            description = input.description,
            price = input.price,
            imageUrl = input.imageUrl
        )
    }
}