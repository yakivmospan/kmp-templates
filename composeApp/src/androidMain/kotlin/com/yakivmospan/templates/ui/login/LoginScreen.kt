package com.yakivmospan.templates.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yakivmospan.templates.feature.login.presentation.MR
import com.yakivmospan.templates.feature.login.presentation.login.LoginEvent
import com.yakivmospan.templates.feature.login.presentation.login.LoginState
import com.yakivmospan.templates.feature.login.presentation.login.LoginViewModel
import com.yakivmospan.templates.presentation.theme.AppTheme
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel

// ── Public overload ───────────────────────────────────────────────────────────

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LoginScreen(state = state, onEvent = viewModel::onEvent, modifier = modifier)
}

// ── Private overload ──────────────────────────────────────────────────────────

@Composable
private fun LoginScreen(
    state: LoginState,
    onEvent: (LoginEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is LoginState.Loading              -> LoadingContent(modifier)
        is LoginState.CreatePin            -> CreatePinContent(state, onEvent, modifier)
        is LoginState.ConfirmPin           -> ConfirmPinContent(state, onEvent, modifier)
        is LoginState.BiometricSetupCheck  -> LoadingContent(modifier)
        is LoginState.BiometricNotEnrolled -> BiometricNotEnrolledContent(state, onEvent, modifier)
        is LoginState.LoginWithPin         -> LoginWithPinContent(state, onEvent, modifier)
        is LoginState.LoginWithBiometric   -> LoginWithBiometricContent(state, onEvent, modifier)
    }
}

// ── Loading ───────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
    }
}

// ── Create PIN ────────────────────────────────────────────────────────────────

