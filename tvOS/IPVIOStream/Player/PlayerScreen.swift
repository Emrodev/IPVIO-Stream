import KSPlayer
import KSPlayerUI
import SwiftUI

// MARK: - Container

@MainActor
struct PlayerContainer: View {
    @EnvironmentObject private var app: AppModel
    let request: PlaybackRequest

    var body: some View {
        if let item = app.item(id: request.itemID) {
            let queue = request.episodes.isEmpty ? item.episodes : request.episodes
            let initial: Episode? = {
                if let id = request.episodeID, let match = queue.first(where: { $0.id == id }) {
                    return match
                }
                return item.kind == .series ? queue.first : nil
            }()
            let start = request.startAt ?? app.resumePosition(for: item, episode: initial)
            PlayerScreen(
                item: item,
                episodes: queue,
                initialEpisode: initial,
                startAt: start,
                metadata: app.metadataByItem[item.id],
                settings: app.settings
            )
        } else {
            MissingPlayerView()
        }
    }
}

private struct MissingPlayerView: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack {
            Theme.background.ignoresSafeArea()
            EmptyStateView(
                symbol: "exclamationmark.triangle",
                title: L.t("error.title"),
                message: L.t("error.selection"),
                actionTitle: L.t("common.close"),
                action: { dismiss() }
            )
        }
    }
}

// MARK: - Player

