package com.yakivmospan.templates.ui.feature.productcatalog

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.yakivmospan.templates.feature.productcatalog.presentation.MR
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductViewData
import com.yakivmospan.templates.feature.productcatalog.presentation.details.ProductDetailsEvent
import com.yakivmospan.templates.feature.productcatalog.presentation.details.ProductDetailsSideEffect
import com.yakivmospan.templates.feature.productcatalog.presentation.details.ProductDetailsState
import com.yakivmospan.templates.feature.productcatalog.presentation.details.ProductDetailsViewModel
import com.yakivmospan.templates.presentation.theme.AppTheme
import com.yakivmospan.templates.ui.utils.CollectSideEffects
import dev.icerock.moko.resources.compose.localized
import dev.icerock.moko.resources.compose.stringResource
import dev.icerock.moko.resources.desc.desc
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

// ─────────────────────────────────────────────────────────────────────────────
// Public overload — ViewModel-connected
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsScreen(
    productId: Int,
    viewModel: ProductDetailsViewModel = koinViewModel {
        parametersOf(productId)
    }
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val retryButtonLabel = stringResource(MR.strings.pd_catalog_feature_retry_button)
    val context = LocalContext.current
    val onEvent: (ProductDetailsEvent) -> Unit = viewModel::onEvent

    CollectSideEffects(viewModel.sideEffects) { sideEffect ->
        when (sideEffect) {
            is ProductDetailsSideEffect.ShowError -> handleShowError(
                sideEffect = sideEffect,
                snackbarHostState = snackbarHostState,
                retryButtonLabel = retryButtonLabel,
                context = context,
                onEvent = onEvent,
            )
        }
    }

    ProductDetailsScreen(
        state = state.value,
        snackbarHostState = snackbarHostState,
        onEvent = onEvent,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Side effects
// ─────────────────────────────────────────────────────────────────────────────

private suspend fun handleShowError(
    sideEffect: ProductDetailsSideEffect.ShowError,
    snackbarHostState: SnackbarHostState,
    retryButtonLabel: String,
    context: Context,
    onEvent: (ProductDetailsEvent) -> Unit,
) {
    val result = snackbarHostState.showSnackbar(
        message = sideEffect.message.toString(context),
        actionLabel = retryButtonLabel,
        duration = SnackbarDuration.Long
    )
    if (result == SnackbarResult.ActionPerformed) {
        onEvent(ProductDetailsEvent.Retry)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private overload — stateless, Preview-friendly
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductDetailsScreen(
    state: ProductDetailsState,
    snackbarHostState: SnackbarHostState,
    onEvent: (ProductDetailsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = state.product?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = { onEvent(ProductDetailsEvent.NavigateBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(MR.strings.pd_catalog_feature_navigate_back_accessibility)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
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
        },
        modifier = modifier,
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
        ) {
            val product = state.product

            when {
                // Loading state (initial load only)
                state.isLoading && product == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp)
                    )
                }

                // Content state with PullToRefresh
                product != null -> {
                    PullToRefreshBox(
                        isRefreshing = state.isLoading,
                        onRefresh = { onEvent(ProductDetailsEvent.Retry) },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Image section with FAB
                            ProductImageSection(
                                product = product,
                                onToggleFavorite = { onEvent(ProductDetailsEvent.ToggleFavorite) }
                            )

                            // Details section
                            ProductDetailsSection(product = product)
                        }
                    }
                }

                // Error state (failed to load product)
                !state.isLoading -> {
                    ErrorState(
                        errorMessage = state.errorMessage?.localized()
                            ?: MR.strings.pd_catalog_feature_generic_error_message.desc().localized(),
                        onRetry = { onEvent(ProductDetailsEvent.Retry) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProductImageSection(
    product: ProductViewData,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    ) {
        // Product Image
        AsyncImage(
            model = product.imageUrl,
            contentDescription = product.title,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop
        )

        // Favorite FAB - positioned at bottom-right corner
        FloatingActionButton(
            onClick = onToggleFavorite,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Icon(
                imageVector = if (product.isFavorite) {
                    Icons.Filled.Favorite
                } else {
                    Icons.Filled.FavoriteBorder
                },
                contentDescription = if (product.isFavorite) {
                    stringResource(MR.strings.pd_catalog_feature_remove_from_favorites_accessibility)
                } else {
                    stringResource(MR.strings.pd_catalog_feature_add_to_favorites_accessibility)
                }
            )
        }
    }
}

@Composable
private fun ProductDetailsSection(
    product: ProductViewData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Product Title
        Text(
            text = product.title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Product Price
        Text(
            text = stringResource(
                MR.strings.pd_catalog_feature_price_format,
                product.formattedPrice
            ),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        // Divider
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Product Description
        Text(
            text = product.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(text = stringResource(MR.strings.pd_catalog_feature_retry_button))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
private fun ProductDetailsScreenLoadingPreview() {
    AppTheme {
        ProductDetailsScreen(
            state = ProductDetailsState(isLoading = true),
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ProductDetailsScreenLoadedPreview() {
    AppTheme {
        ProductDetailsScreen(
            state = ProductDetailsState(
                product = ProductViewData(
                    id = 1,
                    title = "Wireless Headphones",
                    description = "Premium noise-cancelling wireless headphones with 30-hour battery life and exceptional sound quality.",
                    price = 299.99,
                    formattedPrice = "$299.99",
                    imageUrl = "",
                    isFavorite = false,
                )
            ),
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

@PreviewLightDark
@Composable
private fun ProductDetailsScreenErrorPreview() {
    AppTheme {
        ProductDetailsScreen(
            state = ProductDetailsState(
                errorMessage = "Network error. Please check your connection.".desc()
            ),
            snackbarHostState = SnackbarHostState(),
            onEvent = {},
        )
    }
}

