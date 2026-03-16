package com.yakivmospan.templates.core.biometrics

import com.yakivmospan.templates.core.common.Result
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAErrorAuthenticationFailed
import platform.LocalAuthentication.LAErrorBiometryLockout
import platform.LocalAuthentication.LAErrorBiometryNotAvailable
import platform.LocalAuthentication.LAErrorBiometryNotEnrolled
import platform.LocalAuthentication.LAErrorUserCancel
import platform.LocalAuthentication.LAErrorUserFallback
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IOSBiometricAuthenticatorImpl : BiometricAuthenticator {
    override suspend fun isBiometricAvailable(): Boolean {
        val context = LAContext()
        return context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, error = null)
    }

    override suspend fun isDeviceSecured(): Boolean {
        val context = LAContext()
        return context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, error = null)
    }

    override suspend fun authenticate(): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
            val context = LAContext()

            continuation.invokeOnCancellation { context.invalidate() }

            context.evaluatePolicy(
                policy = LAPolicyDeviceOwnerAuthenticationWithBiometrics,
                localizedReason = "Confirm your identity",
            ) { success, error ->
                if (!continuation.isActive) return@evaluatePolicy

                if (success) {
                    continuation.resume(Result.Success(Unit))
                } else {
                    continuation.resume(Result.Error(error.toException()))
                }
            }
        }
    }

    override fun openBiometricSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        UIApplication.sharedApplication.openURL(url)
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun NSError?.toException(): BiometricAuthenticationException {
        val reason = this?.toFailureReason() ?: BiometricAuthenticationFailureReason.UNKNOWN
        val message = this?.localizedDescription ?: "Unknown biometric error"
        return BiometricAuthenticationException(reason, message)
    }

    private fun NSError.toFailureReason() = when (code) {
        LAErrorUserCancel,
        LAErrorUserFallback -> BiometricAuthenticationFailureReason.USER_CANCELLED

        LAErrorBiometryLockout -> BiometricAuthenticationFailureReason.LOCKOUT
        LAErrorBiometryNotAvailable -> BiometricAuthenticationFailureReason.NOT_AVAILABLE
        LAErrorBiometryNotEnrolled -> BiometricAuthenticationFailureReason.NOT_ENROLLED
        LAErrorAuthenticationFailed -> BiometricAuthenticationFailureReason.UNKNOWN
        else -> BiometricAuthenticationFailureReason.UNKNOWN
    }
}