package com.yakivmospan.templates.core.domain

sealed class DomainException(
    val errorMessage: String
) : Exception(errorMessage) {

    class NetworkError(
        errorMessage: String
    ) : DomainException(errorMessage)

    class ServerError(
        errorMessage: String,
        val errorCode: String? = null,
        val errorDetails: Map<String, String>? = null
    ) : DomainException(errorMessage) {
        override fun toString(): String {
            return buildString {
                append("ServerError(errorMessage='$errorMessage'")
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

    class Unauthorized(
        errorMessage: String,
        val errorCode: String? = null
    ) : DomainException(errorMessage)

    class NotFound(
        errorMessage: String,
        val errorCode: String? = null
    ) : DomainException(errorMessage)

    class Timeout(
        errorMessage: String
    ) : DomainException(errorMessage)

    class ValidationError(
        errorMessage: String,
        val fieldErrors: Map<String, String> = emptyMap()
    ) : DomainException(errorMessage)

    class Unknown(
        errorMessage: String,
    ) : DomainException(errorMessage)
}