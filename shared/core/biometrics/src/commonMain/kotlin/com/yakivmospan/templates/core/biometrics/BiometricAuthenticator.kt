package com.yakivmospan.templates.core.biometrics

import com.yakivmospan.templates.core.common.Result

/**
 * Platform-agnostic biometric / device-security interface.
 */
interface BiometricAuthenticator {
    /** True if the device has biometric hardware AND at least one enrolled credential. */
    suspend fun isBiometricAvailable(): Boolean

    /** True if the user has a device PIN / pattern / password set. */
    suspend fun isDeviceSecured(): Boolean

    /**
     * Shows the platform biometric prompt and suspends until the user completes or
     * dismisses it. Returns [Result.Success] on approval, [Result.Error] on failure
     * or cancellation.
     *
     * Android: wraps BiometricPrompt callbacks in suspendCancellableCoroutine.
     * iOS: wraps LAContext.evaluatePolicy in suspendCancellableCoroutine.
     */
    suspend fun authenticate(): Result<Unit>

    /**
     * Opens the system Settings screen for biometric / security enrollment.
     * Platform handles the actual intent / URL scheme.
     */
    fun openBiometricSettings()
}
