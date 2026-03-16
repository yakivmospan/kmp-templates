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

### `:shared:core:presentation`
Base `ViewModel<Event, State, SideEffect>` class extending `androidx.lifecycle.ViewModel`. Provides:
- `state: StateFlow<State>` — UI state
- `sideEffects: SharedFlow<SideEffect>` — one-shot effects (navigation, toasts, etc.)
- `emitSideEffect(effect)` — protected helper for subclasses
- `ViewModelEventReceiver<Event>` interface

### `:shared:core:BiometricAuthenticator` *(stubbed in login feature — to be extracted)*
Platform-agnostic biometric / device-security interface. Currently declared inside `LoginViewModel.kt` and marked with `TODO: move to :shared:core:BiometricAuthenticator`.
- `BiometricAuthenticator` interface — `isBiometricAvailable()`, `isDeviceSecured()`, `suspend authenticate(): Result<Unit>`, `openBiometricSettings()`
- `BiometricResult` — `Result<Unit>` alias
- `MockBiometricAuthenticator` — mock implementation for development
- Platform implementations: `BiometricManager + BiometricPrompt` on Android, `LAContext` on iOS
- Wired via Koin; ViewModel depends on interface only

### `:shared:core:di`
Koin module that wires core services: navigator, HTTP client, dispatcher provider, coroutine scope provider.

## Feature Modules

### `product-catalog` (reference implementation)

#### `:shared:feature:product-catalog:domain`
- Domain model: `Product`
- Repository interface: `ProductRepository`
- Use cases: `GetProductsUseCase`, `GetProductDetailsUseCase`, `SearchProductsUseCase`, `ToggleFavoriteUseCase`, `ObserveFavoritesUseCase`

#### `:shared:feature:product-catalog:data`
- Remote: `ProductRemoteDataSource` / `ProductRemoteDataSourceImpl` (Ktor)
- Local: `FavoriteLocalDataSource` / `FavoriteLocalDataSourceImpl` (SQLDelight)
- Repository: `ProductRepositoryImpl` — in-memory keyed cache (30 s TTL) + single-flight deduplication
- Mappers: `ProductMapper`, `PaginatedProductsMapper`, `FavoriteProductEntityMapper`

#### `:shared:feature:product-catalog:presentation`
- ViewModels: `ProductCatalogHomeViewModel`, `ProductCatalogViewModel`, `ProductDetailsViewModel`, `ProductFavoritesViewModel`
- State: `ProductCatalogState`, `ProductFavoritesState`
- Events (sealed interfaces): `ProductCatalogHomeEvent`, `ProductDetailsEvent`, `ProductFavoritesEvent`
- View data: `ProductViewData`; mappers: `ProductToViewDataMapper`, `ProductViewDataToEntityMapper`
- Navigation: `ProductCatalogNavigation`; Config: `ProductCatalogConfig`

#### `:shared:feature:product-catalog:di`
Koin modules: `productCatalogDataModule`, `productCatalogDomainModule`, `productCatalogPresentationModule`, `productCatalogDatabaseModule`.

### `login` (in progress)

#### `:shared:feature:login:presentation`
- `LoginState` — sealed class: `Loading`, `CreatePin`, `ConfirmPin`, `BiometricSetupCheck`, `BiometricNotEnrolled`, `LoginWithPin`, `LoginWithBiometric`
- `LoginEvent` — sealed class covering keypad, create-pin flow, biometric setup, and login flow interactions
- `LoginViewModel` — depends on `BiometricAuthenticator` (stubbed) and `Navigator`; no side effects (Navigator + BiometricAuthenticator called directly)
- `LoginNavigationRoutes` — `Login` (entry), `ExitToProductCatalog` (generic exit; mapped to real catalog route in app module)
- `LoginNavigationTargets` — `ToProductCatalog` clears back stack including `Login`

#### `composeApp` — Android UI (`:composeApp`)
- `LoginScreen` — two-overload pattern (public ViewModel-connected, private stateless)
- `PinDisplay` — animated dot row; `PinKeyboard` — 3×4 grid with portrait/landscape sizing
- Landscape layout: title/dots left, keyboard right, both centred
- `PinKeyboard` uses `BoxWithConstraints` to compute key size from available width or height (`constrainToHeight` flag)
- `MainActivity` updated: `startDestination = LoginNavigationRoutes.Login`; `ExitToProductCatalog` mapped to `ProductCatalogHomeScreen` in `NavHost`

## Platform App Modules

### `:composeApp` (Android)
Jetpack Compose UI. `App` (Application) initialises Koin. Single `AppActivity`. Start destination is now the login screen.

### `iosApp` (iOS)
SwiftUI UI. `KoinInitializer` bootstraps Koin from `iosAppFramework`. `ObservableViewModel` bridges Kotlin `ViewModel` + SKIE StateFlow to `@ObservableObject`.

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
    │   ├── presentation/
    │   ├── biometrics/    # stubbed inside login for now
    │   └── di/
    └── feature/
        ├── product-catalog/
        │   ├── domain/
        │   ├── data/
        │   ├── presentation/
        │   └── di/
        └── login/
            └── presentation/ # domain/data/di not needed yet
```

## Key Patterns
- **Repository pattern** with `UpdateStrategy` (ALWAYS_FETCH / ALWAYS_CACHED / TRY_FETCH_ELSE_CACHED / TRY_CACHED_ELSE_FETCH)
- **Single-flight** deduplication for concurrent identical network requests
- **In-memory cache** with TTL expiry backed by `MutableStateFlow`
- **ViewModel events** (sealed class) — UI sends events, ViewModel updates state; simplifies Swift interop
- **ViewModel side effects** (`SharedFlow<SideEffect>`) for one-shot effects — collected by UI once
- **No side effects when Navigator / BiometricAuthenticator handle the action directly** — ViewModel calls them as regular dependencies
- **Cross-feature navigation** — feature emits a generic `ExitTo*` route; app module maps it to the real destination in `NavHost`
- **BiometricAuthenticator** — suspend function pattern (`authenticate(): Result<Unit>`); no SharedFlow/callback needed
- **SKIE** for seamless Kotlin Flow to Swift async/await bridging
- **Moko Resources** for shared string resources across platforms (`MR.strings.*`)