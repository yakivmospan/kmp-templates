import SwiftUI
import Shared

// MARK: - Observable ViewModel
class ObservableProductCatalogViewModel:
    ObservableViewModel<ProductCatalogViewModel, ProductCatalogState, ProductCatalogEvent> {
    init(viewModel: ProductCatalogViewModel) {
        super.init(
            viewModel: viewModel,
            stateFlow: viewModel.state,
            eventSender: viewModel.onEvent
        )
    }
}

// MARK: - View
struct ProductCatalogView: View {

    @StateObject private var observable: ObservableProductCatalogViewModel
    @State private var errorMessage: String? = nil

    init(viewModel: ProductCatalogViewModel) {
        _observable = StateObject(wrappedValue: ObservableProductCatalogViewModel(viewModel: viewModel))
    }

    var body: some View {
        NavigationStack {
            content
                .navigationTitle(MR.strings().pd_catalog_feature_title.desc().localized())
        }
    }

    // MARK: - Content
    @ViewBuilder
    private var content: some View {
        let state = observable.state

        if state.isLoading && state.products.isEmpty {
            fullScreenLoader
        } else {
            productList
        }
    }

    // MARK: - Product List
    private var productList: some View {
        let state = observable.state

        return List {
            searchBar

            let items = state.searchResult.isEmpty ? state.products : state.searchResult

            if state.isSearching {
                HStack {
                    Spacer()
                    ProgressView()
                    Spacer()
                }
                .listRowSeparator(.hidden)
            } else if items.isEmpty {
                emptyState
            } else {
                ForEach(items, id: \.id) { product in
                    ProductRowView(product: product)
                    .onTapGesture {
                        observable.send(ProductCatalogEvent.SelectProduct(productId: product.id))
                    }
                    .listRowSeparator(.hidden)
                }

                if state.hasNextPage {
                    paginationTrigger
                }
            }
        }
        .listStyle(.plain)
        .alert(
            MR.strings().pd_catalog_feature_error_title.desc().localized(),
            isPresented: Binding(
                get: { errorMessage != nil },
                set: { if !$0 { errorMessage = nil } }
            )
        ) {
            Button(MR.strings().pd_catalog_feature_retry_button.desc().localized()) {
                errorMessage = nil
                observable.send(ProductCatalogEvent.Retry())
            }
            Button(MR.strings().pd_catalog_feature_cancel.desc().localized(), role: .cancel) {
                errorMessage = nil
            }
        } message: {
            Text(errorMessage ?? "")
        }
    }

    // MARK: - Search Bar
    private var searchBar: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.secondary)

            TextField(
                MR.strings().pd_catalog_feature_search_placeholder.desc().localized(),
                text: Binding(
                    get: { observable.viewModel.searchQuery.value },
                    set: { observable.send(ProductCatalogEvent.SearchProducts(query: $0)) }
                )
            )
                .autocorrectionDisabled()

            if !observable.viewModel.searchQuery.value.isEmpty {
                Button {
                    observable.send(ProductCatalogEvent.ClearSearch())
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.secondary)
                }
            }
        }
        .listRowSeparator(.hidden)
    }

    // MARK: - Pagination Trigger
    private var paginationTrigger: some View {
        ProgressView()
            .frame(maxWidth: .infinity)
            .listRowSeparator(.hidden)
            .onAppear {
                observable.send(ProductCatalogEvent.LoadNextPage())
            }
    }

    // MARK: - Full Screen Loader
    private var fullScreenLoader: some View {
        VStack {
            Spacer()
            ProgressView()
                .scaleEffect(1.5)
            Spacer()
        }
    }

    // MARK: - Empty State
    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: "tray")
                .font(.system(size: 48))
                .foregroundColor(.secondary)
            Text(MR.strings().pd_catalog_feature_no_products_title.desc().localized())
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .listRowSeparator(.hidden)
    }
}

// MARK: - Product Row
private struct ProductRowView: View {
    let product: ProductViewData

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(product.title)
                .font(.headline)
            Text(product.description_)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .lineLimit(2)
        }
        .padding(.vertical, 4)
    }
}
