import Combine
import Foundation
import SwiftUI

struct GroupCount: Identifiable, Hashable, Sendable {
    var name: String
    var count: Int
    var id: String { name }
}

struct LanguageCount: Identifiable, Hashable, Sendable {
    var code: String
    var count: Int
    var id: String { code }
}

@MainActor
final class AppModel: ObservableObject {
    // Library
    @Published private(set) var playlists: [Playlist] = []
    @Published private(set) var items: [MediaItem] = []
    @Published private(set) var movies: [MediaItem] = []
    @Published private(set) var seriesItems: [MediaItem] = []
    @Published private(set) var liveItems: [MediaItem] = []
    @Published private(set) var topRated: [MediaItem] = []
    @Published private(set) var recentItems: [MediaItem] = []
    @Published private(set) var heroItems: [MediaItem] = []
    @Published private(set) var continueItems: [WatchProgress] = []
    @Published private(set) var favorites: Set<String> = []
    @Published private(set) var myList: Set<String> = []
    @Published private(set) var progress: [String: WatchProgress] = [:]
    @Published private(set) var metadataByItem: [String: EnrichedMetadata] = [:]

    // UI state
    @Published var settings = AppSettings() {
        didSet { settingsDidChange(from: oldValue) }
    }
    @Published var selectedSection: AppSection = .home
    @Published var playback: PlaybackRequest?
    @Published var job: LoadJob?
    @Published var toast: String?
    @Published var bootstrapped = false
    @Published var startupError: String?

    // Caches
    private var catalogByPlaylist: [UUID: [MediaItem]] = [:]
    private var indexByID: [String: MediaItem] = [:]
    private var groupCache: [MediaKind: [GroupCount]] = [:]
    private var episodeCache: [String: [Episode]] = [:]
    private var languageCountCache: [LanguageCount] = []
    private var saveTask: Task<Void, Never>?
    private var toastTask: Task<Void, Never>?
    private var isLoaded = false
    private var didBootstrap = false

    init() {
        let cache = URLCache(memoryCapacity: 64 * 1024 * 1024, diskCapacity: 512 * 1024 * 1024, diskPath: "ipvio")
        URLCache.shared = cache
        L.current = .en
    }

    // MARK: - Lifecycle

    func bootstrap() async {
        guard !didBootstrap else { return }
        didBootstrap = true

        let state = await Storage.shared.loadState()
        playlists = state.playlists
        settings = state.settings
        favorites = state.favorites
        myList = state.myList
        progress = state.progress
        selectedSection = AppSection(rawValue: state.lastSection) ?? .home
        L.current = settings.resolvedLanguage
        isLoaded = true

        for playlist in playlists where playlist.isEnabled {
            let cached = await Storage.shared.loadCatalog(playlistID: playlist.id)
            if !cached.isEmpty {
                catalogByPlaylist[playlist.id] = cached
            }
        }
        rebuildDerived()
        bootstrapped = true

        let missing = playlists.filter { $0.isEnabled && (catalogByPlaylist[$0.id]?.isEmpty ?? true) }
        for playlist in missing {
            await refresh(playlistID: playlist.id, announce: false)
        }
    }

    func saveNow() {
        guard isLoaded else { return }
        let snapshot = snapshotState()
        Task { await Storage.shared.saveState(snapshot) }
    }

    // MARK: - Derived library

    var interfaceLanguage: Lang { settings.resolvedLanguage }

    var activeSourceSummary: String {
        let active = playlists.filter(\.isEnabled)
        if active.isEmpty { return L.t("error.noPlaylist") }
        if active.count == 1 { return active[0].name }
        return "\(active.count) \(L.t("common.sources"))"
    }

    func item(id: String) -> MediaItem? { indexByID[id] }

    func sourceName(for playlistID: UUID) -> String {
        playlists.first { $0.id == playlistID }?.name ?? L.t("common.unknown")
    }

    func source(for playlistID: UUID) -> Playlist? {
        playlists.first { $0.id == playlistID }
    }

    func groups(kind: MediaKind) -> [GroupCount] {
        groupCache[kind] ?? []
    }

    func items(kind: MediaKind, group: String? = nil, playlistID: UUID? = nil) -> [MediaItem] {
        var result = items.filter { $0.kind == kind }
        if let group, !group.isEmpty, group != MenuSentinel.allGroups {
            result = result.filter { $0.group == group }
        }
        if let playlistID {
            result = result.filter { $0.playlistID == playlistID }
        }
        return result
    }

    var favoriteItems: [MediaItem] {
        favorites.compactMap { indexByID[$0] }.sortedByTitle()
    }

    var myListItems: [MediaItem] {
        myList.compactMap { indexByID[$0] }.sortedByTitle()
    }

    func languageCounts() -> [LanguageCount] {
        languageCountCache
    }

