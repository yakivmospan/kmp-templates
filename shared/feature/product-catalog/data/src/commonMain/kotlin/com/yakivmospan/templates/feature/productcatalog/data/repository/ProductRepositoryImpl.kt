package com.yakivmospan.templates.feature.productcatalog.data.repository

import com.yakivmospan.templates.core.common.DispatcherProvider
import com.yakivmospan.templates.core.common.PageRequest
import com.yakivmospan.templates.core.common.PaginatedData
import com.yakivmospan.templates.core.common.Result
import com.yakivmospan.templates.core.data.mapper.ExceptionMapper
import com.yakivmospan.templates.feature.productcatalog.data.mapper.PaginatedProductsMapper
import com.yakivmospan.templates.feature.productcatalog.data.mapper.ProductMapper
import com.yakivmospan.templates.feature.productcatalog.data.remote.ProductRemoteDataSource
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product
import com.yakivmospan.templates.feature.productcatalog.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProductRepositoryImpl(
    private val remoteDataSource: ProductRemoteDataSource,
    private val productMapper: ProductMapper,
    private val paginatedProductsMapper: PaginatedProductsMapper,
    private val exceptionMapper: ExceptionMapper,
    private val dispatchers: DispatcherProvider
) : ProductRepository {

    // TODO: Replace with actual local database (Room/SQLDelight) Flow
    private val favoritesMapFlow = MutableStateFlow<Map<Int, Product>>(emptyMap())

    // Internal helper - not exposed in interface
    private fun getFavoriteIds(): Set<Int> = favoritesMapFlow.value.keys

    override suspend fun getProducts(pageRequest: PageRequest): Result<PaginatedData<Product>> =
        withContext(dispatchers.io) {
            try {
                val apiResponse = remoteDataSource.getProducts(pageRequest)
                val domainData = paginatedProductsMapper.map(apiResponse)

                // Merge with local favorites
                val favoriteIds = getFavoriteIds()
                val mergedProducts = domainData.items.map { product ->
                    product.copy(isFavorite = favoriteIds.contains(product.id))
                }

                val mergedData = domainData.copy(items = mergedProducts)
                Result.Success(mergedData)
            } catch (e: Exception) {
                Result.Error(exceptionMapper.map(e))
            }
        }

    override suspend fun getProductById(id: Int): Result<Product> =
        withContext(dispatchers.io) {
            try {
                val dto = remoteDataSource.getProductById(id.toString())
                val product = productMapper.map(dto).copy(
                    isFavorite = getFavoriteIds().contains(dto.id)
                )
                Result.Success(product)
            } catch (e: Exception) {
                Result.Error(exceptionMapper.map(e))
            }
        }

    override suspend fun searchProducts(query: String, pageRequest: PageRequest): Result<PaginatedData<Product>> =
        withContext(dispatchers.io) {
            try {
                val apiResponse = remoteDataSource.searchProducts(query, pageRequest)
                val domainData = paginatedProductsMapper.map(apiResponse)

                // Merge with local favorites
                val favoriteIds = getFavoriteIds()
                val mergedProducts = domainData.items.map { product ->
                    product.copy(isFavorite = favoriteIds.contains(product.id))
                }

                val mergedData = domainData.copy(items = mergedProducts)
                Result.Success(mergedData)
            } catch (e: Exception) {
                Result.Error(exceptionMapper.map(e))
            }
        }

    // Favorites implementation
    override suspend fun addToFavorites(product: Product): Result<Unit> =
        withContext(dispatchers.io) {
            try {
                // TODO: Replace with database insert operation
                val updatedMap = favoritesMapFlow.value.toMutableMap()
                updatedMap[product.id] = product.copy(isFavorite = true)
                favoritesMapFlow.value = updatedMap
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(exceptionMapper.map(e))
            }
        }

    override suspend fun removeFromFavorites(productId: Int): Result<Unit> = withContext(dispatchers.io) {
        try {
            // TODO: Replace with database delete operation
            val updatedMap = favoritesMapFlow.value.toMutableMap()
            updatedMap.remove(productId)
            favoritesMapFlow.value = updatedMap
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(exceptionMapper.map(e))
        }
    }

    override suspend fun isFavorite(productId: Int): Result<Boolean> = withContext(dispatchers.io) {
        try {
            // TODO: Replace with database query
            Result.Success(favoritesMapFlow.value.containsKey(productId))
        } catch (e: Exception) {
            Result.Error(exceptionMapper.map(e))
        }
    }

    override fun observeFavorites(): Flow<List<Product>> = favoritesMapFlow
        .map { it.values.toList() }
        .flowOn(dispatchers.io)
}