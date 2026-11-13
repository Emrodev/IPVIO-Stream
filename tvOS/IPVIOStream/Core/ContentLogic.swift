import Foundation

// MARK: - Language detection

enum ContentLanguage {
    static let unknown = "?"
    static let multi = "multi"

    static let common: [String] = [
        "ar", "en", "fr", "de", "tr", "es", "it", "nl", "pt", "ru",
        "hi", "ur", "fa", "pl", "ro", "el", "sv", "da", "no", "fi",
        "cs", "hu", "bg", "uk", "he", "sq", "hr", "sr", "bs", "ms", "id", "th", "vi", "tl",
    ]

    private static let aliases: [String: String] = [
        "ara": "ar", "arab": "ar", "ar": "ar",
        "eng": "en", "en": "en",
        "fre": "fr", "fra": "fr", "fr": "fr", "francais": "fr", "français": "fr",
        "ger": "de", "deu": "de", "de": "de", "german": "de", "deutsch": "de",
        "spa": "es", "es": "es", "spanish": "es", "espanol": "es", "español": "es",
        "ita": "it", "it": "it", "italian": "it",
        "dut": "nl", "nld": "nl", "nl": "nl", "dutch": "nl",
        "por": "pt", "pt": "pt", "portuguese": "pt", "brazil": "pt",
        "rus": "ru", "ru": "ru", "russian": "ru",
        "tur": "tr", "tr": "tr", "turkish": "tr", "türkçe": "tr", "turkce": "tr",
        "hin": "hi", "hi": "hi", "hindi": "hi",
        "urd": "ur", "ur": "ur", "urdu": "ur",
        "per": "fa", "fas": "fa", "fa": "fa", "farsi": "fa", "persian": "fa",
        "pol": "pl", "pl": "pl", "polish": "pl",
        "rum": "ro", "ron": "ro", "ro": "ro", "romanian": "ro",
        "gre": "el", "ell": "el", "el": "el", "greek": "el",
        "swe": "sv", "sv": "sv", "swedish": "sv",
        "dan": "da", "da": "da", "danish": "da",
        "nor": "no", "no": "no", "norwegian": "no",
        "fin": "fi", "fi": "fi", "finnish": "fi",
        "cze": "cs", "ces": "cs", "cs": "cs", "czech": "cs",
        "hun": "hu", "hu": "hu", "hungarian": "hu",
        "bul": "bg", "bg": "bg", "bulgarian": "bg",
        "ukr": "uk", "uk": "uk", "ukrainian": "uk",
        "heb": "he", "he": "he", "hebrew": "he",
        "alb": "sq", "sqi": "sq", "sq": "sq", "albanian": "sq",
        "hrv": "hr", "hr": "hr", "croatian": "hr",
        "srp": "sr", "sr": "sr", "serbian": "sr",
        "bos": "bs", "bs": "bs", "bosnian": "bs",
        "may": "ms", "msa": "ms", "ms": "ms", "malay": "ms",
        "ind": "id", "id": "id", "indonesian": "id",
        "tha": "th", "th": "th", "thai": "th",
        "vie": "vi", "vi": "vi", "vietnamese": "vi",
        "fil": "tl", "tgl": "tl", "tl": "tl", "tagalog": "tl",
        "multi": "multi", "mult": "multi", "multi-sub": "multi", "multisub": "multi",
    ]

