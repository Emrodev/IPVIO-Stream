import Foundation

// MARK: - Intro / recap / credits markers

struct IntroSegment: Identifiable, Hashable, Sendable {
    enum Kind: String, Hashable, Sendable {
        case intro
        case recap
        case credits

        var titleKey: String {
            switch self {
            case .intro: return "player.skipIntro"
            case .recap: return "player.skipRecap"
            case .credits: return "player.skipCredits"
            }
        }
    }

    var kind: Kind
    var start: Double
    var end: Double

    var id: String { "\(kind.rawValue)-\(Int(start))" }
}

actor IntroDBService {
    static let shared = IntroDBService()

    private var cache: [String: [IntroSegment]] = [:]

    func segments(tmdbID: Int, season: Int, episode: Int, duration: Double) async -> [IntroSegment] {
        guard tmdbID > 0, duration > 30 else { return [] }
        let key = "\(tmdbID)|\(season)|\(episode)|\(Int(duration))"
        if let cached = cache[key] { return cached }

        var components = URLComponents(string: "https://api.theintrodb.org/v3/media")
        var query = [
            URLQueryItem(name: "tmdb_id", value: String(tmdbID)),
            URLQueryItem(name: "duration_ms", value: String(Int(duration * 1000))),
        ]
        if season > 0, episode > 0 {
            query.append(URLQueryItem(name: "season", value: String(season)))
            query.append(URLQueryItem(name: "episode", value: String(episode)))
        }
        components?.queryItems = query
        guard let url = components?.url else { return [] }

        var request = URLRequest(url: url)
        request.timeoutInterval = 15
        request.setValue("IPVIOStream/1.0 (tvOS)", forHTTPHeaderField: "User-Agent")
        guard let (data, response) = try? await URLSession.shared.data(for: request),
              let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            return []
        }

        guard let payload = try? JSONDecoder().decode(Payload.self, from: data) else { return [] }
        var result: [IntroSegment] = []

        func append(_ spans: [Span]?, kind: IntroSegment.Kind, lower: Double, upper: Double) {
            for span in spans ?? [] {
                guard let rawStart = span.startMs, let rawEnd = span.endMs else { continue }
                let start = rawStart / 1000
                var end = rawEnd / 1000
                guard start >= 0, end > start + 1 else { continue }
                if kind == .credits, end > duration + 10 { end = duration }
                guard start <= duration * upper, end >= duration * lower, start < duration else { continue }
                switch kind {
                case .intro:
                    guard start <= min(600, duration * 0.3), end <= max(duration * 0.45, 120) else { continue }
                case .recap:
                    guard end <= max(duration * 0.3, 180) else { continue }
                case .credits:
                    guard start >= duration * 0.55 else { continue }
                    end = min(max(end, start + 5), duration + 5)
                }
                result.append(IntroSegment(kind: kind, start: start, end: end))
            }
        }

        append(payload.intro, kind: .intro, lower: 0, upper: 0.45)
        append(payload.recap, kind: .recap, lower: 0, upper: 0.3)
        append(payload.credits, kind: .credits, lower: 0.55, upper: 1.05)

        result.sort { $0.start < $1.start }
        cache[key] = result
        return result
    }

    private struct Payload: Decodable {
        var intro: [Span]?
        var recap: [Span]?
        var credits: [Span]?
    }

    private struct Span: Decodable {
        var startMs: Double?
        var endMs: Double?

        enum CodingKeys: String, CodingKey {
            case startMs = "start_ms"
            case endMs = "end_ms"
        }
    }
}
