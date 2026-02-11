import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.mokoResources)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    sourceSets {
        androidMain.dependencies {
            // Core modules
            implementation(projects.shared.core.navigation)
            implementation(projects.shared.core.di)

            // Feature modules
            implementation(projects.shared.feature.productCatalog.presentation)
            implementation(projects.shared.feature.productCatalog.di)

            // Compose
            implementation(libs.compose.ui)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.material3)
            implementation(libs.androidx.activity.compose)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.components.resources)

            // Koin
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)

            // AndroidX
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.navigation.runtime.ktx)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.compose.material.icons.extended)

            // Coil
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            // moko-resources
            implementation(libs.moko.resources.core)
            implementation(libs.moko.resources.compose)
        }

        commonMain.dependencies {
            // Common dependencies if needed
            implementation(libs.compose.components.resources)
        }
    }
}

android {
    namespace = "com.yakivmospan.templates"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.yakivmospan.templates"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}