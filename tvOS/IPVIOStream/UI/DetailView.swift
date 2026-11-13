import SwiftUI

struct DetailView: View {
    @EnvironmentObject private var app: AppModel
    @Environment(\.dismiss) private var dismiss
    let itemID: String

    @State private var metadata: EnrichedMetadata?
    @State private var episodes: [Episode] = []
    @State private var selectedSeason: Int = 1
    @State private var loadingEpisodes = false
    @State private var episodeError: String?

    var body: some View {
        Group {
            if let item = app.item(id: itemID) {
                content(item)
            } else {
                EmptyStateView(
                    symbol: "questionmark.folder",
                    title: L.t("error.title"),
                    message: L.t("error.selection")
                )
            }
        }
        .background(Theme.background)
        .toolbar(.hidden, for: .navigationBar)
        .overlay(alignment: .topLeading) {
            Button {
                dismiss()
            } label: {
                Image(systemName: "chevron.backward")
                    .font(.system(size: 19, weight: .bold))
                    .frame(width: 46, height: 46)
            }
            .buttonStyle(.bordered)
            .padding(.top, 34)
            .padding(.leading, 34)
        }
        .task(id: itemID) {
            guard let item = app.item(id: itemID) else { return }
            episodes = item.episodes
            selectedSeason = seasons.first ?? 1
            metadata = app.metadataByItem[itemID]
            if metadata == nil {
                metadata = await app.metadata(for: item)
            }
            if item.kind == .series, episodes.isEmpty {
                await loadEpisodes(item)
            }
        }
    }

    private func content(_ item: MediaItem) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                header(item)