    private func rebuildLanguageCounts(_ source: [MediaItem]) {
        var counts: [String: Int] = [:]
        for item in source {
            counts[item.language, default: 0] += 1
        }
        let commonOrder = Dictionary(uniqueKeysWithValues: ContentLanguage.common.enumerated().map { ($0.element, $0.offset) })
        languageCountCache = counts.map { LanguageCount(code: $0.key, count: $0.value) }
            .sorted { lhs, rhs in
                let l = commonOrder[lhs.code] ?? 999
                let r = commonOrder[rhs.code] ?? 999
                if l != r { return l < r }
                if lhs.count != rhs.count { return lhs.count > rhs.count }
                return lhs.code < rhs.code
            }
    }

    func search(_ query: String, kind: MediaKind? = nil) -> [MediaItem] {
        let needle = query.trimmed
        guard !needle.isEmpty else { return [] }
        var results: [MediaItem] = []
        let pool = kind.map { items(kind: $0) } ?? items
        for item in pool {
            if item.title.localizedCaseInsensitiveContains(needle)
                || item.group.localizedCaseInsensitiveContains(needle) {
                results.append(item)
                if results.count >= 180 { break }
            }
        }
        return results
    }

    func itemCount(kind: MediaKind) -> Int {
        switch kind {
        case .live: return liveItems.count
        case .movie: return movies.count
        case .series: return seriesItems.count
        }
    }

    func displayGroup(_ group: String) -> String {
        group == "Other" ? L.t("common.other") : group
    }

    private func rebuildDerived() {
        let enabled = playlists.filter(\.isEnabled)
        var merged: [MediaItem] = []
        for playlist in enabled {
            merged.append(contentsOf: catalogByPlaylist[playlist.id] ?? [])
        }

        rebuildLanguageCounts(merged)
        if !settings.languageFilter.isEmpty {
            merged = merged.filter {
                $0.language == ContentLanguage.unknown || settings.languageFilter.contains($0.language)
            }
        }
        items = merged
        indexByID = Dictionary(merged.map { ($0.id, $0) }, uniquingKeysWith: { first, _ in first })
        movies = merged.filter { $0.kind == .movie }
        seriesItems = merged.filter { $0.kind == .series }
        liveItems = merged.filter { $0.kind == .live }

        var groups: [MediaKind: [String: Int]] = [:]
        for item in merged {
            groups[item.kind, default: [:]][item.group, default: 0] += 1
        }
        groupCache = groups.mapValues { values in
            values.map { GroupCount(name: $0.key, count: $0.value) }
                .sorted { $0.name.localizedStandardCompare($1.name) == .orderedAscending }
        }

        topRated = Array(
            merged.filter { ($0.rating ?? 0) > 0 && $0.kind != .live }
                .sorted { ($0.rating ?? 0) > ($1.rating ?? 0) }
                .prefix(24)
        )
        recentItems = Array(merged.filter { $0.kind != .live }.suffix(24).reversed())

        let heroCandidates = merged.filter {
            ($0.kind == .movie || $0.kind == .series) && $0.logo?.isEmpty == false
        }
        let ratedHeroes = heroCandidates.filter { ($0.rating ?? 0) >= 6 }
        heroItems = Array((ratedHeroes.isEmpty ? heroCandidates : ratedHeroes).prefix(12))

        rebuildContinue()
    }

    private func rebuildContinue() {
        continueItems = Array(
            progress.values
                .filter { !$0.isFinished && $0.kind != .live }
                .filter { indexByID[$0.itemID] != nil }
                .sorted { $0.updatedAt > $1.updatedAt }
                .prefix(30)
        )
    }

    // MARK: - Playback positions

    func resumePosition(for item: MediaItem, episode: Episode?) -> Double? {
        guard settings.resumePlayback, item.kind != .live else { return nil }
        let key = item.progressKey(episode: episode)
        guard let entry = progress[key], entry.duration > 60 else { return nil }
        let fraction = entry.position / entry.duration
        guard fraction > 0.02, fraction < 0.95 else { return nil }
        return entry.position
    }

    func progress(for item: MediaItem, episode: Episode?) -> WatchProgress? {
        progress[item.progressKey(episode: episode)]
    }

    func saveProgress(item: MediaItem, episode: Episode?, position: Double, duration: Double) {
        guard item.kind != .live, duration > 60, position.isFinite, position > 5 else { return }
        let key = item.progressKey(episode: episode)

        if position >= duration - 20 || position / duration >= 0.97 {
            if progress.removeValue(forKey: key) != nil {
                rebuildContinue()
                persist()
            }
            return
        }

        let entry = WatchProgress(
            itemID: item.id,
            episodeID: episode?.id,
            title: item.title,
            subtitle: episode?.displayTitle,
            artwork: item.logo,
            position: position,
            duration: duration,
            updatedAt: Date(),
            kind: item.kind,
            playlistID: item.playlistID
        )
        progress[key] = entry
        rebuildContinue()
        persist()
    }

