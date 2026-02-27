# Project Brief

## Project Name
KMP Templates — Kotlin Multiplatform Clean Architecture Template

## Project Goal
Provide a production-ready, modularized Kotlin Multiplatform template targeting Android and iOS that demonstrates Clean Architecture principles, strict layer separation, and a scalable multi-feature module structure.

## Core Requirements
- Kotlin Multiplatform targeting Android and iOS
- Clean Architecture with strict dependency rules enforced at module level
- Multi-module setup: core modules + per-feature modules (domain / data / presentation / di)
- Shared business logic (domain models, use cases, ViewModels) across platforms
- Native UI: Jetpack Compose on Android, SwiftUI on iOS
- Scalable to 10+ features without architectural degradation
- Dependency Injection via Koin (shared + platform-specific modules)
- Offline support via SQLDelight local database
- Remote API access via Ktor HTTP client
- Unit-tested domain, data, and presentation layers (MockK + kotlin-test)

## Coding Style
- Kotlin idiomatic code with coroutines and `Flow`
- Interfaces for all major abstractions (repositories, use cases, mappers, caches, data sources)
- `expect`/`actual` for platform-specific implementations (dispatchers, HTTP engines)
- Mapper objects for cross-layer model transformation
- ViewModel events pattern (sealed interfaces) — UI sends events, ViewModel updates state

## Timeline
Production-quality reference template. No fixed deadline. Focused on correctness, maintainability, and serving as a reference implementation for new KMP projects.

# Product Context

## Purpose
A reference / starter template for Kotlin Multiplatform (KMP) apps using Clean Architecture. Designed to be cloned and extended for real production apps that share business logic between Android and iOS while keeping fully native UIs on each platform.

## Target Users
- Mobile engineers building new KMP projects who want a pre-structured, scalable foundation.
- Teams migrating existing Android or iOS apps to KMP.
- Developers learning best-practice KMP architecture through a working, end-to-end example.

## Problem Statement
Starting a KMP project from scratch involves a large number of architectural decisions: module boundaries, layer separation, DI setup, platform interop, shared navigation, caching strategy, error handling, and more. Without a solid template, teams often end up with tight coupling, poor testability, and difficulty scaling beyond a few features.

## User Experience
- **Android**: Native Jetpack Compose UI with tab-based navigation. Product catalog feature with list, search, details, and favourites screens.
- **iOS**: Native SwiftUI UI consuming shared ViewModels via SKIE-bridged StateFlows. Same feature set mirrored from Android.
- Both platforms share: domain models, repository interfaces, use cases, ViewModels (`androidx.lifecycle.ViewModel`), and navigation contracts.
- A single **product-catalog** feature is implemented end-to-end as a concrete example of how to build and wire a complete feature.

## Known Trade-offs
- UI was mostly AI-generated and only manually tested; may contain hardcoded values.
- All list and detail screens share one `ProductViewData` object (not ideal but kept simple for speed).
- No UI module separation (kept focus on shared architecture, not UI modularization).
- List state is not preserved when navigating between tabs — requires Navigator improvements.
- Overall state handling with navigation needs further improvements.

# Architecture

## Overview
Clean Architecture with strict unidirectional dependency flow, applied to a multi-module Kotlin Multiplatform project.

```
Apps (composeApp / iosApp)
  └─▶ Feature DI  (wires all layers)
        ├─▶ Feature Presentation  (ViewModels, UI state, events)
        │     └─▶ Feature Domain  (use cases, repository interfaces, domain models)
        ├─▶ Feature Data          (repository impls, data sources, mappers)
        │     └─▶ Feature Domain
        └─▶ Core modules
```

## Dependency Rules
1. **Core modules** — foundational, no feature dependencies.
2. **Feature domain** — depends only on `core:common` and `core:domain`.
3. **Feature data** — depends on `core:network`, `core:data`, `core:common` + own domain.
4. **Feature presentation** — depends only on own domain and `core:common`, `core:navigation`. NEVER on data.
5. **Feature DI** — knows all layers; wires them together with Koin.
6. **Apps** — depend on feature presentation + DI modules (or all feature modules with custom DI).

## Core Modules

