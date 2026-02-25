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
