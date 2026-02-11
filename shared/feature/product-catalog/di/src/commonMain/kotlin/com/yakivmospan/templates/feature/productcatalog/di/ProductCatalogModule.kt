package com.yakivmospan.templates.feature.productcatalog.di

import com.yakivmospan.templates.core.data.mapper.DefaultExceptionMapper
import com.yakivmospan.templates.core.data.mapper.ExceptionMapper
import com.yakivmospan.templates.feature.productcatalog.data.mapper.PaginatedProductsMapper
import com.yakivmospan.templates.feature.productcatalog.data.mapper.ProductMapper
import com.yakivmospan.templates.feature.productcatalog.data.remote.ProductRemoteDataSource
import com.yakivmospan.templates.feature.productcatalog.data.repository.ProductRepositoryImpl
import com.yakivmospan.templates.feature.productcatalog.domain.repository.ProductRepository
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.GetProductDetailsUseCase
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.GetProductsUseCase
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.SearchProductsUseCase
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.ToggleFavoriteUseCase
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogViewModel
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductDetailsViewModel
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductToViewDataMapper
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductViewDataToEntityMapper
import org.koin.dsl.module

fun productCatalogModules() = listOf(
    productCatalogDataModule,
    productCatalogDomainModule,
    productCatalogPresentationModule
)

val productCatalogDataModule = module {
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
}

val productCatalogDomainModule = module {
    factory { GetProductsUseCase(repository = get()) }
    factory { SearchProductsUseCase(repository = get()) }
    factory { GetProductDetailsUseCase(repository = get()) }
    factory { ToggleFavoriteUseCase(repository = get()) }
}

val productCatalogPresentationModule = module {
    single { ProductToViewDataMapper() }
    single { ProductViewDataToEntityMapper() }

    factory {
        ProductCatalogViewModel(
            navigator = get(),
            getProductsUseCase = get(),
            searchProductsUseCase = get(),
            viewDataMapper = get()
        )
    }

    factory { params ->
        ProductDetailsViewModel(
            productId = params.get(),
            navigator = get(),
            getProductDetailsUseCase = get(),
            toggleFavoriteUseCase = get(),
            productToViewDataMapper = get(),
            viewDataToProductMapper = get()
        )
    }
}