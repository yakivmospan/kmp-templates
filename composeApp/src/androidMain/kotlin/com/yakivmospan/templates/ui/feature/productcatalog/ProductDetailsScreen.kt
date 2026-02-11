package com.yakivmospan.templates.ui.feature.productcatalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductDetailsEvent
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductDetailsViewModel
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductViewData
import com.yakivmospan.templates.feature.productcatalog.presentation.MR
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsScreen(
    productId: Int,
    innerPadding: PaddingValues,
    viewModel: ProductDetailsViewModel = koinViewModel {
        parametersOf(productId)
    }
) {
    val state = viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val retryButtonLabel = stringResource(MR.strings.pd_catalog_feature_retry_button)

    // Handle error display in Snackbar (only when product is loaded)
    LaunchedEffect(state.value.error) {
        state.value.error?.let { errorMessage ->
            // Only show snackbar if we have a product (for refresh errors)
            if (state.value.product != null) {
                val result = snackbarHostState.showSnackbar(
                    message = errorMessage,
                    actionLabel = retryButtonLabel,
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.onEvent(ProductDetailsEvent.Retry)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = state.value.product?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.onEvent(ProductDetailsEvent.NavigateBack)
                    }) {
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
        modifier = Modifier.padding(innerPadding)
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
        ) {
            when {
                // Loading state (initial load only)
                state.value.isLoading && state.value.product == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp)
                    )
                }

                // Content state with PullToRefresh
                state.value.product != null -> {
                    PullToRefreshBox(
                        isRefreshing = state.value.isLoading,
                        onRefresh = {
                            viewModel.onEvent(ProductDetailsEvent.Retry)
                        },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Image section with FAB
                            ProductImageSection(
                                product = state.value.product!!,
                                onToggleFavorite = {
                                    viewModel.onEvent(ProductDetailsEvent.ToggleFavorite)
                                }
                            )

                            // Details section
                            ProductDetailsSection(
                                product = state.value.product!!
                            )
                        }
                    }
                }

                // Error state (failed to load product)
                !state.value.isLoading && state.value.product == null && state.value.error != null -> {
                    ErrorState(
                        errorMessage = state.value.error!!,
                        onRetry = {
                            viewModel.onEvent(ProductDetailsEvent.Retry)
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Empty state (product not found after loading)
                !state.value.isLoading && state.value.product == null && state.value.error == null -> {
                    EmptyProductState(
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

@Composable
private fun EmptyProductState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.BrokenImage,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(MR.strings.pd_catalog_feature_product_not_found_error),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}