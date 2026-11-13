import Foundation

enum AppLanguage: String, Codable, CaseIterable, Hashable, Sendable {
    case system
    case arabic
    case english

    var titleKey: String {
        switch self {
        case .system: return "language.system"
        case .arabic: return "language.arabic"
        case .english: return "language.english"
        }
    }

    var resolved: Lang {
        switch self {
        case .arabic: return .ar
        case .english: return .en
        case .system:
            let preferred = Locale.preferredLanguages.first?.lowercased() ?? "en"
            return preferred.hasPrefix("ar") ? .ar : .en
        }
    }

    var locale: Locale {
        Locale(identifier: resolved == .ar ? "ar" : "en")
    }
}

enum QualityCap: String, Codable, CaseIterable, Hashable, Sendable {
    case auto
    case hd
    case fhd
    case uhd

    var titleKey: String {
        switch self {
        case .auto: return "quality.auto"
        case .hd: return "quality.hd"
        case .fhd: return "quality.fhd"
        case .uhd: return "quality.uhd"
        }
    }

    var rank: Int {
        switch self {
        case .auto: return 4
        case .hd: return 2
        case .fhd: return 3
        case .uhd: return 4
        }
    }
}

struct AppSettings: Codable, Sendable {
    var appLanguage: AppLanguage = .system
    var qualityCap: QualityCap = .auto
    var autoPlayNext: Bool = true
    var resumePlayback: Bool = true
    var preferFFmpeg: Bool = true
    var preferredAudioLanguage: String = "ar"
    var preferredSubtitleLanguage: String = ""
    var subtitlesEnabled: Bool = false
    var languageFilter: Set<String> = []
    var showLiveEPG: Bool = true
    var tmdbKey: String = ""
    var omdbKey: String = ""
    var bufferSeconds: Int = 15

    var resolvedLanguage: Lang { appLanguage.resolved }
}

struct PersistedState: Codable, Sendable {
    var playlists: [Playlist] = []
    var settings: AppSettings = AppSettings()
    var favorites: Set<String> = []
    var myList: Set<String> = []
    var progress: [String: WatchProgress] = [:]
    var lastSection: String = AppSection.home.rawValue

    static let empty = PersistedState()
}

struct LoadJob: Equatable, Sendable {
    var playlistName: String
    var progress: Double
    var status: String
    var isIndeterminate: Bool = false
}
