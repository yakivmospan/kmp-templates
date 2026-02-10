package com.yakivmospan.templates.core.network

sealed class ApiException(
    val errorMessage: String
) : Exception(errorMessage) {

    class NetworkException(
        errorMessage: String
    ) : ApiException(errorMessage)

    class ServerException(
        val code: Int,
        errorMessage: String,
        val errorCode: String? = null,
        val errorDetails: Map<String, String>? = null
    ) : ApiException(errorMessage) {
        override fun toString(): String {
            return buildString {
                append("ServerException(httpCode=$code, errorMessage='$errorMessage'")
                errorCode?.let { append(", errorCode='$it'") }
                errorDetails?.let {
                    if (it.isNotEmpty()) {
                        append(", details=$it")
                    }
                }
                append(")")
            }
        }
    }

    class UnauthorizedException(
        errorMessage: String,
        val errorCode: String? = null
    ) : ApiException(errorMessage)

    class NotFoundException(
        errorMessage: String,
        val errorCode: String? = null
    ) : ApiException(errorMessage)

    class TimeoutException(
        errorMessage: String
    ) : ApiException(errorMessage)

    class UnknownException(
        errorMessage: String
    ) : ApiException(errorMessage)
}