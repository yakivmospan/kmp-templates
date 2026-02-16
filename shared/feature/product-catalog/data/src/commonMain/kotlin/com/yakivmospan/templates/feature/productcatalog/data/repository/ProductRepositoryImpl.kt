package com.yakivmospan.templates.feature.productcatalog.data.repository

import com.yakivmospan.templates.core.common.CoroutineScopeProvider
import com.yakivmospan.templates.core.common.DispatcherProvider
import com.yakivmospan.templates.core.common.PageRequest
import com.yakivmospan.templates.core.common.PaginatedData
import com.yakivmospan.templates.core.common.Result
import com.yakivmospan.templates.core.data.SingleFlightCache
import com.yakivmospan.templates.core.data.mapper.ExceptionMapper
import com.yakivmospan.templates.feature.productcatalog.data.local.FavoriteLocalDataSource
import com.yakivmospan.templates.feature.productcatalog.data.mapper.FavoriteProductEntityMapper
import com.yakivmospan.templates.feature.productcatalog.data.mapper.PaginatedProductsMapper
import com.yakivmospan.templates.feature.productcatalog.data.mapper.ProductMapper
import com.yakivmospan.templates.feature.productcatalog.data.remote.ProductRemoteDataSource
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product
import com.yakivmospan.templates.feature.productcatalog.domain.repository.ProductRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.minutes

class ProductRepositoryImpl(
    private val remoteDataSource: ProductRemoteDataSource,
    private val localDataSource: FavoriteLocalDataSource,
    private val productMapper: ProductMapper,
    private val paginatedProductsMapper: PaginatedProductsMapper,
    private val favoriteProductEntityMapper: FavoriteProductEntityMapper,
    private val exceptionMapper: ExceptionMapper,
    private val dispatchers: DispatcherProvider,
    private val scopes: CoroutineScopeProvider
) : ProductRepository {

    private val getProductByIdSingleFlightCache = SingleFlightCache<Int, Result<Product>>(
        scope = scopes.appScope, worker = ::getProductByIdWorker, keepFor = 5.minutes
    )

    override suspend fun getProducts(pageRequest: PageRequest): Result<PaginatedData<Product>> =
        withContext(dispatchers.io) {
            try {
                val apiResponse = remoteDataSource.getProducts(pageRequest)
                val domainData = paginatedProductsMapper.map(apiResponse)

                // Merge with local favorites
                val mergedProducts = domainData.items.map { product ->
                    val isFavorite = localDataSource.isFavorite(product.id)
                    product.copy(isFavorite = isFavorite)
                }

                val mergedData = domainData.copy(items = mergedProducts)
                Result.Success(mergedData)
            } catch (e: Exception) {
                Result.Error(exceptionMapper.map(e))
            }
        }

    override suspend fun getProductById(id: Int): Result<Product> {
        return getProductByIdSingleFlightCache.get(id)
    }

    private suspend fun getProductByIdWorker(id: Int): Result<Product> = withContext(dispatchers.io) {
        try {
            val dto = remoteDataSource.getProductById(id.toString())
            val product = productMapper.map(dto).copy(
                isFavorite = localDataSource.isFavorite(dto.id)
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
                val mergedProducts = domainData.items.map { product ->
                    val isFavorite = localDataSource.isFavorite(product.id)
                    product.copy(isFavorite = isFavorite)
                }

                val mergedData = domainData.copy(items = mergedProducts)
                Result.Success(mergedData)
            } catch (e: Exception) {
                Result.Error(exceptionMapper.map(e))
            }
        }

    override suspend fun addToFavorites(product: Product): Result<Unit> = withContext(dispatchers.io) {
        try {
            localDataSource.insertFavorite(product)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(exceptionMapper.map(e))
        }
    }

    override suspend fun removeFromFavorites(productId: Int): Result<Unit> = withContext(dispatchers.io) {
        try {
            localDataSource.deleteFavorite(productId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(exceptionMapper.map(e))
        }
    }

    override suspend fun isFavorite(productId: Int): Result<Boolean> = withContext(dispatchers.io) {
        try {
            val isFavorite = localDataSource.isFavorite(productId)
            Result.Success(isFavorite)
        } catch (e: Exception) {
            Result.Error(exceptionMapper.map(e))
        }
    }

    override fun observeFavorites(): Flow<List<Product>> =
        localDataSource.observeFavorites()
            .map { entities ->
                entities.map { favoriteProductEntityMapper.map(it) }
            }
}