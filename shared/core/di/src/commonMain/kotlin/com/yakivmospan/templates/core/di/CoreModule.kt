package com.yakivmospan.templates.core.di

import com.yakivmospan.templates.core.biometrics.BiometricAuthenticator
import com.yakivmospan.templates.core.common.CoroutineScopeProvider
import com.yakivmospan.templates.core.common.DispatcherProvider
import com.yakivmospan.templates.core.common.defaultCoroutineScopeProvider
import com.yakivmospan.templates.core.common.defaultDispatcherProvider
import com.yakivmospan.templates.core.navigation.FlowNavigator
import com.yakivmospan.templates.core.navigation.Navigator
import com.yakivmospan.templates.core.navigation.NavigatorCommandsFlow
import com.yakivmospan.templates.core.navigation.NavigatorResultsFlow
import com.yakivmospan.templates.core.network.defaultHttpClientFactoryProvider
import org.koin.core.module.Module
import org.koin.dsl.module

fun coreModules() = listOf(
    coreNavigationModule,
    coreServicesModule,
    coreBiometricsModule
)

val coreNavigationModule = module {
    val flowNavigator = FlowNavigator()
    single<Navigator> { flowNavigator }
    single<NavigatorCommandsFlow> { flowNavigator }
    single<NavigatorResultsFlow> { flowNavigator }
}

val coreServicesModule = module {
    single { defaultHttpClientFactoryProvider().create() }
    single<DispatcherProvider> { defaultDispatcherProvider() }
    single<CoroutineScopeProvider> { defaultCoroutineScopeProvider(dispatchers = get()) }
}

expect val coreBiometricsModule: Module