@MainActor
struct PlayerScreen: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var app: AppModel

    private enum FocusTarget: Hashable {
        case catcher
        case play
        case error
        case ended
    }

    let item: MediaItem
    let episodes: [Episode]
    let metadata: EnrichedMetadata?

    @StateObject private var vm: PlayerViewModel
    @State private var showOptions = false
    @FocusState private var focus: FocusTarget?

    init(
        item: MediaItem,
        episodes: [Episode],
        initialEpisode: Episode?,
        startAt: Double?,
        metadata: EnrichedMetadata?,
        settings: AppSettings
    ) {
        self.item = item
        self.episodes = episodes
        self.metadata = metadata
        _vm = StateObject(wrappedValue: PlayerViewModel(
            item: item,
            episodes: episodes,
            initialEpisode: initialEpisode,
            startAt: startAt,
            settings: settings,
            tmdbID: metadata?.tmdbID
        ))
    }

    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()

            if let url = vm.url {
                KSVideoPlayer(coordinator: vm.coordinator, url: url, options: vm.options)
                    .ignoresSafeArea()
                    .id(vm.generation)
            }

            focusCatcher

            if vm.showControls {
                controls
                    .transition(.opacity.combined(with: .scale(scale: 1.01)))
            }

            if vm.state == .buffering, vm.errorMessage == nil {
                bufferingIndicator
            }

            if let feedback = vm.seekFeedback {
                seekFeedback(feedback)
            }

            if let banner = vm.banner {
                bannerView(banner)
            }

            if let error = vm.errorMessage, !vm.retrying {
                errorOverlay(message: error)
            }

            if vm.retrying {
                retryingOverlay
            }

            if vm.ended {
                endedOverlay
            }
        }
        .animation(.easeInOut(duration: 0.22), value: vm.showControls)
        .animation(.easeInOut(duration: 0.20), value: vm.ended)
        .onAppear {
            vm.onProgress = { position, duration in
                app.saveProgress(item: item, episode: vm.currentEpisode, position: position, duration: duration)
            }
            vm.resumeProvider = { episode in
                app.resumePosition(for: item, episode: episode)
            }
            vm.start()
            DispatchQueue.main.async {
                focus = .play
            }
        }
        .onDisappear {
            vm.stop()
        }
        .onChange(of: vm.showControls) { _, visible in
            DispatchQueue.main.async {
                focus = visible ? .play : .catcher
            }
        }
        .onChange(of: showOptions) { _, presented in
            if !presented {
                DispatchQueue.main.async { focus = .play }
            }
        }
        .onChange(of: vm.errorMessage) { _, message in
            if message != nil {
                DispatchQueue.main.async { focus = .error }
            } else if !vm.ended {
                DispatchQueue.main.async { focus = .play }
            }
        }
        .onChange(of: vm.ended) { _, ended in
            if ended {
                DispatchQueue.main.async { focus = .ended }
            } else {
                DispatchQueue.main.async { focus = .play }
            }
        }
        .onChange(of: vm.episodeIndex) { _, _ in
            DispatchQueue.main.async { focus = .play }
        }
        .onPlayPauseCommand {
            vm.togglePlayback()
        }
        .onExitCommand {
            if showOptions { return }
            if vm.errorMessage != nil || vm.ended {
                dismiss()
            } else if vm.showControls, vm.isPlaying {
                vm.hideControls()
            } else {
                dismiss()
            }
        }
        .onMoveCommand { direction in
            guard !vm.showControls, vm.errorMessage == nil, !vm.ended else { return }
            switch direction {
            case .left:
                vm.seek(by: -15)
            case .right:
                vm.seek(by: 15)
            case .up, .down:
                vm.revealControls()
            default:
                break
            }
        }
        .sheet(isPresented: $showOptions) {
            PlayerOptionsView(vm: vm)
        }
        .preferredColorScheme(.dark)
    }

    // MARK: Layers

    private var focusCatcher: some View {
        Color.white.opacity(0.001)
            .ignoresSafeArea()
            .contentShape(Rectangle())
            .focusable(!vm.showControls)
            .focused($focus, equals: .catcher)
            .onTapGesture {
                vm.revealControls()
            }
    }

    private var controls: some View {
        ZStack {
            LinearGradient(
                colors: [Color.black.opacity(0.78), Color.black.opacity(0.18), Color.black.opacity(0.86)],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
            .allowsHitTesting(false)

            VStack(spacing: 0) {
                topBar
                Spacer()
                centerControls
                Spacer()
                bottomControls
            }
            .padding(.horizontal, 64)
            .padding(.vertical, 40)
        }
    }

    private var topBar: some View {
        HStack(alignment: .center, spacing: 18) {
            PlayerCircleButton(symbol: "xmark", action: { dismiss() })
            VStack(alignment: .leading, spacing: 3) {
                Text(vm.title)
                    .font(.system(size: 30, weight: .bold, design: .rounded))
                    .foregroundStyle(.white)
                    .lineLimit(1)
                if let subtitle = vm.subtitle {
                    Text(subtitle)
                        .font(.system(size: 18, weight: .medium))
                        .foregroundStyle(Theme.textDim)
                        .lineLimit(1)
                }
            }
            Spacer()
            if item.kind == .live {
                liveBadge
            }
            if let quality = vm.selectedVariant?.quality, !quality.isEmpty {
                TagBadge(text: quality, tint: .black, background: Theme.green)
            }
            if let rating = metadata?.bestRating {
                TagBadge(text: String(format: "★ %.1f", rating), tint: .black, background: Theme.gold)
            }
            Text(Date(), style: .time)
                .font(.system(size: 18, weight: .semibold, design: .monospaced))
                .foregroundStyle(Theme.textDim)
        }
    }

    private var centerControls: some View {
        HStack(spacing: 28) {
            if vm.hasPrevious {
                PlayerIconButton(symbol: "backward.end.fill", label: "") {
                    vm.playPrevious()
                }
            }

            if !item.kind.isLive {
                PlayerIconButton(symbol: "gobackward.15", label: L.t("player.back15")) {
                    vm.seek(by: -15)
                }
                .disabled(vm.duration <= 0)
            }

            Button {
                vm.togglePlayback()
            } label: {
                Image(systemName: iconForPlayState)
                    .font(.system(size: 38, weight: .bold))
                    .foregroundStyle(Theme.background)
                    .frame(width: 96, height: 96)
                    .background(Theme.green, in: Circle())
                    .shadow(color: Theme.green.opacity(0.45), radius: 22)
            }
            .buttonStyle(.plain)
            .focused($focus, equals: .play)
            .scaleEffect(focus == .play ? 1.06 : 1)
            .animation(.easeOut(duration: 0.15), value: focus)

            if !item.kind.isLive {
                PlayerIconButton(symbol: "goforward.15", label: L.t("player.forward15")) {
                    vm.seek(by: 15)
                }
                .disabled(vm.duration <= 0)
            }

            if vm.hasNext {
                PlayerIconButton(symbol: "forward.end.fill", label: L.t("player.nextEpisode")) {
                    vm.playNext()
                }
            }
        }
    }

    private var bottomControls: some View {
        VStack(spacing: 18) {
            if let segment = vm.currentSegment {
                HStack {
                    Spacer()
                    Button {
                        vm.skipCurrentSegment()
                    } label: {
                        HStack(spacing: 10) {
                            Image(systemName: "forward.fill")
                            Text(L.t(segment.kind.titleKey))
                                .font(.system(size: 19, weight: .bold))
                        }
                        .padding(.horizontal, 22)
                        .padding(.vertical, 12)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(Theme.green)
                    .foregroundStyle(Theme.background)
                }
            }

            if !item.kind.isLive, vm.duration > 0 {
                PlayerScrubber(vm: vm)
            } else if item.kind == .live {
                HStack(spacing: 12) {
                    liveBadge
                    Text(L.t("player.liveHint"))
                        .font(.system(size: 17))
                        .foregroundStyle(Theme.textDim)
                    Spacer()
                }
            }

            HStack(spacing: 10) {
                PlayerIconButton(symbol: "waveform", label: vm.selectedAudioName) {
                    showOptions = true
                }
                PlayerIconButton(symbol: "captions.bubble", label: vm.selectedSubtitleName) {
                    showOptions = true
                }
                if vm.variants.count > 1 {
                    PlayerIconButton(symbol: "arrow.up.arrow.down.circle", label: vm.selectedVariant?.quality ?? L.t("player.quality")) {
                        showOptions = true
                    }
                }
                PlayerIconButton(symbol: "rectangle.arrowtriangle.2.outward", label: L.t("player.aspect")) {
                    vm.toggleAspect()
                }
                PlayerIconButton(symbol: "gauge.with.dots.needle.67percent", label: String(format: "%.2g×", vm.playbackRate)) {
                    vm.cycleSpeed()
                }
                PlayerIconButton(symbol: "speaker.wave.2", label: L.t("player.audio")) {
                    vm.toggleMute()
                }
                PlayerIconButton(symbol: "info.circle", label: L.t("player.info")) {
                    showOptions = true
                }
                PlayerIconButton(symbol: "moon.zzz", label: L.t("player.sleepTimer")) {
                    showOptions = true
                }
                Spacer()
                if vm.hasNext {
                    Button {
                        vm.playNext()
                    } label: {
                        HStack(spacing: 10) {
                            Text(L.t("player.nextEpisode"))
                            Image(systemName: "forward.fill")
                        }
                        .font(.system(size: 18, weight: .bold))
                        .padding(.horizontal, 20)
                        .padding(.vertical, 10)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(.white)
                    .foregroundStyle(Theme.background)
                }
            }
        }
    }

    // MARK: Overlays

    private var bufferingIndicator: some View {
        VStack(spacing: 14) {
            ProgressView()
                .controlSize(.large)
                .tint(.white)
            Text(L.t("player.buffering"))
                .font(.system(size: 17, weight: .medium))
                .foregroundStyle(Theme.textDim)
        }
        .padding(26)
        .background(.black.opacity(0.45), in: RoundedRectangle(cornerRadius: 20, style: .continuous))
        .allowsHitTesting(false)
    }

    private func seekFeedback(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 30, weight: .bold, design: .rounded))
            .foregroundStyle(.white)
            .padding(.horizontal, 22)
            .padding(.vertical, 12)
            .background(.black.opacity(0.55), in: Capsule())
            .allowsHitTesting(false)
    }

    private func bannerView(_ text: String) -> some View {
        VStack {
            ToastView(message: text)
                .padding(.top, 26)
            Spacer()
        }
        .allowsHitTesting(false)
    }

    private var liveBadge: some View {
        HStack(spacing: 7) {
            Circle()
                .fill(Theme.red)
                .frame(width: 8, height: 8)
            Text(L.t("player.live"))
                .font(.system(size: 15, weight: .heavy))
        }
        .foregroundStyle(.white)
        .padding(.horizontal, 12)
        .padding(.vertical, 6)
        .background(Theme.red.opacity(0.20), in: Capsule())
        .overlay(Capsule().stroke(Theme.red.opacity(0.55), lineWidth: 1))
    }

    private func errorOverlay(message: String) -> some View {
        ZStack {
            Color.black.opacity(0.82).ignoresSafeArea()
            VStack(spacing: 20) {
                Image(systemName: "wifi.exclamationmark")
                    .font(.system(size: 48, weight: .light))
                    .foregroundStyle(Theme.gold)
                Text(L.t("player.error"))
                    .font(.system(size: 34, weight: .bold, design: .rounded))
                    .foregroundStyle(.white)
                Text(message)
                    .font(.system(size: 19))
                    .foregroundStyle(Theme.textDim)
                    .multilineTextAlignment(.center)
                    .frame(maxWidth: 720)
                    .lineLimit(4)
                HStack(spacing: 18) {
                    Button(L.t("player.tryAgain")) {
                        vm.retry()
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(Theme.green)
                    .foregroundStyle(Theme.background)
                    .focused($focus, equals: .error)

                    if vm.variants.count > 1, vm.selectedVariantIndex + 1 < vm.variants.count {
                        Button(L.t("player.nextSource")) {
                            vm.nextSource()
                        }
                        .buttonStyle(.bordered)
                    }

                    Button(L.t("common.close")) {
                        dismiss()
                    }
                    .buttonStyle(.bordered)
                }
                .font(.system(size: 19, weight: .semibold))
            }
            .padding(44)
            .background(Theme.surface.opacity(0.95), in: RoundedRectangle(cornerRadius: 26, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 26, style: .continuous).stroke(Theme.border, lineWidth: 1))
        }
    }

    private var retryingOverlay: some View {
        ZStack {
            Color.black.opacity(0.55).ignoresSafeArea()
            VStack(spacing: 16) {
                ProgressView()
                    .controlSize(.large)
                Text(L.t("live.retry"))
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(.white)
            }
            .padding(32)
            .background(Theme.surface.opacity(0.92), in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        }
    }

    private var endedOverlay: some View {
        ZStack {
            Color.black.opacity(0.78).ignoresSafeArea()
            VStack(spacing: 18) {
                Text(L.t("player.ended"))
                    .font(.system(size: 38, weight: .heavy, design: .rounded))
                    .foregroundStyle(.white)
                Text(vm.title)
                    .font(.system(size: 21))
                    .foregroundStyle(Theme.textDim)
                if let countdown = vm.nextCountdown {
                    Text(L.f("player.autoNext", countdown))
                        .font(.system(size: 18, weight: .medium))
                        .foregroundStyle(Theme.greenBright)
                }
                HStack(spacing: 16) {
                    if vm.hasNext {
                        Button(L.t("player.nextEpisode")) {
                            vm.playNext()
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(Theme.green)
                        .foregroundStyle(Theme.background)
                        .focused($focus, equals: .ended)
                    }
                    Button(L.t("player.replay")) {
                        vm.replay()
                    }
                    .buttonStyle(.bordered)
                    Button(L.t("common.close")) {
                        dismiss()
                    }
                    .buttonStyle(.bordered)
                }
                .font(.system(size: 19, weight: .semibold))
            }
            .padding(40)
            .background(Theme.surface.opacity(0.95), in: RoundedRectangle(cornerRadius: 26, style: .continuous))
        }
    }

    private var iconForPlayState: String {
        if vm.ended { return "arrow.counterclockwise" }
        return vm.state?.isPlaying == true ? "pause.fill" : "play.fill"
    }
}

extension MediaKind {
    var isLive: Bool { self == .live }
}

// MARK: - Small player buttons

struct PlayerCircleButton: View {
    let symbol: String
    let action: () -> Void
    @FocusState private var focused: Bool

    var body: some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 20, weight: .bold))
                .frame(width: 48, height: 48)
                .background(focused ? Color.white : Color.black.opacity(0.35), in: Circle())
                .foregroundStyle(focused ? Color.black : Color.white)
        }
        .buttonStyle(.plain)
        .focused($focused)
    }
}

