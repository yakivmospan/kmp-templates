package com.yakivmospan.templates.core.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    @SerialName("data") val data: T? = null,
    @SerialName("message") val message: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("error") val error: ApiError? = null
)

@Serializable
data class ApiError(
    @SerialName("code") val code: String,
    @SerialName("message") val message: String,
    @SerialName("details") val details: Map<String, String>? = null
)