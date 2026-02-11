package com.yakivmospan.templates.feature.productcatalog.data.local

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.yakivmospan.templates.core.common.DispatcherProvider
import com.yakivmospan.templates.feature.productcatalog.data.local.database.FavoriteProductEntity
import com.yakivmospan.templates.feature.productcatalog.data.local.database.ProductCatalogDatabase
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface FavoriteLocalDataSource {
    suspend fun insertFavorite(product: Product)
    suspend fun deleteFavorite(productId: Int)
    fun observeFavorites(): Flow<List<FavoriteProductEntity>>
    suspend fun isFavorite(productId: Int): Boolean
    suspend fun getFavoriteById(productId: Int): FavoriteProductEntity?
    suspend fun deleteAllFavorites()
}

/**
 * Implementation of FavoriteLocalDataSource using SQLDelight.
 */
class FavoriteLocalDataSourceImpl(
    private val database: ProductCatalogDatabase,
    private val dispatchers: DispatcherProvider
) : FavoriteLocalDataSource {

    private val queries = database.favoriteProductEntityQueries

    override suspend fun insertFavorite(product: Product): Unit = withContext(dispatchers.io) {
        queries.insertFavorite(
            id = product.id.toLong(),
            title = product.title,
            description = product.description,
            price = product.price,
            imageUrl = product.imageUrl
        )
    }

    override suspend fun deleteFavorite(productId: Int): Unit = withContext(dispatchers.io) {
        queries.deleteFavorite(productId.toLong())
    }

    override fun observeFavorites(): Flow<List<FavoriteProductEntity>> {
        return queries.getAllFavorites()
            .asFlow()
            .mapToList(dispatchers.io)
    }

    override suspend fun isFavorite(productId: Int): Boolean = withContext(dispatchers.io) {
        queries.isFavorite(productId.toLong()).executeAsOne()
    }

    override suspend fun getFavoriteById(productId: Int): FavoriteProductEntity? = withContext(dispatchers.io) {
        queries.getFavoriteById(productId.toLong()).executeAsOneOrNull()
    }

    override suspend fun deleteAllFavorites(): Unit = withContext(dispatchers.io) {
        queries.deleteAllFavorites()
    }
}








