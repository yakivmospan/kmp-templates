package com.yakivmospan.templates.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.navigation.NavController
import com.yakivmospan.templates.core.navigation.NavigationResult
import com.yakivmospan.templates.core.navigation.NavigationRoute
import com.yakivmospan.templates.core.navigation.NavigatorCommand
import com.yakivmospan.templates.core.navigation.NavigatorCommandsFlow
import com.yakivmospan.templates.core.navigation.NavigatorResultsFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
fun ComposeNavigatorCommandsHandler(
    navController: NavController,
    navigatorCommandsFlow: NavigatorCommandsFlow,
    navigatorResultsFlow: NavigatorResultsFlow,
    uriHandler: UriHandler = LocalUriHandler.current,
    onNothingToPop: () -> (Unit)
) = LaunchedEffect("ComposeNavigatorCommandsHandler.NavigatorCommandsFlow") {
    navigatorCommandsFlow.values().onEach { command ->
        when (command) {
            is NavigatorCommand.Target -> navigate(navController, command)
            is NavigatorCommand.Pop -> pop(
                navController,
                navigatorResultsFlow,
                command.to,
                command.result,
                command.inclusive,
                onNothingToPop
            )

            is NavigatorCommand.URI -> uriHandler.openUri(command.uri)
            is NavigatorCommand.Exit -> exit()
        }
    }.launchIn(this)
}

private fun navigate(
    navController: NavController,
    navTarget: NavigatorCommand.Target
) {
    navController.navigate(navTarget.route) {
        navTarget.clearBackStackUntil?.let { popUpToRoute ->
            popUpTo(popUpToRoute) { inclusive = navTarget.clearBackInclusively }
        }
    }
}

// currentBackStack is internal, moved this from KMP Compose, was no time to investigate better approach yet.
@SuppressLint("RestrictedApi")
private fun pop(
    navController: NavController,
    navigatorResultsFlow: NavigatorResultsFlow,
    to: NavigationRoute?,
    result: NavigationResult?,
    inclusive: Boolean = false,
    onNothingToPop: () -> Unit
) {
    // Check if there's anything to pop
    if (navController.currentBackStack.value.size <= 2) {
        onNothingToPop()
        return
    }

    if (to != null) {
        navController.popBackStack(to, inclusive)
    } else {
        navController.popBackStack()
    }

    if (result != null) {
        navigatorResultsFlow.setResult(result = result)
    }
}

private fun exit() {
    TODO("Implement app exit logic, e.g. by calling activity.finish() or similar")
}
