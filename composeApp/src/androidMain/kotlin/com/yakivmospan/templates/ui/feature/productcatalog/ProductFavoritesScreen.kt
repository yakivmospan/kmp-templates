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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yakivmospan.templates.feature.productcatalog.presentation.MR
import com.yakivmospan.templates.feature.productcatalog.presentation.favorites.ProductFavoritesEvent
import com.yakivmospan.templates.feature.productcatalog.presentation.favorites.ProductFavoritesSideEffect
import com.yakivmospan.templates.feature.productcatalog.presentation.favorites.ProductFavoritesState
import com.yakivmospan.templates.feature.productcatalog.presentation.favorites.ProductFavoritesViewModel
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductViewData
import com.yakivmospan.templates.presentation.theme.AppTheme
import com.yakivmospan.templates.ui.utils.CollectSideEffects
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel

// ─────────────────────────────────────────────────────────────────────────────
// Public overload — ViewModel-connected
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProductFavoritesScreen(
    innerPadding: PaddingValues,
    viewModel: ProductFavoritesViewModel = koinViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val searchQuery = viewModel.searchQuery.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val onEvent: (ProductFavoritesEvent) -> Unit = viewModel::onEvent
    val retryButtonLabel = stringResource(MR.strings.pd_catalog_feature_retry_button)
    val context = LocalContext.current

    CollectSideEffects(viewModel.sideEffects) { sideEffect ->
        when (sideEffect) {
            is ProductFavoritesSideEffect.ShowError -> handleShowError(
                sideEffect = sideEffect,
                snackbarHostState = snackbarHostState,
                retryButtonLabel = retryButtonLabel,
                context = context,
                onEvent = onEvent,
            )
        }
    }

    ProductFavoritesScreen(
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
    sideEffect: ProductFavoritesSideEffect.ShowError,
    snackbarHostState: SnackbarHostState,
    retryButtonLabel: String,
    context: android.content.Context,
    onEvent: (ProductFavoritesEvent) -> Unit,
) {
    val result = snackbarHostState.showSnackbar(
        message = sideEffect.message.toString(context),
        actionLabel = retryButtonLabel,
        duration = SnackbarDuration.Long
    )
    if (result == SnackbarResult.ActionPerformed) {
        onEvent(ProductFavoritesEvent.Retry)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private overload — stateless, Preview-friendly
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProductFavoritesScreen(
    innerPadding: PaddingValues,
    state: ProductFavoritesState,
    searchQuery: String,
    snackbarHostState: SnackbarHostState,
    onEvent: (ProductFavoritesEvent) -> Unit,
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
            // Show SearchBar only when there are favorites (not empty)
            if (state.favorites.isNotEmpty()) {
                SearchBar(
                    query = searchQuery,
                    isSearching = state.isSearching,
                    onQueryChange = { query -> onEvent(ProductFavoritesEvent.SearchFavorites(query)) },
                    onClearClick = { onEvent(ProductFavoritesEvent.ClearSearch) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            val isSearchMode = searchQuery.isNotBlank()
            val displayItems = if (isSearchMode) state.searchResult else state.favorites

            // Content Area - Handle loading, empty, and content states
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when {
                    // Loading state (initial load)
                    state.isLoading -> {
                        LoadingState(
                            modifier = Modifier.align(Alignment.Center),
                            message = stringResource(MR.strings.pd_catalog_feature_loading_products_label)
                        )
                    }

                    // Empty state - No favorites yet
                    state.favorites.isEmpty() && !state.isLoading -> {
                        NoFavoritesEmptyState(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    // Empty state - No search results
                    displayItems.isEmpty() && !state.isSearching -> {
                        EmptyState(
                            modifier = Modifier.align(Alignment.Center),
                            isSearchActive = true,
                            searchQuery = searchQuery
                        )
                    }

                    // Content state (has favorites)
                    else -> {
                        FavoritesList(
                            displayItems = displayItems,
                            onProductClick = { productId ->
                                onEvent(ProductFavoritesEvent.SelectProduct(productId))
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


// ─────────────────────────────────────────────────────────────────────────────
// Sub-components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FavoritesList(
    displayItems: ImmutableList<ProductViewData>,
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

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
private fun ProductFavoritesScreenLoadingPreview() {
    AppTheme {
        ProductFavoritesScreen(
            innerPadding = PaddingValues(0.dp),
            state = ProductFavoritesState(isLoading = true),
            searchQuery = "",
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ProductFavoritesScreenNoFavoritesPreview() {
    AppTheme {
        ProductFavoritesScreen(
            innerPadding = PaddingValues(0.dp),
            state = ProductFavoritesState(),
            searchQuery = "",
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ProductFavoritesScreenLoadedPreview() {
    val items = persistentListOf(
        ProductViewData(id = 1, title = "Smartphone Pro", description = "Latest model", price = 999.99, formattedPrice = "$999.99", imageUrl = "", isFavorite = true),
        ProductViewData(id = 2, title = "Wireless Headphones", description = "Noise cancelling", price = 199.99, formattedPrice = "$199.99", imageUrl = "", isFavorite = true),
    )
    AppTheme {
        ProductFavoritesScreen(
            innerPadding = PaddingValues(0.dp),
            state = ProductFavoritesState(favorites = items, searchResult = items),
            searchQuery = "",
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ProductFavoritesScreenEmptySearchPreview() {
    val items = persistentListOf(
        ProductViewData(id = 1, title = "Smartphone Pro", description = "Latest model", price = 999.99, formattedPrice = "$999.99", imageUrl = "", isFavorite = true),
    )
    AppTheme {
        ProductFavoritesScreen(
            innerPadding = PaddingValues(0.dp),
            state = ProductFavoritesState(
                favorites = items,
                searchResult = persistentListOf(),
                isSearching = false,
            ),
            searchQuery = "xyz",
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

