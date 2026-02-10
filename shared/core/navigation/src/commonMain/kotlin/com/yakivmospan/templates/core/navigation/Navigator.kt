package com.yakivmospan.templates.core.navigation

import kotlinx.coroutines.flow.SharedFlow

interface Navigator {
    fun navigate(target: NavigationTarget)
    fun openUri(uri: String)
    fun back(to: NavigationRoute? = null, inclusive: Boolean = false, with: NavigationResult? = null)
    fun exit()

    // Returns a StateFlow that emits the NavigationResult when it's set for the given key
    // The StateFlow will emit updates whenever the result changes
    // The Flow is contracted to replay(0), so it only emits new results after collection
    fun waitForResults(key: String): SharedFlow<NavigationResult>
}

// TODO NavigationRoute should be reimplemented to use String routes instead on Kotlin Serialization - to support iOS navigation.
interface NavigationRoute

open class NavigationTarget(
    val route: NavigationRoute,
    val clearBackStackUntil: NavigationRoute?,
    val clearBackInclusively: Boolean = true
)

data class NavigationResult(
    val key: String,
    val data: Map<String, Any> = emptyMap(),
) {
    fun isEmpty() = data.isEmpty()
}

sealed interface NavigatorCommand {
    data class Target(
        val route: NavigationRoute,
        val clearBackStackUntil: NavigationRoute? = null,
        val clearBackInclusively: Boolean = false
    ) : NavigatorCommand

    data class URI(val uri: String) : NavigatorCommand

    data class Pop(val to: NavigationRoute? = null, val inclusive: Boolean = false, val result: NavigationResult? = null) :
        NavigatorCommand

    object Exit : NavigatorCommand
}

interface NavigatorCommandsFlow {
    fun values(): SharedFlow<NavigatorCommand>
}

interface NavigatorResultsFlow {
    fun setResult(result: NavigationResult)
    fun resultsFor(key: String): SharedFlow<NavigationResult>
}