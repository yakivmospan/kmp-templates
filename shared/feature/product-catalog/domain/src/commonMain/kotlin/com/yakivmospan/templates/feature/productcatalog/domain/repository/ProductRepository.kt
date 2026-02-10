package com.yakivmospan.templates.feature.productcatalog.domain.repository

import com.yakivmospan.templates.core.common.PageRequest
import com.yakivmospan.templates.core.common.PaginatedData
import com.yakivmospan.templates.core.common.Result
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    suspend fun getProducts(pageRequest: PageRequest): Result<PaginatedData<Product>>
    suspend fun getProductById(id: Int): Result<Product>
    suspend fun searchProducts(query: String, pageRequest: PageRequest): Result<PaginatedData<Product>>

    suspend fun addToFavorites(product: Product): Result<Unit>
    suspend fun removeFromFavorites(productId: Int): Result<Unit>
    suspend fun isFavorite(productId: Int): Result<Boolean>
    fun observeFavorites(): Flow<List<Product>>
}