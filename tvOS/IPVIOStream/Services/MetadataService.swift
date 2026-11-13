import Foundation

// MARK: - TMDB / OMDb enrichment

actor MetadataService {
    static let shared = MetadataService()

    private var cache: [String: EnrichedMetadata] = [:]
    private var loadedDiskCache = false

    func metadata(
        for item: MediaItem,
        language: Lang,
        tmdbKey: String,
        omdbKey: String
    ) async -> EnrichedMetadata? {
        let key = cacheKey(item: item, language: language)
        if let cached = cache[key] { return cached }
        await loadDiskCacheIfNeeded()
        if let cached = cache[key] { return cached }

        let cleanKey = tmdbKey.trimmed
        guard !cleanKey.isEmpty else { return nil }

        guard let search = await search(item: item, language: language, apiKey: cleanKey),
              let result = choose(search.results, for: item) else {
            return nil
        }

        var metadata = await details(
            tmdbID: result.id,
            kind: item.kind,
            language: language,
            apiKey: cleanKey
        )
        if metadata == nil {
            metadata = EnrichedMetadata(
                tmdbID: result.id,
                overview: result.overview,
                posterURL: imageURL(result.posterPath, size: "w500"),
                backdropURL: imageURL(result.backdropPath, size: "w1280"),
                tmdbRating: result.voteAverage
            )
        }

        if !omdbKey.trimmed.isEmpty, let imdbID = metadata?.imdbID,
           let omdb = await omdbData(imdbID: imdbID, apiKey: omdbKey.trimmed) {
            metadata?.imdbID = imdbID
            metadata?.imdbRating = omdb.imdb
            metadata?.rottenRating = omdb.rotten
        }

        if let metadata {
            cache[key] = metadata
            await Storage.shared.saveMetadataCache(cache)
            return metadata
        }
        return nil
    }

    func cached(for item: MediaItem, language: Lang) -> EnrichedMetadata? {
        cache[cacheKey(item: item, language: language)]
    }

    func preloadDiskCache() async {
        await loadDiskCacheIfNeeded()
    }

    func clearCache() async {
        cache.removeAll()
        loadedDiskCache = true
        await Storage.shared.clearMetadataCache()
    }

    private func loadDiskCacheIfNeeded() async {
        guard !loadedDiskCache else { return }
        loadedDiskCache = true
        cache = await Storage.shared.loadMetadataCache()
    }

    private func cacheKey(item: MediaItem, language: Lang) -> String {
        let title = item.title.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: Locale(identifier: "en"))
        return "\(language.rawValue)|\(item.kind.rawValue)|\(title)|\(item.year ?? 0)"
    }

    // MARK: TMDB

    private struct TMDBSearchResponse: Decodable {
        var results: [TMDBSearchResult]
    }

    private struct TMDBSearchResult: Decodable {
        var id: Int
        var title: String?
        var name: String?
        var releaseDate: String?
        var firstAirDate: String?
        var posterPath: String?
        var backdropPath: String?
        var overview: String?
        var voteAverage: Double?

        enum CodingKeys: String, CodingKey {
            case id, title, name, overview
            case releaseDate = "release_date"
            case firstAirDate = "first_air_date"
            case posterPath = "poster_path"
            case backdropPath = "backdrop_path"
            case voteAverage = "vote_average"
        }
    }

    private struct TMDBDetails: Decodable {
        var id: Int?
        var overview: String?
        var posterPath: String?
        var backdropPath: String?
        var voteAverage: Double?
        var runtime: Int?
        var episodeRunTime: [Int]?
        var genres: [TMDBGenre]?
        var credits: TMDBCredits?
        var externalIds: TMDBExternalIDs?

        enum CodingKeys: String, CodingKey {
            case id, overview, runtime, genres, credits
            case posterPath = "poster_path"
            case backdropPath = "backdrop_path"
            case voteAverage = "vote_average"
            case episodeRunTime = "episode_run_time"
            case externalIds = "external_ids"
        }
    }

    private struct TMDBGenre: Decodable {
        var name: String
    }

    private struct TMDBCredits: Decodable {
        var cast: [TMDBCast]?
    }

    private struct TMDBCast: Decodable {
        var name: String
    }

    private struct TMDBExternalIDs: Decodable {
        var imdbId: String?

        enum CodingKeys: String, CodingKey {
            case imdbId = "imdb_id"
        }
    }

    private func search(item: MediaItem, language: Lang, apiKey: String) async -> TMDBSearchResponse? {
        var components = URLComponents(string: "https://api.themoviedb.org/3/search/\(item.kind == .series ? "tv" : "movie")")
        var query: [URLQueryItem] = [
            URLQueryItem(name: "api_key", value: apiKey),
            URLQueryItem(name: "language", value: language == .ar ? "ar" : "en-US"),
            URLQueryItem(name: "query", value: item.title),
            URLQueryItem(name: "include_adult", value: "false"),
        ]
        if let year = item.year {
            query.append(URLQueryItem(name: item.kind == .series ? "first_air_date_year" : "year", value: String(year)))
        }
        components?.queryItems = query
        guard let url = components?.url else { return nil }
        return await get(TMDBSearchResponse.self, from: url)
    }

    private func details(tmdbID: Int, kind: MediaKind, language: Lang, apiKey: String) async -> EnrichedMetadata? {
        var components = URLComponents(string: "https://api.themoviedb.org/3/\(kind == .series ? "tv" : "movie")/\(tmdbID)")
        components?.queryItems = [
            URLQueryItem(name: "api_key", value: apiKey),
            URLQueryItem(name: "language", value: language == .ar ? "ar" : "en-US"),
            URLQueryItem(name: "append_to_response", value: "credits,external_ids"),
        ]
        guard let url = components?.url, let details = await get(TMDBDetails.self, from: url) else {
            return nil
        }

        return EnrichedMetadata(
            tmdbID: tmdbID,
            imdbID: details.externalIds?.imdbId?.trimmed.nilIfEmpty,
            overview: details.overview?.trimmed.nilIfEmpty,
            posterURL: imageURL(details.posterPath, size: "w500"),
            backdropURL: imageURL(details.backdropPath, size: "w1280"),
            tmdbRating: details.voteAverage,
            imdbRating: nil,
            rottenRating: nil,
            genres: details.genres?.map(\.name) ?? [],
            cast: Array((details.credits?.cast ?? []).prefix(10).map(\.name)),
            runtime: details.runtime ?? details.episodeRunTime?.first
        )
    }

    private func choose(_ results: [TMDBSearchResult], for item: MediaItem) -> TMDBSearchResult? {
        guard !results.isEmpty else { return nil }
        let target = normalize(item.title)
        var best: (score: Int, result: TMDBSearchResult)?
        for result in results {
            let candidate = normalize(result.title ?? result.name ?? "")
            var score = 0
            if candidate == target { score += 6 }
            else if candidate.contains(target) || target.contains(candidate) { score += 3 }
            let candidateYear = Int((result.releaseDate ?? result.firstAirDate ?? "").prefix(4)) ?? 0
            if let year = item.year, candidateYear == year { score += 4 }
            else if item.year == nil { score += 1 }
            if let current = best, current.score >= score { continue }
            best = (score, result)
        }
        return best?.result ?? results.first
    }

    private func imageURL(_ path: String?, size: String) -> String? {
        guard let path, !path.isEmpty else { return nil }
        return "https://image.tmdb.org/t/p/\(size)\(path.hasPrefix("/") ? path : "/" + path)"
    }

    private func normalize(_ value: String) -> String {
        value.folding(options: [.diacriticInsensitive, .caseInsensitive], locale: Locale(identifier: "en"))
            .replacingOccurrences(of: "[^a-z0-9\\p{Arabic} ]", with: "", options: .regularExpression)
            .trimmingCharacters(in: .whitespaces)
    }

    // MARK: OMDb

    private struct OMDBResponse: Decodable {
        var response: String?
        var imdbRating: String?
        var ratings: [OMDBRating]?

        enum CodingKeys: String, CodingKey {
            case response = "Response"
            case imdbRating
            case ratings = "Ratings"
        }
    }

    private struct OMDBRating: Decodable {
        var source: String
        var value: String

        enum CodingKeys: String, CodingKey {
            case source = "Source"
            case value = "Value"
        }
    }

    private func omdbData(imdbID: String, apiKey: String) async -> (imdb: Double?, rotten: Double?)? {
        var components = URLComponents(string: "https://www.omdbapi.com/")
        components?.queryItems = [
            URLQueryItem(name: "apikey", value: apiKey),
            URLQueryItem(name: "i", value: imdbID),
            URLQueryItem(name: "plot", value: "short"),
        ]
        guard let url = components?.url, let response = await get(OMDBResponse.self, from: url),
              response.response?.lowercased() != "false" else { return nil }

        let imdb = response.imdbRating.flatMap { Double($0) }
        let rottenValue = response.ratings?.first(where: { $0.source == "Rotten Tomatoes" })?.value
        let rotten = rottenValue.flatMap { Double($0.replacingOccurrences(of: "%", with: "")) }
        return (imdb, rotten)
    }

    // MARK: transport

    private func get<T: Decodable>(_ type: T.Type, from url: URL) async -> T? {
        var request = URLRequest(url: url)
        request.timeoutInterval = 20
        request.setValue("IPVIOStream/1.0 (tvOS)", forHTTPHeaderField: "User-Agent")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        guard let (data, response) = try? await URLSession.shared.data(for: request),
              let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            return nil
        }
        return try? JSONDecoder().decode(T.self, from: data)
    }
}