@Composable
private fun CreatePinContent(
    state: LoginState.CreatePin,
    onEvent: (LoginEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    PinEntryLayout(
        modifier = modifier,
        title = stringResource(MR.strings.login_create_pin_title),
        pin = state.pin,
        hasError = false,
        errorMessage = null,
        onDigit = { onEvent(LoginEvent.DigitPressed(it)) },
        onBackspace = { onEvent(LoginEvent.BackspacePressed) },
        onClear = { onEvent(LoginEvent.ClearPressed) },
        bottomContent = {
            if (state.biometricAvailable) {
                BiometricCheckbox(
                    checked = state.biometricEnabled,
                    onCheckedChange = { onEvent(LoginEvent.BiometricToggled(it)) },
                )
            }
        },
    )
}

// ── Confirm PIN ───────────────────────────────────────────────────────────────

@Composable
private fun ConfirmPinContent(
    state: LoginState.ConfirmPin,
    onEvent: (LoginEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    PinEntryLayout(
        modifier = modifier,
        title = stringResource(MR.strings.login_confirm_pin_title),
        pin = state.confirmPin,
        hasError = state.error != null,
        errorMessage = state.error,
        onDigit = { onEvent(LoginEvent.DigitPressed(it)) },
        onBackspace = { onEvent(LoginEvent.BackspacePressed) },
        onClear = { onEvent(LoginEvent.ClearPressed) },
    )
}

// ── Login with PIN ────────────────────────────────────────────────────────────

@Composable
private fun LoginWithPinContent(
    state: LoginState.LoginWithPin,
    onEvent: (LoginEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    PinEntryLayout(
        modifier = modifier,
        title = stringResource(MR.strings.login_title),
        pin = state.pin,
        hasError = state.error != null,
        errorMessage = state.error,
        onDigit = { onEvent(LoginEvent.DigitPressed(it)) },
        onBackspace = { onEvent(LoginEvent.BackspacePressed) },
        onClear = { onEvent(LoginEvent.ClearPressed) },
    )
}

// ── Login with Biometric ──────────────────────────────────────────────────────

@Composable
private fun LoginWithBiometricContent(
    state: LoginState.LoginWithBiometric,
    onEvent: (LoginEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.showKeyboard) {
        PinEntryLayout(
            modifier = modifier,
            title = stringResource(MR.strings.login_title),
            pin = state.pin,
            hasError = state.error != null,
            errorMessage = state.error,
            onDigit = { onEvent(LoginEvent.DigitPressed(it)) },
            onBackspace = { onEvent(LoginEvent.BackspacePressed) },
            onClear = { onEvent(LoginEvent.ClearPressed) },
        )
    } else {
        BiometricLoginContent(
            error = state.error,
            onEvent = onEvent,
            modifier = modifier,
        )
    }
}

@Composable
private fun BiometricLoginContent(
    error: String?,
    onEvent: (LoginEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val fingerprintDescription = stringResource(MR.strings.login_biometric_button_description)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(MR.strings.login_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .semantics { heading() },
        )

        Spacer(modifier = Modifier.height(48.dp))

        IconButton(
            onClick = { onEvent(LoginEvent.BiometricLoginRequested) },
            modifier = Modifier
                .size(72.dp)
                .semantics {
                    contentDescription = fingerprintDescription
                    role = Role.Button
                },
        ) {
            Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(onClick = { onEvent(LoginEvent.SwitchToPinLogin) }) {
            Text(text = stringResource(MR.strings.login_use_pin_instead))
        }
    }
}

// ── Biometric not enrolled ────────────────────────────────────────────────────

@Composable
private fun BiometricNotEnrolledContent(
    state: LoginState.BiometricNotEnrolled,
    onEvent: (LoginEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(MR.strings.login_biometric_not_enrolled_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .semantics { heading() },
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(MR.strings.login_biometric_not_enrolled_message),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onEvent(LoginEvent.OpenBiometricSettingsClicked) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(MR.strings.login_biometric_open_settings))
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
            onClick = { onEvent(LoginEvent.SkipBiometricSetupClicked) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(MR.strings.login_biometric_skip))
        }
    }
}

// ── Shared PIN entry layout ───────────────────────────────────────────────────

@Composable
private fun PinEntryLayout(
    title: String,
    pin: String,
    hasError: Boolean,
    errorMessage: String?,
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    bottomContent: @Composable () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .semantics { heading() },
        )

        Spacer(modifier = Modifier.height(32.dp))

        PinDisplay(
            enteredLength = pin.length,
            totalLength = LoginState.PIN_LENGTH,
            hasError = hasError,
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        PinKeyboard(
            onDigit = onDigit,
            onBackspace = onBackspace,
            onClear = onClear,
        )

        Spacer(modifier = Modifier.height(16.dp))

        bottomContent()

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ── Biometric checkbox ────────────────────────────────────────────────────────

@Composable
private fun BiometricCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        Text(
            text = stringResource(MR.strings.login_biometric_checkbox_label),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
private fun LoginScreenLoadingPreview() {
    AppTheme { LoginScreen(state = LoginState.Loading, onEvent = {}) }
}

@PreviewLightDark
@Composable
private fun LoginScreenCreatePinPreview() {
    AppTheme {
        LoginScreen(
            state = LoginState.CreatePin(pin = "12", biometricAvailable = true, biometricEnabled = false),
            onEvent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun LoginScreenConfirmPinPreview() {
    AppTheme {
        LoginScreen(
            state = LoginState.ConfirmPin(originalPin = "1234", confirmPin = "12"),
            onEvent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun LoginScreenConfirmPinErrorPreview() {
    AppTheme {
        LoginScreen(
            state = LoginState.ConfirmPin(
                originalPin = "1234",
                confirmPin = "",
                error = "PINs do not match. Please try again.",
            ),
            onEvent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun LoginScreenLoginWithPinPreview() {
    AppTheme {
        LoginScreen(
            state = LoginState.LoginWithPin(pin = "1"),
            onEvent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun LoginScreenLoginWithBiometricPreview() {
    AppTheme {
        LoginScreen(
            state = LoginState.LoginWithBiometric(),
            onEvent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun LoginScreenBiometricNotEnrolledPreview() {
    AppTheme {
        LoginScreen(
            state = LoginState.BiometricNotEnrolled,
            onEvent = {},
        )
    }
}

@Preview(fontScale = 2f, name = "Large font — Create PIN")
@Composable
private fun LoginScreenLargeFontPreview() {
    AppTheme {
        LoginScreen(
            state = LoginState.CreatePin(biometricAvailable = true),
            onEvent = {},
        )
    }
}