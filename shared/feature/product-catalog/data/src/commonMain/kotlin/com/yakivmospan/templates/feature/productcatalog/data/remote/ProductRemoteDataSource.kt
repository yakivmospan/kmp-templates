package com.yakivmospan.templates.feature.productcatalog.data.remote

import com.yakivmospan.templates.core.common.PageRequest
import com.yakivmospan.templates.core.common.getOrThrow
import com.yakivmospan.templates.core.network.NetworkErrorHandler
import com.yakivmospan.templates.feature.productcatalog.data.remote.dto.PaginatedProductResponse
import com.yakivmospan.templates.feature.productcatalog.data.remote.dto.ProductResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

class ProductRemoteDataSource(
    private val httpClient: HttpClient
) {
    private val baseUrl = "https://dummyjson.com"

    suspend fun getProducts(pageRequest: PageRequest): PaginatedProductResponse<ProductResponse> {
        return NetworkErrorHandler.safeApiCall<PaginatedProductResponse<ProductResponse>> {
            httpClient.get("$baseUrl/products") {
                val skip = (pageRequest.page - 1) * pageRequest.pageSize
                parameter("limit", pageRequest.pageSize)
                parameter("skip", skip)
            }.body()
        }.getOrThrow()
    }

    suspend fun getProductById(id: String): ProductResponse {
        return NetworkErrorHandler.safeApiCall {
            httpClient.get("$baseUrl/products/$id").body<ProductResponse>()
        }.getOrThrow()
    }

    suspend fun searchProducts(query: String, pageRequest: PageRequest): PaginatedProductResponse<ProductResponse> {
        return NetworkErrorHandler.safeApiCall<PaginatedProductResponse<ProductResponse>> {
            httpClient.get("$baseUrl/products/search") {
                parameter("q", query)
                val skip = (pageRequest.page - 1) * pageRequest.pageSize
                parameter("limit", pageRequest.pageSize)
                parameter("skip", skip)
            }.body()
        }.getOrThrow()
    }
}