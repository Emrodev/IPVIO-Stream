import Foundation

// MARK: - Flexible primitives

struct FlexString: Decodable, Hashable, Sendable {
    var value: String

    init(_ value: String) {
        self.value = value
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        if let string = try? container.decode(String.self) {
            value = string
        } else if let int = try? container.decode(Int.self) {
            value = String(int)
        } else if let double = try? container.decode(Double.self) {
            value = String(double)
        } else if let bool = try? container.decode(Bool.self) {
            value = bool ? "1" : "0"
        } else {
            value = ""
        }
    }

    var int: Int? {
        if let value = Int(self.value) { return value }
        if let value = Double(self.value) { return Int(value) }
        return nil
    }

    var double: Double? { Double(value) }
    var bool: Bool { value == "1" || value.lowercased() == "true" }
}

// MARK: - API models

struct XtreamUserInfo: Decodable, Sendable {
    var auth: FlexString?
    var status: String?
    var expDate: FlexString?
    var isTrial: FlexString?
    var activeConnections: FlexString?

    enum CodingKeys: String, CodingKey {
        case auth, status
        case expDate = "exp_date"
        case isTrial = "is_trial"
        case activeConnections = "active_cons"
    }
}

struct XtreamServerInfo: Decodable, Sendable {
    var url: String?
    var port: FlexString?
    var httpsPort: FlexString?
    var serverProtocol: String?

    enum CodingKeys: String, CodingKey {
        case url, port
        case httpsPort = "https_port"
        case serverProtocol = "server_protocol"
    }
}

struct XtreamLoginResponse: Decodable, Sendable {
    var userInfo: XtreamUserInfo?
    var serverInfo: XtreamServerInfo?

    enum CodingKeys: String, CodingKey {
        case userInfo = "user_info"
        case serverInfo = "server_info"
    }
}

struct XtreamCategory: Decodable, Sendable {
    var categoryId: FlexString?
    var categoryName: String?
    var parentId: FlexString?

    enum CodingKeys: String, CodingKey {
        case categoryId = "category_id"
        case categoryName = "category_name"
        case parentId = "parent_id"
    }
}

struct XtreamStream: Decodable, Sendable {
    var num: FlexString?
    var name: String?
    var streamType: String?
    var streamId: FlexString?
    var streamIcon: String?
    var epgChannelId: String?
    var added: String?
    var categoryId: FlexString?
    var tvArchive: FlexString?
    var directSource: String?
    var tvArchiveDuration: FlexString?
    var containerExtension: String?
    var rating: FlexString?
    var rating5based: FlexString?
    var plot: String?
    var duration: FlexString?
    var year: FlexString?
    var releaseDate: String?
    var genre: String?

    enum CodingKeys: String, CodingKey {
        case num, name, rating, plot, duration, year, genre, added
        case streamType = "stream_type"
        case streamId = "stream_id"
        case streamIcon = "stream_icon"
        case epgChannelId = "epg_channel_id"
        case categoryId = "category_id"
        case tvArchive = "tv_archive"
        case directSource = "direct_source"
        case tvArchiveDuration = "tv_archive_duration"
        case containerExtension = "container_extension"
        case rating5based = "rating_5based"
        case releaseDate = "releasedate"
    }
}

struct XtreamSeries: Decodable, Sendable {
    var seriesId: FlexString?
    var name: String?
    var cover: String?
    var plot: String?
    var rating: FlexString?
    var genre: String?
    var releaseDate: String?
    var categoryId: FlexString?
    var backdropPath: String?
    var youtubeTrailer: String?
    var episodeRunTime: FlexString?

    enum CodingKeys: String, CodingKey {
        case name, cover, plot, rating, genre
        case seriesId = "series_id"
        case releaseDate = "releaseDate"
        case categoryId = "category_id"
        case backdropPath = "backdrop_path"
        case youtubeTrailer = "youtube_trailer"
        case episodeRunTime = "episode_run_time"
    }
}

struct XtreamEpisodeInfo: Decodable, Sendable {
    var plot: String?
    var duration: FlexString?
    var rating: FlexString?
    var movieImage: String?
    var releaseDate: String?
    var genre: String?

    enum CodingKeys: String, CodingKey {
        case plot, duration, rating, genre
        case movieImage = "movie_image"
        case releaseDate = "releasedate"
    }
}

struct XtreamEpisode: Decodable, Sendable {
    var id: FlexString?
    var episodeNum: FlexString?
    var season: FlexString?
    var title: String?
    var containerExtension: String?
    var plot: String?
    var duration: FlexString?
    var rating: FlexString?
    var info: XtreamEpisodeInfo?

    enum CodingKeys: String, CodingKey {
        case id, season, title, plot, duration, rating, info
        case episodeNum = "episode_num"
        case containerExtension = "container_extension"
    }

    var seasonNumber: Int {
        season?.int ?? 1
    }

    var episodeNumber: Int {
        episodeNum?.int ?? 0
    }
}

struct XtreamSeriesInfo: Decodable, Sendable {
    var episodes: [String: [XtreamEpisode]]
    var info: XtreamEpisodeInfo?

    enum CodingKeys: String, CodingKey {
        case episodes, info
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        info = try? container.decode(XtreamEpisodeInfo.self, forKey: .info)
        if let dictionary = try? container.decode([String: [XtreamEpisode]].self, forKey: .episodes) {
            episodes = dictionary
        } else if let array = try? container.decode([XtreamEpisode].self, forKey: .episodes) {
            episodes = ["1": array]
        } else {
            episodes = [:]
        }
    }
}

struct XtreamEPGListing: Decodable, Sendable {
    var id: FlexString?
    var title: String?
    var description: String?
    var start: String?
    var end: String?
    var startTimestamp: FlexString?
    var stopTimestamp: FlexString?
    var nowPlaying: FlexString?

    enum CodingKeys: String, CodingKey {
        case id, title, description, start, end
        case startTimestamp = "start_timestamp"
        case stopTimestamp = "stop_timestamp"
        case nowPlaying = "now_playing"
    }
}

struct XtreamEPGResponse: Decodable, Sendable {
    var listings: [XtreamEPGListing]

    enum CodingKeys: String, CodingKey {
        case listings = "epg_listings"
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        listings = (try? container.decode([XtreamEPGListing].self, forKey: .listings)) ?? []
    }
}