struct PlayerIconButton: View {
    let symbol: String
    let label: String
    let action: () -> Void
    @FocusState private var focused: Bool

    var body: some View {
        Button(action: action) {
            VStack(spacing: 4) {
                Image(systemName: symbol)
                    .font(.system(size: 22, weight: .semibold))
                    .frame(height: 24)
                if !label.isEmpty {
                    Text(label)
                        .font(.system(size: 11, weight: .medium))
                        .lineLimit(1)
                        .minimumScaleFactor(0.75)
                }
            }
            .frame(minWidth: 68, minHeight: 54)
            .padding(.horizontal, 6)
            .background(
                RoundedRectangle(cornerRadius: 13, style: .continuous)
                    .fill(focused ? Color.white : Color.white.opacity(0.10))
            )
            .foregroundStyle(focused ? Color.black : Color.white)
            .scaleEffect(focused ? 1.05 : 1)
            .animation(.easeOut(duration: 0.12), value: focused)
        }
        .buttonStyle(.plain)
        .focused($focused)
    }
}

/// tvOS-friendly scrubber: focus it and use left/right on the remote to seek.
/// A native Slider is ambiguous across tvOS SDK versions and is hard to use
/// with the focus engine, so the player draws its own progress bar.
struct PlayerScrubber: View {
    @ObservedObject var vm: PlayerViewModel
    @FocusState private var focused: Bool

    private var fraction: CGFloat {
        guard vm.duration > 0 else { return 0 }
        return CGFloat(min(max(vm.currentTime / vm.duration, 0), 1))
    }

    var body: some View {
        VStack(spacing: 6) {
            GeometryReader { proxy in
                ZStack(alignment: .leading) {
                    Capsule()
                        .fill(Color.white.opacity(0.20))
                    Capsule()
                        .fill(Theme.green)
                        .frame(width: max(0, proxy.size.width * fraction))
                }
            }
            .frame(height: 12)

            HStack {
                Text(TimeText.clock(vm.currentTime))
                Spacer()
                Text(TimeText.clock(vm.duration))
            }
            .font(.system(size: 16, weight: .medium, design: .monospaced))
            .foregroundStyle(Theme.textDim)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .contentShape(Rectangle())
        .background(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .fill(focused ? Color.white.opacity(0.08) : Color.clear)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(focused ? Theme.green.opacity(0.85) : Color.clear, lineWidth: 2)
        )
        .focusable()
        .focused($focused)
        .onMoveCommand { direction in
            switch direction {
            case .left:
                vm.seek(by: -10)
            case .right:
                vm.seek(by: 10)
            default:
                break
            }
        }
    }
}