    private static let wordMap: [String: [String]] = [
        "ar": ["arabic", "arab", "عربي", "عربية", "arabisch"],
        "en": ["english", "ingles", "anglais", "englisch"],
        "fr": ["french", "francais", "français", "franzosisch", "französisch"],
        "de": ["german", "deutsch", "allemand", "aleman"],
        "tr": ["turkish", "turkce", "türkçe", "turkisch"],
        "es": ["spanish", "espanol", "español", "espagnol", "spanisch"],
        "it": ["italian", "italiano", "italien", "italienisch"],
        "nl": ["dutch", "nederlands", "holland"],
        "pt": ["portuguese", "portugues", "português", "brasil", "brazil"],
        "ru": ["russian", "russki", "русский"],
        "hi": ["hindi", "हिन्दी"],
        "ur": ["urdu", "اردو"],
        "fa": ["farsi", "persian", "فارسی"],
        "pl": ["polish", "polski"],
        "ro": ["romanian", "romana", "română"],
        "el": ["greek", "ellinika", "ελληνικά"],
        "sv": ["swedish", "svenska"],
        "da": ["danish", "dansk"],
        "no": ["norwegian", "norsk"],
        "fi": ["finnish", "suomi"],
        "cs": ["czech", "cesky", "česky"],
        "hu": ["hungarian", "magyar"],
        "bg": ["bulgarian", "български"],
        "uk": ["ukrainian", "українська"],
        "he": ["hebrew", "עברית"],
        "sq": ["albanian", "shqip"],
        "hr": ["croatian", "hrvatski"],
        "sr": ["serbian", "srpski"],
        "bs": ["bosnian", "bosanski"],
        "ms": ["malay", "melayu"],
        "id": ["indonesian", "bahasa"],
        "th": ["thai", "ไทย"],
        "vi": ["vietnamese", "tiếng việt"],
        "tl": ["tagalog", "filipino"],
    ]

    static func normalize(_ raw: String?) -> String? {
        guard var code = raw?.lowercased().trimmed, !code.isEmpty else { return nil }
        code = code.replacingOccurrences(of: "[", with: "")
            .replacingOccurrences(of: "]", with: "")
            .replacingOccurrences(of: "|", with: "")
            .trimmed
        if let mapped = aliases[code] { return mapped }
        if code.count > 3 {
            for (alias, value) in aliases where alias.count > 3 && code.contains(alias) {
                return value
            }
        }
        return nil
    }

    static func detect(group: String, name: String, isLive: Bool = false) -> String {
        _ = isLive
        let combined = group + " " + name

        // "AR | Channel" / "EN: Channel" style prefixes.
        if let match = RegexTool.firstMatch("^\\s*([A-Za-z]{2,4})\\s*[|:]\\s*", in: name),
           match.count > 1, let code = normalize(match[1]) {
            return code
        }

        // Bracket / pipe tags.
        for pattern in ["\\[([A-Za-z]{2,4})\\]", "\\|\\s*([A-Za-z]{2,4})\\s*\\|", "\\(([A-Za-z]{2,4})\\)"] {
            if let match = RegexTool.firstMatch(pattern, in: combined), match.count > 1,
               let code = normalize(match[1]) {
                return code
            }
        }

        // Script detection is more reliable than words for Arabic / Cyrillic / Hindi.
        if name.range(of: "\\p{Arabic}", options: .regularExpression) != nil { return "ar" }
        if group.range(of: "\\p{Arabic}", options: .regularExpression) != nil { return "ar" }
        if name.range(of: "\\p{Cyrillic}", options: .regularExpression) != nil { return "ru" }
        if name.range(of: "\\p{Devanagari}", options: .regularExpression) != nil { return "hi" }

        let haystack = combined.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: Locale(identifier: "en"))
        for (code, words) in wordMap {
            for word in words {
                let needle = word.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: Locale(identifier: "en"))
                if haystack.range(of: "\\b" + NSRegularExpression.escapedPattern(for: needle) + "\\b", options: .regularExpression) != nil {
                    return code
                }
            }
        }
        return unknown
    }

    static func displayName(_ code: String) -> String {
        let key = "lang." + (code == unknown ? "unknown" : code)
        let value = L.t(key)
        return value == key ? code.uppercased() : value
    }

    static func isKnown(_ code: String) -> Bool {
        code == multi || code == unknown || common.contains(code)
    }
}

/// Caches the language per provider group. Large playlists repeat the same few
/// category names thousands of times; detecting the script/words once per group
/// keeps the import fast.
final class ContentLanguageCache: @unchecked Sendable {
    private var groups: [String: String] = [:]

    func detect(group: String, name: String, isLive: Bool = false) -> String {
        if let cached = groups[group], cached != ContentLanguage.unknown {
            return cached
        }
        let value = ContentLanguage.detect(group: group, name: name, isLive: isLive)
        if value != ContentLanguage.unknown {
            groups[group] = value
        }
        return value
    }
}

// MARK: - Quality

