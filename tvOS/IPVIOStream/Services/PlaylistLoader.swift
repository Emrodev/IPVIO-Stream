import Foundation

enum PlaylistLoader {
    static let userAgent = "VLC/3.0.21 LibVLC/3.0.21"

    static func load(
        playlist: Playlist,
        progress: @escaping @Sendable (Double, String) -> Void
    ) async throws -> LoadedPlaylist {
        switch playlist.source {
        case let .xtream(account):
            return try await XtreamClient.load(account: account, playlistID: playlist.id, progress: progress)
        case let .m3u(url):
            return try await loadM3U(url: url, playlistID: playlist.id, progress: progress)
        }
    }

    // MARK: - M3U download

    private static func loadM3U(
        url: String,
        playlistID: UUID,
        progress: @escaping @Sendable (Double, String) -> Void
    ) async throws -> LoadedPlaylist {
        guard let remote = URLTool.normalized(url), let scheme = remote.scheme,
              scheme.lowercased() == "http" || scheme.lowercased() == "https" else {
            throw PlaylistError.invalidURL
        }

        progress(0.05, L.t("load.download"))
        var request = URLRequest(url: remote)
        request.timeoutInterval = 120
        request.cachePolicy = .reloadIgnoringLocalCacheData
        request.setValue(userAgent, forHTTPHeaderField: "User-Agent")
        request.setValue("*/*", forHTTPHeaderField: "Accept")

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
        try Task.checkCancellation()

        let text = String(data: data, encoding: .utf8)
            ?? String(data: data, encoding: .isoLatin1)
            ?? ""
        guard !text.isEmpty else { throw PlaylistError.empty }

        let head = String(text.prefix(4096))
        guard head.contains("#EXTM3U") || head.contains("#EXTINF") else {
            throw PlaylistError.notPlaylist
        }

        progress(0.55, L.t("load.parse"))
        let loaded = try M3UParser.parse(text: text, playlistID: playlistID) { value, status in
            progress(0.55 + min(max(value, 0), 1) * 0.44, status)
        }
        progress(1, L.t("load.finish"))
        return loaded
    }
}
