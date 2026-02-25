# Progress

## Current Status
✅ Template is feature-complete for the initial `product-catalog` reference implementation. Both Android and iOS apps build and run.

## Completed
- **Project scaffolding**: Gradle multi-module setup with version catalog, settings, and build files for all modules.
- **Core modules**: `common`, `domain`, `data`, `network`, `navigation`, `di` — all implemented and wired.
- **product-catalog domain**: `Product` model, `ProductRepository` interface, all 5 use cases.
- **product-catalog data**: Ktor remote data source, SQLDelight local data source (favourites), `ProductRepositoryImpl` with caching + single-flight.
- **product-catalog presentation**: 4 ViewModels, state/event sealed classes, view data mapper, navigation contract.
- **product-catalog di**: Koin modules for all layers including platform-specific database module.
- **Android app** (`composeApp`): Compose UI with product list, search, details, favourites screens; Koin initialisation.
- **iOS app** (`iosApp`): SwiftUI `ProductCatalogView`; `ObservableViewModel` base; `KoinInitializer` via `iosAppFramework`.
- **iOS framework** (`iosAppFramework`): Static framework exporting shared code + SKIE bridging.
- **Unit tests**: Domain use cases, repository, ViewModels, mappers, cache, single-flight command.

## In Progress
- Navigation improvements (tab state preservation, String-based routes for iOS).

## Pending
- Second feature module to validate multi-feature scalability.
- Core UI shared module (if needed).
- End-to-end / integration tests.
- CI/CD pipeline configuration.

## Known Issues
- List state not preserved when navigating between tabs.
- `NavigationRoute` uses Kotlin Serialization — not compatible with iOS navigation without adaptation.
- UI may contain hardcoded values (AI-generated, only manually tested).
- All screens share a single `ProductViewData` object (details screen shows same data as list — simplification).