enum QualityLogic {
    static func detect(name: String, group: String = "") -> String? {
        let text = name + " " + group
        if text.range(of: "\\b(4K|UHD|2160p?)\\b", options: [.regularExpression, .caseInsensitive]) != nil {
            return "4K"
        }
        if text.range(of: "\\b(FHD|1080[pi]?)\\b", options: [.regularExpression, .caseInsensitive]) != nil {
            return "FHD"
        }
        if text.range(of: "\\b(HD|720p?)\\b", options: [.regularExpression, .caseInsensitive]) != nil {
            return "HD"
        }
        return nil
    }

    static func rank(_ quality: String?) -> Int {
        switch quality?.uppercased() {
        case "4K": return 4
        case "FHD": return 3
        case "HD": return 2
        default: return 1
        }
    }

    static func pick(from variants: [Variant], cap: QualityCap, language: String? = nil) -> Variant? {
        var candidates = variants.filter { !$0.url.trimmed.isEmpty }
        guard !candidates.isEmpty else { return nil }

        if let language, !language.isEmpty {
            let matching = candidates.filter {
                ($0.language ?? "").lowercased().hasPrefix(language.lowercased())
            }
            if !matching.isEmpty { candidates = matching }
        }

        let limited = candidates.filter { rank($0.quality) <= cap.rank }
        let pool = limited.isEmpty ? candidates : limited
        return pool.enumerated().sorted { lhs, rhs in
            let left = rank(lhs.element.quality)
            let right = rank(rhs.element.quality)
            if left != right { return left > right }
            return lhs.offset < rhs.offset
        }.first?.element
    }
}

// MARK: - Titles

enum TitleLogic {
    private static let qualityTags = "\\b(4K|UHD|FHD|HD|HEVC|H265|H264|1080p?|720p?|2160p?|MULTI|DUAL|SUB|DUB)\\b"

    static func clean(_ raw: String) -> String {
        var value = raw.htmlDecoded.trimmed
        value = value.replacingOccurrences(of: "^\\s*[A-Za-z]{2,4}\\s*[|:]\\s*", with: "", options: .regularExpression)
        value = value.replacingOccurrences(of: "^\\s*\\[[A-Za-z]{2,4}\\]\\s*", with: "", options: .regularExpression)
        value = value.replacingOccurrences(of: qualityTags, with: "", options: [.regularExpression, .caseInsensitive])
        value = value.replacingOccurrences(of: "\\s{2,}", with: " ", options: .regularExpression)
        value = value.trimmingCharacters(in: CharacterSet(charactersIn: " ._-|:"))
        return value.isEmpty ? raw.htmlDecoded.trimmed : value
    }

    static func movie(_ raw: String) -> (title: String, year: Int?) {
        let decoded = raw.htmlDecoded.trimmed
        var year: Int?
        for pattern in ["[(\\[]((?:19|20)\\d{2})[)\\]]", "\\b((?:19|20)\\d{2})\\s*$"] {
            if let match = RegexTool.firstMatch(pattern, in: decoded), match.count > 1, let value = Int(match[1]) {
                year = value
                break
            }
        }
        var title = decoded
        if year != nil {
            title = title.replacingOccurrences(of: "[(\\[]((?:19|20)\\d{2})[)\\]]", with: "", options: .regularExpression)
            title = title.replacingOccurrences(of: "\\s*((?:19|20)\\d{2})\\s*$", with: "", options: .regularExpression)
        }
        title = clean(title)
        return (title.isEmpty ? decoded : title, year)
    }

    static func seriesBase(_ raw: String, episodeMatch: [String]?) -> String {
        let base = episodeMatch?.count ?? 0 > 1 ? episodeMatch![1] : raw
        var title = clean(base)
        title = title.replacingOccurrences(of: "[(\\[]((?:19|20)\\d{2})[)\\]]", with: "", options: .regularExpression)
        title = title.trimmingCharacters(in: CharacterSet(charactersIn: " ._-|:"))
        return title.isEmpty ? clean(raw) : title
    }

