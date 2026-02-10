package com.yakivmospan.templates.feature.productcatalog.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PaginatedProductResponse<T>(
    @SerialName("products") val products: List<T>,
    @SerialName("total") val total: Int,
    @SerialName("skip") val skip: Int,
    @SerialName("limit") val limit: Int
)