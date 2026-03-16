# Active Context

## Current Focus
`login` feature — platform `BiometricAuthenticator` implementations complete. Next: `login:di` Koin module, PIN persistence, iOS SwiftUI login screen.

## Recent Decisions
- `BiometricAuthenticator` extracted to `:shared:core:biometrics` — interface in `commonMain`, platform impls in `androidMain` / `iosMain`.
- `BiometricAuthenticationException` lives in `commonMain` with `BiometricAuthenticationFailureReason` enum — platform error codes mapped to it in each impl.
- Android impl uses `androidx.biometric:biometric` (stable 1.1.0) with `BiometricPrompt(FragmentActivity, executor, callback)` — `biometric-ktx` dropped (auth API requires alpha).
- `MainActivity` changed to extend `AppCompatActivity` (extends `FragmentActivity`) to satisfy `BiometricPrompt` constructor.
- `BiometricsActivityProvider` / `DefaultBiometricsActivityProvider` in `androidMain` — mirrors `FlowNavigator` pattern; `MainActivity` calls `setActivity(this)` in `onCreate`, `setActivity(null)` in `onDestroy`, injected via Koin `by inject()` delegate.
- `coreBiometricsModule` is `expect`/`actual` — Android binds `BiometricsActivityProvider` + `BiometricAuthenticatorImpl(activityProvider)`; iOS binds `BiometricAuthenticatorImpl()` only.
- iOS impl uses `LAContext` per call — fresh instance each `authenticate()` invocation to avoid cached auth result; `@file:OptIn(ExperimentalForeignApi::class)` required.
- `openBiometricSettings` on Android guards `Settings.ACTION_BIOMETRIC_ENROLL` behind `Build.VERSION.SDK_INT >= R`; falls back to `ACTION_SECURITY_SETTINGS` on API 24–29.
- Mock stub class removed from `LoginViewModel.kt` — real interface imported from `com.yakivmospan.templates.core.biometrics`.
- `BiometricCheckboxSpacer` replaced with plain `Spacer(Modifier.height(48.dp))` — invisible empty checkbox was wasteful.
- `checkBiometricEnrollment` in `LoginViewModel` now calls `isBiometricAvailable()` (not `isDeviceSecured()`) to trigger prompt only when biometrics are enrolled.
- Login feature uses no `SideEffect` — `Navigator` and `BiometricAuthenticator` called directly as dependencies.
- Cross-feature navigation: `ExitToProductCatalog` route in `login:presentation`; app module maps it to catalog in `NavHost`.
- `LoginNavigationTargets.ToProductCatalog` clears back stack including `Login`.
- Moko Resources for all strings (`MR.strings.*`, import from feature presentation package).

## Open Questions
- `NavigationRoute` should be reimplemented with String-based routes to support iOS navigation.
- Tab state not preserved on navigation — `FlowNavigator` / `NavigatorCommandsFlow` needs backstack-aware update.
- Login data persistence (PIN hash, biometric enabled flag) not yet implemented.
- iOS SwiftUI login screen not yet implemented.

## Next Steps
- Implement PIN persistence (DataStore / EncryptedSharedPrefs).
- Wire login into iOS (`iosAppFramework` + SwiftUI screen).