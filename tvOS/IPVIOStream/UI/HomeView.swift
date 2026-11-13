import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var app: AppModel
    @EnvironmentObject private var router: Router

    private var featured: [MediaItem] {
        Array(app.heroItems.prefix(8))
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 34) {
                featuredRow
                quickRow

                if !app.continueItems.isEmpty {
                    continueRow
                }
                if !app.liveItems.isEmpty {
                    liveRow
                }
                if !favoriteLiveItems.isEmpty {
                    favoriteLiveRow
                }

                PosterRow(
                    title: L.t("home.movies"),
                    items: Array(app.movies.prefix(20)),
                    onSeeAll: { app.setSection(.movies) }
                )
                PosterRow(
                    title: L.t("home.series"),
                    items: Array(app.seriesItems.prefix(20)),
                    onSeeAll: { app.setSection(.series) }
                )
                PosterRow(
                    title: L.t("home.topRated"),
                    items: app.topRated
                )
                PosterRow(
                    title: L.t("home.favorites"),
                    items: Array(favoriteVideoItems.prefix(20))
                )
                PosterRow(
                    title: L.t("home.myList"),
                    items: Array(app.myListItems.prefix(20))
                )
            }
            .padding(.horizontal, 52)
            .padding(.top, 26)
            .padding(.bottom, 70)
        }
        .background(Theme.background)
    }

    // MARK: - Featured carousel

    private var featuredRow: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .firstTextBaseline) {
                Text(L.t("home.featured"))
                    .font(.system(size: 31.9, weight: .bold, design: .rounded))
                    .foregroundStyle(Theme.text)
                Spacer()
                Text(app.activeSourceSummary)
                    .font(.system(size: 16.5, weight: .medium))
                    .foregroundStyle(Theme.textFaint)
                    .lineLimit(1)
            }

            if featured.isEmpty {
                EmptyStateView(
                    symbol: "play.rectangle.on.rectangle",
                    title: L.t("home.emptyTitle"),
                    message: L.t("home.emptyMessage")
                )
                .frame(height: 360)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHStack(spacing: 20) {
                        ForEach(featured) { item in
                            FeaturedCard(item: item) {
                                open(item)
                            }
                        }
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 14)
                }
                .scrollClipDisabled()
                .focusSection()
            }
        }
    }

    // MARK: - Quick tiles

    private var quickRow: some View {
        HStack(spacing: 16) {
            QuickTile(
                symbol: "dot.radiowaves.left.and.right",
                title: L.t("nav.live"),
                subtitle: L.f("live.channelCount", app.liveItems.count),
                tint: Theme.accent
            ) {
                app.setSection(.live)
            }
            QuickTile(
                symbol: "film.fill",
                title: L.t("nav.movies"),
                subtitle: L.f("browse.count", app.movies.count),
                tint: Color(hex: 0x5B8DEF)
            ) {
                app.setSection(.movies)
            }
            QuickTile(
                symbol: "rectangle.stack.fill",
                title: L.t("nav.series"),
                subtitle: L.f("browse.count", app.seriesItems.count),
                tint: Color(hex: 0xA78BFA)
            ) {
                app.setSection(.series)
            }
            QuickTile(
                symbol: "heart.fill",
                title: L.t("nav.favorites"),
                subtitle: L.f("browse.count", app.favorites.count),
                tint: Theme.accent
            ) {
                app.setSection(.favorites)
            }
            QuickTile(
                symbol: "magnifyingglass",
                title: L.t("nav.search"),
                subtitle: L.t("search.hint"),
                tint: Theme.accentLight
            ) {
                app.setSection(.search)
            }
        }
        .focusSection()
    }

    // MARK: - Rows

    private var continueRow: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: L.t("home.continue"))
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: 18) {
                    ForEach(app.continueItems, id: \.key) { entry in
                        ContinueCard(entry: entry)
                    }
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 14)
            }
            .scrollClipDisabled()
                .focusSection()
        }
    }

    private var liveRow: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(
                title: L.t("home.liveNow"),
                actionTitle: L.t("common.seeAll"),
                action: { app.setSection(.live) }
            )
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: 18) {
                    ForEach(Array(app.liveItems.prefix(20))) { item in
                        LiveChannelCard(item: item)
                    }
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 14)
            }
            .scrollClipDisabled()
                .focusSection()
        }
    }

    // MARK: - Actions

    private var favoriteLiveItems: [MediaItem] {
        app.favoriteItems.filter { $0.kind == .live }
    }

    private var favoriteVideoItems: [MediaItem] {
        app.favoriteItems.filter { $0.kind != .live }
    }

    private var favoriteLiveRow: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(
                title: L.t("favorites.live"),
                actionTitle: L.t("common.seeAll"),
                action: { app.setSection(.favorites) }
            )
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: 18) {
                    ForEach(Array(favoriteLiveItems.prefix(20))) { item in
                        LiveChannelCard(item: item)
                    }
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 14)
            }
            .scrollClipDisabled()
            .focusSection()
        }
    }

    private func open(_ item: MediaItem) {
        if item.kind == .series {
            router.detail(item.id)
        } else {
            app.play(item: item)
        }
    }
}