                VStack(alignment: .leading, spacing: 36) {
                    overviewSection(item)
                    if item.kind == .series {
                        episodesSection(item)
                    } else {
                        movieInfo(item)
                    }
                }
                .padding(.horizontal, Theme.pagePadding)
                .padding(.top, 6)
                .padding(.bottom, 70)
            }
        }
    }

    // MARK: Header

    private func header(_ item: MediaItem) -> some View {
        GeometryReader { proxy in
            ZStack(alignment: .bottomLeading) {
                RemoteImage(urlString: metadata?.backdropURL ?? item.logo, fallbackText: item.title)
                    .frame(width: proxy.size.width, height: 600)
                    .blur(radius: metadata?.backdropURL == nil ? 22 : 0)
                    .scaleEffect(metadata?.backdropURL == nil ? 1.16 : 1)
                    .clipped()

                LinearGradient(
                    colors: [.black.opacity(0.12), Theme.background.opacity(0.55), Theme.background],
                    startPoint: .top,
                    endPoint: .bottom
                )

                LinearGradient(
                    colors: [Theme.background.opacity(0.9), .clear],
                    startPoint: .leading,
                    endPoint: .trailing
                )
                .frame(width: proxy.size.width * 0.62)

                HStack(alignment: .bottom, spacing: 34) {
                    RemoteImage(urlString: item.logo, fallbackText: item.title)
                        .frame(width: 236, height: 350)
                        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                        .overlay(
                            RoundedRectangle(cornerRadius: 16, style: .continuous)
                                .stroke(Color.white.opacity(0.12), lineWidth: 1)
                        )
                        .shadow(color: .black.opacity(0.55), radius: 28, y: 14)

                    VStack(alignment: .leading, spacing: 16) {
                        Text(item.title)
                            .font(.system(size: 52, weight: .heavy, design: .rounded))
                            .foregroundStyle(.white)
                            .lineLimit(2)
                            .fixedSize(horizontal: false, vertical: true)

                        HStack(spacing: 12) {
                            if let year = item.year {
                                InfoPill(symbol: "calendar", text: String(year), tint: .white.opacity(0.78))
                            }
                            if let quality = item.quality {
                                TagBadge(text: quality, tint: Theme.background, background: Theme.green)
                            }
                            if let duration = durationText(item) {
                                InfoPill(symbol: "clock", text: duration, tint: .white.opacity(0.78))
                            }
                            if item.kind == .series {
                                InfoPill(
                                    symbol: "rectangle.stack",
                                    text: L.f("detail.seasonCount", max(1, Set(episodes.map(\.season)).count)),
                                    tint: .white.opacity(0.78)
                                )
                            }
                            InfoPill(symbol: "globe", text: ContentLanguage.displayName(item.language), tint: .white.opacity(0.6))
                        }

                        if let ratings = metadata {
                            HStack(spacing: 10) {
                                if let imdb = ratings.imdbRating {
                                    ratingRow(label: "IMDb", value: String(format: "%.1f", imdb), tint: Theme.gold)
                                }
                                if let tmdb = ratings.tmdbRating, ratings.imdbRating == nil {
                                    ratingRow(label: "TMDB", value: String(format: "%.1f", tmdb), tint: Theme.greenBright)
                                }
                                if let rotten = ratings.rottenRating {
                                    ratingRow(label: "RT", value: "\(Int(rotten))%", tint: Theme.red)
                                }
                            }
                        } else if let rating = item.rating {
                            ratingRow(label: "IMDb", value: String(format: "%.1f", rating), tint: Theme.gold)
                        }

                        HStack(spacing: 12) {
                            Text(app.displayGroup(item.group))
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(.white.opacity(0.62))
                                .lineLimit(1)
                            Text("·")
                                .foregroundStyle(.white.opacity(0.35))
                            Text(app.sourceName(for: item.playlistID))
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(.white.opacity(0.62))
                                .lineLimit(1)
                        }

                        HStack(spacing: 14) {
                            Button {
                                play(item)
                            } label: {
                                HStack(spacing: 10) {
                                    Image(systemName: "play.fill")
                                    Text(playTitle(item))
                                }
                                .font(.system(size: 19, weight: .bold))
                                .padding(.horizontal, 24)
                                .padding(.vertical, 12)
                            }
                            .buttonStyle(.borderedProminent)
                            .tint(Theme.green)
                            .foregroundStyle(Theme.background)

                            Button {
                                app.toggleFavorite(item.id)
                            } label: {
                                Label(
                                    app.isFavorite(item.id) ? L.t("detail.removeFavorite") : L.t("detail.addFavorite"),
                                    systemImage: app.isFavorite(item.id) ? "heart.fill" : "heart"
                                )
                                .font(.system(size: 17, weight: .semibold))
                                .padding(.horizontal, 18)
                                .padding(.vertical, 11)
                            }
                            .buttonStyle(.bordered)

                            Button {
                                app.toggleMyList(item.id)
                            } label: {
                                Label(
                                    app.isInMyList(item.id) ? L.t("detail.removeList") : L.t("detail.addList"),
                                    systemImage: app.isInMyList(item.id) ? "bookmark.fill" : "bookmark"
                                )
                                .font(.system(size: 17, weight: .semibold))
                                .padding(.horizontal, 18)
                                .padding(.vertical, 11)
                            }
                            .buttonStyle(.bordered)
                        }
                        .padding(.top, 4)
                    }
                    Spacer(minLength: 0)
                }
                .padding(.horizontal, Theme.pagePadding)
                .padding(.bottom, 42)
            }
            .frame(width: proxy.size.width, height: 600)
        }
        .frame(height: 600)
        .clipped()
        .ignoresSafeArea(edges: .top)
    }

    private func ratingRow(label: String, value: String, tint: Color) -> some View {
        HStack(spacing: 6) {
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

    // MARK: Sections

    private func overviewSection(_ item: MediaItem) -> some View {
        VStack(alignment: .leading, spacing: 14) {
            Text(L.t("detail.overview"))
                .font(.system(size: 27, weight: .bold, design: .rounded))
                .foregroundStyle(Theme.text)
            Text(metadata?.overview ?? item.displayPlot ?? L.t("detail.noOverview"))
                .font(.system(size: 19))
                .foregroundStyle(Theme.textDim)
                .lineSpacing(5)
                .frame(maxWidth: 1150, alignment: .leading)
                .fixedSize(horizontal: false, vertical: true)

            if let genres = metadata?.genres, !genres.isEmpty {
                HStack(spacing: 8) {
                    ForEach(genres.prefix(6), id: \.self) { genre in
                        TagBadge(text: genre, tint: Theme.text, background: Theme.surface2)
                    }
                }
            }

            if let cast = metadata?.cast, !cast.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Text(L.t("detail.cast"))
                        .font(.system(size: 20, weight: .bold, design: .rounded))
                        .foregroundStyle(Theme.text)
                    Text(cast.prefix(8).joined(separator: " · "))
                        .font(.system(size: 17))
                        .foregroundStyle(Theme.textDim)
                        .lineLimit(2)
                }
                .padding(.top, 4)
            }
        }
    }

    private func movieInfo(_ item: MediaItem) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(L.t("detail.overview"))
                .font(.system(size: 27, weight: .bold, design: .rounded))
                .foregroundStyle(Theme.text)
            HStack(spacing: 40) {
                infoBlock(label: L.t("detail.sourceLabel"), value: app.sourceName(for: item.playlistID))
                infoBlock(label: L.t("detail.qualityLabel"), value: item.quality ?? L.t("common.auto"))
                infoBlock(label: L.t("detail.yearLabel"), value: item.year.map(String.init) ?? L.t("misc.notAvailable"))
                infoBlock(label: L.t("detail.durationLabel"), value: durationText(item) ?? L.t("misc.notAvailable"))
            }
            .padding(.top, 4)
        }
    }

    private func infoBlock(label: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(Theme.textFaint)
            Text(value)
                .font(.system(size: 18, weight: .semibold))
                .foregroundStyle(Theme.text)
        }
    }

    private func episodesSection(_ item: MediaItem) -> some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack(alignment: .firstTextBaseline) {
                Text(L.t("detail.episodes"))
                    .font(.system(size: 30, weight: .bold, design: .rounded))
                    .foregroundStyle(Theme.text)
                Text(L.f("detail.episodeCount", episodes.count))
                    .font(.system(size: 17))
                    .foregroundStyle(Theme.textDim)
                Spacer()
            }

            if loadingEpisodes {
                HStack(spacing: 12) {
                    ProgressView()
                        .tint(Theme.green)
                    Text(L.t("detail.loadingEpisodes"))
                        .font(.system(size: 17))
                        .foregroundStyle(Theme.textDim)
                }
                .padding(.vertical, 20)
            } else if let episodeError {
                VStack(alignment: .leading, spacing: 12) {
                    Text(episodeError)
                        .font(.system(size: 17))
                        .foregroundStyle(Theme.textDim)
                    Button(L.t("common.retry")) {
                        Task { await loadEpisodes(item) }
                    }
                    .buttonStyle(.bordered)
                }
            } else if episodes.isEmpty {
                Text(L.t("detail.noEpisodes"))
                    .font(.system(size: 17))
                    .foregroundStyle(Theme.textDim)
                    .padding(.vertical, 20)
            } else {
                seasonPicker
                LazyVStack(spacing: 10) {
                    ForEach(seasonEpisodes) { episode in
                        episodeRow(item, episode)
                    }
                }
            }
        }
    }

    private var seasonPicker: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 10) {
                ForEach(seasons, id: \.self) { season in
                    SelectChip(
                        title: L.f("detail.seasonN", season),
                        selected: selectedSeason == season
                    ) {
                        selectedSeason = season
                    }
                }
            }
            .padding(.vertical, 4)
        }
        .scrollClipDisabled()
    }

    private func episodeRow(_ item: MediaItem, _ episode: Episode) -> some View {
        Button {
            app.play(item: item, episode: episode, episodes: episodes)
        } label: {
            HStack(alignment: .top, spacing: 18) {
                RemoteImage(urlString: item.logo, fallbackText: "\(episode.number)")
                    .frame(width: 190, height: 107)
                    .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    .overlay(alignment: .bottomLeading) {
                        if let progress = app.progress(for: item, episode: episode), !progress.isFinished {
                            ProgressBadge(fraction: progress.fraction)
                                .padding(8)
                        }
                    }

                VStack(alignment: .leading, spacing: 8) {
                    HStack(spacing: 10) {
                        Text("E\(episode.number)")
                            .font(.system(size: 13, weight: .heavy, design: .rounded))
                            .foregroundStyle(Theme.greenBright)
                        Text(episode.displayTitle)
                            .font(.system(size: 19, weight: .semibold))
                            .foregroundStyle(Theme.text)
                            .lineLimit(1)
                        if let quality = episode.quality {
                            Text(quality)
                                .font(.system(size: 11, weight: .bold))
                                .foregroundStyle(Theme.textDim)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(Theme.surface2, in: Capsule())
                        }
                    }
                    Text("S\(episode.season) · \(L.f("detail.episodeN", episode.number))")
                        .font(.system(size: 14))
                        .foregroundStyle(Theme.textFaint)
                    if let progress = app.progress(for: item, episode: episode), !progress.isFinished {
                        Text(L.f("detail.episodeProgress", TimeText.clock(progress.remaining)))
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(Theme.greenBright)
                    }
                }
                Spacer()
                Image(systemName: "play.circle.fill")
                    .font(.system(size: 30))
                    .foregroundStyle(Theme.textDim)
                    .padding(.top, 10)
            }
            .padding(10)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .buttonStyle(.card)
    }

    // MARK: Helpers

    private var seasons: [Int] {
        let values = Set(episodes.map(\.season))
        return values.isEmpty ? [1] : values.sorted()
    }

    private var seasonEpisodes: [Episode] {
        episodes.filter { $0.season == selectedSeason }.sorted { $0.number < $1.number }
    }

    private func loadEpisodes(_ item: MediaItem) async {
        guard !loadingEpisodes else { return }
        loadingEpisodes = true
        episodeError = nil
        do {
            let loaded = try await app.episodes(for: item)
            episodes = loaded
            selectedSeason = seasons.first ?? 1
        } catch {
            episodeError = (error as? LocalizedError)?.errorDescription ?? L.t("detail.episodeError")
        }
        loadingEpisodes = false
    }

    private func play(_ item: MediaItem) {
        guard item.kind == .series else {
            app.play(item: item)
            return
        }
        if episodes.isEmpty {
            Task {
                await loadEpisodes(item)
                guard !episodes.isEmpty else { return }
                let episode = nextEpisode(item) ?? episodes[0]
                app.play(item: item, episode: episode, episodes: episodes)
            }
            return
        }
        let episode = nextEpisode(item) ?? episodes[0]
        app.play(item: item, episode: episode, episodes: episodes)
    }

    private func nextEpisode(_ item: MediaItem) -> Episode? {
        let inProgress = episodes
            .compactMap { episode -> (Episode, WatchProgress)? in
                guard let progress = app.progress(for: item, episode: episode), !progress.isFinished else { return nil }
                return (episode, progress)
            }
            .sorted { $0.1.updatedAt > $1.1.updatedAt }
            .first?.0
        if let inProgress { return inProgress }
        return episodes.first
    }

    private func playTitle(_ item: MediaItem) -> String {
        if item.kind == .live {
            return L.t("common.play")
        }
        if item.kind == .series {
            return episodes.isEmpty ? L.t("detail.playSeries") : L.t("common.play")
        }
        if let episode = nextEpisode(item), app.progress(for: item, episode: episode) != nil {
            return L.t("common.resume")
        }
        if app.resumePosition(for: item, episode: nil) != nil {
            return L.t("common.resume")
        }
        return L.t("detail.playMovie")
    }

    private func durationText(_ item: MediaItem) -> String? {
        if let runtime = metadata?.runtime, runtime > 0 {
            return TimeText.duration(runtime * 60)
        }
        return TimeText.duration(item.duration)
    }
}