    static func episodeTitle(rawName: String, match: [String]?, number: Int) -> String {
        guard let match, match.count > 3 else { return "Episode \(number)" }
        let full = match[0]
        guard let range = rawName.range(of: full) else { return "Episode \(number)" }
        var suffix = String(rawName[range.upperBound...])
        suffix = suffix.replacingOccurrences(of: "^[\\s._-]+", with: "", options: .regularExpression)
        suffix = suffix.replacingOccurrences(of: qualityTags, with: "", options: [.regularExpression, .caseInsensitive])
        suffix = suffix.trimmingCharacters(in: CharacterSet(charactersIn: " ._-|:"))
        return suffix.isEmpty ? "Episode \(number)" : suffix
    }

    static func isSeparator(_ name: String) -> Bool {
        let value = name.trimmed
        return value.isEmpty || value.hasPrefix("***") || value.hasPrefix("---") ||
            value.hasPrefix("===") || value.hasPrefix("###") || value.hasPrefix("___")
    }
}

// MARK: - Playlist input

enum PlaylistInputError: LocalizedError {
    case invalidURL
    case missingCredentials
    case unsupported

    var errorDescription: String? {
        switch self {
        case .invalidURL: return L.t("error.invalidURL")
        case .missingCredentials: return L.t("error.missingCredentials")
        case .unsupported: return L.t("error.unsupportedSource")
        }
    }
}

enum PlaylistInput {
    /// Parses a full `get.php` / `player_api.php` URL and extracts server, user and password.
    static func xtreamAccount(from raw: String) -> XtreamAccount? {
        guard let url = URLTool.normalized(raw),
              let components = URLComponents(url: url, resolvingAgainstBaseURL: false) else { return nil }
        let file = components.path.split(separator: "/").last?.lowercased() ?? ""
        guard file == "get.php" || file == "player_api.php" else { return nil }
        let items = components.queryItems ?? []
        func query(_ name: String) -> String? {
            items.first { $0.name.lowercased() == name }?.value?.trimmed.nilIfEmpty
        }
        guard let user = query("username"), let pass = query("password") else { return nil }

        var base = URLComponents()
        base.scheme = components.scheme
        base.host = components.host
        base.port = components.port
        let directory = (components.path as NSString).deletingLastPathComponent
        if !directory.isEmpty, directory != "/" { base.path = directory }
        guard let baseURL = base.url else { return nil }
        return XtreamAccount(base: normalizeBase(baseURL.absoluteString), username: user, password: pass)
    }

    static func normalizeBase(_ raw: String) -> String {
        var value = raw.trimmed
        if !value.contains("://") { value = "http://" + value }
        if let range = value.range(of: "/(player_api|get)\\.php", options: [.regularExpression, .caseInsensitive]) {
            value = String(value[..<range.lowerBound])
        }
        if let query = value.firstIndex(of: "?") { value = String(value[..<query]) }
        while value.hasSuffix("/") { value.removeLast() }
        return value
    }

    static func makeSource(mode: PlaylistMode, name: String, url: String, server: String, username: String, password: String) throws -> PlaylistSource {
        _ = name
        switch mode {
        case .xtream:
            let cleanServer = normalizeBase(server)
            guard let parsed = URLTool.normalized(cleanServer), let host = parsed.host, !host.isEmpty else {
                throw PlaylistInputError.invalidURL
            }
            guard !username.trimmed.isEmpty, !password.isEmpty else {
                throw PlaylistInputError.missingCredentials
            }
            return .xtream(XtreamAccount(base: cleanServer, username: username.trimmed, password: password))
        case .m3u:
            guard let parsed = URLTool.normalized(url), let scheme = parsed.scheme,
                  scheme.lowercased() == "http" || scheme.lowercased() == "https" else {
                throw PlaylistInputError.invalidURL
            }
            if let account = xtreamAccount(from: url) {
                return .xtream(account)
            }
            return .m3u(url: parsed.absoluteString)
        }
    }

    static func suggestedName(for source: PlaylistSource) -> String {
        source.hostLabel
    }
}

enum PlaylistMode: String, CaseIterable, Identifiable, Hashable, Sendable {
    case xtream
    case m3u

    var id: String { rawValue }
    var titleKey: String { self == .xtream ? "add.xtream" : "add.m3u" }
}
