package com.yakivmospan.templates.feature.productcatalog.domain.repository

import com.yakivmospan.templates.core.common.PageRequest
import com.yakivmospan.templates.core.common.PaginatedData
import com.yakivmospan.templates.core.common.Result
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    suspend fun getProducts(pageRequest: PageRequest = PageRequest()): Result<PaginatedData<Product>>
    suspend fun getProductById(id: String): Result<Product>
    suspend fun searchProducts(query: String, pageRequest: PageRequest = PageRequest()): Result<PaginatedData<Product>>
    fun observeProducts(): Flow<List<Product>>
}