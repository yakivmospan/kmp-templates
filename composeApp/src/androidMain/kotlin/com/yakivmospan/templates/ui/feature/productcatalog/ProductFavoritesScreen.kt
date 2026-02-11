package com.yakivmospan.templates.ui.feature.productcatalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yakivmospan.templates.feature.productcatalog.presentation.MR
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductFavoritesEvent
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductFavoritesViewModel
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductViewData
import dev.icerock.moko.resources.compose.localized
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProductFavoritesScreen(
    innerPadding: PaddingValues,
    viewModel: ProductFavoritesViewModel = koinViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val searchQuery = viewModel.searchQuery.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val retryButtonLabel = stringResource(MR.strings.pd_catalog_feature_retry_button)

    // Handle error display in Snackbar
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.errorEvent.collect { error ->
            val result = snackbarHostState.showSnackbar(
                message = error.toString(context),
                actionLabel = retryButtonLabel,
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.onEvent(ProductFavoritesEvent.Retry)
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
            // Show SearchBar only when there are favorites (not empty)
            if (state.value.favorites.isNotEmpty()) {
                SearchBar(
                    query = searchQuery.value,
                    isSearching = state.value.isSearching,
                    onQueryChange = { query ->
                        viewModel.onEvent(ProductFavoritesEvent.SearchFavorites(query))
                    },
                    onClearClick = {
                        viewModel.onEvent(ProductFavoritesEvent.ClearSearch)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            val isSearchMode = searchQuery.value.isNotBlank()

            // Determine which items to display
            val displayItems = if (isSearchMode) {
                state.value.searchResult
            } else {
                state.value.favorites
            }

            // Content Area - Handle loading, empty, and content states
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    // Loading state (initial load)
                    state.value.isLoading -> {
                        LoadingState(
                            modifier = Modifier.align(Alignment.Center),
                            message = stringResource(MR.strings.pd_catalog_feature_loading_products_label)
                        )
                    }

                    // Empty state - No favorites yet
                    state.value.favorites.isEmpty() && !state.value.isLoading -> {
                        NoFavoritesEmptyState(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // Empty state - No search results
                    displayItems.isEmpty() && isSearchMode && !state.value.isSearching -> {
                        EmptyState(
                            modifier = Modifier.align(Alignment.Center),
                            isSearchActive = true,
                            searchQuery = searchQuery.value
                        )
                    }

                    // Content state (has favorites)
                    else -> {
                        FavoritesList(
                            displayItems = displayItems,
                            onProductClick = { productId ->
                                viewModel.onEvent(
                                    ProductFavoritesEvent.SelectProduct(productId)
                                )
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
private fun FavoritesList(
    displayItems: List<ProductViewData>,
    onProductClick: (Int) -> Unit
) {
    LazyColumn(
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
    }
}

@Composable
private fun NoFavoritesEmptyState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.FavoriteBorder,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(MR.strings.pd_catalog_feature_no_favorites_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(MR.strings.pd_catalog_feature_no_favorites_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}