package com.yakivmospan.templates

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.yakivmospan.templates.core.navigation.NavigatorCommandsFlow
import com.yakivmospan.templates.core.navigation.NavigatorResultsFlow
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogNavigationRoutes
import com.yakivmospan.templates.presentation.theme.AppTheme
import com.yakivmospan.templates.ui.feature.productcatalog.ProductCatalogHomeScreen
import com.yakivmospan.templates.ui.feature.productcatalog.ProductDetailsScreen
import com.yakivmospan.templates.ui.navigation.ComposeNavigatorCommandsHandler
import org.koin.compose.getKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme() {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(
                        innerPadding,
                        navController = rememberNavController(),
                        navigatorCommands = getKoin().get(),
                        navigatorResults = getKoin().get()
                    )
                }
            }
        }
    }
}

@Composable
fun AppNavigation(
    innerPadding: PaddingValues,
    navController: NavHostController,
    navigatorCommands: NavigatorCommandsFlow = getKoin().get(),
    navigatorResults: NavigatorResultsFlow = getKoin().get()
) {
    // Connects navigation commands & results from Navigator with NavController
    ComposeNavigatorCommandsHandler(navController, navigatorCommands, navigatorResults, onNothingToPop = {/*finish?*/ })

    // Destination graph
    NavHost(navController, startDestination = ProductCatalogNavigationRoutes.ProductCatalogHome) {
        composable<ProductCatalogNavigationRoutes.ProductCatalogHome> {
            ProductCatalogHomeScreen(innerPadding)
        }

        composable<ProductCatalogNavigationRoutes.ProductDetails> { backStackEntry ->
            val route = backStackEntry.toRoute<ProductCatalogNavigationRoutes.ProductDetails>()
            ProductDetailsScreen(
                innerPadding = innerPadding,
                productId = route.id
            )
        }
    }
}