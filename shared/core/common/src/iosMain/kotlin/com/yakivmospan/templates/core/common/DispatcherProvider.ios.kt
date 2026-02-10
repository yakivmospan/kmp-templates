package com.yakivmospan.templates.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual fun defaultDispatcherProvider(): DispatcherProvider {
    return IOSDispatcherProvider()
}

class IOSDispatcherProvider() : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.Default // iOS doesn't have IO dispatcher
    override val default: CoroutineDispatcher = Dispatchers.Default
}