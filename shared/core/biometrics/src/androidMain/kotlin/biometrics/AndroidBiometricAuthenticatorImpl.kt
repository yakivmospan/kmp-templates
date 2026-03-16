package com.yakivmospan.templates.core.biometrics

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.yakivmospan.templates.core.common.Result
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidBiometricAuthenticatorImpl(
    private val activityProvider: BiometricsActivityProvider
) : BiometricAuthenticator {

    override suspend fun isBiometricAvailable(): Boolean {
        val activity = activityProvider.getActivity() ?: return false
        return BiometricManager.from(activity).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    override suspend fun isDeviceSecured(): Boolean {
        val activity = activityProvider.getActivity() ?: return false
        return BiometricManager.from(activity).canAuthenticate(DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
    }

    override suspend fun authenticate(): Result<Unit> {
        val activity = activityProvider.getActivity()
            ?: return Result.Error(IllegalStateException("No active Activity"))

        return suspendCancellableCoroutine { continuation ->
            val prompt = buildPrompt(activity) { result ->
                if (continuation.isActive) continuation.resume(result)
            }
            continuation.invokeOnCancellation { prompt.cancelAuthentication() }
            prompt.authenticate(buildPromptInfo())
        }
    }

    override fun openBiometricSettings() {
        val activity = activityProvider.getActivity() ?: return
        activity.startActivity(buildBiometricSettingsIntent())
    }

    // ── Builders ──────────────────────────────────────────────────────────────

    private fun buildPrompt(
        activity: FragmentActivity,
        onResult: (Result<Unit>) -> Unit
    ): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = buildCallback(onResult)
        return BiometricPrompt(activity, executor, callback)
    }

    private fun buildCallback(
        onResult: (Result<Unit>) -> Unit
    ) = object : BiometricPrompt.AuthenticationCallback() {

        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onResult(Result.Success(Unit))
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            onResult(Result.Error(errorCode.toException(errString.toString())))
        }

        override fun onAuthenticationFailed() {
            // Called on each failed attempt — do not resolve, let the user retry
        }
    }

    private fun buildPromptInfo() = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Biometric Authentication")
        .setSubtitle("Confirm your identity")
        .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        .build()

    private fun buildBiometricSettingsIntent() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                putExtra(
                    Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                    BIOMETRIC_STRONG or DEVICE_CREDENTIAL
                )
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private fun Int.toException(message: String) = BiometricAuthenticationException(
        reason = toFailureReason(),
        message = message
    )

    private fun Int.toFailureReason() = when (this) {
        BiometricPrompt.ERROR_USER_CANCELED,
        BiometricPrompt.ERROR_NEGATIVE_BUTTON -> BiometricAuthenticationFailureReason.USER_CANCELLED

        BiometricPrompt.ERROR_LOCKOUT -> BiometricAuthenticationFailureReason.LOCKOUT
        BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> BiometricAuthenticationFailureReason.LOCKOUT_PERMANENT
        BiometricPrompt.ERROR_HW_NOT_PRESENT,
        BiometricPrompt.ERROR_HW_UNAVAILABLE -> BiometricAuthenticationFailureReason.NOT_AVAILABLE

        BiometricPrompt.ERROR_NO_BIOMETRICS -> BiometricAuthenticationFailureReason.NOT_ENROLLED
        else -> BiometricAuthenticationFailureReason.UNKNOWN
    }
}