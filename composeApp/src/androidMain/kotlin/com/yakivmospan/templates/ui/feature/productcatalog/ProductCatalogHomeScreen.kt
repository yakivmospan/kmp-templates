package com.yakivmospan.templates.ui.feature.productcatalog

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yakivmospan.templates.feature.productcatalog.presentation.MR
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogHomeEvent
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogHomeViewModel
import dev.icerock.moko.resources.compose.stringResource
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProductCatalogHomeScreen(
    innerPadding: PaddingValues,
    viewModel: ProductCatalogHomeViewModel = koinViewModel()
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = state.value.selectedTabIndex == 0,
                    onClick = { viewModel.onEvent(ProductCatalogHomeEvent.SelectTab(0)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = stringResource(MR.strings.pd_catalog_feature_catalog_tab_label)
                        )
                    },
                    label = {
                        Text(text = stringResource(MR.strings.pd_catalog_feature_catalog_tab_label))
                    }
                )

                NavigationBarItem(
                    selected = state.value.selectedTabIndex == 1,
                    onClick = { viewModel.onEvent(ProductCatalogHomeEvent.SelectTab(1)) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = stringResource(MR.strings.pd_catalog_feature_favorites_tab_label)
                        )
                    },
                    label = {
                        Text(text = stringResource(MR.strings.pd_catalog_feature_favorites_tab_label))
                    }
                )
            }
        }
    ) { scaffoldPadding ->
        // Only use scaffoldPadding which already accounts for the bottom navigation bar
        // innerPadding from MainActivity is already handled by the outer padding modifier

        // Use rememberSaveable key to preserve ViewModels across tab switches
        when (state.value.selectedTabIndex) {
            0 -> {
                // Key ensures ViewModel is preserved when switching tabs
                ProductCatalogScreen(
                    innerPadding = scaffoldPadding,
                    productCatalogViewModel = koinViewModel(key = "catalog_vm")
                )
            }

            1 -> {
                ProductFavoritesScreen(
                    innerPadding = scaffoldPadding,
                    viewModel = koinViewModel(key = "favorites_vm")
                )
            }
        }
    }
}