import Foundation

// MARK: - M3U parser

enum M3UParser {
    private struct Pending {
        var info: String
        var name: String
        var group: String
        var logo: String?
        var tvgID: String?
        var catchupDays: Int?
    }

    private struct SeriesAccumulator {
        var title: String
        var logo: String?
        var group: String
        var quality: String?
        var language: String
        var episodes: [Episode] = []
    }

    private static let seriesPattern = "^(.*?)[\\s._-]*[Ss](\\d{1,2})[\\s._-]*[Ee](\\d{1,4})"
    private static let vodGroupPattern = "\\b(film|movie|vod|kino|cinema)\\b"

    static func parse(
        text: String,
        playlistID: UUID,
        progress: (@Sendable (Double, String) -> Void)? = nil
    ) throws -> LoadedPlaylist {
        let cleanText = text.hasPrefix("\u{FEFF}") ? String(text.dropFirst()) : text
        let lines = cleanText.split(whereSeparator: { $0 == "\n" || $0 == "\r" })
        guard !lines.isEmpty else { throw PlaylistError.empty }

        var items: [MediaItem] = []
        var series: [String: SeriesAccumulator] = [:]
        var seriesOrder: [String] = []
        var epgURLs: [String] = []
        var pending: Pending?
        var counts: [MediaKind: Int] = [.live: 0, .movie: 0, .series: 0]
        var lineNumber = 0
        let languageCache = ContentLanguageCache()

        func progressUpdate(_ value: Double, _ key: String) {
            progress?(value, L.t(key))
        }

        for rawLine in lines {
            lineNumber += 1
            if lineNumber % 4000 == 0 {
                progress?(min(0.95, 0.6 + Double(lineNumber) / Double(lines.count) * 0.35), L.t("load.parse"))
            }
            let line = String(rawLine).trimmingCharacters(in: .whitespaces)
            guard !line.isEmpty else { continue }

            if line.hasPrefix("#EXTM3U") {
                for key in ["url-tvg", "x-tvg-url"] {
                    if let value = attribute(key, in: line) {
                        epgURLs += value.split(separator: ",")
                            .map { String($0).trimmed }
                            .filter { $0.hasPrefix("http") }
                    }
                }
                continue
            }

            if line.hasPrefix("#EXTINF") {
                let attrsAndTitle = String(line.dropFirst(7))
                let title = titlePart(of: attrsAndTitle)
                var group = attribute("group-title", in: line)?.nilIfEmpty ?? "Other"
                if let tvgGroup = attribute("tvg-group", in: line)?.nilIfEmpty, group == "Other" {
                    group = tvgGroup
                }
                pending = Pending(
                    info: line,
                    name: title,
                    group: group,
                    logo: httpURL(attribute("tvg-logo", in: line)),
                    tvgID: attribute("tvg-id", in: line)?.nilIfEmpty,
                    catchupDays: intValue(attribute("catchup-days", in: line))
                )
                continue
            }

            if line.hasPrefix("#EXTGRP:") {
                if var value = pending {
                    value.group = String(line.dropFirst(8)).trimmed.nilIfEmpty ?? value.group
                    pending = value
                }
                continue
            }

            if line.hasPrefix("#") { continue }

            guard let entry = pending else { continue }
            pending = nil
            if TitleLogic.isSeparator(entry.name) { continue }

            let url = line
            guard url.contains("://") || url.hasPrefix("rtmp") else { continue }

            let isSeriesURL = url.contains("/series/")
            let episodeMatch = RegexTool.firstMatch(seriesPattern, in: entry.name)
            let isVODURL = url.contains("/movie/")
            let isVideo = isVideoFile(url)
            let vodGroup = entry.group.range(of: vodGroupPattern, options: [.regularExpression, .caseInsensitive]) != nil

            let kind: MediaKind
            if episodeMatch != nil || isSeriesURL {
                kind = .series
            } else if isVODURL || isVideo || vodGroup {
                kind = .movie
            } else {
                kind = .live
            }

            let language = languageCache.detect(group: entry.group, name: entry.name, isLive: kind == .live)
            let quality = QualityLogic.detect(name: entry.name, group: entry.group)

            switch kind {
            case .series:
                let base = TitleLogic.seriesBase(entry.name, episodeMatch: episodeMatch)
                let key = base.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: Locale(identifier: "en"))
                let season = intValue(episodeMatch?[safe: 2]) ?? 1
                let number = intValue(episodeMatch?[safe: 3]) ?? ((series[key]?.episodes.count ?? 0) + 1)
                let episodeTitle = TitleLogic.episodeTitle(rawName: entry.name, match: episodeMatch, number: number)
                let labelParts: [String?] = [
                    language == ContentLanguage.unknown ? nil : language.uppercased(),
                    quality,
                    entry.group.prefix(18).description,
                ]
                let variant = Variant(
                    url: url,
                    label: labelParts.compactMap { $0 }.filter { !$0.isEmpty }.joined(separator: " · "),
                    quality: quality,
                    language: language == ContentLanguage.unknown ? nil : language
                )
                var acc = series[key] ?? SeriesAccumulator(
                    title: base,
                    logo: entry.logo,
                    group: entry.group,
                    quality: quality,
                    language: language
                )
                if acc.logo == nil { acc.logo = entry.logo }
                if acc.quality == nil { acc.quality = quality }
                if let index = acc.episodes.firstIndex(where: { $0.season == season && $0.number == number }) {
                    if acc.episodes[index].variants.isEmpty {
                        let old = acc.episodes[index]
                        acc.episodes[index].variants = [
                            Variant(url: old.url, label: old.title, quality: old.quality, language: old.language)
                        ]
                    }
                    if !acc.episodes[index].variants.contains(where: { $0.url == variant.url }) {
                        acc.episodes[index].variants.append(variant)
                    }
                } else {
                    acc.episodes.append(Episode(
                        season: season,
                        number: number,
                        title: episodeTitle,
                        url: url,
                        quality: quality,
                        language: language == ContentLanguage.unknown ? nil : language
                    ))
                }
                if series[key] == nil { seriesOrder.append(key) }
                series[key] = acc
            case .movie:
                let parsed = TitleLogic.movie(entry.name)
                let key = url + "|" + parsed.title.lowercased()
                items.append(MediaItem(
                    id: StableID.item(playlistID: playlistID, kind: .movie, key: key),
                    playlistID: playlistID,
                    kind: .movie,
                    title: parsed.title,
                    year: parsed.year,
                    url: url,
                    logo: entry.logo,
                    group: entry.group,
                    plot: nil,
                    rating: nil,
                    duration: nil,
                    tvgID: entry.tvgID,
                    language: language,
                    quality: quality
                ))
                counts[.movie, default: 0] += 1
            case .live:
                let title = TitleLogic.clean(entry.name)
                let key = url + "|" + title.lowercased()
                items.append(MediaItem(
                    id: StableID.item(playlistID: playlistID, kind: .live, key: key),
                    playlistID: playlistID,
                    kind: .live,
                    title: title,
                    year: nil,
                    url: url,
                    logo: entry.logo,
                    group: entry.group,
                    plot: nil,
                    rating: nil,
                    duration: nil,
                    tvgID: entry.tvgID,
                    language: language,
                    quality: quality,
                    catchupDays: entry.catchupDays
                ))
                counts[.live, default: 0] += 1
            }
        }

