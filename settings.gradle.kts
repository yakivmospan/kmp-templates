rootProject.name = "CleanArhitectureTemplate"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

// Android App
include(":composeApp")

// iOS App Shared Framework
include(":iosAppFramework")

// Core Modules
include(":shared:core:biometrics")
include(":shared:core:common")
include(":shared:core:testing")
include(":shared:core:domain")
include(":shared:core:data")
include(":shared:core:presentation")
include(":shared:core:network")
include(":shared:core:navigation")
include(":shared:core:di")

// Login Feature Modules
include(":shared:feature:login:domain")
include(":shared:feature:login:data")
include(":shared:feature:login:presentation")
include(":shared:feature:login:di")

// Product Catalog Feature Modules
include(":shared:feature:product-catalog:domain")
include(":shared:feature:product-catalog:data")
include(":shared:feature:product-catalog:presentation")
include(":shared:feature:product-catalog:di")