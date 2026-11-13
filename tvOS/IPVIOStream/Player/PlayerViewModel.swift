import Foundation
import KSPlayer
import KSPlayerUI
import SwiftUI

@MainActor
final class PlayerViewModel: ObservableObject {
    let item: MediaItem
    let episodes: [Episode]
    let settings: AppSettings
    let tmdbID: Int?

    @Published private(set) var coordinator: KSVideoPlayer.Coordinator
    @Published private(set) var options: KSOptions
    @Published private(set) var url: URL?
    @Published private(set) var generation = 0
    @Published private(set) var state: KSPlayerState?
    @Published private(set) var currentTime: Double = 0
    @Published private(set) var duration: Double = 0
    @Published private(set) var bufferTime: Double = 0
    @Published private(set) var audioTracks: [MediaPlayerTrack] = []
    @Published private(set) var subtitleInfos: [any SubtitleInfo] = []
    @Published private(set) var selectedAudioID: Int32?
    @Published private(set) var selectedSubtitleID: String?
    @Published private(set) var variants: [Variant] = []
    @Published private(set) var selectedVariantIndex = 0
    @Published private(set) var episodeIndex = 0
    @Published private(set) var showControls = true
    @Published private(set) var aspectFill = false
    @Published private(set) var playbackRate: Float = 1
    @Published private(set) var errorMessage: String?
    @Published private(set) var retrying = false
    @Published private(set) var ended = false
    @Published private(set) var introSegments: [IntroSegment] = []
    @Published private(set) var currentSegment: IntroSegment?
    @Published private(set) var nextCountdown: Int?
    @Published private(set) var sleepMinutes: Int?
    @Published private(set) var banner: String?
    @Published private(set) var seekFeedback: String?
    @Published private(set) var isScrubbing = false

    var onProgress: ((Double, Double) -> Void)?
    var resumeProvider: ((Episode?) -> Double?)?

    private let isLive: Bool
    private var initialStart: Double
    private var pendingStart: Double = 0
    private var lastSavedTime: Double = 0
    private var didApplyPreferredAudio = false
    private var didApplySubtitles = false
    private var didSeekToStart = false
    private var didFetchIntro = false
    private var dismissedSegments: Set<String> = []
    private var retryCount = 0
    private var fallbackCount = 0
    private var handledError = false
    private var hideTask: Task<Void, Never>?
    private var retryTask: Task<Void, Never>?
    private var nextTask: Task<Void, Never>?
    private var introTask: Task<Void, Never>?
    private var sleepTask: Task<Void, Never>?
    private var bannerTask: Task<Void, Never>?
    private var feedbackTask: Task<Void, Never>?

    init(
        item: MediaItem,
        episodes: [Episode],
        initialEpisode: Episode?,
        startAt: Double?,
        settings: AppSettings,
        tmdbID: Int?
    ) {
        self.item = item
        self.episodes = episodes
        self.settings = settings
        self.tmdbID = tmdbID
        self.isLive = item.kind == .live
        self.initialStart = max(0, startAt ?? 0)
        self.options = KSOptions()
        self.coordinator = KSVideoPlayer.Coordinator()

        if let initialEpisode, let index = episodes.firstIndex(where: { $0.id == initialEpisode.id }) {
            episodeIndex = index
        } else {
            episodeIndex = 0
        }
    }

    // MARK: - Derived

    var currentEpisode: Episode? {
        guard item.kind == .series, episodes.indices.contains(episodeIndex) else { return nil }
        return episodes[episodeIndex]
    }

    var hasNext: Bool {
        item.kind == .series && episodeIndex + 1 < episodes.count
    }

    var hasPrevious: Bool {
        item.kind == .series && episodeIndex > 0
    }

    var title: String { item.title }

