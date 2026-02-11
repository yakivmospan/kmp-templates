package com.yakivmospan.templates.feature.productcatalog.presentation

import com.yakivmospan.templates.core.common.Mapper
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product

data class ProductViewData(
    val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val imageUrl: String,
    val isFavorite: Boolean
)

class ProductToViewDataMapper : Mapper<Product, ProductViewData> {
    override fun map(input: Product): ProductViewData {
        return ProductViewData(
            id = input.id,
            title = input.title,
            description = input.description,
            price = input.price,
            imageUrl = input.imageUrl,
            isFavorite = input.isFavorite
        )
    }
}

class ProductViewDataToEntityMapper : Mapper<ProductViewData, Product> {
    override fun map(input: ProductViewData): Product {
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
