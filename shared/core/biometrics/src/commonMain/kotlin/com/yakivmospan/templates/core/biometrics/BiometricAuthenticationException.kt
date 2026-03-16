package com.yakivmospan.templates.core.biometrics

class BiometricAuthenticationException(
    val reason: BiometricAuthenticationFailureReason,
    message: String
) : Exception(message)

enum class BiometricAuthenticationFailureReason {
    USER_CANCELLED,
    LOCKOUT,        // Too many failed attempts
    LOCKOUT_PERMANENT,
    NOT_AVAILABLE,
    NOT_ENROLLED,
    UNKNOWN
}