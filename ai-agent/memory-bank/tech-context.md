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

