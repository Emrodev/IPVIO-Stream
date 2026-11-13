import SwiftUI

struct BrowseView: View {
    @EnvironmentObject private var app: AppModel
    let kind: MediaKind

    @State private var selectedGroup = MenuSentinel.allGroups
    @State private var selectedSource: UUID?
    @State private var sort: BrowseSort = .title
    @State private var displayed: [MediaItem] = []

    private var groups: [GroupCount] { app.groups(kind: kind) }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 26) {
                header

                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 10) {
                        SelectChip(
                            title: L.t("common.all"),
                            selected: selectedGroup == MenuSentinel.allGroups,
                            count: app.itemCount(kind: kind)
                        ) {
                            selectedGroup = MenuSentinel.allGroups
                        }
                        ForEach(groups) { group in
                            SelectChip(
                                title: app.displayGroup(group.name),
                                selected: selectedGroup == group.name,
                                count: group.count
                            ) {
                                selectedGroup = group.name
                            }
                        }
                    }
                    .padding(.vertical, 6)
                    .padding(.horizontal, 4)
                }
                .scrollClipDisabled()

                filters

                if displayed.isEmpty {
                    EmptyStateView(
                        symbol: "rectangle.on.rectangle.slash",
                        title: L.t("browse.noItems"),
                        message: L.t("browse.noResultsHint")
                    )
                    .frame(minHeight: 420)
                } else {
                    LazyVGrid(
                        columns: [GridItem(.adaptive(minimum: 210, maximum: 250), spacing: 24)],
                        alignment: .leading,
                        spacing: 38
                    ) {
                        ForEach(displayed) { item in
                            PosterCard(item: item, width: 210)
                        }
                    }
                    .padding(.horizontal, 8)
                    .padding(.bottom, 50)
                }
            }
            .padding(.horizontal, Theme.pagePadding)
            .padding(.top, 38)
        }
        .background(Theme.background)
        .navigationTitle("")
        .task(id: reloadToken) {
            rebuild()
        }
    }

    private var header: some View {
        HStack(alignment: .bottom) {
            ScreenTitle(
                title: L.t(kind.titleKey),
                subtitle: L.f("browse.count", displayed.count)
            )
            Spacer()
        }
    }

    private var filters: some View {
        HStack(spacing: 12) {
            if app.playlists.count > 1 {
                Menu {
                    Button(L.t("nav.allSources")) { selectedSource = nil }
                    ForEach(app.playlists) { playlist in
                        Button(playlist.name) { selectedSource = playlist.id }
                    }
                } label: {
                    Label(
                        selectedSource.flatMap { app.sourceName(for: $0) } ?? L.t("nav.allSources"),
                        systemImage: "externaldrive"
                    )
                    .font(.system(size: 16, weight: .semibold))
                }
                .buttonStyle(.bordered)
            }

            Menu {
                ForEach(BrowseSort.allCases, id: \.self) { option in
                    Button(L.t(option.titleKey)) { sort = option }
                }
            } label: {
                Label(L.t(sort.titleKey), systemImage: "arrow.up.arrow.down")
                    .font(.system(size: 16, weight: .semibold))
            }
            .buttonStyle(.bordered)

            if let language = selectedLanguageName {
                InfoPill(symbol: "globe", text: language)
            }
            Spacer()
        }
    }

    private var selectedLanguageName: String? {
        guard !app.settings.languageFilter.isEmpty else { return nil }
        return app.settings.languageFilter.map { ContentLanguage.displayName($0) }.sorted().joined(separator: " · ")
    }

    private var reloadToken: String {
        var parts = [
            kind.rawValue,
            selectedGroup,
            selectedSource?.uuidString ?? "all",
            sort.rawValue,
            String(app.items.count),
            app.settings.languageFilter.sorted().joined(separator: ","),
        ]
        if let current = app.groups(kind: kind).first(where: { $0.name == selectedGroup }) {
            parts.append(String(current.count))
        }
        return parts.joined(separator: "|")
    }

    private func rebuild() {
        var values = app.items(kind: kind, group: selectedGroup == MenuSentinel.allGroups ? nil : selectedGroup, playlistID: selectedSource)
        switch sort {
        case .title:
            values.sort { $0.stableSortTitle.localizedStandardCompare($1.stableSortTitle) == .orderedAscending }
        case .rating:
            values.sort { ($0.rating ?? 0) > ($1.rating ?? 0) }
        case .year:
            values.sort { ($0.year ?? 0) > ($1.year ?? 0) }
        }
        displayed = values
    }
}

enum BrowseSort: String, CaseIterable, Hashable {
    case title
    case rating
    case year

    var titleKey: String {
        switch self {
        case .title: return "browse.sortTitle"
        case .rating: return "browse.sortRating"
        case .year: return "browse.sortYear"
        }
    }
}
