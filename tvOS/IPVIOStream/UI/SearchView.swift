import SwiftUI

struct SearchView: View {
    @EnvironmentObject private var app: AppModel
    @State private var query = ""
    @State private var results: [MediaItem] = []
    @State private var searchTask: Task<Void, Never>?
    @State private var filter: SearchFilter = .all
    @FocusState private var searchFocused: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 22) {
            ScreenTitle(title: L.t("search.title"), subtitle: L.t("search.hint"))

            HStack(spacing: 14) {
                Image(systemName: "magnifyingglass")
                    .font(.system(size: 28.3, weight: .semibold))
                    .foregroundStyle(searchFocused ? Theme.accent : Theme.textDim)
                TextField(L.t("browse.searchPlaceholder"), text: $query)
                    .textFieldStyle(.plain)
                    .font(.system(size: 28.3, weight: .medium))
                    .focused($searchFocused)
                if !query.isEmpty {
                    Button {
                        query = ""
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 23.6))
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
                    .stroke(searchFocused ? Theme.accent.opacity(0.7) : Theme.border, lineWidth: 1.5)
            )

            HStack(spacing: 10) {
                ForEach(SearchFilter.allCases) { option in
                    SelectChip(
                        title: L.t(option.titleKey),
                        selected: filter == option
                    ) {
                        filter = option
                        runSearch(immediate: true)
                    }
                }
            }

            content
        }
        .padding(.horizontal, Theme.pagePadding)
        .padding(.top, 40)
        .background(Theme.background)
        .onAppear {
            DispatchQueue.main.async { searchFocused = true }
        }
        .onChange(of: query) { _, _ in
            runSearch(immediate: false)
        }
    }

    private func runSearch(immediate: Bool) {
        searchTask?.cancel()
        let needle = query.trimmed
        guard !needle.isEmpty else {
            results = []
            return
        }
        let kind = filter.kind
        searchTask = Task {
            if !immediate {
                try? await Task.sleep(nanoseconds: 220_000_000)
            }
            guard !Task.isCancelled else { return }
            let found = app.search(needle, kind: kind)
            await MainActor.run {
                if !Task.isCancelled { results = found }
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
                    .font(.system(size: 20.1, weight: .semibold))
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
                    .focusSection()
                }
            }
        }
    }
}


private enum SearchFilter: String, CaseIterable, Identifiable {
    case all
    case live
    case movie
    case series

    var id: String { rawValue }

    var titleKey: String {
        switch self {
        case .all: return "common.all"
        case .live: return "nav.live"
        case .movie: return "nav.movies"
        case .series: return "nav.series"
        }
    }

    var kind: MediaKind? {
        switch self {
        case .all: return nil
        case .live: return .live
        case .movie: return .movie
        case .series: return .series
        }
    }
}
