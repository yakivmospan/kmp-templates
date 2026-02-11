package com.yakivmospan.templates.feature.productcatalog.presentation

import com.yakivmospan.templates.core.common.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.TestDispatcher

/**
 * Test implementation of DispatcherProvider that uses TestDispatcher for all dispatchers.
 * This allows for deterministic testing of coroutines.
 */
class TestDispatcherProvider(
    private val mainDispatcher: TestDispatcher,
    private val ioDispatcher: TestDispatcher,
    private val defaultDispatcher: TestDispatcher
) : DispatcherProvider {

    constructor(testDispatcher: TestDispatcher) : this(
        mainDispatcher = testDispatcher,
        ioDispatcher = testDispatcher,
        defaultDispatcher = testDispatcher
    )

    override val main: CoroutineDispatcher = mainDispatcher
    override val io: CoroutineDispatcher = ioDispatcher
    override val default: CoroutineDispatcher = defaultDispatcher
}