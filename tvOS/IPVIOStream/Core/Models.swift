import Foundation

// MARK: - Media

enum MediaKind: String, Codable, CaseIterable, Hashable, Sendable {
    case live
    case movie
    case series

    var titleKey: String {
        switch self {
        case .live: return "nav.live"
        case .movie: return "nav.movies"
        case .series: return "nav.series"
        }
    }

    var symbol: String {
        switch self {
        case .live: return "dot.radiowaves.left.and.right"
        case .movie: return "film.fill"
        case .series: return "rectangle.stack.fill"
        }
    }
}

struct Variant: Codable, Hashable, Identifiable, Sendable {
    var url: String
    var label: String
    var quality: String?
    var language: String?

    var id: String { url + "|" + label + "|" + (quality ?? "") }

    var displayLabel: String {
        let parts: [String?] = [label, quality, language?.uppercased()]
        let values = parts
            .compactMap { $0?.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        return values.isEmpty ? "Auto" : values.joined(separator: " · ")
    }
}

struct Episode: Codable, Hashable, Identifiable, Sendable {
    var season: Int
    var number: Int
    var title: String
    var url: String
    var quality: String?
    var language: String?
    var variants: [Variant] = []

    var id: String { "s\(season)e\(number)" }

    var displayTitle: String {
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? "Episode \(number)" : trimmed
    }

    var allVariants: [Variant] {
        if !variants.isEmpty { return variants }
        return [Variant(url: url, label: "", quality: quality, language: language)]
    }
}

struct XtreamIDs: Codable, Hashable, Sendable {
    var streamID: String?
    var seriesID: String?
    var containerExtension: String?
}

struct MediaItem: Codable, Identifiable, Hashable, Sendable {
    var id: String
    var playlistID: UUID
    var kind: MediaKind
    var title: String
    var year: Int?
    var url: String
    var logo: String?
    var group: String
    var plot: String?
    var rating: Double?
    var duration: Int?
    var tvgID: String?
    var language: String
    var quality: String?
    var episodes: [Episode] = []
    var xtream: XtreamIDs?
    var catchupDays: Int?

    var isSeries: Bool { kind == .series }

    var stableSortTitle: String {
        title.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: Locale(identifier: "en"))
    }

    var metaLine: String {
        var parts: [String] = []
        if let year, year > 1900 { parts.append(String(year)) }
        if let quality, !quality.isEmpty { parts.append(quality) }
        if kind == .series, !episodes.isEmpty {
            parts.append("\(episodes.count) EP")
        }
        return parts.joined(separator: " · ")
    }

    func progressKey(episode: Episode?) -> String {
        if let episode { return id + "|" + episode.id }
        return id
    }

    func playbackVariants(episode: Episode?) -> [Variant] {
        if let episode, !episode.allVariants.isEmpty { return episode.allVariants }
        if !url.isEmpty {
            return [Variant(url: url, label: "", quality: quality, language: language)]
        }
        return []
    }

    var displayPlot: String? {
        guard let plot else { return nil }
        let cleaned = plot.replacingOccurrences(of: "<br />", with: "\n")
            .replacingOccurrences(of: "<br/>", with: "\n")
            .replacingOccurrences(of: "<br>", with: "\n")
            .replacingOccurrences(of: "&amp;", with: "&")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        return cleaned.isEmpty ? nil : cleaned
    }
}

// MARK: - Playlists

struct XtreamAccount: Codable, Hashable, Sendable {
    var base: String
    var username: String
    var password: String

    var hostLabel: String { URL(string: base)?.host ?? base }
}

enum PlaylistSource: Codable, Hashable, Sendable {
    case xtream(XtreamAccount)
    case m3u(url: String)

    var isXtream: Bool {
        if case .xtream = self { return true }
        return false
    }

    var xtreamAccount: XtreamAccount? {
        if case let .xtream(account) = self { return account }
        return nil
    }

    var hostLabel: String {
        switch self {
        case let .xtream(account):
            return account.hostLabel
        case let .m3u(url):
            return URL(string: url)?.host ?? url
        }
    }

    var typeLabel: String {
        switch self {
        case .xtream: return "Xtream Codes"
        case .m3u: return "M3U"
        }
    }
}

struct Playlist: Codable, Identifiable, Hashable, Sendable {
    var id: UUID
    var name: String
    var source: PlaylistSource
    var isEnabled: Bool = true
    var lastUpdated: Date?
    var itemCount: Int = 0
    var epgURL: String?
    var lastError: String?
}

struct LoadedPlaylist: Sendable {
    var items: [MediaItem]
    var epgURLs: [String]
    var counts: [MediaKind: Int]
}

// MARK: - Progress

struct WatchProgress: Codable, Hashable, Sendable {
    var itemID: String
    var episodeID: String?
    var title: String
    var subtitle: String?
    var artwork: String?
    var position: Double
    var duration: Double
    var updatedAt: Date
    var kind: MediaKind
    var playlistID: UUID

    var key: String {
        if let episodeID { return itemID + "|" + episodeID }
        return itemID
    }

    var fraction: Double {
        guard duration > 1 else { return 0 }
        return min(max(position / duration, 0), 1)
    }

    var isFinished: Bool {
        duration > 1 && position / duration >= 0.95
    }

    var remaining: Double {
        max(duration - position, 0)
    }
}

// MARK: - EPG

struct EPGProgram: Codable, Hashable, Identifiable, Sendable {
    var id: String
    var title: String
    var plot: String?
    var start: Date
    var end: Date

    var isNow: Bool {
        let now = Date()
        return now >= start && now < end
    }

    var timeRange: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm"
        return "\(formatter.string(from: start)) – \(formatter.string(from: end))"
    }
}

// MARK: - Metadata

struct EnrichedMetadata: Codable, Hashable, Sendable {
    var tmdbID: Int?
    var imdbID: String?
    var overview: String?
    var posterURL: String?
    var backdropURL: String?
    var tmdbRating: Double?
    var imdbRating: Double?
    var rottenRating: Double?
    var genres: [String] = []
    var cast: [String] = []
    var runtime: Int?

    var bestRating: Double? { imdbRating ?? tmdbRating }
}

// MARK: - Playback navigation

struct PlaybackRequest: Identifiable, Sendable {
    let id = UUID()
    var itemID: String
    var episodeID: String?
    var episodes: [Episode] = []
    var startAt: Double?
}

enum AppSection: String, CaseIterable, Hashable, Sendable {
    case home
    case live
    case movies
    case series
    case search
    case settings

    var titleKey: String { "nav.\(rawValue)" }

    var symbol: String {
        switch self {
        case .home: return "house.fill"
        case .live: return "dot.radiowaves.left.and.right"
        case .movies: return "film.fill"
        case .series: return "rectangle.stack.fill"
        case .search: return "magnifyingglass"
        case .settings: return "gearshape.fill"
        }
    }
}
