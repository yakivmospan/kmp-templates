This is a Kotlin Multiplatform project targeting Android, iOS.

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
    - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
      For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
      the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
      Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
      folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for the code that will be shared between all targets in the project.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

## Architecture

Assumptions made:

1. We are building an app that is as close to our production app as possible
2. We are building a modularized app with multiple features, each feature has its own domain, data, presentation layers.
3. We have more than 10 features, so we need to ensure scalability of the architecture.
4. We want to enforce strict separation of concerns and dependencies between layers.
5. We want to have a clear dependency flow from core modules to features to apps.
6. We are moderate to large scale, so we need to manage complexity and maintainability.

Dependency rules:

1. Core modules - foundational, no feature dependencies
2. Feature domain - depends only on core (common, domain)
3. Feature data - depends on core (network, database) + own domain
4. Feature presentation - depends only on own and core domains (NOT data)
5. Feature DI - knows about all layers, wires them together
6. Apps - depend on feature presentation + DI modules, or on all feature modules + custom DI

Key architectural enforcement:

- Presentation NEVER depends on Data (only Domain)
- Domain NEVER depends on Data or Presentation
- Data implements Domain contracts
- Apps choose which features to include

Technologies used:

- Kotlin Multiplatform for shared code
- Compose for UI Android
- SwiftUI for UI iOS
- Koin for Dependency Injection
- Coroutines for asynchronous programming
- Ktor for networking
- coil for image loading
- Moko resources for cross-platform resource management
- *SQLDelight for database*?
- *MockK for testing* ?