// MARK: - Featured card

private struct FeaturedCard: View {
    @EnvironmentObject private var app: AppModel
    let item: MediaItem
    let action: () -> Void
    @FocusState private var focused: Bool

    private var progress: WatchProgress? {
        app.progress(for: item, episode: nil)
    }

    var body: some View {
        Button(action: action) {
            ZStack(alignment: .bottomLeading) {
                RemoteImage(urlString: item.logo, fallbackText: item.title)
                    .frame(width: 560, height: 322)
                    .clipped()

                LinearGradient(
                    colors: [.clear, .black.opacity(0.18), .black.opacity(0.92)],
                    startPoint: .top,
                    endPoint: .bottom
                )

                LinearGradient(
                    colors: [.black.opacity(0.6), .clear],
                    startPoint: .leading,
                    endPoint: .trailing
                )
                .frame(width: 260)

                VStack(alignment: .leading, spacing: 8) {
                    Spacer()
                    HStack(spacing: 8) {
                        if let quality = item.quality {
                            Text(quality)
                                .font(.system(size: 11.8, weight: .heavy))
                                .foregroundStyle(.black)
                                .padding(.horizontal, 7)
                                .padding(.vertical, 3)
                                .background(Theme.accent, in: Capsule())
                        }
                        if let year = item.year {
                            Text(String(year))
                                .font(.system(size: 15.3, weight: .semibold))
                                .foregroundStyle(.white.opacity(0.75))
                        }
                        if let rating = item.rating, rating > 0 {
                            HStack(spacing: 3) {
                                Image(systemName: "star.fill")
                                    .font(.system(size: 11.8))
                                Text(String(format: "%.1f", rating))
                                    .font(.system(size: 15.3, weight: .semibold))
                            }
                            .foregroundStyle(Theme.gold)
                        }
                    }

                    Text(item.title)
                        .font(.system(size: 35.4, weight: .bold, design: .rounded))
                        .foregroundStyle(.white)
                        .lineLimit(1)

                    Text(item.displayPlot ?? app.displayGroup(item.group))
                        .font(.system(size: 17.7))
                        .foregroundStyle(.white.opacity(0.66))
                        .lineLimit(1)

                    HStack(spacing: 8) {
                        Image(systemName: item.kind == .series ? "rectangle.stack.fill" : "play.fill")
                            .font(.system(size: 13, weight: .bold))
                        Text(item.kind == .series ? L.t("detail.episodes") : L.t("common.play"))
                            .font(.system(size: 16.5, weight: .semibold))
                        Text("·")
                            .foregroundStyle(.white.opacity(0.35))
                        Text(app.sourceName(for: item.playlistID))
                            .font(.system(size: 15.3, weight: .medium))
                            .foregroundStyle(.white.opacity(0.55))
                            .lineLimit(1)
                    }
                    .foregroundStyle(.white.opacity(0.9))

                    if let progress, progress.fraction > 0.01 {
                        ProgressBadge(fraction: progress.fraction)
                            .frame(height: 4)
                            .padding(.top, 4)
                    }
                }
                .padding(22)
                .frame(width: 560, height: 322, alignment: .bottomLeading)

                VStack {
                    HStack {
                        Spacer()
                        Text(app.sourceName(for: item.playlistID).prefix(1).uppercased())
                            .font(.system(size: 14.2, weight: .heavy, design: .rounded))
                            .foregroundStyle(.black)
                            .frame(width: 30, height: 30)
                            .background(Theme.accent, in: Circle())
                            .overlay(Circle().stroke(.white.opacity(0.35), lineWidth: 1))
                    }
                    Spacer()
                }
                .padding(14)
            }
            .frame(width: 560, height: 322)
            .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .stroke(focused ? Color.white.opacity(0.9) : Color.white.opacity(0.08), lineWidth: focused ? 2.5 : 1)
            )
            .scaleEffect(focused ? 1.035 : 1)
            .shadow(color: .black.opacity(focused ? 0.6 : 0.3), radius: focused ? 26 : 12, y: 12)
            .animation(.easeOut(duration: 0.16), value: focused)
            .padding(6)
        }
        .buttonStyle(.plain)
        .focused($focused)
    }
}

