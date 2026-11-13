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

// MARK: - XMLTV guide

/// Downloads a full XMLTV guide once per playlist and keeps only the channels
/// that the guide screen is currently showing. Parsing happens off the main
/// thread and the downloaded file is streamed by XMLParser, so large guides do
/// not have to be held in memory as raw XML.
actor XMLTVService {
    static let shared = XMLTVService()

    private struct Entry {
        var loadedAt: Date
        var channelIDs: Set<String>
        var programs: [String: [EPGProgram]]
    }

    private var cache: [String: Entry] = [:]
    private let ttl: TimeInterval = 15 * 60

    func programs(epgURL: String, channelIDs: Set<String>) async -> [String: [EPGProgram]] {
        guard !channelIDs.isEmpty, let url = URL(string: epgURL) else { return [:] }

        if let entry = cache[epgURL],
           Date().timeIntervalSince(entry.loadedAt) < ttl,
           channelIDs.isSubset(of: entry.channelIDs) {
            return entry.programs.filter { channelIDs.contains($0.key) }
        }

        var request = URLRequest(url: url)
        request.timeoutInterval = 75
        request.cachePolicy = .reloadIgnoringLocalCacheData

        let parsed: [String: [EPGProgram]]
        do {
            let (fileURL, _) = try await URLSession.shared.download(for: request)
            defer { try? FileManager.default.removeItem(at: fileURL) }
            guard let parser = XMLParser(contentsOf: fileURL) else { return [:] }
            let delegate = XMLTVDelegate(channelIDs: channelIDs)
            parser.delegate = delegate
            parser.shouldResolveExternalEntities = false
            guard parser.parse() else { return [:] }
            parsed = delegate.programs.mapValues { values in
                Array(values.sorted { $0.start < $1.start }.prefix(32))
            }
        } catch {
            return [:]
        }

        var entry = cache[epgURL] ?? Entry(loadedAt: Date(), channelIDs: [], programs: [:])
        for (channel, list) in parsed {
            entry.programs[channel] = list
        }
        entry.channelIDs.formUnion(channelIDs)
        entry.loadedAt = Date()
        cache[epgURL] = entry

        return parsed
    }

    func clear() {
        cache.removeAll()
    }

    fileprivate static func parseDate(_ raw: String?) -> Date? {
        XMLTVDateParser.parse(raw)
    }
}

private enum XMLTVDateParser {
    private static let formatters: [DateFormatter] = {
        ["yyyyMMddHHmmss Z", "yyyyMMddHHmmss"].map { format in
            let formatter = DateFormatter()
            formatter.locale = Locale(identifier: "en_US_POSIX")
            formatter.timeZone = TimeZone(secondsFromGMT: 0)
            formatter.dateFormat = format
            return formatter
        }
    }()

    static func parse(_ raw: String?) -> Date? {
        guard let value = raw?.trimmed, !value.isEmpty else { return nil }
        for formatter in formatters {
            if let date = formatter.date(from: value) { return date }
        }
        return nil
    }
}

private final class XMLTVDelegate: NSObject, XMLParserDelegate {
    private let channelIDs: Set<String>
    private let earliest: Date
    private let latest: Date

    private var currentChannel: String?
    private var currentStart: Date?
    private var currentEnd: Date?
    private var currentTitle: String?
    private var currentPlot: String?
    private var text = ""
    private var inTitle = false
    private var inDescription = false

    private(set) var programs: [String: [EPGProgram]] = [:]

    init(channelIDs: Set<String>) {
        self.channelIDs = channelIDs
        let now = Date()
        self.earliest = now.addingTimeInterval(-2 * 3600)
        self.latest = now.addingTimeInterval(36 * 3600)
        super.init()
    }

    func parser(
        _ parser: XMLParser,
        didStartElement elementName: String,
        namespaceURI: String?,
        qualifiedName qName: String?,
        attributes attributeDict: [String: String] = [:]
    ) {
        switch elementName {
        case "programme":
            currentChannel = nil
            currentStart = nil
            currentEnd = nil
            currentTitle = nil
            currentPlot = nil
            inTitle = false
            inDescription = false

            guard let channel = attributeDict["channel"],
                  channelIDs.contains(channel),
                  let start = XMLTVService.parseDate(attributeDict["start"]),
                  let end = XMLTVService.parseDate(attributeDict["stop"]),
                  end > start, end > earliest, start < latest else { return }

            currentChannel = channel
            currentStart = start
            currentEnd = end
        case "title":
            if currentChannel != nil {
                inTitle = true
                text = ""
            }
        case "desc":
            if currentChannel != nil {
                inDescription = true
                text = ""
            }
        default:
            break
        }
    }

    func parser(_ parser: XMLParser, foundCharacters string: String) {
        if inTitle || inDescription { text += string }
    }

    func parser(
        _ parser: XMLParser,
        didEndElement elementName: String,
        namespaceURI: String?,
        qualifiedName qName: String?
    ) {
        switch elementName {
        case "title" where inTitle:
            if currentTitle == nil { currentTitle = text.trimmed.nilIfEmpty }
            inTitle = false
            text = ""
        case "desc" where inDescription:
            if currentPlot == nil {
                let value = text.trimmed
                currentPlot = value.isEmpty ? nil : String(value.prefix(400))
            }
            inDescription = false
            text = ""
        case "programme":
            if let channel = currentChannel,
               let start = currentStart,
               let end = currentEnd,
               let title = currentTitle?.trimmed,
               !title.isEmpty {
                programs[channel, default: []].append(
                    EPGProgram(
                        id: "\(channel)-\(Int(start.timeIntervalSince1970))",
                        title: title,
                        plot: currentPlot,
                        start: start,
                        end: end
                    )
                )
            }
            currentChannel = nil
            currentStart = nil
            currentEnd = nil
            currentTitle = nil
            currentPlot = nil
        default:
            break
        }
    }
}

