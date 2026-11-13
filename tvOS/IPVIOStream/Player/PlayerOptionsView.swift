import KSPlayer
import SwiftUI

@MainActor
struct PlayerOptionsView: View {
    @ObservedObject var vm: PlayerViewModel
    @Environment(\.dismiss) private var dismiss

    private let speeds: [Float] = [0.75, 1, 1.25, 1.5, 2]
    private let sleepOptions: [Int?] = [nil, 15, 30, 45, 60]

    var body: some View {
        ZStack {
            Theme.background.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 30) {
                    header

                    if vm.variants.count > 1 {
                        section(title: L.t("player.quality"), symbol: "arrow.up.arrow.down.circle") {
                            ForEach(vm.variants.indices, id: \.self) { index in
                                let variant = vm.variants[index]
                                PlayerOptionRow(
                                    title: variant.displayLabel,
                                    subtitle: nil,
                                    selected: index == vm.selectedVariantIndex
                                ) {
                                    vm.changeVariant(index: index)
                                    dismiss()
                                }
                            }
                        }
                    }

                    section(title: L.t("player.audio"), symbol: "waveform") {
                        if vm.audioTracks.isEmpty {
                            Text(L.t("player.noTracks"))
                                .font(.system(size: 18))
                                .foregroundStyle(Theme.textDim)
                        } else {
                            ForEach(vm.audioTracks, id: \.trackID) { track in
                                PlayerOptionRow(
                                    title: track.name,
                                    subtitle: track.languageCode?.uppercased(),
                                    selected: track.trackID == vm.selectedAudioID
                                ) {
                                    vm.selectAudio(id: track.trackID)
                                    dismiss()
                                }
                            }
                        }
                    }

                    section(title: L.t("player.subtitles"), symbol: "captions.bubble") {
                        PlayerOptionRow(
                            title: L.t("player.subtitleOff"),
                            subtitle: nil,
                            selected: vm.selectedSubtitleID == nil
                        ) {
                            vm.selectSubtitle(id: nil)
                            dismiss()
                        }
                        ForEach(vm.subtitleInfos, id: \.subtitleID) { info in
                            PlayerOptionRow(
                                title: info.name,
                                subtitle: nil,
                                selected: vm.selectedSubtitleID == info.subtitleID
                            ) {
                                vm.selectSubtitle(id: info.subtitleID)
                                dismiss()
                            }
                        }
                    }

                    section(title: L.t("player.speed"), symbol: "gauge.with.dots.needle.67percent") {
                        HStack(spacing: 12) {
                            ForEach(speeds, id: \.self) { rate in
                                PlayerChoiceChip(
                                    title: String(format: "%.2g×", rate),
                                    selected: abs(vm.playbackRate - rate) < 0.01
                                ) {
                                    vm.setSpeed(rate)
                                }
                            }
                        }
                    }

                    section(title: L.t("player.sleepTimer"), symbol: "moon.zzz") {
                        HStack(spacing: 12) {
                            ForEach(sleepOptions, id: \.self) { minutes in
                                PlayerChoiceChip(
                                    title: minutes.map { "\($0) \(L.t("common.min"))" } ?? L.t("common.off"),
                                    selected: vm.sleepMinutes == minutes
                                ) {
                                    vm.setSleepTimer(minutes: minutes)
                                }
                            }
                        }
                    }

                    section(title: L.t("player.info"), symbol: "info.circle") {
                        InfoLine(label: L.t("player.status"), value: vm.stateText)
                        InfoLine(label: L.t("player.resolution"), value: vm.resolutionText)
                        InfoLine(label: L.t("player.buffer"), value: vm.bufferText)
                        InfoLine(label: L.t("settings.quality"), value: vm.selectedVariant?.quality ?? L.t("common.auto"))
                        InfoLine(label: L.t("settings.engine"), value: vm.settings.preferFFmpeg ? "KSPlayer / FFmpeg" : "KSPlayer / AVPlayer")
                        if let host = vm.url?.host {
                            InfoLine(label: L.t("misc.source"), value: host)
                        }
                        if let video = videoDescription {
                            InfoLine(label: L.t("player.video"), value: video)
                        }
                    }
                }
                .padding(48)
                .frame(maxWidth: 1100, alignment: .leading)
                .frame(maxWidth: .infinity)
            }
        }
        .preferredColorScheme(.dark)
    }

    private var header: some View {
        HStack {
            VStack(alignment: .leading, spacing: 5) {
                Text(L.t("player.info"))
                    .font(.system(size: 40, weight: .heavy, design: .rounded))
                    .foregroundStyle(Theme.text)
                Text(vm.title)
                    .font(.system(size: 20))
                    .foregroundStyle(Theme.textDim)
            }
            Spacer()
            Button(L.t("common.done")) {
                dismiss()
            }
            .buttonStyle(.borderedProminent)
            .tint(Theme.accent)
            .foregroundStyle(Theme.background)
            .font(.system(size: 18, weight: .bold))
        }
    }

    private var videoDescription: String? {
        guard let layer = vm.coordinator.playerLayer else { return nil }
        let tracks = layer.player.tracks(mediaType: .video)
        guard let first = tracks.first else { return nil }
        return first.description
    }

    private func section<Content: View>(
        title: String,
        symbol: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 10) {
                Image(systemName: symbol)
                    .foregroundStyle(Theme.accent)
                Text(title)
                    .font(.system(size: 25, weight: .bold, design: .rounded))
                    .foregroundStyle(Theme.text)
            }
            VStack(alignment: .leading, spacing: 8) {
                content()
            }
        }
    }
}

private struct PlayerOptionRow: View {
    let title: String
    let subtitle: String?
    let selected: Bool
    let action: () -> Void
    @FocusState private var focused: Bool

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Text(title)
                    .font(.system(size: 20, weight: .medium))
                    .lineLimit(1)
                if let subtitle, !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.system(size: 14, weight: .bold))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(focused ? Color.black.opacity(0.12) : Color.white.opacity(0.08), in: Capsule())
                }
                Spacer()
                if selected {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(focused ? Color.black : Theme.accent)
                }
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(focused ? Color.white : Theme.surface2, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            .foregroundStyle(focused ? Color.black : Theme.text)
        }
        .buttonStyle(.plain)
        .focused($focused)
        .scaleEffect(focused ? 1.01 : 1)
        .animation(.easeOut(duration: 0.12), value: focused)
    }
}

private struct PlayerChoiceChip: View {
    let title: String
    let selected: Bool
    let action: () -> Void
    @FocusState private var focused: Bool

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.system(size: 18, weight: .semibold))
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
                .background(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(focused ? Color.white : (selected ? Theme.accent.opacity(0.20) : Theme.surface2))
                )
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(selected && !focused ? Theme.accent : Color.clear, lineWidth: 1.5)
                )
                .foregroundStyle(focused ? Color.black : (selected ? Theme.accentLight : Theme.text))
        }
        .buttonStyle(.plain)
        .focused($focused)
    }
}

private struct InfoLine: View {
    let label: String
    let value: String

    var body: some View {
        HStack(alignment: .top, spacing: 18) {
            Text(label)
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(Theme.textDim)
                .frame(width: 180, alignment: .leading)
            Text(value)
                .font(.system(size: 17, design: .monospaced))
                .foregroundStyle(Theme.text)
                .lineLimit(2)
            Spacer()
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 10)
        .background(Theme.surface.opacity(0.55), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
    }
}
