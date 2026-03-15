package com.yakivmospan.templates.feature.login.presentation.login

sealed class LoginState {

    // ── Transient ─────────────────────────────────────────────────────────────

    data object Loading : LoginState()

    // ── Create-pin flow ───────────────────────────────────────────────────────

    /**
     * User is entering their PIN for the first time.
     *
     * @param pin              Digits entered so far (max [PIN_LENGTH]).
     * @param biometricAvailable  Device supports biometrics AND the system has them enrolled.
     * @param biometricEnabled    Whether the user ticked the "use fingerprint / face ID" checkbox.
     */
    data class CreatePin(
        val pin: String = "",
        val biometricAvailable: Boolean = false,
        val biometricEnabled: Boolean = false,
    ) : LoginState()

    /**
     * User is re-entering the PIN to confirm it matches.
     *
     * @param originalPin  The pin chosen in [CreatePin] — kept for comparison only.
     * @param confirmPin   Digits entered so far in the confirmation step.
     * @param biometricEnabled  Carried over from [CreatePin].
     * @param error        Non-null when the two pins do not match.
     */
    data class ConfirmPin(
        val originalPin: String,
        val confirmPin: String = "",
        val biometricEnabled: Boolean = false,
        val error: String? = null,
    ) : LoginState()

    /**
     * Device has biometrics enabled and the user opted in.
     * The biometric prompt should be shown immediately on entering this state.
     *
     * @param pinFallbackAvailable  True — user can still type the PIN instead.
     */
    data class BiometricSetupCheck(
        val pinFallbackAvailable: Boolean = true,
    ) : LoginState()

    /**
     * Biometrics opted-in but the device has no biometrics enrolled.
     * Prompt the user to open system Settings.
     */
    data object BiometricNotEnrolled : LoginState()

    // ── Login flow ────────────────────────────────────────────────────────────

    /**
     * A PIN has already been created. Biometrics were NOT opted in (or are unavailable).
     * Show the keypad so the user can log in with their PIN.
     *
     * @param pin    Digits entered so far.
     * @param error  Non-null when the entered PIN was wrong.
     */
    data class LoginWithPin(
        val pin: String = "",
        val error: String? = null,
    ) : LoginState()

    /**
     * A PIN has already been created AND biometrics were opted in.
     * Show the fingerprint button; the biometric prompt is triggered immediately.
     *
     * @param pin          Digits typed so far (user may fall back to keypad).
     * @param showKeyboard True when the user chose to type instead.
     */
    data class LoginWithBiometric(
        val pin: String = "",
        val showKeyboard: Boolean = false,
        val error: String? = null,
    ) : LoginState()

    companion object {
        const val PIN_LENGTH = 4
    }
}