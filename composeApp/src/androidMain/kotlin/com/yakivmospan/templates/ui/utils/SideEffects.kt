package com.yakivmospan.templates.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.SharedFlow

/**
 * Collects a [SharedFlow] of side effects and dispatches each emission to [onSideEffect].
 *
 * The collection is started inside a [androidx.compose.runtime.LaunchedEffect] keyed on [key].
 * Re-launching only happens when [key] changes — use the default [Unit] key when the flow
 * should be collected for the entire lifetime of the composable.
 *
 * Intended to be used inside a dedicated `HandleSideEffects` composable, keeping side-effect
 * handling decoupled from both the ViewModel and the screen layout:
 *
 * ```kotlin
 * @Composable
 * private fun HandleSideEffects(
 *     sideEffects: SharedFlow<MySideEffect>,
 *     onEvent: (MyEvent) -> Unit,
 * ) {
 *     CollectSideEffects(sideEffects) { sideEffect ->
 *         when (sideEffect) {
 *             is MySideEffect.ShowError -> handleShowError(sideEffect, onEvent)
 *         }
 *     }
 * }
 * ```
 *
 * @param sideEffects The [SharedFlow] of side effects emitted by the ViewModel.
 * @param key Restarts the collection when this value changes. Defaults to [Unit].
 * @param onSideEffect Suspend handler invoked for each emitted side effect.
 */
@Composable
fun <SideEffect> CollectSideEffects(
    sideEffects: SharedFlow<SideEffect>,
    key: Any = Unit,
    onSideEffect: suspend (SideEffect) -> Unit,
) = LaunchedEffect(key) {
    sideEffects.collect { onSideEffect(it) }
}

