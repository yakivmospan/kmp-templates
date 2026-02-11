# Kotlin Multiplatform App - Clean Architecture

This is a Kotlin Multiplatform project targeting Android, iOS.

* [/composeApp](./composeApp/src) contain Android application code, including UI implemented with Compose.
  This is where you should add your Android-specific code and resources.

* [/iosApp](./iosApp/iosApp) contains iOS applications. This is entry point for your iOS app.
  This is also where you should add SwiftUI code for your project.

* [/iosAppFramework](./iosAppFramework) module that is designed to setup `Shared` static library dependencies for `iosApp`.

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
- SQLDelight for database
- MockK and kotlin test for unit testing

## iOS Considerations

- Koin allows easily create specific iOS modules if needed.
- Used Moko to share resources between both platforms (it is v0.26.0.. maybe for prod we would add our own interface and
  implementations.)
- Added general navigation interface, to be later implemented in iOS
- ViewModels uses events to communicate with UI, not methods - this make it easier to call them from SwiftUI, less adapter code is
  needed, less error prone.
- Created `iosAppFramework` module to setup `Shared` framework for iOS, this is where we can create the Koin module and initialize
  it, so that it can be used in the iOS app. Had not time to setup it further.

## iOS Integration Example:

```
import SwiftUI
import Shared

struct ProductCatalogView: View {
@StateObject private var observableState: ObservableProductCatalogState

    var body: some View {
        VStack {
            TextField("Search", text: $observableState.searchQuery)
            
            if observableState.state.isSearching {
                ProgressView()
            }
            
            List(observableState.state.products, id: \.id) { product in
                Text(product.title)
            }
        }
    }
}

// Helper to bridge Flow to SwiftUI
@MainActor
class ObservableProductCatalogState: ObservableObject {
private let viewModel: ProductCatalogViewModel

    @Published var state: ProductCatalogState
    @Published var searchQuery: String = ""
    
    init(viewModel: ProductCatalogViewModel) {
        self.viewModel = viewModel
        self.state = viewModel.state.value
        
        // Observe state
        viewModel.state.watch { [weak self] newState in
            self?.state = newState
        }
        
        // Observe search query
        viewModel.searchQuery.watch { [weak self] query in
            self?.searchQuery = query
        }
    }
    
    func send(_ event: ProductCatalogEvent) {
        viewModel.onEvent(event: event)
    }
}
```

# Trade-offs

- UI is mostly done with AI and its code was not properly reviewed, only manually tested and iterated over.
- UI may have hardcoded values.
- Price formatting is done on the UI layer.
- All UI items share one ViewData object, so Details screen shows the same data as the List screen, which is not ideal but was
  done for simplicity and speed of development.
- There is no UI Module separation, im not sure if you are doing it in your production app, but for this sample i wanted to keep
  it simple, focusing more on shared architecture and code sharing, rather than on UI modularization.
- List state is not preserved when navigating between tabs - this will require to update Navigator and think about iOS handling of
  it.
- Overall state handling with navigation is not ideal and requires time for improvements.
- `ViewModelTest`, `UseCaseTest` and `TestDispatcherProvider` should be moved to separate common test module
- Favorite button can be added directly on the list items on Catalog and Favorites screens, but for simplicity it was added only
  on the Details scree.
- No documentation was added to view models, in real life it would be good to have a solid contract for each view model,
  describing what it does, what are its inputs and outputs, and how it should be used.
- Not all view models have tests. Added one to show the approach.
- Not all mappers have tests, added UI mappers only.
- Search states can be improved, again UI is a black box here.
- SQL exceptions are not handled in Default Domain ExceptionMapper
- Database is created per feature. If required can be moved to a core module like we have with network. It has its own advantages
  and disadvantages.
- No database migration was designed.
- NO ASC, DESC implemented, even though PageRequest has it. No sorting is implemented at all, but it can be easily added in the
  future.