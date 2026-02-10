package com.yakivmospan.templates.ui.feature.productcatalog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogEvent
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProductCatalogScreen(
    innerPadding: PaddingValues,
    productCatalogViewModel: ProductCatalogViewModel = koinViewModel()
) {
    val state = productCatalogViewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "ProductCatalogScreen",
            modifier = Modifier.clickable {
                productCatalogViewModel.onEvent(ProductCatalogEvent.SelectProduct(1))
            }
        )
    }
}