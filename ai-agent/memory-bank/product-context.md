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
