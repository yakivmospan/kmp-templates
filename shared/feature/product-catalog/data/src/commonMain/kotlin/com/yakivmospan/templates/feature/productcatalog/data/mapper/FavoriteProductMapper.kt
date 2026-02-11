package com.yakivmospan.templates.feature.productcatalog.data.mapper

import com.yakivmospan.templates.core.common.Mapper
import com.yakivmospan.templates.feature.productcatalog.data.local.database.FavoriteProductEntity
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product

class FavoriteProductEntityMapper : Mapper<FavoriteProductEntity, Product> {

    override fun map(input: FavoriteProductEntity): Product {
        return Product(
            id = input.id.toInt(),
            title = input.title,
            description = input.description,
            price = input.price,
            imageUrl = input.imageUrl,
            isFavorite = true // All favorites are marked as favorite by definition
        )
    }
}