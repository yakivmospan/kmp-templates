package com.yakivmospan.templates.ui.feature.productcatalog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yakivmospan.templates.feature.productcatalog.presentation.MR
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogHomeEvent
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogHomeState
import com.yakivmospan.templates.feature.productcatalog.presentation.home.ProductCatalogHomeViewModel
import com.yakivmospan.templates.presentation.theme.AppTheme
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel

// ─────────────────────────────────────────────────────────────────────────────
// Public overload — ViewModel-connected
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProductCatalogHomeScreen(
    viewModel: ProductCatalogHomeViewModel = koinViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    ProductCatalogHomeScreen(
        state = state.value,
        onEvent = viewModel::onEvent,
        content = { scaffoldPadding ->
            when (state.value.selectedTabIndex) {
                0 -> ProductCatalogScreen(
                    innerPadding = scaffoldPadding,
                    productCatalogViewModel = koinViewModel(key = "catalog_vm")
                )
                1 -> ProductFavoritesScreen(
                    innerPadding = scaffoldPadding,
                    viewModel = koinViewModel(key = "favorites_vm")
                )
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Private overload — stateless, Preview-friendly
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProductCatalogHomeScreen(
    state: ProductCatalogHomeState,
    onEvent: (ProductCatalogHomeEvent) -> Unit,
    content: @Composable (PaddingValues) -> Unit = {},
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = state.selectedTabIndex == 0,
                    onClick = { onEvent(ProductCatalogHomeEvent.SelectTab(0)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = stringResource(MR.strings.pd_catalog_feature_catalog_tab)
                        )
                    },
                    label = { Text(text = stringResource(MR.strings.pd_catalog_feature_catalog_tab)) }
                )
                NavigationBarItem(
                    selected = state.selectedTabIndex == 1,
                    onClick = { onEvent(ProductCatalogHomeEvent.SelectTab(1)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = stringResource(MR.strings.pd_catalog_feature_favorites_tab)
                        )
                    },
                    label = { Text(text = stringResource(MR.strings.pd_catalog_feature_favorites_tab)) }
                )
            }
        }
    ) { scaffoldPadding ->
        content(scaffoldPadding)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────────────────────────────────────

@PreviewLightDark
@Composable
private fun PreviewCatalogTabSelected() {
    AppTheme {
        ProductCatalogHomeScreen(
            state = ProductCatalogHomeState(selectedTabIndex = 0),
            onEvent = {},
            content = { Box(modifier = Modifier.fillMaxSize()) }
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewFavoritesTabSelected() {
    AppTheme {
        ProductCatalogHomeScreen(
            state = ProductCatalogHomeState(selectedTabIndex = 1),
            onEvent = {},
            content = { Box(modifier = Modifier.fillMaxSize()) }
        )
    }
}