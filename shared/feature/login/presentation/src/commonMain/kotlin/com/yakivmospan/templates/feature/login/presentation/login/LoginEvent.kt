package com.yakivmospan.templates.feature.login.presentation.login

sealed class LoginEvent {

    // ── Keypad interactions ───────────────────────────────────────────────────

    /** A digit key (0–9) was pressed. */
    data class DigitPressed(val digit: Int) : LoginEvent()

    /** The backspace key was pressed — removes the last digit. */
    data object BackspacePressed : LoginEvent()

    /** The clear key was pressed — removes all entered digits. */
    data object ClearPressed : LoginEvent()

    // ── Create-pin flow ───────────────────────────────────────────────────────

    /** User toggled the "use fingerprint / face ID" checkbox. */
    data class BiometricToggled(val enabled: Boolean) : LoginEvent()

    /**
     * User finished entering the confirmation PIN.
     * ViewModel validates it against the original and transitions state.
     */
    data object ConfirmPinSubmitted : LoginEvent()

    // ── Biometric setup ───────────────────────────────────────────────────────

    /** User tapped "Open Settings" on the [LoginState.BiometricNotEnrolled] screen. */
    data object OpenBiometricSettingsClicked : LoginEvent()

    /** User dismissed the "go to settings" prompt — skip biometrics, proceed to catalog. */
    data object SkipBiometricSetupClicked : LoginEvent()

    // ── Login flow ────────────────────────────────────────────────────────────

    /** User tapped the fingerprint / face-ID button to trigger the system auth prompt. */
    data object BiometricLoginRequested : LoginEvent()

    /**
     * User tapped "use PIN instead" while on the biometric login screen.
     * Reveals the keypad.
     */
    data object SwitchToPinLogin : LoginEvent()
}