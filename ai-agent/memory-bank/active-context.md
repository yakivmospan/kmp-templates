# Active Context

## Current Focus
Template is in a stable, working state. The `product-catalog` feature is fully implemented end-to-end on both Android and iOS and serves as the reference implementation for adding new features.

## Recent Decisions
- Used `androidx.lifecycle.ViewModel` (not a custom base class) so ViewModels work natively on Android and can be consumed from iOS via SKIE.
- Navigation contracts (`Navigator`, `NavigationRoute`) are cross-platform Kotlin interfaces; concrete routing is handled per-platform in the app modules.
- `iosAppFramework` module was introduced to encapsulate Koin initialisation and the static framework export for iOS, keeping `iosApp` (Swift) clean.
- `UpdateStrategy` enum on `ProductRepository` centralises cache/fetch decision logic so callers are not aware of caching internals.
- ViewModel events (sealed interface) pattern chosen over direct ViewModel methods to reduce Swift adapter boilerplate.

## Open Questions
- `NavigationRoute` should be reimplemented with String-based routes (instead of Kotlin Serialization) to properly support iOS navigation.
- Tab state is not preserved on navigation — `FlowNavigator` / `NavigatorCommandsFlow` needs a backstack-aware update.
- UI modularisation (separate UI modules per feature) is not done; assess whether it is needed for this template.
- Moko Resources version (0.26.0) combined with SKIE (0.10.10) may have compatibility constraints to watch.

## Next Steps
- Improve iOS navigation to support proper backstack and tab state preservation.
- Replace Kotlin Serialization–based `NavigationRoute` with a String-route approach for iOS compatibility.
- Consider adding a `core:ui` shared module for Compose Multiplatform UI utilities if targeting non-Android platforms with Compose.