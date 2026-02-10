package com.yakivmospan.templates.feature.productcatalog.presentation

import com.yakivmospan.templates.core.common.Mapper
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product

data class ProductDetailsViewData(
    val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val isFavorite: Boolean
)

class ProductToDetailsViewDataMapper : Mapper<Product, ProductDetailsViewData> {
    override fun map(input: Product): ProductDetailsViewData {
        return ProductDetailsViewData(
            id = input.id,
            title = input.title,
            description = input.description,
            price = input.price,
            imageUrl = input.imageUrl,
            isFavorite = input.isFavorite
        )
    }
}

class DetailsViewDataToProductMapper : Mapper<ProductDetailsViewData, Product> {
    override fun map(input: ProductDetailsViewData): Product {
        return Product(
            id = input.id,
            title = input.title,
            description = input.description,
            price = input.price,
            imageUrl = input.imageUrl,
            isFavorite = input.isFavorite
        )
    }

}
