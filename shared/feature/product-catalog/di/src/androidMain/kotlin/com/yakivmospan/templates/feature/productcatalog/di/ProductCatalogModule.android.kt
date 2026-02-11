package com.yakivmospan.templates.feature.productcatalog.di

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.yakivmospan.templates.feature.productcatalog.data.local.database.ProductCatalogDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun productCatalogDatabaseModule(): Module = module {
    single {
        val driver = createAndroidProductDetailsSqlDriver(androidContext())
        ProductCatalogDatabase(driver)
    }
}

private fun createAndroidProductDetailsSqlDriver(context: Context): SqlDriver {
    return AndroidSqliteDriver(
        schema = ProductCatalogDatabase.Schema,
        context = context,
        name = DATABASE_NAME
    )
}