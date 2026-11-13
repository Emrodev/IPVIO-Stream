import Foundation

// MARK: - Live guide

actor EPGService {
    static let shared = EPGService()

    private var cache: [String: (loadedAt: Date, programs: [EPGProgram])] = [:]
    private let ttl: TimeInterval = 150

    func programs(item: MediaItem, account: XtreamAccount) async -> [EPGProgram] {
        guard let streamID = item.xtream?.streamID else { return [] }
        if let entry = cache[item.id], Date().timeIntervalSince(entry.loadedAt) < ttl {
            return entry.programs
        }
        let programs = await XtreamClient.shortEPG(account: account, streamID: streamID)
        if !programs.isEmpty {
            cache[item.id] = (Date(), programs)
        }
        return programs
    }

    func cached(itemID: String) -> [EPGProgram]? {
        guard let entry = cache[itemID], Date().timeIntervalSince(entry.loadedAt) < ttl else { return nil }
        return entry.programs
    }

    func clear() {
        cache.removeAll()
    }
}

extension Array where Element == EPGProgram {
    func now() -> EPGProgram? {
        let date = Date()
        return first { date >= $0.start && date < $0.end }
    }

    func next() -> EPGProgram? {
        let date = Date()
        return first { $0.start > date }
    }
}
