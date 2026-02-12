import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        KoinInitializer().start()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}