    var subtitle: String? {
        if item.kind == .live {
            return item.group
        }
        if let episode = currentEpisode {
            let label = L.f("player.episodeOf", episode.season, episode.number)
            let episodeTitle = episode.displayTitle
            return episodeTitle.isEmpty ? label : "\(label) · \(episodeTitle)"
        }
        var parts: [String] = []
        if let year = item.year { parts.append(String(year)) }
        if let quality = item.quality { parts.append(quality) }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }

    var selectedVariant: Variant? {
        variants.indices.contains(selectedVariantIndex) ? variants[selectedVariantIndex] : variants.first
    }

    var selectedAudioName: String {
        guard let id = selectedAudioID else { return L.t("common.auto") }
        return audioTracks.first { $0.trackID == id }?.name ?? L.t("common.auto")
    }

    var selectedSubtitleName: String {
        guard let id = selectedSubtitleID else { return L.t("player.subtitleOff") }
        return subtitleInfos.first { $0.subtitleID == id }?.name ?? L.t("player.subtitleOff")
    }

    var resolutionText: String {
        guard let size = coordinator.playerLayer?.player.naturalSize, size.width > 0 else { return "—" }
        return "\(Int(size.width))×\(Int(size.height))"
    }

    var bufferText: String { TimeText.clock(bufferTime) }

    var stateText: String {
        guard let state else { return "—" }
        return String(describing: state)
    }

    var isPlaying: Bool { state?.isPlaying ?? false }

    // MARK: - Lifecycle

    func start() {
        guard url == nil else { return }
        load(startAt: initialStart)
    }

    func stop() {
        if !isLive, duration > 60, currentTime > 5 {
            onProgress?(currentTime, duration)
        }
        hideTask?.cancel()
        retryTask?.cancel()
        nextTask?.cancel()
        introTask?.cancel()
        sleepTask?.cancel()
        bannerTask?.cancel()
        feedbackTask?.cancel()
        coordinator.resetPlayer()
    }

    // MARK: - Loading

    private func load(startAt: Double, preferredURL: String? = nil) {
        pendingStart = max(0, startAt)
        coordinator.resetPlayer()
        handledError = false
        ended = false
        errorMessage = nil
        retrying = false
        currentTime = 0
        duration = 0
        bufferTime = 0
        audioTracks = []
        subtitleInfos = []
        selectedAudioID = nil
        selectedSubtitleID = nil
        didApplyPreferredAudio = false
        didApplySubtitles = false
        didSeekToStart = false
        didFetchIntro = false
        introSegments = []
        currentSegment = nil
        banner = nil
        seekFeedback = nil
        nextCountdown = nil
        nextTask?.cancel()
        dismissedSegments.removeAll()

        variants = makeVariants()
        let picked: Variant?
        if let preferredURL, let match = variants.first(where: { $0.url == preferredURL }) {
            picked = match
        } else {
            picked = QualityLogic.pick(
                from: variants,
                cap: settings.qualityCap,
                language: settings.preferredAudioLanguage.nilIfEmpty
            )
        }
        guard let variant = picked, let playbackURL = URL(string: variant.url) else {
            errorMessage = L.t("player.errorHint")
            showControls = true
            return
        }

        selectedVariantIndex = variants.firstIndex(where: { $0.id == variant.id }) ?? 0
        url = playbackURL
        options = makeOptions(startAt: max(0, startAt))
        state = nil

        let newCoordinator = KSVideoPlayer.Coordinator()
        coordinator = newCoordinator
        attachCallbacks(to: newCoordinator)
        generation += 1
        showControls = true
        scheduleHide()
    }

    private func makeVariants() -> [Variant] {
        var list = item.playbackVariants(episode: currentEpisode)
        if isLive, let first = list.first,
           var components = URLComponents(string: first.url),
           components.path.lowercased().hasSuffix(".ts") {
            components.path = String(components.path.dropLast(3)) + ".m3u8"
            if let hlsURL = components.url?.absoluteString, !list.contains(where: { $0.url == hlsURL }) {
                list.append(Variant(url: hlsURL, label: "HLS", quality: item.quality, language: nil))
            }
        }
        return list.sorted { lhs, rhs in
            let l = QualityLogic.rank(lhs.quality)
            let r = QualityLogic.rank(rhs.quality)
            if l != r { return l > r }
            if lhs.url.hasSuffix(".m3u8") != rhs.url.hasSuffix(".m3u8") {
                return !lhs.url.hasSuffix(".m3u8")
            }
            return lhs.label < rhs.label
        }
    }

