import SwiftUI

struct SearchView: View {
    @EnvironmentObject private var app: AppModel
    @State private var query = ""
    @State private var results: [MediaItem] = []
    @State private var searchTask: Task<Void, Never>?
    @FocusState private var searchFocused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 22) {
            ScreenTitle(title: L.t("search.title"), subtitle: L.t("search.hint"))

            HStack(spacing: 14) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 24, weight: .semibold))
                    .foregroundStyle(searchFocused ? Theme.green : Theme.textDim)
                TextField(L.t("browse.searchPlaceholder"), text: $query)
                    .textFieldStyle(.plain)
                    .font(.system(size: 24, weight: .medium))
                    .focused($searchFocused)
                if !query.isEmpty {
                    Button {
                        query = ""
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 20))
                            .foregroundStyle(Theme.textFaint)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 22)
            .padding(.vertical, 17)
            .background(Theme.surface, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(searchFocused ? Theme.green.opacity(0.7) : Theme.border, lineWidth: 1.5)
            )

            content
        }
        .padding(.horizontal, Theme.pagePadding)
        .padding(.top, 40)
        .background(Theme.background)
        .onAppear {
            DispatchQueue.main.async { searchFocused = true }
        }
        .onChange(of: query) { _, value in
            searchTask?.cancel()
            let needle = value.trimmed
            guard !needle.isEmpty else {
                results = []
                return
            }
            searchTask = Task {
                try? await Task.sleep(nanoseconds: 250_000_000)
                guard !Task.isCancelled else { return }
                let found = app.search(needle)
                await MainActor.run {
                    if !Task.isCancelled { results = found }
                }
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        if query.trimmed.isEmpty {
            EmptyStateView(
                symbol: "magnifyingglass",
                title: L.t("search.start"),
                message: L.t("search.hint")
            )
        } else if results.isEmpty {
            EmptyStateView(
                symbol: "magnifyingglass",
                title: L.f("search.empty", query),
                message: L.t("search.tryOther")
            )
        } else {
            VStack(alignment: .leading, spacing: 14) {
                Text(L.f("search.results", results.count))
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(Theme.textDim)
                ScrollView {
                    LazyVGrid(
                        columns: [GridItem(.adaptive(minimum: 210, maximum: 250), spacing: 24)],
                        alignment: .leading,
                        spacing: 38
                    ) {
                        ForEach(results) { item in
                            PosterCard(item: item, width: 210)
                        }
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 14)
                    .padding(.bottom, 50)
                }
            }
        }
    }
}
