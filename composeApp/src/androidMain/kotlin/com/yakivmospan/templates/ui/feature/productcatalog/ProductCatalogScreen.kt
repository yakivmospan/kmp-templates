package com.yakivmospan.templates.ui.feature.productcatalog

import android.content.Context
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
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yakivmospan.templates.feature.productcatalog.presentation.MR
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductViewData
import com.yakivmospan.templates.feature.productcatalog.presentation.catalog.ProductCatalogEvent
import com.yakivmospan.templates.feature.productcatalog.presentation.catalog.ProductCatalogSideEffect
import com.yakivmospan.templates.feature.productcatalog.presentation.catalog.ProductCatalogState
import com.yakivmospan.templates.feature.productcatalog.presentation.catalog.ProductCatalogViewModel
import com.yakivmospan.templates.presentation.theme.AppTheme
import com.yakivmospan.templates.ui.utils.CollectSideEffects
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.collections.immutable.persistentListOf
import org.koin.androidx.compose.koinViewModel

// ─────────────────────────────────────────────────────────────────────────────
// Public overload — ViewModel-connected
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProductCatalogScreen(
    innerPadding: PaddingValues,
    productCatalogViewModel: ProductCatalogViewModel = koinViewModel()
) {
    val state = productCatalogViewModel.state.collectAsStateWithLifecycle()
    val searchQuery = productCatalogViewModel.searchQuery.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val onEvent: (ProductCatalogEvent) -> Unit = productCatalogViewModel::onEvent
    val retryButtonLabel = stringResource(MR.strings.pd_catalog_feature_retry_button)
    val context = LocalContext.current

    CollectSideEffects(productCatalogViewModel.sideEffects) { sideEffect ->
        when (sideEffect) {
            is ProductCatalogSideEffect.ShowError -> handleShowError(
                sideEffect = sideEffect,
                snackbarHostState = snackbarHostState,
                retryButtonLabel = retryButtonLabel,
                context = context,
                onEvent = onEvent,
            )
        }
    }

    ProductCatalogScreen(
        innerPadding = innerPadding,
        state = state.value,
        searchQuery = searchQuery.value,
        snackbarHostState = snackbarHostState,
        onEvent = onEvent,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Side effects
// ─────────────────────────────────────────────────────────────────────────────


private suspend fun handleShowError(
    sideEffect: ProductCatalogSideEffect.ShowError,
    snackbarHostState: SnackbarHostState,
    retryButtonLabel: String,
    context: Context,
    onEvent: (ProductCatalogEvent) -> Unit,
) {
    val result = snackbarHostState.showSnackbar(
        message = sideEffect.message.toString(context),
        actionLabel = retryButtonLabel,
        duration = SnackbarDuration.Long
    )
    if (result == SnackbarResult.ActionPerformed) {
        onEvent(ProductCatalogEvent.Retry)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private overload — stateless, Preview-friendly
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProductCatalogScreen(
    innerPadding: PaddingValues,
    state: ProductCatalogState,
    searchQuery: String,
    snackbarHostState: SnackbarHostState,
    onEvent: (ProductCatalogEvent) -> Unit,
) {
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
            // Search Bar — generic reusable component
            SearchBar(
                query = searchQuery,
                isSearching = state.isSearching,
                onQueryChange = { query -> onEvent(ProductCatalogEvent.SearchProducts(query)) },
                onClearClick = { onEvent(ProductCatalogEvent.ClearSearch) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Content Area - Handle loading, empty, and content states
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    // Loading state (initial load)
                    state.isLoading && state.displayItems.isEmpty() -> {
                        LoadingState(
                            modifier = Modifier.align(Alignment.Center),
                            message = if (state.isSearchMode) {
                                stringResource(MR.strings.pd_catalog_feature_searching_label)
                            } else {
                                stringResource(MR.strings.pd_catalog_feature_loading_products_label)
                            }
                        )
                    }

                    // Empty state (no products found)
                    state.displayItems.isEmpty() && !state.isLoading && !state.isSearching -> {
                        EmptyState(
                            modifier = Modifier.align(Alignment.Center),
                            isSearchActive = state.isSearchMode,
                            searchQuery = searchQuery
                        )
                    }

                    // Content state (has products)
                    else -> {
                        ProductList(
                            state = state,
                            onEvent = onEvent,
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

// ─────────────────────────────────────────────────────────────────────────────
// Sub-components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProductList(
    state: ProductCatalogState,
    onEvent: (ProductCatalogEvent) -> Unit,
) {
    val isSearchMode = state.isSearchMode
    val displayItems = state.displayItems
    val isLoading = state.isLoading
    val hasNextPage = state.hasNextPage

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
            onEvent(ProductCatalogEvent.LoadNextPage)
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
            ProductCard(
                product = product,
                onClick = { onEvent(ProductCatalogEvent.SelectProduct(product.id)) }
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

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

private val previewProducts = persistentListOf(
    ProductViewData(id = 1, title = "Smartphone Pro", description = "Latest flagship model", price = 999.99, formattedPrice = "$999.99", imageUrl = "", isFavorite = false),
    ProductViewData(id = 2, title = "Wireless Headphones", description = "Noise cancelling", price = 199.99, formattedPrice = "$199.99", imageUrl = "", isFavorite = true),
    ProductViewData(id = 3, title = "Laptop Ultra", description = "Thin and light", price = 1299.99, formattedPrice = "$1299.99", imageUrl = "", isFavorite = false),
)

@PreviewLightDark
@Composable
private fun ProductCatalogScreenLoadingPreview() {
    AppTheme {
        ProductCatalogScreen(
            innerPadding = PaddingValues(0.dp),
            state = ProductCatalogState(isLoading = true),
            searchQuery = "",
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ProductCatalogScreenEmptyPreview() {
    AppTheme {
        ProductCatalogScreen(
            innerPadding = PaddingValues(0.dp),
            state = ProductCatalogState(),
            searchQuery = "",
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ProductCatalogScreenLoadedPreview() {
    AppTheme {
        ProductCatalogScreen(
            innerPadding = PaddingValues(0.dp),
            state = ProductCatalogState(products = previewProducts, hasNextPage = true),
            searchQuery = "",
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ProductCatalogScreenEmptySearchPreview() {
    AppTheme {
        ProductCatalogScreen(
            innerPadding = PaddingValues(0.dp),
            state = ProductCatalogState(
                products = previewProducts,
                searchResult = persistentListOf(),
                isSearchMode = true,
                isSearching = false,
            ),
            searchQuery = "xyz",
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}
