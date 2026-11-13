import Foundation

enum XtreamClient {
    static let userAgent = "VLC/3.0.21 LibVLC/3.0.21"

    // MARK: - URL construction

    static func apiURL(account: XtreamAccount, action: String? = nil, extra: [URLQueryItem] = []) -> URL? {
        guard var components = URLComponents(string: account.base) else { return nil }
        var path = components.path
        if path.isEmpty { path = "/" }
        path = (path as NSString).appendingPathComponent("player_api.php")
        components.path = path
        var query = [
            URLQueryItem(name: "username", value: account.username),
            URLQueryItem(name: "password", value: account.password),
        ]
        if let action { query.append(URLQueryItem(name: "action", value: action)) }
        query.append(contentsOf: extra)
        components.queryItems = query
        return components.url
    }

    static func streamURL(account: XtreamAccount, kind: MediaKind, id: String, fileExtension ext: String?) -> URL? {
        let folder: String
        switch kind {
        case .live: folder = "live"
        case .movie: folder = "movie"
        case .series: folder = "series"
        }
        let user = URLTool.encodedPathSegment(account.username)
        let pass = URLTool.encodedPathSegment(account.password)
        let safeID = URLTool.encodedPathSegment(id)
        let safeExt = sanitizeExtension(ext, fallback: "mp4")
        let base = account.base.hasSuffix("/") ? String(account.base.dropLast()) : account.base
        return URL(string: "\(base)/\(folder)/\(user)/\(pass)/\(safeID).\(safeExt)")
    }

    static func liveURL(account: XtreamAccount, streamID: String, hls: Bool) -> URL? {
        let user = URLTool.encodedPathSegment(account.username)
        let pass = URLTool.encodedPathSegment(account.password)
        let safeID = URLTool.encodedPathSegment(streamID)
        let base = account.base.hasSuffix("/") ? String(account.base.dropLast()) : account.base
        return URL(string: "\(base)/live/\(user)/\(pass)/\(safeID).\(hls ? "m3u8" : "ts")")
    }

    private static func sanitizeExtension(_ ext: String?, fallback: String) -> String {
        guard let value = ext?.trimmed.trimmingCharacters(in: CharacterSet(charactersIn: ".")).lowercased(),
              value.count <= 5, !value.isEmpty,
              value.range(of: "^[a-z0-9]+$", options: .regularExpression) != nil else {
            return fallback
        }
        return value
    }

    private static func epgURL(account: XtreamAccount) -> URL? {
        guard var components = URLComponents(string: account.base) else { return nil }
        var path = components.path
        if path.isEmpty { path = "/" }
        path = (path as NSString).appendingPathComponent("xmltv.php")
        components.path = path
        components.queryItems = [
            URLQueryItem(name: "username", value: account.username),
            URLQueryItem(name: "password", value: account.password),
        ]
        return components.url
    }

    // MARK: - Networking

    private static func fetch<T: Decodable>(_ type: T.Type, from url: URL) async throws -> T {
        var request = URLRequest(url: url)
        request.timeoutInterval = 120
        request.cachePolicy = .reloadIgnoringLocalCacheData
        request.setValue(userAgent, forHTTPHeaderField: "User-Agent")
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await URLSession.shared.data(for: request)
        } catch is CancellationError {
            throw PlaylistError.cancelled
        } catch let error as URLError where error.code == .cancelled {
            throw PlaylistError.cancelled
        } catch {
            throw PlaylistError.network(error.localizedDescription)
        }

