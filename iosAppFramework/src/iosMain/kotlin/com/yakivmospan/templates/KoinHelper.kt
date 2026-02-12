package com.yakivmospan.templates

import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogViewModel
import org.koin.core.component.KoinComponent

class KoinHelper : KoinComponent {
    fun getProductCatalogViewModel(): ProductCatalogViewModel = getKoin().get()
}