// MARK: - Quick tile

private struct QuickTile: View {
    let symbol: String
    let title: String
    let subtitle: String
    let tint: Color
    let action: () -> Void
    @FocusState private var focused: Bool

    var body: some View {
        Button(action: action) {
            HStack(spacing: 14) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(focused ? Color.black.opacity(0.12) : tint.opacity(0.15))
                    Image(systemName: symbol)
                        .font(.system(size: 24.8, weight: .semibold))
                        .foregroundStyle(focused ? Color.black : tint)
                }
                .frame(width: 48, height: 48)

                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 21.2, weight: .semibold))
                        .foregroundStyle(focused ? Color.black : Theme.text)
                        .lineLimit(1)
                    Text(subtitle)
                        .font(.system(size: 15.3))
                        .foregroundStyle(focused ? Color.black.opacity(0.6) : Theme.textFaint)
                        .lineLimit(1)
                }
                Spacer(minLength: 0)
                Image(systemName: "chevron.right")
                    .font(.system(size: 15.3, weight: .bold))
                    .foregroundStyle(focused ? Color.black.opacity(0.45) : Theme.textFaint)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 13)
            .frame(maxWidth: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 17, style: .continuous)
                    .fill(focused ? Color.white : Theme.surface)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 17, style: .continuous)
                    .stroke(focused ? Color.clear : Theme.border, lineWidth: 1)
            )
            .scaleEffect(focused ? 1.025 : 1)
            .animation(.easeOut(duration: 0.13), value: focused)
        }
        .buttonStyle(.plain)
        .focused($focused)
    }
}

// MARK: - Favourites

struct FavoritesView: View {
    @EnvironmentObject private var app: AppModel
    @State private var filter: FavoritesFilter = .all

    private var allItems: [MediaItem] {
        app.favorites
            .compactMap { app.item(id: $0) }
            .sorted { $0.title.localizedStandardCompare($1.title) == .orderedAscending }
    }

    private var items: [MediaItem] {
        guard let kind = filter.kind else { return allItems }
        return allItems.filter { $0.kind == kind }
    }

    private var liveItems: [MediaItem] { items.filter { $0.kind == .live } }
    private var vodItems: [MediaItem] { items.filter { $0.kind != .live } }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 22) {
                ScreenTitle(
                    title: L.t("nav.favorites"),
                    subtitle: L.f("browse.count", allItems.count)
                )

                HStack(spacing: 9) {
                    ForEach(FavoritesFilter.allCases) { option in
                        SelectChip(
                            title: L.t(option.titleKey),
                            selected: filter == option
                        ) {
                            filter = option
                        }
                    }
                }
                .focusSection()

                if items.isEmpty {
                    EmptyStateView(
                        symbol: "heart",
                        title: L.t("favorites.empty"),
                        message: L.t("favorites.emptyHint")
                    )
                    .frame(maxWidth: .infinity, minHeight: 420)
                } else {
                    if !liveItems.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            SectionHeader(title: L.t("favorites.live"))
                            LazyVGrid(
                                columns: [GridItem(.adaptive(minimum: 300, maximum: 330), spacing: 18)],
                                alignment: .leading,
                                spacing: 22
                            ) {
                                ForEach(liveItems) { item in
                                    LiveChannelCard(item: item, width: 300)
                                }
                            }
                            .padding(.vertical, 8)
                            .focusSection()
                        }
                    }

                    if !vodItems.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            SectionHeader(title: L.t("favorites.vod"))
                            LazyVGrid(
                                columns: [GridItem(.adaptive(minimum: 200, maximum: 240), spacing: 22)],
                                alignment: .leading,
                                spacing: 30
                            ) {
                                ForEach(vodItems) { item in
                                    PosterCard(item: item, width: 210)
                                }
                            }
                            .padding(.vertical, 8)
                            .focusSection()
                        }
                    }
                }
            }
            .padding(.horizontal, Theme.pagePadding)
            .padding(.top, 32)
            .padding(.bottom, 70)
        }
        .background(Theme.background)
    }
}

private enum FavoritesFilter: String, CaseIterable, Identifiable {
    case all
    case live
    case movies
    case series

    var id: String { rawValue }

    var titleKey: String {
        switch self {
        case .all: return "common.all"
        case .live: return "nav.live"
        case .movies: return "nav.movies"
        case .series: return "nav.series"
        }
    }

    var kind: MediaKind? {
        switch self {
        case .all: return nil
        case .live: return .live
        case .movies: return .movie
        case .series: return .series
        }
    }
}
