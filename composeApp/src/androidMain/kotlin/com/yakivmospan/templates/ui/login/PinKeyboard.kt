package com.yakivmospan.templates.ui.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yakivmospan.templates.feature.login.presentation.MR
import com.yakivmospan.templates.presentation.theme.AppTheme
import dev.icerock.moko.resources.compose.stringResource

private const val ROWS = 4
private const val COLS = 3
private val RowSpacing = 8.dp
private val ColSpacing = 8.dp

// ── Key model ─────────────────────────────────────────────────────────────────

private sealed class KeyType {
    data class Digit(val value: Int) : KeyType()
    data object Backspace : KeyType()
    data object Clear : KeyType()
    data object Empty : KeyType()
}

private val keyboardRows: List<List<KeyType>> = listOf(
    listOf(KeyType.Digit(1), KeyType.Digit(2), KeyType.Digit(3)),
    listOf(KeyType.Digit(4), KeyType.Digit(5), KeyType.Digit(6)),
    listOf(KeyType.Digit(7), KeyType.Digit(8), KeyType.Digit(9)),
    listOf(KeyType.Clear,    KeyType.Digit(0), KeyType.Backspace),
)

// ── PinKeyboard ───────────────────────────────────────────────────────────────

/**
 * A 3×4 PIN keypad that fills its available space.
 *
 * In portrait the key size is driven by width (square keys via aspectRatio).
 * In landscape [constrainToHeight] = true drives key size from available height
 * so the keyboard never overflows vertically.
 *
 * Layout:
 * ```
 *  1   2   3
 *  4   5   6
 *  7   8   9
 * CLR  0   ⌫
 * ```
 */
@Composable
fun PinKeyboard(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    constrainToHeight: Boolean = false,
) {
    BoxWithConstraints(modifier = modifier) {
        val keySize: Dp = if (constrainToHeight) {
            // Distribute available height across 4 rows + 3 gaps
            val totalSpacing = RowSpacing * (ROWS - 1)
            val rowHeight = (maxHeight - totalSpacing) / ROWS
            // Clamp to width so keys never exceed a square on wide screens
            val maxFromWidth = (maxWidth - ColSpacing * (COLS - 1)) / COLS
            minOf(rowHeight, maxFromWidth)
        } else {
            // Portrait: key size driven by width
            (maxWidth - ColSpacing * (COLS - 1)) / COLS
        }

        // Total height the keyboard will actually occupy — used so the Column
        // reports a natural (non-infinite) height, allowing the parent Box to
        // centre it vertically via wrapContentHeight.
        val totalHeight = keySize * ROWS + RowSpacing * (ROWS - 1)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(totalHeight),
            verticalArrangement = Arrangement.spacedBy(RowSpacing),
        ) {
            keyboardRows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(keySize),
                    horizontalArrangement = Arrangement.spacedBy(ColSpacing),
                ) {
                    row.forEach { key ->
                        Box(modifier = Modifier.weight(1f)) {
                            when (key) {
                                is KeyType.Digit -> DigitKey(
                                    digit = key.value,
                                    size = keySize,
                                    enabled = enabled,
                                    onClick = remember(key.value) { { onDigit(key.value) } },
                                )

                                is KeyType.Backspace -> ActionKey(
                                    contentDescription = stringResource(MR.strings.login_key_backspace),
                                    size = keySize,
                                    enabled = enabled,
                                    onClick = onBackspace,
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = null,
                                        modifier = Modifier.size(keySize * 0.35f),
                                    )
                                }

                                is KeyType.Clear -> ActionKey(
                                    contentDescription = stringResource(MR.strings.login_key_clear),
                                    size = keySize,
                                    enabled = enabled,
                                    onClick = onClear,
                                ) {
                                    Text(
                                        text = stringResource(MR.strings.login_key_clear_label),
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }

                                is KeyType.Empty -> Box(modifier = Modifier.size(keySize))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Key components ────────────────────────────────────────────────────────────

@Composable
private fun DigitKey(
    digit: Int,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val description = stringResource(MR.strings.login_key_digit_description, digit)
    KeySurface(
        onClick = onClick,
        size = size,
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
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    KeySurface(
        onClick = onClick,
        size = size,
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
    size: Dp,
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
        // Do NOT use fillMaxSize — may be placed inside verticalScroll which gives
        // infinite height. Size is driven by the parent Row's fixed height.
        modifier = modifier.aspectRatio(1f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
private fun PinKeyboardPortraitPreview() {
    AppTheme {
        PinKeyboard(onDigit = {}, onBackspace = {}, onClear = {})
    }
}

@Preview(widthDp = 320, heightDp = 300, name = "Landscape — fits height")
@Composable
private fun PinKeyboardLandscapePreview() {
    AppTheme {
        PinKeyboard(
            onDigit = {},
            onBackspace = {},
            onClear = {},
            constrainToHeight = true,
        )
    }
}

@PreviewLightDark
@Composable
private fun PinKeyboardDisabledPreview() {
    AppTheme {
        PinKeyboard(onDigit = {}, onBackspace = {}, onClear = {}, enabled = false)
    }
}