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
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogHomeEvent
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogHomeState
import com.yakivmospan.templates.feature.productcatalog.presentation.home.ProductCatalogHomeViewModel
import com.yakivmospan.templates.presentation.theme.AppTheme
import org.koin.androidx.compose.koinViewModel

// --- Stateful entry point ---

@Composable
fun ProductCatalogHomeScreen(
    innerPadding: PaddingValues,
    viewModel: ProductCatalogHomeViewModel = koinViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    ProductCatalogHomeContent(
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

// --- Stateless / hoisted composable ---

@Composable
fun ProductCatalogHomeContent(
    state: ProductCatalogHomeState,
    onEvent: (ProductCatalogHomeEvent) -> Unit,
    content: @Composable (PaddingValues) -> Unit = {}
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
                            contentDescription = "Catalog"
                        )
                    },
                    label = { Text(text = "Catalog") }
                )
                NavigationBarItem(
                    selected = state.selectedTabIndex == 1,
                    onClick = { onEvent(ProductCatalogHomeEvent.SelectTab(1)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorites"
                        )
                    },
                    label = { Text(text = "Favorites") }
                )
            }
        }
    ) { scaffoldPadding ->
        content(scaffoldPadding)
    }
}

// --- Previews ---

@Preview(name = "Home – Catalog tab selected", showBackground = true)
@Composable
private fun PreviewCatalogTabSelected() {
    AppTheme {
        ProductCatalogHomeContent(
            state = ProductCatalogHomeState(selectedTabIndex = 0),
            onEvent = {},
            content = { Box(modifier = Modifier.fillMaxSize()) }
        )
    }
}

@Preview(name = "Home – Favorites tab selected", showBackground = true)
@Composable
private fun PreviewFavoritesTabSelected() {
    AppTheme {
        ProductCatalogHomeContent(
            state = ProductCatalogHomeState(selectedTabIndex = 1),
            onEvent = {},
            content = { Box(modifier = Modifier.fillMaxSize()) }
        )
    }
}

