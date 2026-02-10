package com.yakivmospan.templates.ui.feature.productcatalog

import androidx.compose.foundation.background
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
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductDetailsViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ProductDetailsScreen(
    productId: Int,
    innerPadding: PaddingValues,
    viewModel: ProductDetailsViewModel = koinViewModel {
        parametersOf(productId)
    }
) {
    val state = viewModel.state.collectAsStateWithLifecycle()

    if(!state.value.isLoading && state.value.product != null) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("ProductDetailsScreen :${state.value.product!!}")
        }
    }
}