plugins {
    kotlin("multiplatform")
    alias(libs.plugins.mokoResources)
    alias(libs.plugins.skie)
}

kotlin {
    val iosTargets = listOf(iosX64(), iosArm64(), iosSimulatorArm64())

    iosTargets.forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = false // Consider making static? It will be required additional setup in xCode for tools like linkerOpts and moko res.
            linkerOpts.add("-lsqlite3")

            export(projects.shared.core.navigation)
            export(projects.shared.feature.productCatalog.presentation)

            export(libs.moko.resources.core)
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Core modules
            api(projects.shared.core.navigation)
            implementation(projects.shared.core.di)

            // Feature modules
            api(projects.shared.feature.productCatalog.presentation)
            implementation(projects.shared.feature.productCatalog.di)

            // Koin
            implementation(libs.koin.core)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
