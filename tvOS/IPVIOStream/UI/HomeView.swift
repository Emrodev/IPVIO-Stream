import Combine
import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var app: AppModel
    @Binding var path: NavigationPath

    @State private var heroIndex = 0
    @State private var heroMetadata: EnrichedMetadata?

    private let rotation = Timer.publish(every: 10, on: .main, in: .common).autoconnect()

    private var heroes: [MediaItem] { app.heroItems }
    private var hero: MediaItem? {
        guard !heroes.isEmpty else { return nil }
        return heroes[heroIndex % heroes.count]
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                if let hero {
                    heroSection(hero)
                } else {
                    emptyHero
                }

                VStack(alignment: .leading, spacing: 44) {
                    if !app.continueItems.isEmpty {
                        continueRow
                    }
                    if !app.liveItems.isEmpty {
                        liveRow
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
                        items: app.topRated,
                        onSeeAll: nil
                    )
                    PosterRow(
                        title: L.t("home.favorites"),
                        items: Array(app.favoriteItems.prefix(20)),
                        onSeeAll: nil
                    )
                    PosterRow(
                        title: L.t("home.myList"),
                        items: Array(app.myListItems.prefix(20)),
                        onSeeAll: nil
                    )
                }
                .padding(.horizontal, Theme.pagePadding)
                .padding(.top, 8)
                .padding(.bottom, 70)
            }
        }
        .background(Theme.background)
        .ignoresSafeArea(edges: .top)
        .onReceive(rotation) { _ in
            guard heroes.count > 1 else { return }
            withAnimation(.easeInOut(duration: 0.5)) {
                heroIndex = (heroIndex + 1) % heroes.count
            }
        }
        .task(id: hero?.id) {
            guard let hero else { return }
            heroMetadata = app.metadataByItem[hero.id]
            if heroMetadata == nil {
                heroMetadata = nil
                heroMetadata = await app.metadata(for: hero)
            }
        }
    }

    // MARK: Hero

    private func heroSection(_ item: MediaItem) -> some View {
        GeometryReader { proxy in
            ZStack(alignment: .bottomLeading) {
                RemoteImage(
                    urlString: heroMetadata?.backdropURL ?? item.logo,
                    fallbackText: item.title
                )
                .frame(width: proxy.size.width, height: 660)
                .blur(radius: heroMetadata?.backdropURL == nil ? 18 : 0)
                .scaleEffect(heroMetadata?.backdropURL == nil ? 1.1 : 1)
                .clipped()

                LinearGradient(
                    colors: [
                        .black.opacity(0.05),
                        Theme.background.opacity(0.55),
                        Theme.background,
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )

                LinearGradient(
                    colors: [Theme.background.opacity(0.92), .clear],
                    startPoint: .leading,
                    endPoint: .trailing
                )
                .frame(width: proxy.size.width * 0.68)

                HStack(alignment: .bottom, spacing: 34) {
                    VStack(alignment: .leading, spacing: 18) {
                        HStack(spacing: 8) {
                            Image(systemName: "play.fill")
                                .font(.system(size: 12, weight: .black))
                            Text("IPVIO")
                                .font(.system(size: 14, weight: .heavy, design: .rounded))
                                .tracking(2)
                        }
                        .foregroundStyle(Theme.green)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(Theme.green.opacity(0.12), in: Capsule())

                        Text(item.title)
                            .font(.system(size: 62, weight: .heavy, design: .rounded))
                            .foregroundStyle(.white)
                            .lineLimit(2)
                            .shadow(color: .black.opacity(0.4), radius: 20, y: 6)

                        HStack(spacing: 14) {
                            if let rating = heroMetadata?.imdbRating ?? item.rating {
                                ratingBadge("IMDb", String(format: "%.1f", rating), Theme.gold)
                            }
                            if let rotten = heroMetadata?.rottenRating {
                                ratingBadge("RT", "\(Int(rotten))%", Theme.red)
                            }
                            if let year = item.year {
                                InfoPill(symbol: "calendar", text: String(year), tint: .white.opacity(0.78))
                            }
                            if let quality = item.quality {
                                InfoPill(symbol: "4k.tv", text: quality, tint: Theme.greenBright)
                            }
                            if item.kind == .series {
                                InfoPill(
                                    symbol: "rectangle.stack",
                                    text: L.f("detail.seasonCount", max(1, Set(item.episodes.map(\.season)).count)),
                                    tint: .white.opacity(0.78)
                                )
                            }
                            Text(app.displayGroup(item.group))
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(.white.opacity(0.7))
                                .lineLimit(1)
                        }

                        Text(heroMetadata?.overview ?? item.displayPlot ?? L.t("detail.noOverview"))
                            .font(.system(size: 19))
                            .foregroundStyle(.white.opacity(0.76))
                            .lineLimit(3)
                            .frame(maxWidth: 760, alignment: .leading)
                            .fixedSize(horizontal: false, vertical: true)

                        HStack(spacing: 16) {
                            Button {
                                play(item)
                            } label: {
                                HStack(spacing: 10) {
                                    Image(systemName: playSymbol(item))
                                    Text(playTitle(item))
                                }
                                .font(.system(size: 20, weight: .bold))
                                .padding(.horizontal, 26)
                                .padding(.vertical, 13)
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(Theme.green)
                            .foregroundStyle(Theme.background)

                            Button {
                                path.append(Route.detail(item.id))
                            } label: {
                                HStack(spacing: 8) {
                                    Image(systemName: "info.circle")
                                    Text(L.t("common.details"))
                                }
                                .font(.system(size: 19, weight: .semibold))
                                .padding(.horizontal, 22)
                                .padding(.vertical, 12)
                            }
                            .buttonStyle(.bordered)

                            Button {
                                app.toggleMyList(item.id)
                            } label: {
                                Image(systemName: app.isInMyList(item.id) ? "bookmark.fill" : "bookmark")
                                    .font(.system(size: 21, weight: .semibold))
                                    .frame(width: 48, height: 48)
                            }
                            .buttonStyle(.bordered)
                        }
                        .padding(.top, 4)
                    }
                    Spacer()
                }
                .padding(.horizontal, Theme.pagePadding)
                .padding(.bottom, 54)
            }
            .frame(width: proxy.size.width, height: 660)
        }
        .frame(height: 660)
        .clipped()
    }

    private var emptyHero: some View {
        VStack(spacing: 20) {
            BrandMark(size: 84)
            Text(L.t("home.emptyTitle"))
                .font(.system(size: 44, weight: .heavy, design: .rounded))
                .foregroundStyle(Theme.text)
            Text(L.t("home.emptyMessage"))
                .font(.system(size: 21))
                .foregroundStyle(Theme.textDim)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 420)
        .padding(.top, 60)
    }

    // MARK: Rows

    private var continueRow: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: L.t("home.continue"), subtitle: nil)
            ScrollView(.horizontal, showsIndicators: false) {
                LazyHStack(spacing: 20) {
                    ForEach(app.continueItems, id: \.key) { entry in
                        ContinueCard(entry: entry)
                    }
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 18)
            }
            .scrollClipDisabled()
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
                LazyHStack(spacing: 20) {
                    ForEach(Array(app.liveItems.prefix(20))) { item in
                        LiveChannelCard(item: item)
                    }
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 18)
            }
            .scrollClipDisabled()
        }
    }

    // MARK: Helpers

    private func play(_ item: MediaItem) {
        if item.kind == .series {
            path.append(Route.detail(item.id))
        } else {
            app.play(item: item)
        }
    }

    private func playTitle(_ item: MediaItem) -> String {
        if item.kind == .series {
            return L.t("common.details")
        }
        if app.resumePosition(for: item, episode: nil) != nil {
            return L.t("common.resume")
        }
        return item.kind == .live ? L.t("common.play") : L.t("detail.playMovie")
    }

    private func playSymbol(_ item: MediaItem) -> String {
        if item.kind == .series { return "chevron.right" }
        return "play.fill"
    }

    private func ratingBadge(_ label: String, _ value: String, _ tint: Color) -> some View {
        HStack(spacing: 5) {
            Text(label)
                .font(.system(size: 12, weight: .heavy))
            Text(value)
                .font(.system(size: 16, weight: .bold))
        }
        .foregroundStyle(tint)
        .padding(.horizontal, 10)
        .padding(.vertical, 5)
        .background(.black.opacity(0.35), in: Capsule())
    }
}
