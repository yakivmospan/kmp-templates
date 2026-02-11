package com.yakivmospan.templates.feature.productcatalog.presentation

import com.yakivmospan.templates.core.navigation.NavigationRoute
import com.yakivmospan.templates.core.navigation.NavigationTarget
import kotlinx.serialization.Serializable

@Serializable
sealed class ProductCatalogNavigationRoutes {
    @Serializable
    object ProductCatalogHome : NavigationRoute

    @Serializable
    data class ProductDetails(val id: Int) : NavigationRoute
}

sealed class ProductCatalogNavigationTargets {
    data class ToProductDetails(val id: Int) : NavigationTarget(
        ProductCatalogNavigationRoutes.ProductDetails(id),
        clearBackStackUntil = ProductCatalogNavigationRoutes.ProductCatalogHome,
        clearBackInclusively = false
    )
}