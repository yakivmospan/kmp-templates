package com.yakivmospan.templates.core.di

import com.yakivmospan.templates.core.biometrics.BiometricAuthenticator
import com.yakivmospan.templates.core.biometrics.IOSBiometricAuthenticatorImpl
import org.koin.dsl.module

actual val coreBiometricsModule = module {
    single<BiometricAuthenticator> { IOSBiometricAuthenticatorImpl() }
}