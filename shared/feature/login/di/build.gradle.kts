import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "LoginDI"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.core.common)
            implementation(projects.shared.core.domain)
            implementation(projects.shared.core.data)
            implementation(projects.shared.core.navigation)
            implementation(projects.shared.core.biometrics)

            implementation(projects.shared.feature.login.domain)
            implementation(projects.shared.feature.login.data)
            implementation(projects.shared.feature.login.presentation)

            implementation(libs.koin.core)
        }
    }
}

android {
    namespace = "com.yakivmospan.templates.feature.login.di"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}