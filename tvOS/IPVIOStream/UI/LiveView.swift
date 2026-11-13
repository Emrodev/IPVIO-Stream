import SwiftUI

struct LiveView: View {
    @EnvironmentObject private var app: AppModel
    @State private var selectedGroup = MenuSentinel.allGroups
    @State private var query = ""
    @State private var displayed: [MediaItem] = []

    private var groups: [GroupCount] { app.groups(kind: .live) }

    var body: some View {
        HStack(spacing: 0) {
            categorySidebar
            VStack(alignment: .leading, spacing: 0) {
                header
                channelList
            }
            .padding(.horizontal, Theme.pagePadding)
            .padding(.top, 34)
        }
        .background(Theme.background)
        .task(id: reloadToken) {
            rebuild()
        }
    }

    private var categorySidebar: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(L.t("live.categories"))
                .font(.system(size: 30.7, weight: .bold, design: .rounded))
                .foregroundStyle(Theme.text)
                .padding(.bottom, 4)

            ScrollView {
                LazyVStack(alignment: .leading, spacing: 6) {
                    LiveCategoryRow(
                        title: L.t("live.allChannels"),
                        count: app.liveItems.count,
                        selected: selectedGroup == MenuSentinel.allGroups
                    ) {
                        selectedGroup = MenuSentinel.allGroups
                    }
                    ForEach(groups) { group in
                        LiveCategoryRow(
                            title: app.displayGroup(group.name),
                            count: group.count,
                            selected: selectedGroup == group.name
                        ) {
                            selectedGroup = group.name
                        }
                    }
                }
                .padding(.vertical, 4)
                .padding(.trailing, 8)
                .focusSection()
            }
        }
        .padding(.top, 36)
        .padding(.leading, 28)
        .padding(.trailing, 18)
        .frame(width: 320)
        .frame(maxHeight: .infinity, alignment: .top)
        .background(Theme.backgroundElevated)
        .overlay(alignment: .trailing) {
            Rectangle().fill(Theme.border).frame(width: 1)
        }
    }

    private var header: some View {
        HStack(alignment: .bottom, spacing: 24) {
            ScreenTitle(
                title: L.t("nav.live"),
                subtitle: L.f("browse.count", displayed.count)
            )
            Spacer()
            HStack(spacing: 10) {
                Image(systemName: "magnifyingglass")
                    .foregroundStyle(Theme.textDim)
                TextField(L.t("live.searchChannels"), text: $query)
                    .textFieldStyle(.plain)
                    .font(.system(size: 20.1))
                    .frame(width: 300)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(Theme.surface2, in: Capsule())
            .overlay(Capsule().stroke(Theme.border, lineWidth: 1))
        }
        .padding(.bottom, 18)
    }

    private var channelList: some View {
        Group {
            if displayed.isEmpty {
                EmptyStateView(
                    symbol: "antenna.radiowaves.left.and.right",
                    title: L.t("browse.noResults"),
                    message: L.t("browse.noResultsHint")
                )
            } else {
                ScrollView {
                    LazyVStack(spacing: 8) {
                        ForEach(displayed) { item in
                            LiveChannelRow(item: item)
                        }
                    }
                    .padding(.horizontal, 8)
                    .padding(.vertical, 14)
                    .padding(.bottom, 50)
                }
                .scrollClipDisabled()
                .focusSection()
            }
        }
        .frame(maxHeight: .infinity)
    }

    private var reloadToken: String {
        [
            selectedGroup,
            query,
            String(app.liveItems.count),
            app.settings.languageFilter.sorted().joined(separator: ","),
            app.settings.showLiveEPG ? "epg" : "noepg",
        ].joined(separator: "|")
    }

    private func rebuild() {
        var values = app.items(
            kind: .live,
            group: selectedGroup == MenuSentinel.allGroups ? nil : selectedGroup
        )
        let needle = query.trimmed
        if !needle.isEmpty {
            values = values.filter {
                $0.title.localizedCaseInsensitiveContains(needle) || $0.group.localizedCaseInsensitiveContains(needle)
            }
        }
        displayed = values
    }
}

