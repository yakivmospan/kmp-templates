package com.yakivmospan.templates.core.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Clock

@OptIn(ExperimentalAtomicApi::class) // Use java atomic instead?
class FlowNavigator(
    private val backDebounceMillis: Long = 300L
) : Navigator, NavigatorCommandsFlow, NavigatorResultsFlow {

    private val navigatorCommandsFlow = MutableSharedFlow<NavigatorCommand>(extraBufferCapacity = 1)
    private val results = mutableMapOf<String, MutableSharedFlow<NavigationResult>>()

    // Debouncing timestamps using atomic operations for thread safety
    private val lastNavigateTime = AtomicLong(0L)
    private val lastBackTime = AtomicLong(0L)

    override fun navigate(target: NavigationTarget) {
        if (!shouldAllowNavigation(lastNavigateTime)) {
            return
        }

        navigatorCommandsFlow.tryEmit(
            NavigatorCommand.Target(
                target.route,
                target.clearBackStackUntil,
                target.clearBackInclusively
            )
        )
    }

    override fun openUri(uri: String) {
        navigatorCommandsFlow.tryEmit(NavigatorCommand.URI(uri))
    }

    override fun back(to: NavigationRoute?, inclusive: Boolean, with: NavigationResult?) {
        if (!shouldAllowNavigation(lastBackTime)) {
            return
        }

        navigatorCommandsFlow.tryEmit(NavigatorCommand.Pop(to, inclusive, with))
    }

    override fun exit() {
        navigatorCommandsFlow.tryEmit(NavigatorCommand.Exit)
    }

    override fun waitForResults(key: String): SharedFlow<NavigationResult> {
        return resultsFor(key = key)
    }

    override fun values(): SharedFlow<NavigatorCommand> {
        return navigatorCommandsFlow
    }

    override fun setResult(result: NavigationResult) {
        val flow = results.getOrPut(result.key) { MutableSharedFlow(replay = 0, extraBufferCapacity = 1) }
        flow.tryEmit(result)
    }

    override fun resultsFor(key: String): SharedFlow<NavigationResult> {
        return results.getOrPut(key) { MutableSharedFlow(replay = 0, extraBufferCapacity = 1) }.asSharedFlow()
    }

    /**
     * Checks if enough time has passed since the last navigation to allow a new one.
     * Uses atomic compareAndSet to ensure thread-safe updates.
     *
     * @param lastTime AtomicLong tracking the last navigation timestamp
     * @return true if navigation should be allowed, false if it should be debounced
     */
    private fun shouldAllowNavigation(lastTime: AtomicLong): Boolean {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        val previousTime = lastTime.load()

        // Check if enough time has passed
        if (currentTime - previousTime < backDebounceMillis) {
            return false
        }

        // Atomically update the timestamp only if it hasn't been changed by another thread
        return lastTime.compareAndSet(previousTime, currentTime)
    }
}