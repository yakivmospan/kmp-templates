package com.yakivmospan.templates.ui.login

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.yakivmospan.templates.feature.login.presentation.MR
import com.yakivmospan.templates.presentation.theme.AppTheme
import dev.icerock.moko.resources.compose.stringResource

private val DotSize = 16.dp
private val DotFilledSize = 18.dp
private val DotSpacing = 16.dp

// ── PinDisplay ────────────────────────────────────────────────────────────────

/**
 * Displays a row of dots representing the current PIN entry progress.
 * Filled dots = entered digits; empty dots = remaining slots.
 *
 * @param enteredLength Number of digits entered so far.
 * @param totalLength   Total number of digits required (defaults to [PIN_LENGTH]).
 * @param hasError      When true dots are tinted with the error colour.
 */
@Composable
fun PinDisplay(
    enteredLength: Int,
    modifier: Modifier = Modifier,
    totalLength: Int = 4,
    hasError: Boolean = false,
) {
    val description = stringResource(
        MR.strings.login_pin_display_description,
        enteredLength,
        totalLength,
    )

    Row(
        modifier = modifier.semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(DotSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalLength) { index ->
            PinDot(
                filled = index < enteredLength,
                hasError = hasError,
            )
        }
    }
}

// ── PinDot ────────────────────────────────────────────────────────────────────

@Composable
private fun PinDot(
    filled: Boolean,
    hasError: Boolean,
    modifier: Modifier = Modifier,
) {
    val targetColor = when {
        hasError -> MaterialTheme.colorScheme.error
        filled   -> MaterialTheme.colorScheme.primary
        else     -> MaterialTheme.colorScheme.outline
    }

    val color by animateColorAsState(
        targetValue = targetColor,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pin_dot_color",
    )

    val size by animateDpAsState(
        targetValue = if (filled) DotFilledSize else DotSize,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "pin_dot_size",
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .then(
                if (filled) {
                    Modifier.background(color)
                } else {
                    Modifier.border(
                        width = 2.dp,
                        color = color,
                        shape = CircleShape,
                    )
                }
            ),
    )
}

// ── Previews ──────────────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
private fun PinDisplayEmptyPreview() {
    AppTheme {
        PinDisplay(enteredLength = 0)
    }
}

@PreviewLightDark
@Composable
private fun PinDisplayPartialPreview() {
    AppTheme {
        PinDisplay(enteredLength = 2)
    }
}

@PreviewLightDark
@Composable
private fun PinDisplayFullPreview() {
    AppTheme {
        PinDisplay(enteredLength = 4)
    }
}

@PreviewLightDark
@Composable
private fun PinDisplayErrorPreview() {
    AppTheme {
        PinDisplay(enteredLength = 4, hasError = true)
    }
}