private struct LiveCategoryRow: View {
    let title: String
    let count: Int
    let selected: Bool
    let action: () -> Void
    @FocusState private var focused: Bool

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Text(title)
                    .font(.system(size: 18.9, weight: selected ? .bold : .medium))
                    .lineLimit(1)
                Spacer()
                Text("\(count)")
                    .font(.system(size: 14.2, weight: .bold))
                    .foregroundStyle(focused ? Color.black.opacity(0.65) : Theme.textFaint)
                    .padding(.horizontal, 7)
                    .padding(.vertical, 2)
                    .background(
                        Capsule().fill(focused ? Color.black.opacity(0.10) : Color.white.opacity(0.06))
                    )
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(focused ? Color.white : (selected ? Theme.accent.opacity(0.14) : Color.clear))
            )
            .foregroundStyle(focused ? Color.black : (selected ? Theme.accentLight : Theme.textDim))
        }
        .buttonStyle(.plain)
        .focused($focused)
    }
}

private struct LiveChannelRow: View {
    @EnvironmentObject private var app: AppModel
    let item: MediaItem

    @State private var programs: [EPGProgram] = []
    @State private var loadingEPG = false
    @State private var epgAttempted = false
    @FocusState private var focused: Bool

    var body: some View {
        Button {
            app.play(item: item)
        } label: {
            HStack(spacing: 16) {
                ZStack {
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .fill(Theme.surface2)
                    RemoteImage(urlString: item.logo, contentMode: .fit, fallbackText: item.title)
                        .padding(10)
                }
                .frame(width: 104, height: 66)

                VStack(alignment: .leading, spacing: 5) {
                    HStack(spacing: 8) {
                        Text(item.title)
                            .font(.system(size: 22.4, weight: .semibold))
                            .foregroundStyle(Theme.text)
                            .lineLimit(1)
                        if let quality = item.quality {
                            Text(quality)
                                .font(.system(size: 11.8, weight: .heavy))
                                .foregroundStyle(Theme.accentLight)
                                .padding(.horizontal, 5)
                                .padding(.vertical, 2)
                                .background(Theme.surface2, in: Capsule())
                        }
                        if let days = item.catchupDays {
                            InfoPill(symbol: "clock.arrow.circlepath", text: "\(days)d", tint: Theme.textFaint)
                        }
                    }
                    epgLine
                }

                Spacer(minLength: 8)

                HStack(spacing: 6) {
                    if app.isFavorite(item.id) {
                        Image(systemName: "heart.fill")
                            .font(.system(size: 12.4, weight: .bold))
                            .foregroundStyle(Theme.accent)
                    }
                    Circle()
                        .fill(Theme.red)
                        .frame(width: 7, height: 7)
                    Text(L.t("live.live"))
                        .font(.system(size: 14.2, weight: .heavy))
                        .foregroundStyle(Theme.red)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
                .background(Theme.red.opacity(0.12), in: Capsule())
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 9)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .buttonStyle(.card)
        .focused($focused)
        .contextMenu {
            Button {
                app.toggleFavorite(item.id)
            } label: {
                Label(
                    app.isFavorite(item.id) ? L.t("detail.removeFavorite") : L.t("detail.addFavorite"),
                    systemImage: app.isFavorite(item.id) ? "heart.slash" : "heart"
                )
            }
        }
        .onChange(of: focused) { _, value in
            if value { loadEPGIfNeeded() }
        }
        .task {
            if focused { loadEPGIfNeeded() }
        }
    }

    @ViewBuilder
    private var epgLine: some View {
        if loadingEPG {
            Text(L.t("common.loading"))
                .font(.system(size: 16.5))
                .foregroundStyle(Theme.textFaint)
        } else if let now = programs.now() {
            HStack(spacing: 6) {
                Text(L.t("live.nowPlaying") + ":")
                    .font(.system(size: 16.5, weight: .semibold))
                    .foregroundStyle(Theme.textDim)
                Text(now.title)
                    .font(.system(size: 16.5, weight: .semibold))
                    .foregroundStyle(Theme.accentLight)
                    .lineLimit(1)
                Text(now.timeRange)
                    .font(.system(size: 15.3))
                    .foregroundStyle(Theme.textFaint)
                if let next = programs.next() {
                    Text("·")
                        .foregroundStyle(Theme.textFaint)
                    Text(L.t("live.upNext") + ": " + next.title)
                        .font(.system(size: 15.3))
                        .foregroundStyle(Theme.textDim)
                        .lineLimit(1)
                }
            }
        } else {
            HStack(spacing: 6) {
                Text(app.displayGroup(item.group))
                    .font(.system(size: 16.5))
                    .foregroundStyle(Theme.textDim)
                    .lineLimit(1)
                if !app.settings.showLiveEPG {
                    Text("· \(L.t("live.noEPG"))")
                        .font(.system(size: 15.3))
                        .foregroundStyle(Theme.textFaint)
                }
            }
        }
    }

    private func loadEPGIfNeeded() {
        guard app.settings.showLiveEPG, !loadingEPG, !epgAttempted, programs.isEmpty else { return }
        epgAttempted = true
        loadingEPG = true
        Task {
            let loaded = await app.epg(for: item)
            await MainActor.run {
                programs = loaded
                loadingEPG = false
            }
        }
    }
}

// MARK: - TV Guide

struct TVGuideView: View {
    @EnvironmentObject private var app: AppModel

    @State private var selectedGroup = MenuSentinel.allGroups
    @State private var channels: [MediaItem] = []
    @State private var selectedID: String?
    @State private var programs: [String: [EPGProgram]] = [:]
    @State private var focusedProgram: EPGProgram?
    @State private var isLoading = false
    @State private var fallbackAttempted: Set<String> = []
    @State private var loadGeneration = 0
    @State private var windowStart = TVGuideView.initialWindowStart()
    @FocusState private var watchFocused: Bool

    private let hourWidth: CGFloat = 340
    private let windowHours: Double = 4
    private let rowHeight: CGFloat = 112
    private let maxChannels = 180

    private var totalWidth: CGFloat { hourWidth * CGFloat(windowHours) }
    private var windowEnd: Date { windowStart.addingTimeInterval(windowHours * 3600) }
    private var groups: [GroupCount] { app.groups(kind: .live) }
    private var selected: MediaItem? { channels.first { $0.id == selectedID } }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            header
            groupBar

            if channels.isEmpty {
                emptyView
            } else {
                guideBody
            }
        }
        .padding(.horizontal, Theme.pagePadding)
        .padding(.top, 26)
        .padding(.bottom, 26)
        .background(Theme.background)
        .task(id: reloadToken) {
            await loadGuide()
        }
        .onChange(of: selectedID) { _, _ in
            focusedProgram = nil
            Task { await loadFallbackForSelection() }
        }
    }

