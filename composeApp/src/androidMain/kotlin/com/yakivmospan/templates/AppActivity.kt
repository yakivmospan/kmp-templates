package com.yakivmospan.templates

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.yakivmospan.templates.core.biometrics.BiometricsActivityProvider
import com.yakivmospan.templates.core.navigation.NavigatorCommandsFlow
import com.yakivmospan.templates.core.navigation.NavigatorResultsFlow
import com.yakivmospan.templates.feature.login.presentation.LoginNavigationRoutes
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogNavigationRoutes
import com.yakivmospan.templates.presentation.theme.AppTheme
import com.yakivmospan.templates.ui.feature.productcatalog.ProductCatalogHomeScreen
import com.yakivmospan.templates.ui.feature.productcatalog.ProductDetailsScreen
import com.yakivmospan.templates.ui.login.LoginScreen
import com.yakivmospan.templates.ui.navigation.ComposeNavigatorCommandsHandler
import org.koin.android.ext.android.inject
import org.koin.compose.getKoin

class MainActivity : FragmentActivity() {

    private val biometricsActivityProvider: BiometricsActivityProvider by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        biometricsActivityProvider.setActivity(this)

        setContent {
            AppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AppNavigation(
                        innerPadding = innerPadding,
                        navController = rememberNavController(),
                        navigatorCommands = getKoin().get(),
                        navigatorResults = getKoin().get(),
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        biometricsActivityProvider.setActivity(null)
    }
}

@Composable
fun AppNavigation(
    innerPadding: PaddingValues,
    navController: NavHostController,
    navigatorCommands: NavigatorCommandsFlow = getKoin().get(),
    navigatorResults: NavigatorResultsFlow = getKoin().get(),
) {
    // Connects navigation commands & results from Navigator with NavController
    ComposeNavigatorCommandsHandler(navController, navigatorCommands, navigatorResults, onNothingToPop = {/*finish?*/ })

    NavHost(
        navController = navController,
        startDestination = LoginNavigationRoutes.Login,
    ) {
        // ── Login feature ─────────────────────────────────────────────────────

        composable<LoginNavigationRoutes.Login> {
            LoginScreen()
        }

        composable<LoginNavigationRoutes.ExitToProductCatalog> {
            ProductCatalogHomeScreen()
        }

        // ── Product catalog feature ───────────────────────────────────────────

        composable<ProductCatalogNavigationRoutes.ProductCatalogHome> {
            ProductCatalogHomeScreen()
        }

        composable<ProductCatalogNavigationRoutes.ProductDetails> { backStackEntry ->
            val route = backStackEntry.toRoute<ProductCatalogNavigationRoutes.ProductDetails>()
            ProductDetailsScreen(productId = route.id)
        }
    }
}