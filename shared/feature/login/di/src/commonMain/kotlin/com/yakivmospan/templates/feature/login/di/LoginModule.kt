package com.yakivmospan.templates.feature.login.di

import com.yakivmospan.templates.feature.login.presentation.login.BiometricAuth
import com.yakivmospan.templates.feature.login.presentation.login.LoginViewModel
import com.yakivmospan.templates.feature.login.presentation.login.MockBiometricAuth
import org.koin.core.module.Module
import org.koin.dsl.module


fun loginModules() = listOf(
    loginDataModule,
    loginDomainModule,
    loginPresentationModule,
)

val loginDataModule = module {
    // Mappers

    // Data Sources

    // Repository
}

val loginDomainModule = module {
}

val loginPresentationModule = module {
    single<BiometricAuth> { MockBiometricAuth() }

    factory {
        LoginViewModel(
            navigator = get(),
            biometricAuth = get()
        )
    }
}
