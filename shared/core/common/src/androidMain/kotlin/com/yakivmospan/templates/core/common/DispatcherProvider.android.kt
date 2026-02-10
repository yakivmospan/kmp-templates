package com.yakivmospan.templates.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

actual fun defaultDispatcherProvider(): DispatcherProvider {
    return AndroidDispatcherProvider()
}

class AndroidDispatcherProvider() : DispatcherProvider {
    override val main: CoroutineDispatcher = Dispatchers.Main
    override val io: CoroutineDispatcher = Dispatchers.IO
    override val default: CoroutineDispatcher = Dispatchers.Default
}