package com.yakivmospan.templates.feature.login.presentation

import com.yakivmospan.templates.core.navigation.NavigationRoute
import com.yakivmospan.templates.core.navigation.NavigationTarget
import kotlinx.serialization.Serializable

@Serializable
sealed class LoginNavigationRoutes {

    /** Entry point — the login / create-PIN screen. */
    @Serializable
    data object Login : NavigationRoute

    /**
     * Generic exit route emitted by the login feature after successful auth.
     * The app module maps this to the real catalog destination in the NavHost.
     * Login feature never depends on product-catalog:presentation.
     */
    @Serializable
    data object ExitToProductCatalog : NavigationRoute
}

sealed class LoginNavigationTargets {
    data object ToProductCatalog : NavigationTarget(
        route = LoginNavigationRoutes.ExitToProductCatalog,
        clearBackStackUntil = LoginNavigationRoutes.Login,
        clearBackInclusively = true,
    )
}