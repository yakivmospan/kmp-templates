package com.yakivmospan.templates.feature.productcatalog.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * Base class for ViewModel tests that handles common test setup.
 * Provides test dispatcher configuration and cleanup.
 */
abstract class ViewModelTest {

    protected lateinit var testDispatcher: TestDispatcher

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        setupTest()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        cleanupTest()
        Dispatchers.resetMain()
    }

    /**
     * Override this to perform test-specific setup after dispatcher is configured.
     */
    protected open fun setupTest() {}

    /**
     * Override this to perform test-specific cleanup before dispatcher is reset.
     */
    protected open fun cleanupTest() {}
}