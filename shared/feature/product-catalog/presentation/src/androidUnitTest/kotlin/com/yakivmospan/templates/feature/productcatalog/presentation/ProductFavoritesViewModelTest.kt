package com.yakivmospan.templates.feature.productcatalog.presentation

import com.yakivmospan.templates.core.navigation.Navigator
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.ObserveFavoritesUseCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ProductFavoritesViewModelTest : ViewModelTest() {

    private lateinit var navigator: Navigator
    private lateinit var observeFavoritesUseCase: ObserveFavoritesUseCase
    private lateinit var viewDataMapper: ProductToViewDataMapper

    override fun setupTest() {
        navigator = mockk(relaxed = true)
        observeFavoritesUseCase = mockk()
        viewDataMapper = ProductToViewDataMapper()
    }

    // Step 1: Initial State Test
    @Test
    fun `initial state should be empty with no loading or errors`() = runTest {
        // Given
        every { observeFavoritesUseCase() } returns flowOf(emptyList())

        // When
        val viewModel = ProductFavoritesViewModel(navigator, observeFavoritesUseCase, viewDataMapper)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val expectedState = ProductFavoritesState(
            favorites = emptyList(),
            searchResult = emptyList(), // Empty query returns all favorites (which is empty)
            isLoading = false,
            isSearching = false,
            error = null
        )
        assertEquals(expectedState, viewModel.state.value)
        assertEquals("", viewModel.searchQuery.value)
    }

    // Step 2: Favorites Loading Success
    @Test
    fun `observing favorites should load and map products correctly`() = runTest {
        // Given
        val products = ProductTestFixtures.sampleProducts
        every { observeFavoritesUseCase() } returns flowOf(products)

        // When
        val viewModel = ProductFavoritesViewModel(navigator, observeFavoritesUseCase, viewDataMapper)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val expectedFavorites = products.map { viewDataMapper.map(it) }
        val expectedState = ProductFavoritesState(
            favorites = expectedFavorites,
            searchResult = expectedFavorites, // When query is blank, searchResult equals all favorites
            isLoading = false,
            isSearching = false,
            error = null
        )
        assertEquals(expectedState, viewModel.state.value)
        assertEquals("", viewModel.searchQuery.value)
    }

    // Step 3: Favorites Loading Error
    @Test
    fun `favorites loading error should update state with error message`() = runTest {
        // Given
        val errorMessage = "Network error"
        every { observeFavoritesUseCase() } returns flow {
            throw Exception(errorMessage)
        }

        // When
        val viewModel = ProductFavoritesViewModel(navigator, observeFavoritesUseCase, viewDataMapper)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val expectedState = ProductFavoritesState(
            favorites = emptyList(),
            searchResult = emptyList(),
            isLoading = false,
            isSearching = false,
            error = errorMessage
        )
        assertEquals(expectedState, viewModel.state.value)
        assertEquals("", viewModel.searchQuery.value)
    }

    // Step 4: Retry After Error
    @Test
    fun `retry event should reload favorites after error`() = runTest {
        // Given - Initial error state
        val errorMessage = "Network error"
        every { observeFavoritesUseCase() } returns flow {
            throw Exception(errorMessage)
        }
        val viewModel = ProductFavoritesViewModel(navigator, observeFavoritesUseCase, viewDataMapper)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify error state
        assertEquals(errorMessage, viewModel.state.value.error)

        // Setup successful response for retry
        val products = ProductTestFixtures.sampleProducts
        every { observeFavoritesUseCase() } returns flowOf(products)

        // When
        viewModel.onEvent(ProductFavoritesEvent.Retry)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val expectedFavorites = products.map { viewDataMapper.map(it) }
        val expectedState = ProductFavoritesState(
            favorites = expectedFavorites,
            searchResult = expectedFavorites, // Query is still blank, so searchResult equals all favorites
            isLoading = false,
            isSearching = false,
            error = null
        )
        assertEquals(expectedState, viewModel.state.value)
        assertEquals("", viewModel.searchQuery.value)
    }

    // Step 5: Search Functionality with Debounce
    @Test
    fun `search should debounce and filter favorites by title and description`() = runTest {
        // Given
        val products = ProductTestFixtures.sampleProducts
        every { observeFavoritesUseCase() } returns flowOf(products)
        val viewModel = ProductFavoritesViewModel(navigator, observeFavoritesUseCase, viewDataMapper)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onEvent(ProductFavoritesEvent.SearchFavorites("smartphone"))
        testDispatcher.scheduler.runCurrent()

        // Then - isSearching should be true immediately (before debounce)
        assertEquals(true, viewModel.state.value.isSearching)

        // Advance time by debounce duration
        testDispatcher.scheduler.advanceTimeBy(ProductCatalogConfig.SEARCH_DEBOUNCE_MS)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - After debounce, search is complete
        val allFavorites = products.map { viewDataMapper.map(it) }
        val filteredResults = allFavorites.filter {
            it.title.lowercase().contains("smartphone") ||
                    it.description.lowercase().contains("smartphone")
        }
        val expectedState = ProductFavoritesState(
            favorites = allFavorites,
            searchResult = filteredResults,
            isLoading = false,
            isSearching = false,
            error = null
        )
        assertEquals(expectedState, viewModel.state.value)
        assertEquals("smartphone", viewModel.searchQuery.value)
    }

    // Step 6: Search Query Changes (Debounce Behavior)
    @Test
    fun `rapid search queries should debounce and only process final query`() = runTest {
        // Given
        val products = ProductTestFixtures.sampleProducts
        every { observeFavoritesUseCase() } returns flowOf(products)
        val viewModel = ProductFavoritesViewModel(navigator, observeFavoritesUseCase, viewDataMapper)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Send multiple rapid queries
        viewModel.onEvent(ProductFavoritesEvent.SearchFavorites("smart"))
        testDispatcher.scheduler.runCurrent()
        assertEquals(true, viewModel.state.value.isSearching)

        testDispatcher.scheduler.advanceTimeBy(100)
        viewModel.onEvent(ProductFavoritesEvent.SearchFavorites("laptop"))
        testDispatcher.scheduler.runCurrent()
        assertEquals(true, viewModel.state.value.isSearching)

        testDispatcher.scheduler.advanceTimeBy(100)
        viewModel.onEvent(ProductFavoritesEvent.SearchFavorites("headphones"))
        testDispatcher.scheduler.runCurrent()
        assertEquals(true, viewModel.state.value.isSearching)

        // Advance past debounce for final query
        testDispatcher.scheduler.advanceTimeBy(ProductCatalogConfig.SEARCH_DEBOUNCE_MS)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Only "headphones" query should be processed
        val allFavorites = products.map { viewDataMapper.map(it) }
        val filteredResults = allFavorites.filter {
            it.title.lowercase().contains("headphones") ||
                    it.description.lowercase().contains("headphones")
        }
        val expectedState = ProductFavoritesState(
            favorites = allFavorites,
            searchResult = filteredResults,
            isLoading = false,
            isSearching = false,
            error = null
        )
        assertEquals(expectedState, viewModel.state.value)
        assertEquals("headphones", viewModel.searchQuery.value)
    }

    // Step 6a: Immediate Search Feedback
    @Test
    fun `search should show isSearching true immediately before debounce`() = runTest {
        // Given
        val products = ProductTestFixtures.sampleProducts
        every { observeFavoritesUseCase() } returns flowOf(products)
        val viewModel = ProductFavoritesViewModel(navigator, observeFavoritesUseCase, viewDataMapper)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify initial state
        assertEquals(false, viewModel.state.value.isSearching)

        // When - Send search query but don't advance time
        viewModel.onEvent(ProductFavoritesEvent.SearchFavorites("smartphone"))
        testDispatcher.scheduler.runCurrent()

        // Then - isSearching should be true immediately, even before debounce
        assertEquals(true, viewModel.state.value.isSearching)
        assertEquals("smartphone", viewModel.searchQuery.value)

        // When - Advance past debounce
        testDispatcher.scheduler.advanceTimeBy(ProductCatalogConfig.SEARCH_DEBOUNCE_MS)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - isSearching should be false after search completes
        assertEquals(false, viewModel.state.value.isSearching)
    }

    // Step 7: Clear Search
    @Test
    fun `clear search should reset search results and query`() = runTest {
        // Given - Active search with results
        val products = ProductTestFixtures.sampleProducts
        every { observeFavoritesUseCase() } returns flowOf(products)
        val viewModel = ProductFavoritesViewModel(navigator, observeFavoritesUseCase, viewDataMapper)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ProductFavoritesEvent.SearchFavorites("smartphone"))
        testDispatcher.scheduler.advanceTimeBy(ProductCatalogConfig.SEARCH_DEBOUNCE_MS)
        testDispatcher.scheduler.advanceUntilIdle()

        // When
        viewModel.onEvent(ProductFavoritesEvent.ClearSearch)
        testDispatcher.scheduler.advanceTimeBy(ProductCatalogConfig.SEARCH_DEBOUNCE_MS)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val allFavorites = products.map { viewDataMapper.map(it) }
        val expectedState = ProductFavoritesState(
            favorites = allFavorites,
            searchResult = allFavorites, // Empty query returns all favorites
            isLoading = false,
            isSearching = false,
            error = null
        )
        assertEquals(expectedState, viewModel.state.value)
        assertEquals("", viewModel.searchQuery.value)
    }

    // Step 8: Empty Search Query
    @Test
    fun `empty or blank search query should return all favorites as search results`() = runTest {
        // Given - Active search with results
        val products = ProductTestFixtures.sampleProducts
        every { observeFavoritesUseCase() } returns flowOf(products)
        val viewModel = ProductFavoritesViewModel(navigator, observeFavoritesUseCase, viewDataMapper)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onEvent(ProductFavoritesEvent.SearchFavorites("smartphone"))
        testDispatcher.scheduler.advanceTimeBy(ProductCatalogConfig.SEARCH_DEBOUNCE_MS)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Search with blank string
        viewModel.onEvent(ProductFavoritesEvent.SearchFavorites("   "))
        testDispatcher.scheduler.advanceTimeBy(ProductCatalogConfig.SEARCH_DEBOUNCE_MS)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val allFavorites = products.map { viewDataMapper.map(it) }
        val expectedState = ProductFavoritesState(
            favorites = allFavorites,
            searchResult = allFavorites, // Blank query returns all favorites
            isLoading = false,
            isSearching = false,
            error = null
        )
        assertEquals(expectedState, viewModel.state.value)
        assertEquals("   ", viewModel.searchQuery.value)
    }

    // Step 9: Search with No Results
    @Test
    fun `search query matching no products should return empty search results`() = runTest {
        // Given
        val products = ProductTestFixtures.sampleProducts
        every { observeFavoritesUseCase() } returns flowOf(products)
        val viewModel = ProductFavoritesViewModel(navigator, observeFavoritesUseCase, viewDataMapper)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Search with query that matches nothing
        viewModel.onEvent(ProductFavoritesEvent.SearchFavorites("nonexistent product xyz"))
        testDispatcher.scheduler.advanceTimeBy(ProductCatalogConfig.SEARCH_DEBOUNCE_MS)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val allFavorites = products.map { viewDataMapper.map(it) }
        val expectedState = ProductFavoritesState(
            favorites = allFavorites,
            searchResult = emptyList(),
            isLoading = false,
            isSearching = false,
            error = null
        )
        assertEquals(expectedState, viewModel.state.value)
        assertEquals("nonexistent product xyz", viewModel.searchQuery.value)
    }

    // Step 10: Product Selection Navigation
    @Test
    fun `selecting product should navigate to product details`() = runTest {
        // Given
        every { observeFavoritesUseCase() } returns flowOf(emptyList())
        val viewModel = ProductFavoritesViewModel(navigator, observeFavoritesUseCase, viewDataMapper)
        testDispatcher.scheduler.advanceUntilIdle()

        val productId = 123

        // When
        viewModel.onEvent(ProductFavoritesEvent.SelectProduct(productId))

        // Then
        verify(exactly = 1) {
            navigator.navigate(match {
                it is ProductCatalogNavigationTargets.ToProductDetails && it.id == productId
            })
        }

        // State should remain unchanged
        val expectedState = ProductFavoritesState(
            favorites = emptyList(),
            searchResult = emptyList(),
            isLoading = false,
            isSearching = false,
            error = null
        )
        assertEquals(expectedState, viewModel.state.value)
    }

    // Step 11: Favorites Update During Active Search
    @Test
    fun `favorites update should re-apply current search filter`() = runTest {
        // Given - Initial favorites with active search
        val initialProducts = listOf(
            ProductTestFixtures.createProduct(id = 1, title = "Smartphone", description = "A phone"),
            ProductTestFixtures.createProduct(id = 2, title = "Laptop", description = "A computer")
        )
        val favoritesFlow = MutableStateFlow(initialProducts)
        every { observeFavoritesUseCase() } returns favoritesFlow

        val viewModel = ProductFavoritesViewModel(navigator, observeFavoritesUseCase, viewDataMapper)
        testDispatcher.scheduler.advanceUntilIdle()

        // Activate search
        viewModel.onEvent(ProductFavoritesEvent.SearchFavorites("phone"))
        testDispatcher.scheduler.advanceTimeBy(ProductCatalogConfig.SEARCH_DEBOUNCE_MS)
        testDispatcher.scheduler.advanceUntilIdle()

        // When - Favorites are updated with new products
        val updatedProducts = listOf(
            ProductTestFixtures.createProduct(id = 1, title = "Smartphone", description = "A phone"),
            ProductTestFixtures.createProduct(id = 2, title = "Laptop", description = "A computer"),
            ProductTestFixtures.createProduct(id = 3, title = "Headphones", description = "Wireless phone accessory")
        )
        favoritesFlow.value = updatedProducts
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Search filter should be re-applied to new favorites
        val allFavorites = updatedProducts.map { viewDataMapper.map(it) }
        val filteredResults = allFavorites.filter {
            it.title.lowercase().contains("phone") ||
                    it.description.lowercase().contains("phone")
        }
        val expectedState = ProductFavoritesState(
            favorites = allFavorites,
            searchResult = filteredResults,
            isLoading = false,
            isSearching = false,
            error = null
        )
        assertEquals(expectedState, viewModel.state.value)
        assertEquals("phone", viewModel.searchQuery.value)
        assertEquals(2, filteredResults.size) // Smartphone and Headphones match "phone"
    }
}