    func clearProgress(for item: MediaItem, episode: Episode?) {
        progress.removeValue(forKey: item.progressKey(episode: episode))
        rebuildContinue()
        persist()
    }

    // MARK: - Favourites / list

    func isFavorite(_ id: String) -> Bool { favorites.contains(id) }
    func isInMyList(_ id: String) -> Bool { myList.contains(id) }

    func toggleFavorite(_ id: String) {
        if favorites.contains(id) { favorites.remove(id) } else { favorites.insert(id) }
        persist()
    }

    func toggleMyList(_ id: String) {
        if myList.contains(id) { myList.remove(id) } else { myList.insert(id) }
        persist()
    }

    // MARK: - Playback

    func play(item: MediaItem, episode: Episode? = nil, episodes: [Episode] = []) {
        var queue = episodes
        if queue.isEmpty { queue = item.episodes }
        var selected = episode
        if item.kind == .series, selected == nil {
            selected = queue.first
        }
        guard item.kind != .series || selected != nil else {
            showToast(L.t("error.noEpisodes"))
            return
        }
        playback = PlaybackRequest(
            itemID: item.id,
            episodeID: selected?.id,
            episodes: queue,
            startAt: resumePosition(for: item, episode: selected)
        )
    }

    func play(itemID: String, episodeID: String? = nil, episodes: [Episode] = []) {
        guard let item = item(id: itemID) else { return }
        let episode = episodes.first { $0.id == episodeID }
        play(item: item, episode: episode, episodes: episodes)
    }

    func resume(_ entry: WatchProgress) async {
        guard let item = item(id: entry.itemID) else { return }
        if item.kind == .series {
            let queue = (try? await episodes(for: item)) ?? item.episodes
            let episode = queue.first { $0.id == entry.episodeID } ?? queue.first
            play(item: item, episode: episode, episodes: queue)
        } else {
            play(item: item)
        }
    }

    func closePlayer() {
        playback = nil
    }

    // MARK: - Episodes

    func episodes(for item: MediaItem) async throws -> [Episode] {
        if !item.episodes.isEmpty { return item.episodes }
        if let cached = episodeCache[item.id] { return cached }
        guard item.kind == .series,
              let playlist = playlists.first(where: { $0.id == item.playlistID }),
              let account = playlist.source.xtreamAccount,
              let seriesID = item.xtream?.seriesID else {
            return []
        }
        do {
            let list = try await XtreamClient.episodes(account: account, seriesID: seriesID)
            episodeCache[item.id] = list
            return list
        } catch {
            throw PlaylistError.episodes
        }
    }

    // MARK: - EPG

    func epg(for item: MediaItem) async -> [EPGProgram] {
        guard settings.showLiveEPG, item.kind == .live,
              let playlist = playlists.first(where: { $0.id == item.playlistID }),
              let account = playlist.source.xtreamAccount else {
            return []
        }
        return await EPGService.shared.programs(item: item, account: account)
    }

    // MARK: - Metadata

    func metadata(for item: MediaItem, refresh: Bool = false) async -> EnrichedMetadata? {
        if !refresh, let cached = metadataByItem[item.id] { return cached }
        guard !settings.tmdbKey.trimmed.isEmpty else { return nil }
        let value = await MetadataService.shared.metadata(
            for: item,
            language: settings.resolvedLanguage,
            tmdbKey: settings.tmdbKey,
            omdbKey: settings.omdbKey
        )
        if let value { metadataByItem[item.id] = value }
        return value
    }

    func clearMetadataCache() async {
        metadataByItem.removeAll()
        await MetadataService.shared.clearCache()
        showToast(L.t("settings.clearMetadataDone"))
    }

    // MARK: - Search

    func searchAll(_ query: String) -> [MediaItem] {
        search(query)
    }

    // MARK: - Playlist management

    func addPlaylist(name: String, source: PlaylistSource) async throws {
        let displayName = name.trimmed.nilIfEmpty ?? PlaylistInput.suggestedName(for: source)
        var playlist = Playlist(id: UUID(), name: displayName, source: source)
        let loaded = try await loadData(playlist: playlist)
        playlist.lastUpdated = Date()
        playlist.itemCount = loaded.items.count
        playlist.epgURL = loaded.epgURLs.first
        playlist.lastError = nil

        await Storage.shared.saveCatalog(playlistID: playlist.id, items: loaded.items)
        playlists.append(playlist)
        catalogByPlaylist[playlist.id] = loaded.items
        persist()
        rebuildDerived()
        showToast(L.t("add.success"))
    }

