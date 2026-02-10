package com.yakivmospan.templates.core.di

import com.yakivmospan.templates.core.common.DispatcherProvider
import com.yakivmospan.templates.core.common.defaultDispatcherProvider
import com.yakivmospan.templates.core.network.defaultHttpClientFactoryProvider
import com.yakivmospan.templates.core.storage.defaultStorageFactoryProvider
import org.koin.dsl.module

val coreModule = module {
    single { defaultHttpClientFactoryProvider().create() }
    single { defaultStorageFactoryProvider() }
    single<DispatcherProvider> { defaultDispatcherProvider() }
}