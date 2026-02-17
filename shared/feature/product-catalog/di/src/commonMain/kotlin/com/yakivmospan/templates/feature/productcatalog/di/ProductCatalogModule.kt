package com.yakivmospan.templates.feature.productcatalog.di

import com.yakivmospan.templates.core.data.mapper.DefaultExceptionMapper
import com.yakivmospan.templates.core.data.mapper.ExceptionMapper
import com.yakivmospan.templates.feature.productcatalog.data.local.FavoriteLocalDataSource
import com.yakivmospan.templates.feature.productcatalog.data.local.FavoriteLocalDataSourceImpl
import com.yakivmospan.templates.feature.productcatalog.data.mapper.FavoriteProductEntityMapper
import com.yakivmospan.templates.feature.productcatalog.data.mapper.PaginatedProductsMapper
import com.yakivmospan.templates.feature.productcatalog.data.mapper.ProductMapper
import com.yakivmospan.templates.feature.productcatalog.data.remote.ProductRemoteDataSource
import com.yakivmospan.templates.feature.productcatalog.data.remote.ProductRemoteDataSourceImpl
import com.yakivmospan.templates.feature.productcatalog.data.repository.ProductRepositoryImpl
import com.yakivmospan.templates.feature.productcatalog.domain.repository.ProductRepository
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.GetProductDetailsUseCase
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.GetProductsUseCase
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.ObserveFavoritesUseCase
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.SearchProductsUseCase
import com.yakivmospan.templates.feature.productcatalog.domain.usecase.ToggleFavoriteUseCase
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogHomeViewModel
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductCatalogViewModel
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductDetailsViewModel
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductFavoritesViewModel
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductToViewDataMapper
import com.yakivmospan.templates.feature.productcatalog.presentation.ProductViewDataToEntityMapper
import org.koin.core.module.Module
import org.koin.dsl.module

internal const val DATABASE_NAME = "product_catalog.db"

fun productCatalogModules() = listOf(
    productCatalogDataModule,
    productCatalogDomainModule,
    productCatalogPresentationModule,
    productCatalogDatabaseModule()
)

val productCatalogDataModule = module {
    // Mappers
    factory { ProductMapper() }
    factory { FavoriteProductEntityMapper() }
    factory<ExceptionMapper> { DefaultExceptionMapper() }
    factory { PaginatedProductsMapper(productMapper = get()) }

    // Data Sources
    factory<ProductRemoteDataSource> {
        ProductRemoteDataSourceImpl(httpClient = get())
    }
    factory<FavoriteLocalDataSource> {
        FavoriteLocalDataSourceImpl(
            database = get(),
            dispatchers = get()
        )
    }

    // Repository
    single<ProductRepository> {
        ProductRepositoryImpl(
            remoteDataSource = get(),
            localDataSource = get(),
            productMapper = get(),
            paginatedProductsMapper = get(),
            favoriteProductEntityMapper = get(),
            exceptionMapper = get(),
            dispatchers = get(),
            scopes = get()
        )
    }
}

val productCatalogDomainModule = module {
    factory { GetProductsUseCase(repository = get()) }
    factory { SearchProductsUseCase(repository = get()) }
    factory { GetProductDetailsUseCase(repository = get()) }
    factory { ToggleFavoriteUseCase(repository = get()) }
    factory { ObserveFavoritesUseCase(repository = get()) }
}

val productCatalogPresentationModule = module {
    single { ProductToViewDataMapper() }
    single { ProductViewDataToEntityMapper() }

    factory { ProductCatalogHomeViewModel() }

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

    factory {
        ProductFavoritesViewModel(
            navigator = get(),
            observeFavoritesUseCase = get(),
            viewDataMapper = get()
        )
    }
}

expect fun productCatalogDatabaseModule(): Module