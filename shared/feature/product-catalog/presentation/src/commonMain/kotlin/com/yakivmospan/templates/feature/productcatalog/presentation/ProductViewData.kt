package com.yakivmospan.templates.feature.productcatalog.presentation

import com.yakivmospan.templates.core.common.Mapper
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product

data class ProductViewData(
    val id: Int,
    val title: String,
    val description: String,
    val price: Double,
    val formattedPrice: String,
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
            formattedPrice = formatPrice(input.price),
            imageUrl = input.imageUrl,
            isFavorite = input.isFavorite
        )
    }

    // Not String.format() in commonMain.., manually formatting to 0.00
    // Had no time to search for alternatives or write better ebullition.
    // In real project this should consider locale and currency, but for demo purposes this is enough.
    private fun formatPrice(price: Double): String {
        val rounded = price.toString()
        val parts = rounded.split(".")
        val decimals = parts.getOrElse(1) { "0" }.take(2).padEnd(2, '0')
        return "$${parts[0]}.$decimals"
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