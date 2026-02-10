package com.yakivmospan.templates.core.data

import com.yakivmospan.templates.core.common.Mapper
import com.yakivmospan.templates.core.domain.DomainException
import com.yakivmospan.templates.core.network.ApiException

interface ExceptionMapper : Mapper<Throwable, DomainException>

class DefaultExceptionMapper : ExceptionMapper {
    override fun map(input: Throwable): DomainException {
        return when (input) {
            is ApiException.NetworkException ->
                DomainException.NetworkError(
                    errorMessage = input.errorMessage
                )

            is ApiException.ServerException ->
                DomainException.ServerError(
                    errorMessage = input.errorMessage,
                    errorCode = input.errorCode,
                    errorDetails = input.errorDetails
                )

            is ApiException.UnauthorizedException ->
                DomainException.Unauthorized(
                    errorMessage = input.errorMessage,
                    errorCode = input.errorCode
                )

            is ApiException.NotFoundException ->
                DomainException.NotFound(
                    errorMessage = input.errorMessage,
                    errorCode = input.errorCode
                )

            is ApiException.TimeoutException ->
                DomainException.Timeout(
                    errorMessage = input.errorMessage
                )

            is ApiException.UnknownException ->
                DomainException.Unknown(
                    errorMessage = input.errorMessage
                )

            // If it's already a DomainException, return as-is
            is DomainException -> input

            // Fallback for any other exception
            else -> DomainException.Unknown(
                errorMessage = input.message ?: "An unexpected error occurred",
            )
        }
    }
}