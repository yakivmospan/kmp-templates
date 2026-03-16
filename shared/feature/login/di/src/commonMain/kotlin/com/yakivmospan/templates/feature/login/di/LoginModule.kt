package com.yakivmospan.templates.feature.login.di

import com.yakivmospan.templates.feature.login.presentation.login.LoginViewModel
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
    factory {
        LoginViewModel(
            navigator = get(),
            biometricAuthenticator = get()
        )
    }
}