    private var header: some View {
        HStack(alignment: .bottom, spacing: 26) {
            ScreenTitle(title: L.t("guide.title"), subtitle: L.t("guide.hint"))
            Spacer(minLength: 16)

            Button {
                if let selected { app.play(item: selected) }
            } label: {
                HStack(spacing: 9) {
                    Image(systemName: "play.fill")
                        .font(.system(size: 14, weight: .bold))
                    Text(L.t("guide.watch"))
                        .font(.system(size: 18.9, weight: .bold))
                }
                .padding(.horizontal, 24)
                .padding(.vertical, 13)
                .background(watchFocused ? Color.white : Theme.accent, in: Capsule())
                .foregroundStyle(Color.black.opacity(0.9))
                .scaleEffect(watchFocused ? 1.04 : 1)
                .animation(.easeOut(duration: 0.12), value: watchFocused)
            }
            .buttonStyle(.plain)
            .focused($watchFocused)
            .disabled(selected == nil)
            .opacity(selected == nil ? 0.45 : 1)
        }
    }

    private var groupBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            LazyHStack(spacing: 9) {
                SelectChip(
                    title: L.t("common.all"),
                    selected: selectedGroup == MenuSentinel.allGroups
                ) {
                    selectedGroup = MenuSentinel.allGroups
                }
                ForEach(groups) { group in
                    SelectChip(
                        title: app.displayGroup(group.name),
                        selected: selectedGroup == group.name,
                        count: group.count
                    ) {
                        selectedGroup = group.name
                    }
                }
            }
            .padding(.vertical, 6)
        }
        .focusSection()
    }

    private var guideBody: some View {
        HStack(alignment: .top, spacing: 18) {
            channelRail
            VStack(spacing: 12) {
                if let item = selected {
                    channelHeader(item)
                    timelineCard(item)
                    programDetails(item)
                }
            }
            .frame(maxWidth: .infinity, alignment: .top)
        }
        .frame(maxHeight: .infinity, alignment: .top)
    }

    private var channelRail: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(L.f("guide.count", channels.count))
                .font(.system(size: 16.5, weight: .semibold))
                .foregroundStyle(Theme.textFaint)
                .padding(.leading, 4)

            ScrollView {
                LazyVStack(spacing: 6) {
                    ForEach(channels) { item in
                        GuideChannelRow(
                            item: item,
                            subtitle: app.displayGroup(item.group),
                            selected: selectedID == item.id,
                            hasGuide: hasGuide(item)
                        ) {
                            if selectedID != item.id { selectedID = item.id }
                        } action: {
                            selectedID = item.id
                        }
                    }
                }
                .padding(.vertical, 4)
                .padding(.trailing, 6)
                .focusSection()
            }
        }
        .frame(width: 330)
    }

    private func channelHeader(_ item: MediaItem) -> some View {
        HStack(spacing: 14) {
            ZStack {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(Theme.surface2)
                RemoteImage(urlString: item.logo, contentMode: .fit, fallbackText: item.title)
                    .padding(8)
            }
            .frame(width: 96, height: 60)

            VStack(alignment: .leading, spacing: 3) {
                Text(item.title)
                    .font(.system(size: 25.9, weight: .bold, design: .rounded))
                    .foregroundStyle(Theme.text)
                    .lineLimit(1)
                Text(app.displayGroup(item.group))
                    .font(.system(size: 15.3, weight: .medium))
                    .foregroundStyle(Theme.textDim)
                    .lineLimit(1)
            }

            Spacer(minLength: 12)

            if let now = guideEvents(for: item).now() {
                VStack(alignment: .trailing, spacing: 3) {
                    Text(L.t("guide.now").uppercased())
                        .font(.system(size: 11.8, weight: .heavy))
                        .tracking(1.2)
                        .foregroundStyle(Theme.accentLight)
                    Text(now.title)
                        .font(.system(size: 17.7, weight: .semibold))
                        .foregroundStyle(Theme.text)
                        .lineLimit(1)
                }
            }
        }
        .padding(.horizontal, 2)
    }

    private func timelineCard(_ item: MediaItem) -> some View {
        let events = guideEvents(for: item)
        return VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 10) {
                Text(L.t("guide.programs"))
                    .font(.system(size: 20.1, weight: .bold, design: .rounded))
                    .foregroundStyle(Theme.text)
                Spacer()
                if isLoading {
                    ProgressView()
                        .tint(Theme.accent)
                        .scaleEffect(0.8)
                }
            }

            if events.isEmpty {
                if isLoading {
                    HStack(spacing: 10) {
                        ProgressView().tint(Theme.accent)
                        Text(L.t("guide.loading"))
                            .font(.system(size: 17.7))
                            .foregroundStyle(Theme.textDim)
                    }
                    .frame(maxWidth: .infinity, minHeight: 128)
                } else {
                    VStack(spacing: 9) {
                        Image(systemName: "calendar.badge.exclamationmark")
                            .font(.system(size: 33, weight: .light))
                            .foregroundStyle(Theme.textFaint)
                        Text(L.t("guide.noData"))
                            .font(.system(size: 20.1, weight: .semibold))
                            .foregroundStyle(Theme.textDim)
                        Text(L.t("guide.noDataHint"))
                            .font(.system(size: 15.3))
                            .foregroundStyle(Theme.textFaint)
                            .multilineTextAlignment(.center)
                    }
                    .frame(maxWidth: .infinity, minHeight: 140)
                }
            } else {
                TimelineView(.periodic(from: .now, by: 30)) { context in
                    timeline(events: events, now: context.date)
                }
            }
        }
        .padding(16)
        .background(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .fill(Theme.surface)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 18, style: .continuous)
                .stroke(Theme.border, lineWidth: 1)
        )
    }

    private func timeline(events: [EPGProgram], now: Date) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            VStack(alignment: .leading, spacing: 8) {
                HStack(spacing: 0) {
                    ForEach(0..<8, id: \.self) { slot in
                        Text(timeLabel(windowStart.addingTimeInterval(Double(slot) * 1800)))
                            .font(.system(size: 12.4, weight: .medium, design: .monospaced))
                            .foregroundStyle(Theme.textFaint)
                            .frame(width: hourWidth / 2, alignment: .leading)
                    }
                }
                .frame(width: totalWidth, alignment: .leading)

                ZStack(alignment: .topLeading) {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(Theme.backgroundElevated)

                    ForEach(0...8, id: \.self) { slot in
                        Rectangle()
                            .fill(Theme.border)
                            .frame(width: 1, height: rowHeight)
                            .offset(x: CGFloat(slot) * hourWidth / 2)
                    }

                    ForEach(events) { program in
                        if let slot = blockFrame(for: program) {
                            GuideProgramBlock(
                                program: program,
                                isCurrent: program.isNow,
                                isPast: program.end <= now
                            ) {
                                focusedProgram = program
                            } action: {
                                if let selected { app.play(item: selected) }
                            }
                            .frame(width: slot.width, height: rowHeight - 14)
                            .offset(x: slot.x, y: 7)
                        }
                    }

                    if now >= windowStart, now <= windowEnd {
                        Rectangle()
                            .fill(Theme.accent)
                            .frame(width: 2, height: rowHeight)
                            .offset(x: xPosition(for: now))
                            .shadow(color: Theme.accent.opacity(0.7), radius: 4)
                    }
                }
                .frame(width: totalWidth, height: rowHeight)
            }
        }
    }

    @ViewBuilder
    private func programDetails(_ item: MediaItem) -> some View {
        let events = guideEvents(for: item)
        if let detail = focusedProgram ?? events.now() ?? events.first {
            VStack(alignment: .leading, spacing: 7) {
                HStack(alignment: .firstTextBaseline) {
                    Text(detail.title)
                        .font(.system(size: 21.2, weight: .bold, design: .rounded))
                        .foregroundStyle(Theme.text)
                        .lineLimit(1)
                    Spacer()
                    Text(detail.timeRange)
                        .font(.system(size: 14.2, weight: .medium, design: .monospaced))
                        .foregroundStyle(Theme.accentLight)
                }
                if let plot = detail.plot, !plot.isEmpty {
                    Text(plot)
                        .font(.system(size: 16.5))
                        .foregroundStyle(Theme.textDim)
                        .lineLimit(2)
                } else {
                    Text(L.t("guide.next") + ": " + (events.next()?.title ?? "—"))
                        .font(.system(size: 16.5))
                        .foregroundStyle(Theme.textDim)
                        .lineLimit(1)
                }
            }
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(Theme.surface2)
            )
        }
    }

    private var emptyView: some View {
        EmptyStateView(
            symbol: "calendar",
            title: L.t("guide.noData"),
            message: L.t("guide.noDataHint")
        )
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private func guideEvents(for item: MediaItem) -> [EPGProgram] {
        guard let tvgID = item.tvgID, let values = programs[tvgID] else { return [] }
        return values.sorted { $0.start < $1.start }
    }

    private func hasGuide(_ item: MediaItem) -> Bool {
        guard let tvgID = item.tvgID else { return false }
        return programs[tvgID]?.isEmpty == false
    }

    private func blockFrame(for program: EPGProgram) -> (x: CGFloat, width: CGFloat)? {
        let start = max(program.start, windowStart)
        let end = min(program.end, windowEnd)
        guard end > start else { return nil }
        let x = CGFloat(start.timeIntervalSince(windowStart) / 3600) * hourWidth + 3
        let width = max(58, CGFloat(end.timeIntervalSince(start) / 3600) * hourWidth - 6)
        return (x, width)
    }

    private func xPosition(for date: Date) -> CGFloat {
        CGFloat(date.timeIntervalSince(windowStart) / 3600) * hourWidth
    }

    private func timeLabel(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "HH:mm"
        return formatter.string(from: date)
    }

    private var reloadToken: String {
        [
            selectedGroup,
            String(app.liveItems.count),
            String(app.playlists.count),
            app.settings.showLiveEPG ? "1" : "0",
        ].joined(separator: "|")
    }

    private func loadGuide() async {
        let generation = loadGeneration + 1
        loadGeneration = generation

        let group = selectedGroup
        var values = app.items(kind: .live, group: group == MenuSentinel.allGroups ? nil : group)
        values.sort { $0.title.localizedStandardCompare($1.title) == .orderedAscending }
        let limited = Array(values.prefix(maxChannels))
        channels = limited

        if selectedID == nil || !limited.contains(where: { $0.id == selectedID }) {
            selectedID = limited.first?.id
        }
        focusedProgram = nil

        guard app.settings.showLiveEPG else {
            programs = [:]
            isLoading = false
            return
        }

        var idsByPlaylist: [UUID: Set<String>] = [:]
        for item in limited {
            guard let tvgID = item.tvgID?.trimmed, !tvgID.isEmpty else { continue }
            idsByPlaylist[item.playlistID, default: []].insert(tvgID)
        }

        isLoading = true
        var loaded: [String: [EPGProgram]] = [:]
        for playlist in app.playlists where playlist.isEnabled {
            guard let ids = idsByPlaylist[playlist.id], !ids.isEmpty,
                  playlist.epgURL?.isEmpty == false else { continue }
            let found = await app.guidePrograms(for: playlist, channelIDs: ids)
            for (channel, list) in found {
                loaded[channel] = list
            }
        }

        guard generation == loadGeneration else { return }
        programs = loaded
        isLoading = false
        await loadFallbackForSelection()
    }

    private func loadFallbackForSelection() async {
        guard let item = selected,
              let tvgID = item.tvgID?.trimmed, !tvgID.isEmpty,
              (programs[tvgID]?.isEmpty ?? true),
              !fallbackAttempted.contains(item.id),
              app.settings.showLiveEPG,
              item.xtream != nil else { return }

        fallbackAttempted.insert(item.id)
        let values = await app.epg(for: item)
        guard !values.isEmpty else { return }
        programs[tvgID] = values
    }

    private static func initialWindowStart() -> Date {
        let now = Date()
        let halfHour: TimeInterval = 30 * 60
        let floored = (now.timeIntervalSince1970 / halfHour).rounded(.down) * halfHour
        return Date(timeIntervalSince1970: floored - 3600)
    }
}

