import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var app: AppModel

    @State private var showAddPlaylist = false
    @State private var showClearAll = false
    @State private var deleteTarget: Playlist?
    @State private var tmdbKey = ""
    @State private var omdbKey = ""

    private let bufferOptions = [10, 15, 30, 60]

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 26) {
                ScreenTitle(title: L.t("settings.title"), subtitle: L.t("app.tagline"))

                playlistsCard
                playbackCard
                contentCard
                apiCard
                languageCard
                storageCard
                aboutCard
            }
            .padding(.horizontal, Theme.pagePadding)
            .padding(.top, 40)
            .padding(.bottom, 70)
        }
        .background(Theme.background)
        .onAppear {
            tmdbKey = app.settings.tmdbKey
            omdbKey = app.settings.omdbKey
        }
        .sheet(isPresented: $showAddPlaylist) {
            AddPlaylistView()
                .environmentObject(app)
        }
        .alert(L.t("settings.deletePlaylist"), isPresented: Binding(
            get: { deleteTarget != nil },
            set: { if !$0 { deleteTarget = nil } }
        )) {
            Button(L.t("common.delete"), role: .destructive) {
                if let target = deleteTarget {
                    Task { await app.deletePlaylist(target.id) }
                }
                deleteTarget = nil
            }
            Button(L.t("common.cancel"), role: .cancel) {
                deleteTarget = nil
            }
        } message: {
            Text(L.t("settings.deletePlaylistMessage"))
        }
        .alert(L.t("settings.clearAllConfirm"), isPresented: $showClearAll) {
            Button(L.t("settings.clear"), role: .destructive) {
                Task { await app.clearEverything() }
            }
            Button(L.t("common.cancel"), role: .cancel) {}
        } message: {
            Text(L.t("settings.clearAllMessage"))
        }
    }

    // MARK: Playlists

    private var playlistsCard: some View {
        SettingsCard(
            title: L.t("settings.playlists"),
            symbol: "rectangle.stack",
            subtitle: L.t("settings.playlistsHint")
        ) {
            VStack(spacing: 8) {
                ForEach(app.playlists) { playlist in
                    PlaylistSettingsRow(
                        playlist: playlist,
                        sourceName: app.sourceName(for: playlist.id),
                        onRefresh: {
                            Task { await app.refresh(playlistID: playlist.id) }
                        },
                        onToggle: {
                            app.togglePlaylist(playlist.id)
                        },
                        onDelete: {
                            deleteTarget = playlist
                        }
                    )
                }
            }

            HStack(spacing: 12) {
                Button {
                    showAddPlaylist = true
                } label: {
                    Label(L.t("settings.addPlaylist"), systemImage: "plus")
                        .font(.system(size: 20.1, weight: .semibold))
                }
                .buttonStyle(.borderedProminent)
                .tint(Theme.accent)
                .foregroundStyle(Theme.background)

                Button {
                    Task { await app.refreshAll() }
                } label: {
                    Label(L.t("common.refresh"), systemImage: "arrow.clockwise")
                        .font(.system(size: 20.1, weight: .semibold))
                }
                .buttonStyle(.bordered)
                .disabled(app.job != nil)

                Spacer()
                Text(L.t("settings.secure"))
                    .font(.system(size: 15.3))
                    .foregroundStyle(Theme.textFaint)
            }
            .padding(.top, 4)
        }
    }

    // MARK: Playback

    private var playbackCard: some View {
        SettingsCard(title: L.t("settings.playback"), symbol: "play.circle") {
            VStack(alignment: .leading, spacing: 16) {
                settingLabel(L.t("settings.quality"), hint: L.t("settings.qualityHint"))
                HStack(spacing: 10) {
                    ForEach(QualityCap.allCases, id: \.self) { cap in
                        SelectChip(
                            title: L.t(cap.titleKey),
                            selected: app.settings.qualityCap == cap
                        ) {
                            app.updateSettings { $0.qualityCap = cap }
                        }
                    }
                }

                Divider().overlay(Theme.border)

                toggleRow(L.t("settings.autoNext"), binding: app.binding(\.autoPlayNext))
                toggleRow(L.t("settings.resume"), binding: app.binding(\.resumePlayback))
                toggleRow(
                    L.t("settings.ffmpeg"),
                    subtitle: L.t("settings.ffmpegHint"),
                    binding: app.binding(\.preferFFmpeg)
                )

                Divider().overlay(Theme.border)

                settingLabel(L.t("settings.buffer"))
                HStack(spacing: 10) {
                    ForEach(bufferOptions, id: \.self) { seconds in
                        SelectChip(
                            title: "\(seconds) s",
                            selected: app.settings.bufferSeconds == seconds
                        ) {
                            app.updateSettings { $0.bufferSeconds = seconds }
                        }
                    }
                }

                Divider().overlay(Theme.border)

                settingLabel(L.t("settings.audioLang"))
                languageMenu(
                    selection: app.settings.preferredAudioLanguage,
                    includeNone: false
                ) { code in
                    app.updateSettings { $0.preferredAudioLanguage = code }
                }

                settingLabel(L.t("settings.subtitleLang"))
                languageMenu(
                    selection: app.settings.preferredSubtitleLanguage,
                    includeNone: true
                ) { code in
                    app.updateSettings { $0.preferredSubtitleLanguage = code }
                }

                toggleRow(L.t("settings.subtitles"), binding: app.binding(\.subtitlesEnabled))
            }
        }
    }

    private var contentCard: some View {
        SettingsCard(
            title: L.t("settings.content"),
            symbol: "globe",
            subtitle: L.t("settings.languageFilterHint")
        ) {
            VStack(alignment: .leading, spacing: 14) {
                settingLabel(L.t("settings.languageFilter"))
                let counts = app.languageCounts()
                let visible = counts.filter { ContentLanguage.common.contains($0.code) || $0.code == ContentLanguage.multi || $0.code == ContentLanguage.unknown }
                LazyVGrid(columns: [GridItem(.adaptive(minimum: 150, maximum: 210), spacing: 10)], alignment: .leading, spacing: 10) {
                    ForEach(visible.prefix(24)) { entry in
                        SelectChip(
                            title: ContentLanguage.displayName(entry.code),
                            selected: app.settings.languageFilter.contains(entry.code),
                            count: entry.count
                        ) {
                            app.updateSettings { settings in
                                if settings.languageFilter.contains(entry.code) {
                                    settings.languageFilter.remove(entry.code)
                                } else {
                                    settings.languageFilter.insert(entry.code)
                                }
                            }
                        }
                    }
                }
                if !app.settings.languageFilter.isEmpty {
                    Button(L.t("common.none")) {
                        app.updateSettings { $0.languageFilter = [] }
                    }
                    .buttonStyle(.bordered)
                    .font(.system(size: 17.7, weight: .semibold))
                }
                toggleRow(L.t("settings.liveEPG"), binding: app.binding(\.showLiveEPG))
            }
        }
    }

    private var apiCard: some View {
        SettingsCard(
            title: L.t("settings.api"),
            symbol: "key.fill",
            subtitle: L.t("settings.tmdbHint")
        ) {
            VStack(alignment: .leading, spacing: 14) {
                LabeledTextField(
                    title: L.t("settings.tmdb"),
                    placeholder: "TMDB API key",
                    text: $tmdbKey
                )
                LabeledTextField(
                    title: L.t("settings.omdb"),
                    placeholder: "OMDb API key",
                    text: $omdbKey
                )
                HStack(spacing: 12) {
                    Button(L.t("settings.apiSave")) {
                        app.updateSettings {
                            $0.tmdbKey = tmdbKey.trimmed
                            $0.omdbKey = omdbKey.trimmed
                        }
                        app.showToast(L.t("settings.keysSaved"))
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(Theme.accent)
                    .foregroundStyle(Theme.background)
                    if !app.settings.tmdbKey.isEmpty || !app.settings.omdbKey.isEmpty {
                        Text(L.t("settings.keySaved"))
                            .font(.system(size: 16.5, weight: .semibold))
                            .foregroundStyle(Theme.accentLight)
                    }
                }
            }
        }
    }

    private var languageCard: some View {
        SettingsCard(
            title: L.t("settings.appearance"),
            symbol: "character.bubble",
            subtitle: L.t("settings.appLanguageHint")
        ) {
            HStack(spacing: 10) {
                ForEach(AppLanguage.allCases, id: \.self) { language in
                    SelectChip(
                        title: L.t(language.titleKey),
                        selected: app.settings.appLanguage == language
                    ) {
                        app.updateSettings { $0.appLanguage = language }
                    }
                }
            }
        }
    }

    private var storageCard: some View {
        SettingsCard(title: L.t("settings.storage"), symbol: "externaldrive") {
            HStack(spacing: 12) {
                Button {
                    Task { await app.clearMetadataCache() }
                } label: {
                    Label(L.t("settings.clearMetadata"), systemImage: "photo.on.rectangle.angled")
                        .font(.system(size: 18.9, weight: .semibold))
                }
                .buttonStyle(.bordered)

                Button(role: .destructive) {
                    showClearAll = true
                } label: {
                    Label(L.t("settings.clearAll"), systemImage: "trash")
                        .font(.system(size: 18.9, weight: .semibold))
                }
                .buttonStyle(.bordered)
                .tint(Theme.red)

                Spacer()
            }
            Text(L.t("settings.clearAllHint"))
                .font(.system(size: 16.5))
                .foregroundStyle(Theme.textFaint)
        }
    }

    private var aboutCard: some View {
        SettingsCard(title: L.t("settings.about"), symbol: "info.circle") {
            VStack(alignment: .leading, spacing: 9) {
                aboutLine(L.t("settings.version"), AppInfo.version)
                aboutLine(L.t("settings.device"), L.t("settings.device"))
                Text(L.t("settings.aboutText"))
                    .font(.system(size: 17.7))
                    .foregroundStyle(Theme.textDim)
                    .fixedSize(horizontal: false, vertical: true)
                Text(L.t("settings.ksplayer"))
                    .font(.system(size: 16.5, weight: .semibold))
                    .foregroundStyle(Theme.accentLight)
                Text(L.t("settings.gpl"))
                    .font(.system(size: 15.3))
                    .foregroundStyle(Theme.textFaint)
            }
        }
    }

    // MARK: Helpers

    private func aboutLine(_ label: String, _ value: String) -> some View {
        HStack(spacing: 14) {
            Text(label)
                .font(.system(size: 17.7, weight: .semibold))
                .foregroundStyle(Theme.textDim)
                .frame(width: 120, alignment: .leading)
            Text(value)
                .font(.system(size: 17.7, design: .monospaced))
                .foregroundStyle(Theme.text)
        }
    }

    private func settingLabel(_ text: String, hint: String? = nil) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(text)
                .font(.system(size: 21.2, weight: .semibold))
                .foregroundStyle(Theme.text)
            if let hint {
                Text(hint)
                    .font(.system(size: 15.3))
                    .foregroundStyle(Theme.textFaint)
            }
        }
    }

    private func toggleRow(_ title: String, subtitle: String? = nil, binding: Binding<Bool>) -> some View {
        HStack(spacing: 20) {
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.system(size: 21.2, weight: .medium))
                    .foregroundStyle(Theme.text)
                if let subtitle {
                    Text(subtitle)
                        .font(.system(size: 15.3))
                        .foregroundStyle(Theme.textFaint)
                }
            }
            Spacer()
            Toggle("", isOn: binding)
                .labelsHidden()
                .tint(Theme.accent)
        }
    }

    private func languageMenu(selection: String, includeNone: Bool, onChange: @escaping (String) -> Void) -> some View {
        Menu {
            if includeNone {
                Button(L.t("common.none")) { onChange("") }
            }
            ForEach(languageCodes, id: \.self) { code in
                Button(ContentLanguage.displayName(code)) { onChange(code) }
            }
        } label: {
            Label(
                selection.isEmpty ? L.t("common.none") : ContentLanguage.displayName(selection),
                systemImage: "globe"
            )
            .font(.system(size: 20.1, weight: .semibold))
        }
        .buttonStyle(.bordered)
    }

    private var languageCodes: [String] {
        ["ar", "en", "fr", "de", "tr", "es", "it", "nl", "pt", "ru", "hi", "ur", "fa", "pl", "ro", "el", "sv", "da", "no", "fi", "cs", "hu", "bg", "uk", "he", "sq", "hr", "sr", "bs"]
    }
}

