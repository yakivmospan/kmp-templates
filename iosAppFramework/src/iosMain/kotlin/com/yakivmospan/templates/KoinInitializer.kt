package com.yakivmospan.templates

import com.yakivmospan.templates.core.di.coreModules
import com.yakivmospan.templates.feature.productcatalog.di.productCatalogModules
import org.koin.core.context.startKoin

class KoinInitializer {
    fun start() = startKoin {
        modules(coreModules() + productCatalogModules())
    }
}