    private func makeOptions(startAt: Double) -> KSOptions {
        let options = KSOptions()
        options.isAutoPlay = true
        options.startPlayTime = max(0, startAt)
        options.userAgent = PlaylistLoader.userAgent
        options.appendHeader(["User-Agent": PlaylistLoader.userAgent])
        options.preferredForwardBufferDuration = Double(max(5, settings.bufferSeconds))
        options.maxBufferDuration = max(30, Double(settings.bufferSeconds) * 4)
        options.isSecondOpen = true
        options.isLoopPlay = false
        options.registerRemoteControll = false

        if settings.preferFFmpeg {
            KSOptions.firstPlayerType = KSMEPlayer.self
            KSOptions.secondPlayerType = KSMEPlayer.self
        }
        return options
    }

    private func attachCallbacks(to coordinator: KSVideoPlayer.Coordinator) {
        coordinator.onStateChanged = { [weak self] layer, state in
            Task { @MainActor [weak self] in
                self?.handleState(state, layer: layer)
            }
        }
        coordinator.onPlay = { [weak self] current, total in
            Task { @MainActor [weak self] in
                self?.handleTime(current: current, total: total)
            }
        }
        coordinator.onFinish = { [weak self] _, error in
            Task { @MainActor [weak self] in
                self?.handleFinish(error: error)
            }
        }
        coordinator.onBufferChanged = { [weak self] _, _ in
            Task { @MainActor [weak self] in
                guard let self, let layer = self.coordinator.playerLayer else { return }
                self.bufferTime = max(0, layer.player.playableTime)
            }
        }
    }

    private func handleState(_ newState: KSPlayerState, layer: KSPlayerLayer) {
        state = newState
        if newState == .readyToPlay {
            retryCount = 0
            handledError = false
            errorMessage = nil
            retrying = false
            let durationValue = layer.player.duration
            if durationValue.isFinite, durationValue > 0 {
                duration = durationValue
            }
            applyTracks(layer)
            if !didSeekToStart {
                didSeekToStart = true
                if pendingStart > 3, pendingStart < max(duration - 10, 30) {
                    layer.seek(time: pendingStart)
                }
            }
            fetchIntroIfNeeded()
            showControls = true
            scheduleHide()
        } else if newState == .error {
            handleFailure(nil)
        } else if newState == .playedToTheEnd {
            if isLive {
                handleFailure(nil)
            } else {
                handleEnd()
            }
        } else if !newState.isPlaying {
            revealControls(autoHide: false)
        }
        if newState.isPlaying, showControls, errorMessage == nil, !ended {
            scheduleHide()
        }
    }

    private func handleTime(current: Double, total: Double) {
        guard !isScrubbing else { return }
        if total.isFinite, total > 0 { duration = total }
        currentTime = max(0, current)
        if let layer = coordinator.playerLayer {
            bufferTime = max(0, layer.player.playableTime)
        }
        refreshTracksIfNeeded()

        if !isLive, duration > 60, abs(currentTime - lastSavedTime) >= 8, !ended {
            lastSavedTime = currentTime
            onProgress?(currentTime, duration)
        }
        fetchIntroIfNeeded()
        updateCurrentSegment()
    }

    private func refreshTracksIfNeeded() {
        guard let layer = coordinator.playerLayer else { return }
        if audioTracks.isEmpty {
            applyTracks(layer)
        }
        if subtitleInfos.isEmpty {
            applySubtitleInfos(layer)
        }
    }

