package com.yakivmospan.templates.feature.productcatalog.data.repository

import com.yakivmospan.templates.core.common.CoroutineScopeProvider
import com.yakivmospan.templates.core.common.DispatcherProvider
import com.yakivmospan.templates.core.common.PageRequest
import com.yakivmospan.templates.core.common.PaginatedData
import com.yakivmospan.templates.core.common.Result
import com.yakivmospan.templates.core.data.cache.InMemoryKeyedCache
import com.yakivmospan.templates.core.data.cache.TimestampExpirationValidator
import com.yakivmospan.templates.core.data.mapper.ExceptionMapper
import com.yakivmospan.templates.core.data.singleflight.SingleFlightCommand
import com.yakivmospan.templates.core.domain.UpdateStrategy
import com.yakivmospan.templates.feature.productcatalog.data.local.FavoriteLocalDataSource
import com.yakivmospan.templates.feature.productcatalog.data.mapper.FavoriteProductEntityMapper
import com.yakivmospan.templates.feature.productcatalog.data.mapper.PaginatedProductsMapper
import com.yakivmospan.templates.feature.productcatalog.data.mapper.ProductMapper
import com.yakivmospan.templates.feature.productcatalog.data.remote.ProductRemoteDataSource
import com.yakivmospan.templates.feature.productcatalog.domain.model.Product
import com.yakivmospan.templates.feature.productcatalog.domain.repository.ProductRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

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

    private val getRemoteProductByIdSingleFlightCmd = SingleFlightCommand(scopes.appScope, ::getRemoteProductByIdExecutor)

    // simulating short living in memory cache
    private val productByIdCache = InMemoryKeyedCache<Int, Result<Product>> { TimestampExpirationValidator(30.seconds) }

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

    override suspend fun getProductById(id: Int, strategy: UpdateStrategy): Result<Product> = when (strategy) {
        UpdateStrategy.ALWAYS_FETCH -> getRemoteProductByIdSingleFlightCmd.execute(id)
        UpdateStrategy.ALWAYS_CACHED -> getCachedProduct(id)
        UpdateStrategy.TRY_FETCH_ELSE_CACHED -> tryFetchProductElseCached(id)
        UpdateStrategy.TRY_CACHED_ELSE_FETCH -> tryCachedProductElseFetch(id)
    }

    private suspend fun getCachedProduct(id: Int): Result<Product> {
        return productByIdCache.get(id) ?: getLocalFavorite(id)
    }

    private suspend fun tryCachedProductElseFetch(id: Int): Result<Product> {
        val cached = getCachedProduct(id)
        return if (cached is Result.Error) getRemoteProductByIdSingleFlightCmd.execute(id) else cached
    }

    private suspend fun tryFetchProductElseCached(id: Int): Result<Product> {
        val fetched = getRemoteProductByIdSingleFlightCmd.execute(id)
        return if (fetched is Result.Error) getCachedProduct(id) else fetched

    }

    private suspend fun getLocalFavorite(id: Int): Result<Product> {
        return try {
            val entity = localDataSource.getFavoriteById(id)
            Result.Success(favoriteProductEntityMapper.map(entity ?: throw Exception("Product not found.")))
        } catch (e: Exception) {
            Result.Error(exceptionMapper.map(e))
        }
    }

    private suspend fun getRemoteProductByIdExecutor(id: Int): Result<Product> = withContext(dispatchers.io) {
        try {
            delay(5.seconds) // simulating heavy operation
            val dto = remoteDataSource.getProductById(id.toString())
            val product = productMapper.map(dto).copy(
                isFavorite = localDataSource.isFavorite(dto.id)
            )
            Result.Success(product).also { productByIdCache.put(id, it) }
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