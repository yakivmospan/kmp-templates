import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeCompiler)
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
            implementation(projects.shared.core.presentation)
            implementation(projects.shared.core.navigation)
            implementation(projects.shared.core.di)

            // Feature modules
            implementation(projects.shared.feature.productCatalog.presentation)
            implementation(projects.shared.feature.productCatalog.di)

            // Login modules
            implementation(projects.shared.feature.login.presentation)
            implementation(projects.shared.feature.login.di)

            // AndroidX Compose BOM
            implementation(project.dependencies.platform(libs.androidx.compose.bom))
            implementation(libs.androidx.compose.ui)
            implementation(libs.androidx.compose.ui.tooling.preview)
            implementation(libs.androidx.compose.material3)
            implementation(libs.androidx.compose.foundation)
            implementation(libs.androidx.compose.runtime)

            // Activity Compose
            implementation(libs.androidx.activity.compose)

            // Koin
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)

            // AndroidX
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.appcompat)
            implementation(libs.androidx.navigation.runtime.ktx)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.compose.material.icons.extended)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)

            // KotlinX
            implementation(libs.kotlinx.collections.immutable)

            // Coil
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            // moko-resources
            implementation(libs.moko.resources.core)
            implementation(libs.moko.resources.compose)
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

    dependencies {
        debugImplementation(libs.androidx.compose.ui.tooling)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}