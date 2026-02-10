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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

class ProductRepositoryImpl(
    private val remoteDataSource: ProductRemoteDataSource,
    private val productMapper: ProductMapper,
    private val paginatedProductsMapper: PaginatedProductsMapper,
    private val exceptionMapper: ExceptionMapper,
    private val dispatchers: DispatcherProvider
) : ProductRepository {

    override suspend fun getProducts(pageRequest: PageRequest): Result<PaginatedData<Product>> =
        withContext(dispatchers.io) {
            try {
                val apiResponse = remoteDataSource.getProducts(pageRequest)
                val domainData = paginatedProductsMapper.map(apiResponse)
                Result.Success(domainData)
            } catch (e: Exception) {
                Result.Error(exceptionMapper.map(e))
            }
        }

    override suspend fun getProductById(id: String): Result<Product> =
        withContext(dispatchers.io) {
            try {
                val dto = remoteDataSource.getProductById(id)
                val product = productMapper.map(dto)
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
                Result.Success(domainData)
            } catch (e: Exception) {
                Result.Error(exceptionMapper.map(e))
            }
        }

    override fun observeProducts(): Flow<List<Product>> = flow {
        when (val result = getProducts()) {
            is Result.Success -> emit(result.data.items)
            is Result.Error -> emit(emptyList())
        }
    }.flowOn(dispatchers.io)
}