### `:shared:core:common`
Utilities shared across all layers: `DispatcherProvider`, `CoroutineScopeProvider`, `Result<T>`, `Mapper`, `Pagination` models. Expect/actual for platform dispatchers.

### `:shared:core:domain`
Base abstractions for the domain layer: `UseCase<P,R>`, `DomainException`, `UpdateStrategy`.

### `:shared:core:data`
Reusable data-layer utilities:
- `Cache<T>` / `InMemoryCache<T>` with optional TTL via `TimestampExpirationValidator`
- `KeyedCache<K,V>` / `InMemoryKeyedCache<K,V>`
- `SingleFlightCommand` / `SingleFlightUnitCommand` — deduplicates concurrent identical requests
- `ExceptionMapper` — maps network exceptions to `DomainException`

### `:shared:core:network`
Ktor HTTP client setup: `HttpClientFactory` (expect/actual for Android OkHttp / iOS Darwin engines), `ApiResponse<T>`, `ApiException`, `NetworkErrorHandler`.

### `:shared:core:navigation`
Cross-platform navigation contracts: `Navigator`, `NavigationTarget`, `NavigationRoute`, `NavigationResult`, `FlowNavigator` (SharedFlow-based implementation).

### `:shared:core:di`
Koin module that wires core services: navigator, HTTP client, dispatcher provider, coroutine scope provider.

## Feature Modules (example: `product-catalog`)

### `:shared:feature:product-catalog:domain`
- Domain model: `Product`
- Repository interface: `ProductRepository`
- Use cases: `GetProductsUseCase`, `GetProductDetailsUseCase`, `SearchProductsUseCase`, `ToggleFavoriteUseCase`, `ObserveFavoritesUseCase`

### `:shared:feature:product-catalog:data`
- Remote: `ProductRemoteDataSource` / `ProductRemoteDataSourceImpl` (Ktor)
- Local: `FavoriteLocalDataSource` / `FavoriteLocalDataSourceImpl` (SQLDelight)
- Repository: `ProductRepositoryImpl` — in-memory keyed cache (30 s TTL) + single-flight deduplication
- Mappers: `ProductMapper`, `PaginatedProductsMapper`, `FavoriteProductEntityMapper`

### `:shared:feature:product-catalog:presentation`
- ViewModels: `ProductCatalogHomeViewModel`, `ProductCatalogViewModel`, `ProductDetailsViewModel`, `ProductFavoritesViewModel`
- State: `ProductCatalogState`, `ProductFavoritesState`
- Events (sealed interfaces): `ProductCatalogHomeEvent`, `ProductDetailsEvent`, `ProductFavoritesEvent`
- View data: `ProductViewData`; mappers: `ProductToViewDataMapper`, `ProductViewDataToEntityMapper`
- Navigation: `ProductCatalogNavigation`; Config: `ProductCatalogConfig`

### `:shared:feature:product-catalog:di`
Koin modules: `productCatalogDataModule`, `productCatalogDomainModule`, `productCatalogPresentationModule`, `productCatalogDatabaseModule`.

## Platform App Modules

### `:composeApp` (Android)
Jetpack Compose UI. `App` (Application) initialises Koin with `coreModules() + productCatalogModules()`. Single `AppActivity`. Screens: product list, search, details, favourites.

### `iosApp` (iOS)
SwiftUI UI. `KoinInitializer` bootstraps Koin from `iosAppFramework`. `ObservableViewModel` bridges Kotlin `ViewModel` + SKIE StateFlow to `@ObservableObject`. `ProductCatalogView` demonstrates ViewModel usage.

### `:iosAppFramework`
Kotlin/Native static framework. Sets up the Koin module graph and exposes it to Swift via SKIE.

## Project Structure
```
kmp-templates/
├── composeApp/               # Android app (Compose UI)
├── iosApp/                   # iOS app (SwiftUI)
├── iosAppFramework/          # Kotlin/Native framework for iOS
└── shared/
    ├── core/
    │   ├── common/
    │   ├── domain/
    │   ├── data/
    │   ├── network/
    │   ├── navigation/
    │   └── di/
    └── feature/
        └── product-catalog/
            ├── domain/
            ├── data/
            ├── presentation/
            └── di/
```