        for key in seriesOrder {
            guard var acc = series[key] else { continue }
            acc.episodes.sort { lhs, rhs in
                lhs.season == rhs.season ? lhs.number < rhs.number : lhs.season < rhs.season
            }
            guard let first = acc.episodes.first else { continue }
            items.append(MediaItem(
                id: StableID.item(playlistID: playlistID, kind: .series, key: acc.title.lowercased()),
                playlistID: playlistID,
                kind: .series,
                title: acc.title,
                year: nil,
                url: first.url,
                logo: acc.logo,
                group: acc.group,
                plot: nil,
                rating: nil,
                duration: nil,
                tvgID: nil,
                language: acc.language,
                quality: acc.quality,
                episodes: acc.episodes
            ))
            counts[.series, default: 0] += 1
        }

        guard !items.isEmpty else { throw PlaylistError.empty }
        progress?(1, L.t("load.finish"))
        return LoadedPlaylist(items: items, epgURLs: epgURLs, counts: counts)
    }

    // MARK: Helpers

    private static func attribute(_ key: String, in line: String) -> String? {
        guard let startRange = line.range(of: key + "=\"", options: .caseInsensitive) else { return nil }
        let rest = line[startRange.upperBound...]
        guard let end = rest.firstIndex(of: "\"") else { return nil }
        return String(rest[..<end]).trimmed
    }

    private static func titlePart(of value: String) -> String {
        var inQuotes = false
        var index = value.startIndex
        while index < value.endIndex {
            let character = value[index]
            if character == "\"" {
                inQuotes.toggle()
            } else if character == "," && !inQuotes {
                let title = String(value[value.index(after: index)...]).trimmed
                return title.isEmpty ? "Unnamed" : title
            }
            index = value.index(after: index)
        }
        // No comma: some providers omit the title entirely.
        return "Unnamed"
    }

    private static func isVideoFile(_ url: String) -> Bool {
        guard let components = URLComponents(string: url) else { return false }
        let ext = (components.path as NSString).pathExtension.lowercased()
        return ["mp4", "mkv", "avi", "mov", "m4v", "wmv", "flv", "webm", "mpg", "mpeg"].contains(ext)
    }

    private static func httpURL(_ raw: String?) -> String? {
        guard let raw = raw?.trimmed, raw.lowercased().hasPrefix("http") else { return nil }
        return raw
    }

    private static func intValue(_ raw: String?) -> Int? {
        guard let raw else { return nil }
        return Int(raw.trimmed) ?? Int(Double(raw.trimmed) ?? 0)
    }
}

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}
