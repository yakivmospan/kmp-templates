package com.yakivmospan.templates.core.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

interface CoroutineScopeProvider {
    val appScope: CoroutineScope
}

fun defaultCoroutineScopeProvider(dispatchers: DispatcherProvider): CoroutineScopeProvider =
    object : CoroutineScopeProvider {
        override val appScope = CoroutineScope(dispatchers.main + SupervisorJob())
    }