    private func applyTracks(_ layer: KSPlayerLayer) {
        let tracks = layer.player.tracks(mediaType: .audio)
        if !tracks.isEmpty { audioTracks = tracks }

        if !didApplyPreferredAudio, let preferred = settings.preferredAudioLanguage.nilIfEmpty {
            if let match = tracks.first(where: { ($0.languageCode ?? "").lowercased().hasPrefix(preferred.lowercased()) }) {
                layer.player.select(track: match)
            }
            didApplyPreferredAudio = true
        }
        selectedAudioID = tracks.first(where: { $0.isEnabled })?.trackID ?? tracks.first?.trackID
        applySubtitleInfos(layer)
    }

    private func applySubtitleInfos(_ layer: KSPlayerLayer) {
        let infos = layer.subtitleModel.subtitleInfos
        if !infos.isEmpty { subtitleInfos = infos }
        selectedSubtitleID = layer.subtitleModel.selectedSubtitleInfo?.subtitleID

        if settings.subtitlesEnabled, !didApplySubtitles, let first = preferredSubtitle() {
            layer.select(subtitleInfo: first)
            selectedSubtitleID = first.subtitleID
            didApplySubtitles = true
        }
    }

    private func preferredSubtitle() -> (any SubtitleInfo)? {
        let preferred = settings.preferredSubtitleLanguage.trimmed.lowercased()
        if !preferred.isEmpty,
           let match = subtitleInfos.first(where: {
               ($0.languageCode ?? "").lowercased().hasPrefix(preferred)
                   || $0.name.localizedCaseInsensitiveContains(preferred)
           }) {
            return match
        }
        return subtitleInfos.first
    }

    // MARK: - Failure / retry

    private func handleFinish(error: Error?) {
        if let error {
            handleFailure(error)
        } else if isLive {
            handleFailure(nil)
        } else {
            handleEnd()
        }
    }

    private func handleFailure(_ error: Error?) {
        guard !handledError else { return }
        handledError = true
        if let error, !error.localizedDescription.trimmed.isEmpty {
            errorMessage = error.localizedDescription
        } else {
            errorMessage = L.t("player.errorHint")
        }

        if isLive {
            retryCount += 1
            if retryCount <= 5 {
                retrying = true
                showControls = true
                showBanner(L.t("live.retry"))
                retryTask?.cancel()
                retryTask = Task { [weak self] in
                    try? await Task.sleep(nanoseconds: 2_000_000_000)
                    guard let self, !Task.isCancelled else { return }
                    self.retrying = false
                    self.load(startAt: 0, preferredURL: self.selectedVariant?.url)
                }
                return
            }
            if selectedVariantIndex + 1 < variants.count {
                selectedVariantIndex += 1
                retryCount = 0
                load(startAt: 0, preferredURL: variants[selectedVariantIndex].url)
                return
            }
            retrying = false
            showControls = true
            return
        }

        if fallbackCount < variants.count - 1, selectedVariantIndex + 1 < variants.count {
            fallbackCount += 1
            let resume = currentTime
            selectedVariantIndex += 1
            load(startAt: resume, preferredURL: variants[selectedVariantIndex].url)
            return
        }
        showControls = true
    }

    func retry() {
        retryCount = 0
        fallbackCount = 0
        handledError = false
        errorMessage = nil
        retrying = false
        let resume = isLive ? 0 : currentTime
        load(startAt: resume, preferredURL: selectedVariant?.url)
    }

    func nextSource() {
        guard selectedVariantIndex + 1 < variants.count else { return }
        selectedVariantIndex += 1
        fallbackCount = 0
        handledError = false
        errorMessage = nil
        load(startAt: isLive ? 0 : currentTime, preferredURL: variants[selectedVariantIndex].url)
    }

    // MARK: - Controls / transport

