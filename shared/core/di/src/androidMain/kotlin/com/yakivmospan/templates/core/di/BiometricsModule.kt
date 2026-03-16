package com.yakivmospan.templates.core.di

import com.yakivmospan.templates.core.biometrics.BiometricAuthenticator
import com.yakivmospan.templates.core.biometrics.AndroidBiometricAuthenticatorImpl
import com.yakivmospan.templates.core.biometrics.BiometricsActivityProvider
import com.yakivmospan.templates.core.biometrics.DefaultBiometricsActivityProvider
import org.koin.dsl.module

actual val coreBiometricsModule = module {
    single<BiometricsActivityProvider> { DefaultBiometricsActivityProvider() }
    single<BiometricAuthenticator> { AndroidBiometricAuthenticatorImpl(activityProvider = get()) }
}