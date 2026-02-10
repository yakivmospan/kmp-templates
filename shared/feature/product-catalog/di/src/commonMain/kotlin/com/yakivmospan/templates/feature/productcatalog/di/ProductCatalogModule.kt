package com.yakivmospan.templates.feature.productcatalog.di

import com.yakivmospan.templates.core.data.mapper.DefaultExceptionMapper
import com.yakivmospan.templates.core.data.mapper.ExceptionMapper
import com.yakivmospan.templates.feature.productcatalog.data.mapper.PaginatedProductsMapper
import com.yakivmospan.templates.feature.productcatalog.data.mapper.ProductMapper
import com.yakivmospan.templates.feature.productcatalog.data.remote.ProductRemoteDataSource
import com.yakivmospan.templates.feature.productcatalog.data.repository.ProductRepositoryImpl
import com.yakivmospan.templates.feature.productcatalog.domain.repository.ProductRepository
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.GetProductByIdUseCase
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.GetProductsUseCase
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.SearchProductsUseCase
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogViewModel
import org.koin.dsl.module

val productCatalogModule = module {
    // Data layer
    single { ProductMapper() }
    single<ExceptionMapper> { DefaultExceptionMapper() }
    single { PaginatedProductsMapper(productMapper = get()) }
    single { ProductRemoteDataSource(httpClient = get()) }

    single<ProductRepository> {
        ProductRepositoryImpl(
            remoteDataSource = get(),
            productMapper = get(),
            paginatedProductsMapper = get(),
            exceptionMapper = get(),
            dispatchers = get()
        )
    }

    // Domain layer
    factory { GetProductsUseCase(get()) }
    factory { GetProductByIdUseCase(get()) }
    factory { SearchProductsUseCase(get()) }

    // Presentation layer
    factory { ProductCatalogViewModel(get(), get()) }
}