        if let http = response as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
            throw PlaylistError.http(http.statusCode)
        }

        do {
            return try JSONDecoder().decode(T.self, from: data)
        } catch {
            throw PlaylistError.decoding
        }
    }

    static func login(account: XtreamAccount) async throws -> XtreamLoginResponse {
        guard let url = apiURL(account: account) else { throw PlaylistError.invalidURL }
        return try await fetch(XtreamLoginResponse.self, from: url)
    }

    static func categories(account: XtreamAccount, action: String) async -> [String: String] {
        guard let url = apiURL(account: account, action: action),
              let list = try? await fetch([XtreamCategory].self, from: url) else {
            return [:]
        }
        var map: [String: String] = [:]
        for category in list {
            guard let id = category.categoryId?.value.trimmed.nilIfEmpty,
                  let name = category.categoryName?.trimmed.nilIfEmpty else { continue }
            map[id] = name
        }
        return map
    }

    private static func streams(account: XtreamAccount, action: String) async throws -> [XtreamStream] {
        guard let url = apiURL(account: account, action: action) else { throw PlaylistError.invalidURL }
        return try await fetch([XtreamStream].self, from: url)
    }

    private static func seriesList(account: XtreamAccount) async throws -> [XtreamSeries] {
        guard let url = apiURL(account: account, action: "get_series") else { throw PlaylistError.invalidURL }
        return try await fetch([XtreamSeries].self, from: url)
    }

    // MARK: - Full import

    static func load(
        account: XtreamAccount,
        playlistID: UUID,
        progress: @escaping @Sendable (Double, String) -> Void
    ) async throws -> LoadedPlaylist {
        progress(0.02, L.t("load.login"))
        let loginResponse = try await login(account: account)
        try Task.checkCancellation()

        if let user = loginResponse.userInfo {
            let auth = user.auth?.value
            let status = user.status?.lowercased()
            if auth == "0" || ["disabled", "banned", "expired"].contains(status ?? "") {
                throw PlaylistError.loginRejected
            }
        } else {
            throw PlaylistError.loginRejected
        }

        progress(0.06, L.t("load.categories"))
        async let liveCategories = categories(account: account, action: "get_live_categories")
        async let vodCategories = categories(account: account, action: "get_vod_categories")
        async let seriesCategories = categories(account: account, action: "get_series_categories")
        let (liveCats, vodCats, seriesCats) = await (liveCategories, vodCategories, seriesCategories)
        try Task.checkCancellation()

        var lastError: Error?
        let languageCache = ContentLanguageCache()

        progress(0.12, L.t("load.live"))
        var liveItems: [MediaItem] = []
        do {
            let live = try await streams(account: account, action: "get_live_streams")
            liveItems = live.compactMap { makeLive($0, account: account, categories: liveCats, playlistID: playlistID, languageCache: languageCache) }
        } catch {
            if error is CancellationError { throw error }
            lastError = error
        }
        try Task.checkCancellation()

        progress(0.42, L.t("load.movies"))
        var movieItems: [MediaItem] = []
        do {
            let movies = try await streams(account: account, action: "get_vod_streams")
            movieItems = movies.compactMap { makeMovie($0, account: account, categories: vodCats, playlistID: playlistID, languageCache: languageCache) }
        } catch {
            if error is CancellationError { throw error }
            lastError = error
        }
        try Task.checkCancellation()

        progress(0.74, L.t("load.series"))
        var seriesItems: [MediaItem] = []
        do {
            let list = try await seriesList(account: account)
            seriesItems = list.compactMap { makeSeries($0, categories: seriesCats, playlistID: playlistID, languageCache: languageCache) }
        } catch {
            if error is CancellationError { throw error }
            lastError = error
        }
        try Task.checkCancellation()

        let items = liveItems + movieItems + seriesItems
        guard !items.isEmpty else {
            if let lastError { throw lastError }
            throw PlaylistError.empty
        }

        progress(0.96, L.t("load.finish"))
        let counts: [MediaKind: Int] = [
            .live: liveItems.count,
            .movie: movieItems.count,
            .series: seriesItems.count,
        ]
        return LoadedPlaylist(
            items: items,
            epgURLs: epgURL(account: account).map { [$0.absoluteString] } ?? [],
            counts: counts
        )
    }

    // MARK: - Mapping

    private static func makeLive(
        _ stream: XtreamStream,
        account: XtreamAccount,
        categories: [String: String],
        playlistID: UUID,
        languageCache: ContentLanguageCache
    ) -> MediaItem? {
        guard let id = stream.streamId?.value.trimmed.nilIfEmpty,
              let name = stream.name?.trimmed.nilIfEmpty else { return nil }
        let title = TitleLogic.clean(name)
        let group = categoryName(stream.categoryId, categories)
        let isHLS = (stream.streamType ?? "").lowercased().contains("hls")
        guard let url = liveURL(account: account, streamID: id, hls: isHLS) else { return nil }
        let days = stream.tvArchive?.bool == true ? max(1, stream.tvArchiveDuration?.int ?? 1) : nil
        return MediaItem(
            id: StableID.item(playlistID: playlistID, kind: .live, key: "xc:" + id),
            playlistID: playlistID,
            kind: .live,
            title: title,
            year: nil,
            url: url.absoluteString,
            logo: httpURL(stream.streamIcon),
            group: group,
            plot: nil,
            rating: nil,
            duration: nil,
            tvgID: stream.epgChannelId?.trimmed.nilIfEmpty,
            language: languageCache.detect(group: group, name: name, isLive: true),
            quality: QualityLogic.detect(name: name, group: group),
            xtream: XtreamIDs(streamID: id, seriesID: nil, containerExtension: nil),
            catchupDays: days
        )
    }

    private static func makeMovie(
        _ stream: XtreamStream,
        account: XtreamAccount,
        categories: [String: String],
        playlistID: UUID,
        languageCache: ContentLanguageCache
    ) -> MediaItem? {
        guard let id = stream.streamId?.value.trimmed.nilIfEmpty,
              let name = stream.name?.trimmed.nilIfEmpty else { return nil }
        let ext = sanitizeExtension(stream.containerExtension, fallback: "mp4")
        guard let url = streamURL(account: account, kind: .movie, id: id, fileExtension: ext) else { return nil }
        let parsed = TitleLogic.movie(name)
        let group = categoryName(stream.categoryId, categories)
        let year = parsed.year ?? yearFrom(stream.year?.value) ?? yearFrom(stream.releaseDate)
        return MediaItem(
            id: StableID.item(playlistID: playlistID, kind: .movie, key: "xc:" + id),
            playlistID: playlistID,
            kind: .movie,
            title: parsed.title,
            year: year,
            url: url.absoluteString,
            logo: httpURL(stream.streamIcon),
            group: group,
            plot: stream.plot?.trimmed.nilIfEmpty,
            rating: rating(stream.rating, fallback: stream.rating5based),
            duration: durationSeconds(stream.duration?.value),
            tvgID: nil,
            language: languageCache.detect(group: group, name: name),
            quality: QualityLogic.detect(name: name, group: group),
            xtream: XtreamIDs(streamID: id, seriesID: nil, containerExtension: ext)
        )
    }

    private static func makeSeries(
        _ series: XtreamSeries,
        categories: [String: String],
        playlistID: UUID,
        languageCache: ContentLanguageCache
    ) -> MediaItem? {
        guard let id = series.seriesId?.value.trimmed.nilIfEmpty,
              let name = series.name?.trimmed.nilIfEmpty else { return nil }
        let parsed = TitleLogic.movie(name)
        let group = categoryName(series.categoryId, categories)
        let year = yearFrom(series.releaseDate)
        return MediaItem(
            id: StableID.item(playlistID: playlistID, kind: .series, key: "xc:" + id),
            playlistID: playlistID,
            kind: .series,
            title: parsed.title,
            year: year,
            url: "",
            logo: httpURL(series.cover),
            group: group,
            plot: series.plot?.trimmed.nilIfEmpty,
            rating: rating(series.rating, fallback: nil),
            duration: series.episodeRunTime?.int.map { $0 * 60 },
            tvgID: nil,
            language: languageCache.detect(group: group, name: name),
            quality: QualityLogic.detect(name: name, group: group),
            xtream: XtreamIDs(streamID: nil, seriesID: id, containerExtension: nil)
        )
    }

    // MARK: - Episodes

    static func episodes(account: XtreamAccount, seriesID: String) async throws -> [Episode] {
        guard let url = apiURL(
            account: account,
            action: "get_series_info",
            extra: [URLQueryItem(name: "series_id", value: seriesID)]
        ) else {
            throw PlaylistError.invalidURL
        }

        let response: XtreamSeriesInfo
        do {
            response = try await fetch(XtreamSeriesInfo.self, from: url)
        } catch let error as PlaylistError {
            if case .cancelled = error { throw error }
            throw PlaylistError.episodes
        } catch {
            throw PlaylistError.episodes
        }

        var seen: Set<String> = []
        var result: [Episode] = []

        for (seasonKey, list) in response.episodes {
            let seasonFromKey = Int(seasonKey) ?? 1
            for episode in list {
                guard let id = episode.id?.value.trimmed.nilIfEmpty else { continue }
                let season = episode.season?.int ?? seasonFromKey
                let number = episode.episodeNumber
                guard number > 0 else { continue }
                let key = "\(season)-\(number)"
                guard !seen.contains(key) else { continue }
                seen.insert(key)

                let ext = sanitizeExtension(episode.containerExtension, fallback: "mp4")
                guard let streamURL = streamURL(account: account, kind: .series, id: id, fileExtension: ext) else { continue }
                let rawTitle = episode.title?.trimmed.nilIfEmpty ?? "Episode \(number)"
                let quality = QualityLogic.detect(name: rawTitle)
                result.append(Episode(
                    season: season,
                    number: number,
                    title: rawTitle,
                    url: streamURL.absoluteString,
                    quality: quality,
                    language: nil,
                    variants: []
                ))
            }
        }

        result.sort { lhs, rhs in
            lhs.season == rhs.season ? lhs.number < rhs.number : lhs.season < rhs.season
        }
        return result
    }

    // MARK: - EPG

    static func shortEPG(account: XtreamAccount, streamID: String, limit: Int = 6) async -> [EPGProgram] {
        guard let url = apiURL(
            account: account,
            action: "get_short_epg",
            extra: [
                URLQueryItem(name: "stream_id", value: streamID),
                URLQueryItem(name: "limit", value: String(limit)),
            ]
        ) else {
            return []
        }
        guard let response = try? await fetch(XtreamEPGResponse.self, from: url) else { return [] }

        var programs: [EPGProgram] = []
        for listing in response.listings {
            guard let start = date(from: listing.startTimestamp?.value, fallback: listing.start),
                  let end = date(from: listing.stopTimestamp?.value, fallback: listing.end),
                  end > start else { continue }
            let title = decodeBase64(listing.title) ?? listing.title ?? ""
            let plot = decodeBase64(listing.description) ?? listing.description
            guard !title.trimmed.isEmpty else { continue }
            programs.append(EPGProgram(
                id: "\(streamID)-\(Int(start.timeIntervalSince1970))",
                title: title,
                plot: plot?.trimmed.nilIfEmpty,
                start: start,
                end: end
            ))
        }
        programs.sort { $0.start < $1.start }
        return programs
    }

    // MARK: - Helpers

    private static func categoryName(_ id: FlexString?, _ categories: [String: String]) -> String {
        guard let id = id?.value.trimmed, let name = categories[id], !name.isEmpty else { return "Other" }
        return name
    }

    private static func httpURL(_ raw: String?) -> String? {
        guard let value = raw?.trimmed, value.lowercased().hasPrefix("http") else { return nil }
        return value
    }

    private static func rating(_ raw: FlexString?, fallback: FlexString?) -> Double? {
        if let value = raw?.double, value > 0 {
            return value > 10 ? value / 10 : value
        }
        if let value = fallback?.double, value > 0 {
            return min(value * 2, 10)
        }
        return nil
    }

    private static func yearFrom(_ raw: String?) -> Int? {
        guard let raw, let match = RegexTool.firstMatch("(19|20)\\d{2}", in: raw), let value = Int(match[0]) else {
            return nil
        }
        return value
    }

    private static func durationSeconds(_ raw: String?) -> Int? {
        guard let raw = raw?.trimmed, !raw.isEmpty else { return nil }
        if let seconds = Int(raw), seconds > 0 { return seconds }
        if let seconds = Double(raw), seconds > 0 { return Int(seconds) }
        let parts = raw.split(separator: ":").compactMap { Int($0) }
        if parts.count == 3 { return parts[0] * 3600 + parts[1] * 60 + parts[2] }
        if parts.count == 2 { return parts[0] * 60 + parts[1] }
        return nil
    }

    private static func date(from timestamp: String?, fallback: String?) -> Date? {
        if let timestamp, let value = Double(timestamp) {
            let seconds = value > 10_000_000_000 ? value / 1000 : value
            return Date(timeIntervalSince1970: seconds)
        }
        guard let fallback = fallback?.trimmed, !fallback.isEmpty else { return nil }
        let formats = ["yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ssZ", "yyyy-MM-dd HH:mm"]
        for format in formats {
            let formatter = DateFormatter()
            formatter.locale = Locale(identifier: "en_US_POSIX")
            formatter.timeZone = TimeZone(secondsFromGMT: 0)
            formatter.dateFormat = format
            if let date = formatter.date(from: fallback) { return date }
        }
        return nil
    }

    private static func decodeBase64(_ value: String?) -> String? {
        guard let value, !value.isEmpty, let data = Data(base64Encoded: value, options: [.ignoreUnknownCharacters]),
              let decoded = String(data: data, encoding: .utf8), !decoded.isEmpty else {
            return nil
        }
        return decoded
    }
}