    func togglePlayback() {
        cancelCountdown()
        if state?.isPlaying == true {
            coordinator.playerLayer?.pause()
            revealControls(autoHide: false)
        } else {
            if ended {
                replay()
            } else {
                coordinator.playerLayer?.play()
                scheduleHide()
            }
        }
    }

    func replay() {
        cancelCountdown()
        ended = false
        handledError = false
        errorMessage = nil
        currentTime = 0
        coordinator.seek(time: 0)
        coordinator.playerLayer?.play()
        revealControls()
    }

    func seek(by seconds: Double) {
        guard !isLive else { return }
        let target = min(max(currentTime + seconds, 0), max(duration - 1, 0))
        seek(to: target)
        seekFeedback = (seconds > 0 ? "+" : "") + "\(Int(seconds))s"
        feedbackTask?.cancel()
        feedbackTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: 900_000_000)
            guard let self, !Task.isCancelled else { return }
            self.seekFeedback = nil
        }
    }

    func seek(to time: Double) {
        guard !isLive else { return }
        let target = min(max(time, 0), max(duration - 1, 0))
        currentTime = target
        coordinator.seek(time: target)
    }

    func scrub(to time: Double) {
        guard !isLive else { return }
        currentTime = min(max(time, 0), duration)
    }

    func beginScrubbing() {
        isScrubbing = true
        cancelCountdown()
    }

    func endScrubbing() {
        isScrubbing = false
        seek(to: currentTime)
        revealControls()
    }

    func changeVariant(index: Int) {
        guard variants.indices.contains(index), index != selectedVariantIndex else { return }
        let resume = isLive ? 0 : currentTime
        selectedVariantIndex = index
        fallbackCount = 0
        handledError = false
        errorMessage = nil
        load(startAt: resume, preferredURL: variants[index].url)
    }

    func cycleSpeed() {
        let rates: [Float] = [1, 1.25, 1.5, 2, 0.75]
        let current = rates.firstIndex(of: playbackRate) ?? 0
        playbackRate = rates[(current + 1) % rates.count]
        coordinator.playbackRate = playbackRate
        showBanner(L.t("player.speed") + " " + String(format: "%.2g×", playbackRate))
    }

    func setSpeed(_ rate: Float) {
        playbackRate = rate
        coordinator.playbackRate = rate
    }

    func toggleAspect() {
        coordinator.isScaleAspectFill.toggle()
        aspectFill = coordinator.isScaleAspectFill
        showBanner(aspectFill ? L.t("player.aspectFill") : L.t("player.aspectFit"))
    }

    func toggleMute() {
        coordinator.isMuted.toggle()
        showBanner(coordinator.isMuted ? L.t("common.off") : L.t("common.on"))
    }

    func selectAudio(id: Int32) {
        guard let layer = coordinator.playerLayer,
              let track = audioTracks.first(where: { $0.trackID == id }) else { return }
        layer.player.select(track: track)
        selectedAudioID = track.trackID
    }

    func selectSubtitle(id: String?) {
        guard let layer = coordinator.playerLayer else { return }
        if let id, let info = subtitleInfos.first(where: { $0.subtitleID == id }) {
            layer.select(subtitleInfo: info)
            selectedSubtitleID = id
        } else {
            layer.select(subtitleInfo: nil)
            selectedSubtitleID = nil
        }
    }

    // MARK: - Episodes

    func playNext() {
        guard hasNext else { return }
        cancelCountdown()
        if !isLive, duration > 60, currentTime > 5 {
            onProgress?(currentTime, duration)
        }
        episodeIndex += 1
        fallbackCount = 0
        retryCount = 0
        let start = resumeProvider?(currentEpisode) ?? 0
        load(startAt: start)
        showBanner("\(L.t("player.nextEpisode")): \(currentEpisode?.displayTitle ?? "")")
    }

    func playPrevious() {
        guard hasPrevious else { return }
        cancelCountdown()
        if !isLive, duration > 60, currentTime > 5 {
            onProgress?(currentTime, duration)
        }
        episodeIndex -= 1
        fallbackCount = 0
        retryCount = 0
        let start = resumeProvider?(currentEpisode) ?? 0
        load(startAt: start)
    }

    private func handleEnd() {
        guard !ended else { return }
        ended = true
        currentTime = duration
        revealControls(autoHide: false)
        onProgress?(duration, duration)
        if hasNext, settings.autoPlayNext {
            beginNextCountdown()
        }
    }

    private func beginNextCountdown() {
        nextTask?.cancel()
        nextCountdown = 8
        nextTask = Task { [weak self] in
            for remaining in stride(from: 8, through: 1, by: -1) {
                try? await Task.sleep(nanoseconds: 1_000_000_000)
                guard let self, !Task.isCancelled else { return }
                self.nextCountdown = remaining - 1
            }
            guard let self, !Task.isCancelled else { return }
            self.nextCountdown = nil
            if self.hasNext { self.playNext() }
        }
    }

    func cancelCountdown() {
        nextTask?.cancel()
        nextTask = nil
        nextCountdown = nil
    }

    // MARK: - Intro markers

    private func fetchIntroIfNeeded() {
        guard !didFetchIntro, !isLive, let tmdbID, duration > 60 else { return }
        didFetchIntro = true
        let season = currentEpisode?.season ?? 0
        let number = currentEpisode?.number ?? 0
        let length = duration
        introTask?.cancel()
        introTask = Task { [weak self] in
            let segments = await IntroDBService.shared.segments(
                tmdbID: tmdbID,
                season: season,
                episode: number,
                duration: length
            )
            guard let self, !Task.isCancelled else { return }
            self.introSegments = segments
            self.updateCurrentSegment()
        }
    }

    private func updateCurrentSegment() {
        guard !introSegments.isEmpty else {
            currentSegment = nil
            return
        }
        let segment = introSegments.first {
            !dismissedSegments.contains($0.id) && currentTime >= $0.start && currentTime < $0.end
        }
        if segment?.id != currentSegment?.id {
            currentSegment = segment
        }
    }

    func skipCurrentSegment() {
        guard let segment = currentSegment else { return }
        dismissedSegments.insert(segment.id)
        seek(to: segment.end + 0.2)
        currentSegment = nil
        showBanner(L.t(segment.kind.titleKey))
    }

    // MARK: - Controls visibility

    func revealControls(autoHide: Bool = true) {
        showControls = true
        if autoHide {
            scheduleHide()
        } else {
            hideTask?.cancel()
        }
    }

    func hideControls() {
        guard state?.isPlaying == true, errorMessage == nil, !ended else { return }
        showControls = false
        hideTask?.cancel()
    }

    private func scheduleHide() {
        hideTask?.cancel()
        guard state?.isPlaying == true, errorMessage == nil, !ended else { return }
        hideTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: 6_000_000_000)
            guard let self, !Task.isCancelled else { return }
            if self.state?.isPlaying == true, self.errorMessage == nil, !self.ended {
                self.showControls = false
            }
        }
    }

    // MARK: - Sleep timer

    func setSleepTimer(minutes: Int?) {
        sleepTask?.cancel()
        sleepMinutes = minutes
        guard let minutes else { return }
        showBanner(L.f("player.sleepSet", minutes))
        sleepTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: UInt64(minutes) * 60 * 1_000_000_000)
            guard let self, !Task.isCancelled else { return }
            self.coordinator.playerLayer?.pause()
            self.revealControls(autoHide: false)
            self.showBanner(L.t("player.sleepDone"))
            self.sleepMinutes = nil
        }
    }

    // MARK: - Banner

    private func showBanner(_ message: String) {
        banner = message
        bannerTask?.cancel()
        bannerTask = Task { [weak self] in
            try? await Task.sleep(nanoseconds: 2_200_000_000)
            guard let self, !Task.isCancelled else { return }
            if self.banner == message { self.banner = nil }
        }
    }
}
