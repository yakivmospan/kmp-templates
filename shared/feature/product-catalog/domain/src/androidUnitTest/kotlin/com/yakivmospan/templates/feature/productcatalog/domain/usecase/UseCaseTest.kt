package com.yakivmospan.templates.feature.productcatalog.domain.usecase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

@OptIn(ExperimentalCoroutinesApi::class)
abstract class UseCaseTest {

    protected val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        setupTest()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        tearDownTest()
    }

    /**
     * Override this method to set up test-specific dependencies
     */
    protected open fun setupTest() {}

    /**
     * Override this method to clean up test-specific resources
     */
    protected open fun tearDownTest() {}
}