private struct PlaylistSettingsRow: View {
    let playlist: Playlist
    let sourceName: String
    let onRefresh: () -> Void
    let onToggle: () -> Void
    let onDelete: () -> Void

    var body: some View {
        HStack(spacing: 16) {
            ZStack {
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(Theme.surface2)
                Image(systemName: playlist.source.isXtream ? "server.rack" : "list.bullet.rectangle")
                    .font(.system(size: 23.6, weight: .semibold))
                    .foregroundStyle(playlist.isEnabled ? Theme.accent : Theme.textFaint)
            }
            .frame(width: 48, height: 48)

            VStack(alignment: .leading, spacing: 3) {
                HStack(spacing: 8) {
                    Text(playlist.name)
                        .font(.system(size: 21.2, weight: .semibold))
                        .foregroundStyle(Theme.text)
                    Text(playlist.source.typeLabel)
                        .font(.system(size: 11.8, weight: .bold))
                        .foregroundStyle(Theme.textFaint)
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(Theme.surface2, in: Capsule())
                    if !playlist.isEnabled {
                        Text(L.t("common.disable"))
                            .font(.system(size: 11.8, weight: .bold))
                            .foregroundStyle(Theme.gold)
                    }
                }
                Text(playlist.lastError.map { L.f("settings.sourceError", $0) } ?? summary)
                    .font(.system(size: 15.3))
                    .foregroundStyle(playlist.lastError == nil ? Theme.textDim : Theme.red.opacity(0.85))
                    .lineLimit(1)
            }

            Spacer()

            if playlist.lastUpdated != nil {
                Text(TimeText.relative(playlist.lastUpdated!, lang: L.current))
                    .font(.system(size: 14.2))
                    .foregroundStyle(Theme.textFaint)
            }

            Button {
                onToggle()
            } label: {
                Image(systemName: playlist.isEnabled ? "eye" : "eye.slash")
                    .frame(width: 38, height: 38)
            }
            .buttonStyle(.bordered)

            Button {
                onRefresh()
            } label: {
                Image(systemName: "arrow.clockwise")
                    .frame(width: 38, height: 38)
            }
            .buttonStyle(.bordered)

            Button(role: .destructive) {
                onDelete()
            } label: {
                Image(systemName: "trash")
                    .frame(width: 38, height: 38)
            }
            .buttonStyle(.bordered)
            .tint(Theme.red)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(Theme.surface2.opacity(0.45), in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private var summary: String {
        var parts = [sourceName, "\(playlist.itemCount) \(L.t("common.items"))"]
        if let updated = playlist.lastUpdated {
            parts.append(L.f("settings.lastUpdated", TimeText.relative(updated, lang: L.current)))
        } else {
            parts.append(L.t("settings.neverUpdated"))
        }
        return parts.joined(separator: " · ")
    }
}
