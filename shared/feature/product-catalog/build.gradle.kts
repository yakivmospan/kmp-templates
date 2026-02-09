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
            baseName = "ProductCatalog"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.shared.feature.productCatalog.domain)
            api(projects.shared.feature.productCatalog.data)
            api(projects.shared.feature.productCatalog.presentation)
        }
    }
}

android {
    namespace = "com.yakivmospan.templates.feature.productcatalog"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}