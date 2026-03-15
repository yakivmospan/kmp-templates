package com.yakivmospan.templates

import com.yakivmospan.templates.feature.productcatalog.presentation.catalog.ProductCatalogViewModel
import org.koin.core.component.KoinComponent

class KoinHelper : KoinComponent {
    fun getProductCatalogViewModel(): ProductCatalogViewModel = getKoin().get()
}