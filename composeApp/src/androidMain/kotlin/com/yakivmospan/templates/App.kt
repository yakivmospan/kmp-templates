package com.yakivmospan.templates

import android.app.Application
import com.yakivmospan.templates.core.di.coreModules
import com.yakivmospan.templates.feature.login.di.loginModules
import com.yakivmospan.templates.feature.productcatalog.di.productCatalogModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@App)
            modules(appModules())
        }
    }
}

fun appModules() = coreModules() + productCatalogModules() + loginModules()