## Key Patterns
- **Repository pattern** with `UpdateStrategy` (ALWAYS_FETCH / ALWAYS_CACHED / TRY_FETCH_ELSE_CACHED / TRY_CACHED_ELSE_FETCH)
- **Single-flight** deduplication for concurrent identical network requests
- **In-memory cache** with TTL expiry backed by `MutableStateFlow`
- **ViewModel events** (sealed interface) instead of direct method calls — simplifies Swift interop
- **SKIE** for seamless Kotlin Flow to Swift async/await bridging
- **Moko Resources** for shared string resources across platforms

# Tech Context

## Primary Language
Kotlin (Multiplatform) — shared code; Swift — iOS-only UI code.

## Platforms / Targets
- Android (minSdk 24, compileSdk 36, targetSdk 36)
- iOS (iosX64, iosArm64, iosSimulatorArm64)

## Frameworks & Runtimes
- **Kotlin Multiplatform** (Kotlin 2.3.10) — code sharing
- **Jetpack Compose / Compose Multiplatform** (1.10.1) — Android UI
- **SwiftUI** — iOS UI
- **AndroidX Lifecycle ViewModel** (2.10.0) — shared ViewModels

## Key Libraries

| Library | Version | Purpose |
|---|---|---|
| Koin | 4.1.1 | Dependency injection (shared + Android + iOS) |
| Ktor | 3.4.0 | HTTP client (OkHttp on Android, Darwin on iOS) |
| kotlinx.coroutines | 1.10.2 | Async / concurrency |
| kotlinx.serialization | 1.10.0 | JSON serialization for network DTOs |
| SQLDelight | 2.2.1 | Multiplatform local database |
| SKIE | 0.10.10 | Kotlin Flow → Swift async/await bridging |
| Moko Resources | 0.26.0 | Shared string resources across platforms |
| Coil | 3.3.0 | Image loading (Compose + Ktor network fetcher) |
| Kermit | 2.0.8 | Multiplatform logging |
| MockK | 1.14.9 | Mocking in unit tests |
| AndroidX Navigation | 2.9.7 | Android Compose navigation |

## Database / Storage
- **SQLDelight 2.2.1** — generates type-safe Kotlin API from `.sq` files; Android driver + Native driver for iOS.
- In-memory caches (`InMemoryCache`, `InMemoryKeyedCache`) with configurable TTL for short-lived data.

## Network
- **Ktor** HTTP client with `ContentNegotiation` (kotlinx.serialization JSON) and `Logging` plugins.
- `expect`/`actual` for engine selection: `OkHttpEngine` on Android, `DarwinEngine` on iOS.
- `NetworkErrorHandler` + `ApiException` → `DomainException` mapping pipeline.

## Build Tools
- **Gradle** with Kotlin DSL (`.kts`) and version catalog (`gradle/libs.versions.toml`)
- **AGP** 8.11.2 — Android Gradle Plugin
- **Kotlin Multiplatform Gradle plugin** 2.3.10
- **SKIE Gradle plugin** 0.10.10 — post-processes the Kotlin/Native framework for Swift interop
- **SQLDelight Gradle plugin** 2.2.1 — generates database code
- **Moko Resources Gradle plugin** 0.26.0 — generates resource accessors
- JVM target: Java 11

## Development Setup
1. Android Studio or IntelliJ IDEA with KMP plugin.
2. Xcode required for iOS builds; `iosAppFramework` exports a static `.framework` consumed by `iosApp.xcodeproj`.
3. Run Android: `./gradlew :composeApp:assembleDebug` or via IDE run config.
4. Run iOS: open `iosApp/iosApp.xcodeproj` in Xcode and run, or use IDE run config.
5. Run unit tests: `./gradlew test` (runs `androidUnitTest` source sets).

## Testing Approach
- Test source set: `androidUnitTest` (JVM-based, runs on host machine).
- Frameworks: `kotlin-test`, `kotlinx.coroutines.test`, `MockK`.
- Test fixtures and fake implementations co-located with tests.
- Coverage areas: use cases, repository logic, ViewModels, mappers, caches, single-flight commands.
