import Combine
import Shared

// MARK: - Base Observable ViewModel
// Generic bridge between any KMP ViewModel and SwiftUI.
// VM  = KMP ViewModel type
// State = KMP State type (StateFlow<State>)
// Event = KMP Event type (sealed class)
@MainActor
class ObservableViewModel<VM: AnyObject, State: AnyObject, Event: AnyObject>: ObservableObject {

    let viewModel: VM

    @Published private(set) var state: State

    private let eventSender: (Event) -> Void

    init(
        viewModel: VM,
        stateFlow: SkieSwiftStateFlow<State>,
        eventSender: @escaping (Event) -> Void
    ) {
        self.viewModel = viewModel
        self.state = stateFlow.value
        self.eventSender = eventSender

        Task { [weak self] in
            for await newState in stateFlow {
                self?.state = newState
            }
        }
    }

    func send(_ event: Event) {
        eventSender(event)
    }
}
