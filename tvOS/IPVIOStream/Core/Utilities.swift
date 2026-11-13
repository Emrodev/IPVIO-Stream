import Foundation

// MARK: - Stable identifiers

enum StableID {
    static func hash(_ value: String) -> String {
        var h: UInt64 = 14_695_981_039_346_656_037
        for byte in value.utf8 {
            h ^= UInt64(byte)
            h &*= 1_099_511_628_211
        }
        return String(h, radix: 16)
    }

    static func item(playlistID: UUID, kind: MediaKind, key: String) -> String {
        "\(playlistID.uuidString.lowercased())|\(kind.rawValue)|\(hash(key))"
    }
}

// MARK: - Regular expressions

enum RegexTool {
    static func firstMatch(_ pattern: String, in text: String, options: NSRegularExpression.Options = [.caseInsensitive]) -> [String]? {
        guard let regex = try? NSRegularExpression(pattern: pattern, options: options) else { return nil }
        let range = NSRange(text.startIndex..<text.endIndex, in: text)
        guard let match = regex.firstMatch(in: text, options: [], range: range) else { return nil }
        return (0..<match.numberOfRanges).map { index in
            let r = match.range(at: index)
            guard let swiftRange = Range(r, in: text) else { return "" }
            return String(text[swiftRange])
        }
    }

    static func allMatches(_ pattern: String, in text: String, options: NSRegularExpression.Options = [.caseInsensitive]) -> [[String]] {
        guard let regex = try? NSRegularExpression(pattern: pattern, options: options) else { return [] }
        let range = NSRange(text.startIndex..<text.endIndex, in: text)
        return regex.matches(in: text, options: [], range: range).map { match in
            (0..<match.numberOfRanges).map { index in
                let r = match.range(at: index)
                guard let swiftRange = Range(r, in: text) else { return "" }
                return String(text[swiftRange])
            }
        }
    }
}

// MARK: - Text

extension String {
    var trimmed: String { trimmingCharacters(in: .whitespacesAndNewlines) }

    var nilIfEmpty: String? { trimmed.isEmpty ? nil : trimmed }

    var htmlDecoded: String {
        var value = self
        let entities: [String: String] = [
            "&amp;": "&", "&quot;": "\"", "&apos;": "'", "&#39;": "'",
            "&lt;": "<", "&gt;": ">", "&nbsp;": " ", "&#x27;": "'",
        ]
        for (entity, replacement) in entities {
            value = value.replacingOccurrences(of: entity, with: replacement, options: .caseInsensitive)
        }
        return value
    }
}

enum TimeText {
    static func clock(_ seconds: Double) -> String {
        guard seconds.isFinite, seconds >= 0 else { return "0:00" }
        let total = Int(seconds.rounded(.down))
        let h = total / 3600
        let m = (total % 3600) / 60
        let s = total % 60
        if h > 0 {
            return String(format: "%d:%02d:%02d", h, m, s)
        }
        return String(format: "%d:%02d", m, s)
    }

    static func duration(_ seconds: Int?) -> String? {
        guard let seconds, seconds > 0 else { return nil }
        let h = seconds / 3600
        let m = (seconds % 3600) / 60
        if h > 0 { return L.f("time.hoursMinutes", h, m) }
        return L.f("time.minutes", m)
    }

    static func relative(_ date: Date, lang: Lang) -> String {
        let formatter = RelativeDateTimeFormatter()
        formatter.locale = Locale(identifier: lang == .ar ? "ar" : "en")
        formatter.unitsStyle = .short
        return formatter.localizedString(for: date, relativeTo: Date())
    }
}

// MARK: - URL helpers

enum URLTool {
    static func normalized(_ raw: String, defaultScheme: String = "http") -> URL? {
        var value = raw.trimmed
        guard !value.isEmpty else { return nil }
        if !value.contains("://") {
            value = defaultScheme + "://" + value
        }
        guard let components = URLComponents(string: value), let host = components.host, !host.isEmpty else {
            return nil
        }
        return components.url
    }

    static func join(_ base: String, _ path: String) -> URL? {
        let cleanBase = base.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        let cleanPath = path.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
        return URL(string: cleanBase + "/" + cleanPath)
    }

    static func encodedPathSegment(_ value: String) -> String {
        var allowed = CharacterSet.urlPathAllowed
        allowed.remove(charactersIn: "/?#%")
        return value.addingPercentEncoding(withAllowedCharacters: allowed) ?? value
    }

    static func host(of value: String?) -> String? {
        guard let value, let url = URL(string: value) else { return nil }
        return url.host
    }
}

// MARK: - Misc

enum AppInfo {
    static var version: String {
        let short = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
        let build = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
        return "\(short) (\(build))"
    }

    static var displayName: String {
        Bundle.main.infoDictionary?["CFBundleDisplayName"] as? String ?? "IPVIO Stream"
    }
}

extension Array where Element == MediaItem {
    func sortedByTitle() -> [MediaItem] {
        sorted { $0.stableSortTitle.localizedStandardCompare($1.stableSortTitle) == .orderedAscending }
    }
}
