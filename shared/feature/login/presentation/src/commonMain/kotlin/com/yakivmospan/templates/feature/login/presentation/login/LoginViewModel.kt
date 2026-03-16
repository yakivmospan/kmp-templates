package com.yakivmospan.templates.feature.login.presentation.login

import androidx.lifecycle.viewModelScope
import com.yakivmospan.templates.core.biometrics.BiometricAuthenticator
import com.yakivmospan.templates.core.common.Result
import com.yakivmospan.templates.core.common.onError
import com.yakivmospan.templates.core.common.onSuccess
import com.yakivmospan.templates.core.navigation.Navigator
import com.yakivmospan.templates.core.presentation.ViewModel
import com.yakivmospan.templates.feature.login.presentation.LoginNavigationTargets
import kotlinx.coroutines.launch

// ── LoginViewModel ────────────────────────────────────────────────────────────

class LoginViewModel(
    private val navigator: Navigator,
    private val biometricAuthenticator: BiometricAuthenticator,
    // TODO: inject saved-pin source (DataStore / EncryptedSharedPrefs) once data layer exists
) : ViewModel<LoginEvent, LoginState, Unit>(initialState = LoginState.Loading) {

    init {
        viewModelScope.launch { bootstrap() }
    }

    // ── Bootstrap ─────────────────────────────────────────────────────────────

    private suspend fun bootstrap() {
        // TODO: check DataStore / EncryptedSharedPrefs for an existing PIN hash.
        val pinAlreadyCreated = false // replace with real check

        if (pinAlreadyCreated) startLoginFlow() else startCreatePinFlow()
    }

    private suspend fun startCreatePinFlow() {
        val biometricAvailable = biometricAuthenticator.isBiometricAvailable()
        updateState(LoginState.CreatePin(biometricAvailable = biometricAvailable))
    }

    private suspend fun startLoginFlow() {
        // TODO: read biometricEnabled flag from DataStore
        val biometricEnabled = false // replace with real persistence check

        if (biometricEnabled) {
            updateState(LoginState.LoginWithBiometric())
            authenticate()
        } else {
            updateState(LoginState.LoginWithPin())
        }
    }

    // ── Event handling ────────────────────────────────────────────────────────

    override fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.DigitPressed -> onDigitPressed(event.digit)
            is LoginEvent.BackspacePressed -> onBackspacePressed()
            is LoginEvent.ClearPressed -> onClearPressed()
            is LoginEvent.BiometricToggled -> onBiometricToggled(event.enabled)
            is LoginEvent.ConfirmPinSubmitted -> onConfirmPinSubmitted()
            is LoginEvent.BiometricLoginRequested -> viewModelScope.launch { authenticate() }
            is LoginEvent.OpenBiometricSettingsClicked -> biometricAuthenticator.openBiometricSettings()
            is LoginEvent.SkipBiometricSetupClicked -> navigateToCatalog()
            is LoginEvent.SwitchToPinLogin -> onSwitchToPinLogin()
        }
    }

    // ── Digit / keypad ────────────────────────────────────────────────────────

    private fun onDigitPressed(digit: Int) {
        when (val s = state.value) {
            is LoginState.CreatePin -> {
                if (s.pin.length >= LoginState.PIN_LENGTH) return
                val updated = s.copy(pin = s.pin + digit)
                updateState(updated)
                if (updated.pin.length == LoginState.PIN_LENGTH) {
                    updateState(
                        LoginState.ConfirmPin(
                            originalPin = updated.pin,
                            biometricEnabled = updated.biometricEnabled,
                        )
                    )
                }
            }

            is LoginState.ConfirmPin -> {
                if (s.confirmPin.length >= LoginState.PIN_LENGTH) return
                val updated = s.copy(confirmPin = s.confirmPin + digit, error = null)
                updateState(updated)
                if (updated.confirmPin.length == LoginState.PIN_LENGTH) {
                    onEvent(LoginEvent.ConfirmPinSubmitted)
                }
            }

            is LoginState.LoginWithPin -> {
                if (s.pin.length >= LoginState.PIN_LENGTH) return
                val updated = s.copy(pin = s.pin + digit, error = null)
                updateState(updated)
                if (updated.pin.length == LoginState.PIN_LENGTH) {
                    verifyPin(updated.pin)
                }
            }

            is LoginState.LoginWithBiometric -> {
                if (!s.showKeyboard) return
                if (s.pin.length >= LoginState.PIN_LENGTH) return
                val updated = s.copy(pin = s.pin + digit, error = null)
                updateState(updated)
                if (updated.pin.length == LoginState.PIN_LENGTH) {
                    verifyPin(updated.pin)
                }
            }

            else -> Unit
        }
    }

    private fun onBackspacePressed() {
        when (val s = state.value) {
            is LoginState.CreatePin -> updateState(s.copy(pin = s.pin.dropLast(1)))
            is LoginState.ConfirmPin -> updateState(s.copy(confirmPin = s.confirmPin.dropLast(1), error = null))
            is LoginState.LoginWithPin -> updateState(s.copy(pin = s.pin.dropLast(1), error = null))
            is LoginState.LoginWithBiometric -> updateState(s.copy(pin = s.pin.dropLast(1), error = null))
            else -> Unit
        }
    }

    private fun onClearPressed() {
        when (val s = state.value) {
            is LoginState.CreatePin -> updateState(s.copy(pin = ""))
            is LoginState.ConfirmPin -> updateState(s.copy(confirmPin = "", error = null))
            is LoginState.LoginWithPin -> updateState(s.copy(pin = "", error = null))
            is LoginState.LoginWithBiometric -> updateState(s.copy(pin = "", error = null))
            else -> Unit
        }
    }

    // ── Create-pin flow ───────────────────────────────────────────────────────

    private fun onBiometricToggled(enabled: Boolean) {
        val s = state.value as? LoginState.CreatePin ?: return
        updateState(s.copy(biometricEnabled = enabled))
    }

    private fun onConfirmPinSubmitted() {
        val s = state.value as? LoginState.ConfirmPin ?: return

        if (s.confirmPin != s.originalPin) {
            updateState(s.copy(confirmPin = "", error = "PINs do not match. Please try again."))
            return
        }

        viewModelScope.launch {
            // TODO: persist hashed PIN to DataStore / EncryptedSharedPrefs
            // TODO: persist biometricEnabled flag to DataStore
            if (s.biometricEnabled) checkBiometricEnrollment() else navigateToCatalog()
        }
    }

    private suspend fun checkBiometricEnrollment() {
        val biometricAvailable = biometricAuthenticator.isBiometricAvailable()
        if (biometricAvailable) {
            updateState(LoginState.BiometricSetupCheck())
            authenticate()
        } else {
            updateState(LoginState.BiometricNotEnrolled)
        }
    }

    // ── Login flow ────────────────────────────────────────────────────────────

    private fun onSwitchToPinLogin() {
        val s = state.value as? LoginState.LoginWithBiometric ?: return
        updateState(s.copy(showKeyboard = true))
    }

    // ── Biometric authentication ──────────────────────────────────────────────

    private suspend fun authenticate() {
        biometricAuthenticator.authenticate()
            .onSuccess {
                navigateToCatalog()
            }
            .onError {
                when (val s = state.value) {
                    is LoginState.LoginWithBiometric ->
                        updateState(s.copy(error = "Biometric authentication failed. Use your PIN."))

                    is LoginState.BiometricSetupCheck ->
                        // Setup check failed — still proceed, biometrics just won't auto-trigger on next login
                        navigateToCatalog()

                    else -> Unit
                }
            }
    }

    // ── PIN verification ──────────────────────────────────────────────────────

    private fun verifyPin(pin: String) {
        viewModelScope.launch {
            // TODO: compare hashed input against stored hash from DataStore
            val result: Result<Unit> = Result.Success(Unit) // replace with real check
            result
                .onSuccess { navigateToCatalog() }
                .onError {
                    when (val s = state.value) {
                        is LoginState.LoginWithPin ->
                            updateState(s.copy(pin = "", error = "Incorrect PIN. Please try again."))

                        is LoginState.LoginWithBiometric ->
                            updateState(s.copy(pin = "", error = "Incorrect PIN. Please try again."))

                        else -> Unit
                    }
                }
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private fun navigateToCatalog() {
        navigator.navigate(LoginNavigationTargets.ToProductCatalog)
    }
}