    func refresh(playlistID: UUID, announce: Bool = true) async {
        guard job == nil else { return }
        guard let snapshot = playlists.first(where: { $0.id == playlistID }) else { return }
        do {
            let loaded = try await loadData(playlist: snapshot)
            guard let index = playlists.firstIndex(where: { $0.id == playlistID }) else { return }
            playlists[index].lastUpdated = Date()
            playlists[index].itemCount = loaded.items.count
            playlists[index].epgURL = loaded.epgURLs.first
            playlists[index].lastError = nil
            await Storage.shared.saveCatalog(playlistID: playlistID, items: loaded.items)
            catalogByPlaylist[playlistID] = loaded.items
            persist()
            rebuildDerived()
            if announce { showToast(L.t("toast.playlistUpdated")) }
        } catch is CancellationError {
            return
        } catch {
            if let index = playlists.firstIndex(where: { $0.id == playlistID }) {
                playlists[index].lastError = error.localizedDescription
            }
            persist()
            if announce { showToast(error.localizedDescription) }
        }
    }

    func refreshAll() async {
        for playlist in playlists where playlist.isEnabled {
            guard job == nil else { return }
            await refresh(playlistID: playlist.id, announce: false)
        }
    }

    func togglePlaylist(_ id: UUID) {
        guard let index = playlists.firstIndex(where: { $0.id == id }) else { return }
        playlists[index].isEnabled.toggle()
        let enabled = playlists[index].isEnabled
        persist()
        rebuildDerived()
        if enabled, catalogByPlaylist[id]?.isEmpty ?? true {
            Task { await refresh(playlistID: id, announce: false) }
        }
    }

    func deletePlaylist(_ id: UUID) async {
        playlists.removeAll { $0.id == id }
        catalogByPlaylist.removeValue(forKey: id)
        progress = progress.filter { $0.value.playlistID != id }
        episodeCache.removeAll()
        await Storage.shared.deleteCatalog(playlistID: id)
        persist()
        rebuildDerived()
    }

    func clearEverything() async {
        await Storage.shared.clearAll()
        await MetadataService.shared.clearCache()
        playlists = []
        catalogByPlaylist = [:]
        items = []
        movies = []
        seriesItems = []
        liveItems = []
        favorites = []
        myList = []
        progress = [:]
        continueItems = []
        metadataByItem = [:]
        episodeCache = [:]
        settings = AppSettings()
        L.current = settings.resolvedLanguage
        selectedSection = .home
        startupError = nil
        persist()
    }

    // MARK: - Settings

    func updateSettings(_ change: (inout AppSettings) -> Void) {
        var value = settings
        change(&value)
        settings = value
    }

    func binding<T>(_ keyPath: WritableKeyPath<AppSettings, T>) -> Binding<T> {
        Binding(
            get: { self.settings[keyPath: keyPath] },
            set: { newValue in
                var value = self.settings
                value[keyPath: keyPath] = newValue
                self.settings = value
            }
        )
    }

    private func settingsDidChange(from oldValue: AppSettings) {
        guard isLoaded else { return }
        if oldValue.appLanguage != settings.appLanguage {
            L.current = settings.resolvedLanguage
            metadataByItem.removeAll()
        }
        if oldValue.languageFilter != settings.languageFilter {
            rebuildDerived()
        }
        persist()
    }

    // MARK: - Loading

    private func loadData(playlist: Playlist) async throws -> LoadedPlaylist {
        job = LoadJob(playlistName: playlist.name, progress: 0.01, status: L.t("load.login"))
        defer { job = nil }

        return try await PlaylistLoader.load(playlist: playlist) { [weak self] value, status in
            Task { @MainActor [weak self] in
                guard let self, var current = self.job, current.playlistName == playlist.name else { return }
                let clamped = min(max(value, 0), 1)
                if abs(current.progress - clamped) < 0.004, current.status == status { return }
                current.progress = clamped
                current.status = status
                self.job = current
            }
        }
    }

    // MARK: - Persistence

    private func snapshotState() -> PersistedState {
        PersistedState(
            playlists: playlists,
            settings: settings,
            favorites: favorites,
            myList: myList,
            progress: progress,
            lastSection: selectedSection.rawValue
        )
    }

    private func persist() {
        guard isLoaded else { return }
        let snapshot = snapshotState()
        saveTask?.cancel()
        saveTask = Task {
            try? await Task.sleep(nanoseconds: 350_000_000)
            guard !Task.isCancelled else { return }
            await Storage.shared.saveState(snapshot)
        }
    }

    func showToast(_ message: String) {
        toast = message
        toastTask?.cancel()
        toastTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: 3_000_000_000)
            guard let self, !Task.isCancelled else { return }
            self.toast = nil
        }
    }

    func setSection(_ section: AppSection) {
        selectedSection = section
        persist()
    }
}

enum MenuSentinel {
    static let allGroups = "__all_groups__"
    static let allSources = "__all_sources__"
}
