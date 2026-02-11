package com.yakivmospan.templates.ui.feature.productcatalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.yakivmospan.templates.feature.productcatalog.presentation.MR
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogEvent
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogViewModel
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductViewData
import dev.icerock.moko.resources.compose.stringResource
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
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

    // Handle error display in Snackbar
    LaunchedEffect(state.value.error) {
        state.value.error?.let { errorMessage ->
            val result = snackbarHostState.showSnackbar(
                message = errorMessage,
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
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Search Bar
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

            // Determine which items to display
            val displayItems = if (searchQuery.value.isNotBlank()) {
                state.value.searchResult
            } else {
                state.value.products
            }

            val isSearchMode = searchQuery.value.isNotBlank()

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

        // Snackbar Host - positioned above keyboard with proper insets
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.ime)
                .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
            snackbar = { snackbarData ->
                Snackbar(
                    snackbarData = snackbarData,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    actionColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 8.dp)
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
    LaunchedEffect(listState, isSearchMode, isLoading, hasNextPage) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = layoutInfo.totalItemsCount

            // Create a tuple of the conditions we care about
            Triple(
                lastVisibleItem?.index ?: -1,
                totalItems,
                lastVisibleItem != null && lastVisibleItem.index >= totalItems - 2
            )
        }
            .distinctUntilChanged()
            .filter { (_, _, isNearBottom) -> isNearBottom }
            .collect {
                // Check all conditions before triggering
                if (!isSearchMode && hasNextPage && !isLoading) {
                    onLoadNextPage()
                }
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

@Composable
private fun SearchBar(
    query: String,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text(stringResource(MR.strings.pd_catalog_feature_search_placeholder)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(MR.strings.pd_catalog_feature_search_icon_description)
            )
        },
        trailingIcon = {
            when {
                isSearching -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }

                query.isNotEmpty() -> {
                    IconButton(onClick = onClearClick) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(MR.strings.pd_catalog_feature_clear_search_icon_description)
                        )
                    }
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
private fun ProductCard(
    product: ProductViewData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Product Image
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.title,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )

            // Product Details
            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(MR.strings.pd_catalog_feature_price_format, product.formattedPrice),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun LoadingState(
    modifier: Modifier = Modifier,
    message: String
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState(
    modifier: Modifier = Modifier,
    isSearchActive: Boolean = false,
    searchQuery: String = ""
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isSearchActive) {
                stringResource(MR.strings.pd_catalog_feature_no_search_results_title, searchQuery)
            } else {
                stringResource(MR.strings.pd_catalog_feature_no_products_title)
            },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = if (isSearchActive) {
                stringResource(MR.strings.pd_catalog_feature_no_search_results_description)
            } else {
                stringResource(MR.strings.pd_catalog_feature_no_products_description)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}