private struct GuideChannelRow: View {
    let item: MediaItem
    let subtitle: String
    let selected: Bool
    let hasGuide: Bool
    let onFocus: () -> Void
    let action: () -> Void

    @FocusState private var focused: Bool

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                ZStack {
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .fill(focused ? Color.black.opacity(0.10) : Theme.surface2)
                    RemoteImage(urlString: item.logo, contentMode: .fit, fallbackText: item.title)
                        .padding(4)
                }
                .frame(width: 54, height: 34)

                VStack(alignment: .leading, spacing: 1) {
                    Text(item.title)
                        .font(.system(size: 17.7, weight: selected ? .bold : .medium))
                        .foregroundStyle(focused ? Color.black : Theme.text)
                        .lineLimit(1)
                    Text(subtitle)
                        .font(.system(size: 12.4))
                        .foregroundStyle(focused ? Color.black.opacity(0.55) : Theme.textFaint)
                        .lineLimit(1)
                }

                Spacer(minLength: 4)

                if selected {
                    Circle()
                        .fill(focused ? Color.black : Theme.accent)
                        .frame(width: 6, height: 6)
                } else if hasGuide {
                    Circle()
                        .fill(Theme.accent.opacity(0.35))
                        .frame(width: 5, height: 5)
                }
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 7)
            .background(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(focused ? Color.white : (selected ? Theme.accent.opacity(0.14) : Color.clear))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .stroke(selected && !focused ? Theme.accent.opacity(0.55) : Color.clear, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .focused($focused)
        .onChange(of: focused) { _, value in
            if value { onFocus() }
        }
    }
}

