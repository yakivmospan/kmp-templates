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

// Core Modules
include(":shared:core:common")
include(":shared:core:domain")
include(":shared:core:data")
include(":shared:core:network")
include(":shared:core:storage")
include(":shared:core:di")

// Product Catalog Feature Modules
include(":shared:feature:product-catalog:domain")
include(":shared:feature:product-catalog:data")
include(":shared:feature:product-catalog:presentation")
include(":shared:feature:product-catalog:di")
include(":shared:feature:product-catalog") // Facade module