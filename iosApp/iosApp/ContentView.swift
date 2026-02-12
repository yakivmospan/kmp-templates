import SwiftUI
import Shared

struct ContentView: View {
    var body: some View {
        ProductCatalogView(
            viewModel: KoinHelper().getProductCatalogViewModel()
        )
    }
}
struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
