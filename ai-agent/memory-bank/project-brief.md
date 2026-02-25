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
