package com.yakivmospan.templates.feature.productcatalog.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.yakivmospan.templates.feature.productcatalog.data.local.database.ProductCatalogDatabase
import org.koin.core.module.Module

import org.koin.dsl.module

actual fun productCatalogDatabaseModule(): Module = module {
    single {
        val driver = createIOSProductDetailsSqlDriver()
        ProductCatalogDatabase(driver)
    }
}

private fun createIOSProductDetailsSqlDriver(): SqlDriver {
    return NativeSqliteDriver(
        schema = ProductCatalogDatabase.Schema,
        name = DATABASE_NAME
    )
}