private struct GuideProgramBlock: View {
    let program: EPGProgram
    let isCurrent: Bool
    let isPast: Bool
    let onFocus: () -> Void
    let action: () -> Void

    @FocusState private var focused: Bool

    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 3) {
                Text(program.title)
                    .font(.system(size: 15.3, weight: .semibold))
                    .foregroundStyle(isCurrent ? Color.black : (isPast ? Theme.textDim : Theme.text))
                    .lineLimit(2)
                Spacer(minLength: 0)
                Text(program.timeRange)
                    .font(.system(size: 11.8, weight: .medium, design: .monospaced))
                    .foregroundStyle(isCurrent ? Color.black.opacity(0.65) : Theme.textFaint)
            }
            .padding(9)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            .background(
                RoundedRectangle(cornerRadius: 9, style: .continuous)
                    .fill(
                        isCurrent
                            ? Theme.accent
                            : (isPast ? Theme.surface : Theme.surface2)
                    )
            )
            .overlay(
                RoundedRectangle(cornerRadius: 9, style: .continuous)
                    .stroke(focused ? Color.white : Theme.border, lineWidth: focused ? 2.5 : 1)
            )
            .scaleEffect(focused ? 1.02 : 1)
            .animation(.easeOut(duration: 0.12), value: focused)
        }
        .buttonStyle(.plain)
        .focused($focused)
        .onChange(of: focused) { _, value in
            if value { onFocus() }
        }
    }
}
