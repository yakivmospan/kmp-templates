package com.yakivmospan.templates.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.yakivmospan.templates.feature.login.presentation.MR
import com.yakivmospan.templates.presentation.theme.AppTheme
import dev.icerock.moko.resources.compose.stringResource

// ── Key model ─────────────────────────────────────────────────────────────────

private sealed class KeyType {
    data class Digit(val value: Int) : KeyType()
    data object Backspace : KeyType()
    data object Clear : KeyType()
    data object Empty : KeyType()
}

private val keyboardKeys: List<KeyType> = listOf(
    KeyType.Digit(1), KeyType.Digit(2), KeyType.Digit(3),
    KeyType.Digit(4), KeyType.Digit(5), KeyType.Digit(6),
    KeyType.Digit(7), KeyType.Digit(8), KeyType.Digit(9),
    KeyType.Clear,    KeyType.Digit(0), KeyType.Backspace,
)

// ── PinKeyboard ───────────────────────────────────────────────────────────────

/**
 * A 3×4 PIN keypad.
 *
 * Layout:
 * ```
 *  1   2   3
 *  4   5   6
 *  7   8   9
 * CLR  0   ⌫
 * ```
 *
 * @param onDigit     Called when a digit key is tapped.
 * @param onBackspace Called when the backspace key is tapped.
 * @param onClear     Called when the clear key is tapped.
 * @param enabled     When false all keys are non-interactive (e.g. during loading).
 */
@Composable
fun PinKeyboard(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false,
    ) {
        items(keyboardKeys, key = { it::class.simpleName + (it as? KeyType.Digit)?.value }) { key ->
            when (key) {
                is KeyType.Digit -> DigitKey(
                    digit = key.value,
                    enabled = enabled,
                    onClick = remember(key.value) { { onDigit(key.value) } },
                )

                is KeyType.Backspace -> ActionKey(
                    contentDescription = stringResource(MR.strings.login_key_backspace),
                    enabled = enabled,
                    onClick = onBackspace,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                    )
                }

                is KeyType.Clear -> ActionKey(
                    contentDescription = stringResource(MR.strings.login_key_clear),
                    enabled = enabled,
                    onClick = onClear,
                ) {
                    Text(
                        text = stringResource(MR.strings.login_key_clear_label),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }

                is KeyType.Empty -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
            }
        }
    }
}

// ── Key components ────────────────────────────────────────────────────────────

@Composable
private fun DigitKey(
    digit: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val description = stringResource(MR.strings.login_key_digit_description, digit)
    KeySurface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.semantics {
            contentDescription = description
            role = Role.Button
        },
    ) {
        Text(
            text = digit.toString(),
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

@Composable
private fun ActionKey(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    KeySurface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Button
        },
        content = content,
    )
}

@Composable
private fun KeySurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
private fun PinKeyboardPreview() {
    AppTheme {
        PinKeyboard(
            onDigit = {},
            onBackspace = {},
            onClear = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun PinKeyboardDisabledPreview() {
    AppTheme {
        PinKeyboard(
            onDigit = {},
            onBackspace = {},
            onClear = {},
            enabled = false,
        )
    }
}