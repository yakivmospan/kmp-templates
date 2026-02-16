package com.yakivmospan.templates.ui.feature.productcatalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yakivmospan.templates.feature.productcatalog.presentation.MR
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogEvent
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogViewModel
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductViewData
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProductCatalogScreen(
    innerPadding: PaddingValues,
    productCatalogViewModel: ProductCatalogViewModel = koinViewModel()
) {
    val state = productCatalogViewModel.state.collectAsStateWithLifecycle()
    val searchQuery = productCatalogViewModel.searchQuery.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val retryButtonLabel = stringResource(MR.strings.pd_catalog_feature_retry_button)
    val context = LocalContext.current

    // Handle error display in Snackbar
    LaunchedEffect(Unit) {
        productCatalogViewModel.errorEvent.collect { error ->
            val result = snackbarHostState.showSnackbar(
                message = error.toString(context),
                actionLabel = retryButtonLabel,
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                productCatalogViewModel.onEvent(ProductCatalogEvent.Retry)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Search Bar - using shared component
            SearchBar(
                query = searchQuery.value,
                isSearching = state.value.isSearching,
                onQueryChange = { query ->
                    productCatalogViewModel.onEvent(ProductCatalogEvent.SearchProducts(query))
                },
                onClearClick = {
                    productCatalogViewModel.onEvent(ProductCatalogEvent.ClearSearch)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            val isSearchMode = searchQuery.value.isNotBlank()

            // Determine which items to display
            val displayItems = if (isSearchMode) {
                state.value.searchResult
            } else {
                state.value.products
            }

            // Content Area - Handle loading, empty, and content states
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    // Loading state (initial load)
                    state.value.isLoading && displayItems.isEmpty() -> {
                        LoadingState(
                            modifier = Modifier.align(Alignment.Center),
                            message = if (isSearchMode) {
                                stringResource(MR.strings.pd_catalog_feature_searching_label)
                            } else {
                                stringResource(MR.strings.pd_catalog_feature_loading_products_label)
                            }
                        )
                    }

                    // Empty state (no products found)
                    displayItems.isEmpty() && !state.value.isLoading && !state.value.isSearching -> {
                        EmptyState(
                            modifier = Modifier.align(Alignment.Center),
                            isSearchActive = isSearchMode,
                            searchQuery = searchQuery.value
                        )
                    }

                    // Content state (has products)
                    else -> {
                        ProductList(
                            displayItems = displayItems,
                            isSearchMode = isSearchMode,
                            isLoading = state.value.isLoading,
                            hasNextPage = state.value.hasNextPage,
                            onProductClick = { productId ->
                                productCatalogViewModel.onEvent(
                                    ProductCatalogEvent.SelectProduct(productId)
                                )
                            },
                            onLoadNextPage = {
                                productCatalogViewModel.onEvent(ProductCatalogEvent.LoadNextPage)
                            }
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp)
                .imePadding(),
            snackbar = { snackbarData ->
                Snackbar(
                    snackbarData = snackbarData,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    actionColor = MaterialTheme.colorScheme.error
                )
            }
        )
    }
}

@Composable
private fun ProductList(
    displayItems: List<ProductViewData>,
    isSearchMode: Boolean,
    isLoading: Boolean,
    hasNextPage: Boolean,
    onProductClick: (Int) -> Unit,
    onLoadNextPage: () -> Unit
) {
    val listState = rememberLazyListState()


    // Infinite scroll using snapshotFlow - more reliable than derivedStateOf
    // for parameters that change outside of composition
    val isNearBottom by remember(listState) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = layoutInfo.totalItemsCount
            lastVisibleItem != null && lastVisibleItem.index >= totalItems - 2
        }
    }

    LaunchedEffect(isNearBottom, isSearchMode, isLoading, hasNextPage) {
        if (isNearBottom && !isSearchMode && hasNextPage && !isLoading) {
            onLoadNextPage()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = displayItems,
            key = { product -> product.id }
        ) { product ->
            // Using shared ProductCard component
            ProductCard(
                product = product,
                onClick = { onProductClick(product.id) }
            )
        }

        // Loading indicator at bottom for infinite scroll (only in browse mode)
        if (isLoading && displayItems.isNotEmpty() && !isSearchMode) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // End of list indicator (only in browse mode)
        if (!hasNextPage && displayItems.isNotEmpty() && !isLoading && !isSearchMode) {
            item {
                Text(
                    text = stringResource(MR.strings.pd_catalog_feature_no_more_products_label),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}