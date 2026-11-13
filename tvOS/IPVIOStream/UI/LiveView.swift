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
