package com.yakivmospan.templates.core.network

import com.yakivmospan.templates.core.common.Result
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import kotlinx.io.IOException

object NetworkErrorHandler {
    suspend fun <T> safeApiCall(apiCall: suspend () -> T): Result<T> {
        return try {
            Result.Success(apiCall())
        } catch (e: Exception) {
            Result.Error(mapException(e))
        }
    }

    // If we will have a default ApiResponse with error data structure.
    suspend fun <T> safeApiCallWithResponse(
        apiCall: suspend () -> ApiResponse<T>
    ): Result<T> {
        return try {
            val response = apiCall()
            when {
                response.data != null -> Result.Success(response.data)
                response.error != null -> Result.Error(mapApiError(response.error))
                else -> Result.Error(createUnknownException(response.message))
            }
        } catch (e: Exception) {
            Result.Error(mapException(e))
        }
    }

    private fun mapApiError(apiError: ApiError): ApiException {
        return ApiException.ServerException(
            code = 400, // Default code, could be enhanced if ApiError includes HTTP code
            errorMessage = apiError.message,
            errorCode = apiError.code,
            errorDetails = apiError.details
        )
    }

    private fun mapException(exception: Exception): ApiException {
        return when (exception) {
            is ResponseException -> mapResponseException(exception)
            is IOException -> mapIOException(exception)
            is HttpRequestTimeoutException -> mapTimeoutException()
            is ApiException -> exception
            else -> createUnknownException(exception.message)
        }
    }

    private fun createUnknownException(message: String?): ApiException {
        return ApiException.UnknownException(
            errorMessage = message ?: "Unknown error occurred"
        )
    }

    private fun mapResponseException(exception: ResponseException): ApiException {
        return when (val statusCode = exception.response.status.value) {
            401 -> ApiException.UnauthorizedException(errorMessage = "Unauthorized access - authentication required")
            404 -> ApiException.NotFoundException(errorMessage = "Resource not found")

            in 500..599 -> ApiException.ServerException(
                code = statusCode,
                errorMessage = "Server error occurred (HTTP $statusCode)"
            )

            else -> ApiException.ServerException(
                code = statusCode,
                errorMessage = exception.message ?: "HTTP error $statusCode"
            )
        }
    }

    private fun mapIOException(exception: IOException) = ApiException.NetworkException(
        errorMessage = exception.message?.let { "Network error: $it" } ?: "Network connection failed"
    )

    private fun mapTimeoutException() = ApiException.TimeoutException(errorMessage = "Request timed out")
}