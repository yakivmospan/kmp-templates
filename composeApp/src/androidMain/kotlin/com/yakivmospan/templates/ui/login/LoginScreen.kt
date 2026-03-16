package com.yakivmospan.templates.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
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

private val KeyboardMaxWidth = 320.dp

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
        is LoginState.BiometricNotEnrolled -> BiometricNotEnrolledContent(onEvent, modifier)
        is LoginState.LoginWithPin         -> LoginWithPinContent(state, onEvent, modifier)
        is LoginState.LoginWithBiometric   -> LoginWithBiometricContent(state, onEvent, modifier)
    }
}

// ── Orientation helper ────────────────────────────────────────────────────────

private enum class Orientation { Portrait, Landscape }

@Composable
private fun currentOrientation(): Orientation {
    val config = LocalConfiguration.current
    return if (config.screenWidthDp > config.screenHeightDp) Orientation.Landscape else Orientation.Portrait
}

// ── Loading ───────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
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
            } else {
                BiometricCheckboxSpacer()
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
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = KeyboardMaxWidth)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp),
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
                    modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = { onEvent(LoginEvent.SwitchToPinLogin) }) {
                Text(text = stringResource(MR.strings.login_use_pin_instead))
            }
        }
    }
}

// ── Biometric not enrolled ────────────────────────────────────────────────────

@Composable
private fun BiometricNotEnrolledContent(
    onEvent: (LoginEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = KeyboardMaxWidth)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp),
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
                modifier = Modifier.fillMaxWidth().wrapContentHeight(),
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
    when (currentOrientation()) {
        Orientation.Portrait  -> PinEntryPortrait(
            title = title,
            pin = pin,
            hasError = hasError,
            errorMessage = errorMessage,
            onDigit = onDigit,
            onBackspace = onBackspace,
            onClear = onClear,
            bottomContent = bottomContent,
            modifier = modifier,
        )
        Orientation.Landscape -> PinEntryLandscape(
            title = title,
            pin = pin,
            hasError = hasError,
            errorMessage = errorMessage,
            onDigit = onDigit,
            onBackspace = onBackspace,
            onClear = onClear,
            bottomContent = bottomContent,
            modifier = modifier,
        )
    }
}

// ── Portrait layout ───────────────────────────────────────────────────────────

@Composable
private fun PinEntryPortrait(
    title: String,
    pin: String,
    hasError: Boolean,
    errorMessage: String?,
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    bottomContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = KeyboardMaxWidth)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            PinEntryHeader(title = title, pin = pin, hasError = hasError, errorMessage = errorMessage)

            Spacer(modifier = Modifier.height(32.dp))

            PinKeyboard(
                onDigit = onDigit,
                onBackspace = onBackspace,
                onClear = onClear,
                // wrapContentHeight is critical — keyboard is inside verticalScroll which
                // gives infinite height; fillMaxHeight or fillMaxSize would crash here.
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            bottomContent()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ── Landscape layout ──────────────────────────────────────────────────────────

@Composable
private fun PinEntryLandscape(
    title: String,
    pin: String,
    hasError: Boolean,
    errorMessage: String?,
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    bottomContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left — title, pin dots, error, bottom content — centred vertically
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = KeyboardMaxWidth)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                PinEntryHeader(title = title, pin = pin, hasError = hasError, errorMessage = errorMessage)

                Spacer(modifier = Modifier.height(16.dp))

                bottomContent()
            }
        }

        // Right — keyboard centred horizontally and vertically.
        // fillMaxHeight gives PinKeyboard's internal BoxWithConstraints a finite
        // maxHeight so it can compute key sizes from available height.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            PinKeyboard(
                onDigit = onDigit,
                onBackspace = onBackspace,
                onClear = onClear,
                constrainToHeight = true,
                modifier = Modifier
                    .widthIn(max = KeyboardMaxWidth)
                    .fillMaxWidth()
                    .wrapContentHeight(),
            )
        }
    }
}

// ── Shared header (title + dots + error) ──────────────────────────────────────

@Composable
private fun PinEntryHeader(
    title: String,
    pin: String,
    hasError: Boolean,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .semantics { heading() },
        )

        Spacer(modifier = Modifier.height(24.dp))

        PinDisplay(
            enteredLength = pin.length,
            totalLength = LoginState.PIN_LENGTH,
            hasError = hasError,
        )

        // Reserve space for error to avoid layout shift
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(top = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = errorMessage ?: " ",
                style = MaterialTheme.typography.bodySmall,
                color = if (errorMessage != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.error.copy(alpha = 0f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

// ── Biometric checkbox ────────────────────────────────────────────────────────

@Composable
private fun BiometricCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            text = stringResource(MR.strings.login_biometric_checkbox_label),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * Invisible spacer with the same height as [BiometricCheckbox].
 * Prevents layout jumps when transitioning between CreatePin and ConfirmPin.
 */
/**
 * Invisible spacer with the same height as [BiometricCheckbox].
 * Prevents layout jumps when transitioning between CreatePin and ConfirmPin.
 */
@Composable
private fun BiometricCheckboxSpacer(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier.height(48.dp)
    )
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
        LoginScreen(state = LoginState.LoginWithPin(pin = "1"), onEvent = {})
    }
}

@PreviewLightDark
@Composable
private fun LoginScreenLoginWithBiometricPreview() {
    AppTheme {
        LoginScreen(state = LoginState.LoginWithBiometric(), onEvent = {})
    }
}

@PreviewLightDark
@Composable
private fun LoginScreenBiometricNotEnrolledPreview() {
    AppTheme {
        LoginScreen(state = LoginState.BiometricNotEnrolled, onEvent = {})
    }
}

@Preview(fontScale = 2f, name = "Large font — Create PIN")
@Composable
private fun LoginScreenLargeFontPreview() {
    AppTheme {
        LoginScreen(state = LoginState.CreatePin(biometricAvailable = true), onEvent = {})
    }
}

@Preview(widthDp = 800, heightDp = 400, name = "Landscape — Create PIN")
@Composable
private fun LoginScreenLandscapePreview() {
    AppTheme {
        LoginScreen(
            state = LoginState.CreatePin(pin = "12", biometricAvailable = true, biometricEnabled = false),
            onEvent = {},
        )
    }
}

@Preview(widthDp = 800, heightDp = 400, name = "Landscape — Confirm PIN error")
@Composable
private fun LoginScreenLandscapeErrorPreview() {
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