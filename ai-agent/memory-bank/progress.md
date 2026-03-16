# Progress

## Current Status
✅ `product-catalog` feature complete. 🔄 `login` feature — Android UI, shared ViewModel, and biometric platform implementations done; PIN persistence, DI, and iOS pending.

## Completed
- **Project scaffolding**: Gradle multi-module setup with version catalog, settings, and build files for all modules.
- **Core modules**: `common`, `domain`, `data`, `network`, `navigation`, `presentation`, `di` — all implemented and wired.
- **product-catalog domain**: `Product` model, `ProductRepository` interface, all 5 use cases.
- **product-catalog data**: Ktor remote data source, SQLDelight local data source (favourites), `ProductRepositoryImpl` with caching + single-flight.
- **product-catalog presentation**: 4 ViewModels, state/event sealed classes, view data mapper, navigation contract.
- **product-catalog di**: Koin modules for all layers including platform-specific database module.
- **Android app** (`composeApp`): Compose UI with product list, search, details, favourites screens; Koin initialisation.
- **iOS app** (`iosApp`): SwiftUI `ProductCatalogView`; `ObservableViewModel` base; `KoinInitializer` via `iosAppFramework`.
- **iOS framework** (`iosAppFramework`): Static framework exporting shared code + SKIE bridging.
- **Unit tests**: Domain use cases, repository, ViewModels, mappers, cache, single-flight command.
- **login:presentation (shared)**: `LoginState`, `LoginEvent`, `LoginViewModel`, `LoginNavigationRoutes`, `LoginNavigationTargets`.
- **login Android UI**: `LoginScreen` (all states), `PinDisplay`, `PinKeyboard` — portrait and landscape layouts.
- **MainActivity**: Extended `AppCompatActivity`; login as start destination; `ExitToProductCatalog` mapped to catalog in `NavHost`; `BiometricsActivityProvider` wired via `by inject()`.
- **core:biometrics**: `BiometricAuthenticator` interface, `BiometricAuthenticationException`, `BiometricAuthenticationFailureReason` in `commonMain`; `BiometricAuthenticatorImpl` + `BiometricsActivityProvider` in `androidMain`; `BiometricAuthenticatorImpl` in `iosMain`.
- **core:di**: `coreBiometricsModule` added as `expect`/`actual`; wired into `coreModules()`.

## Pending
- PIN persistence (DataStore / EncryptedSharedPrefs).
- iOS SwiftUI login screen.
- Navigation improvements (tab state preservation, String-based routes for iOS).
- Second feature module to validate multi-feature scalability.
- End-to-end / integration tests.
- CI/CD pipeline configuration.

## Known Issues
- PIN storage not yet implemented — `pinAlreadyCreated` and `biometricEnabled` are hardcoded `false`.
- List state not preserved when navigating between tabs.
- `NavigationRoute` uses Kotlin Serialization — not compatible with iOS navigation without adaptation.
- UI may contain hardcoded values (AI-generated, only manually tested).