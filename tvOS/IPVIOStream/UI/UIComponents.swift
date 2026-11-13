import SwiftUI

enum Route: Hashable {
    case detail(String)
}

// MARK: - Cards

struct PosterCard: View {
    @EnvironmentObject private var app: AppModel
    let item: MediaItem
    var width: CGFloat = 230
    @FocusState private var focused: Bool

    private var progress: WatchProgress? {
        app.progress(for: item, episode: nil)
    }

    var body: some View {
        NavigationLink(value: Route.detail(item.id)) {
            ZStack(alignment: .bottomLeading) {
                RemoteImage(urlString: item.logo, fallbackText: item.title)
                    .frame(width: width, height: width * 1.45)
                    .clipped()

                LinearGradient(
                    colors: [.clear, .clear, .black.opacity(0.35), .black.opacity(0.94)],
                    startPoint: .top,
                    endPoint: .bottom
                )

                VStack(alignment: .leading, spacing: 5) {
                    Spacer()
                    if let quality = item.quality {
                        Text(quality)
                            .font(.system(size: 10, weight: .heavy))
                            .foregroundStyle(.black)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Theme.accent, in: Capsule())
                    }
                    Text(item.title)
                        .font(.system(size: 18, weight: .bold, design: .rounded))
                        .foregroundStyle(.white)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                    HStack(spacing: 7) {
                        if let year = item.year {
                            Text(String(year))
                        }
                        if let rating = item.rating, rating > 0 {
                            HStack(spacing: 3) {
                                Image(systemName: "star.fill")
                                    .font(.system(size: 9))
                                Text(String(format: "%.1f", rating))
                            }
                            .foregroundStyle(Theme.gold)
                        }
                        if progress == nil {
                            Text(app.displayGroup(item.group))
                                .lineLimit(1)
                        }
                    }
                    .font(.system(size: 12, weight: .medium))
                    .foregroundStyle(.white.opacity(0.62))

                    if let progress, progress.fraction > 0.01 {
                        ProgressBadge(fraction: progress.fraction)
                            .frame(height: 4)
                    }
                }
                .padding(12)
                .frame(width: width, height: width * 1.45, alignment: .bottomLeading)
            }
            .frame(width: width, height: width * 1.45)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(focused ? Color.white.opacity(0.9) : Theme.border, lineWidth: focused ? 2.5 : 1)
            )
            .scaleEffect(focused ? 1.04 : 1)
            .shadow(color: .black.opacity(focused ? 0.55 : 0.25), radius: focused ? 20 : 8, y: 10)
            .animation(.easeOut(duration: 0.14), value: focused)
            .padding(5)
        }
        .buttonStyle(.plain)
        .focused($focused)
    }
}

struct LiveChannelCard: View {
    @EnvironmentObject private var app: AppModel
    let item: MediaItem
    var width: CGFloat = 300
    @FocusState private var focused: Bool

    var body: some View {
        Button {
            app.play(item: item)
        } label: {
            ZStack(alignment: .bottomLeading) {
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .fill(Theme.surface2)
                RemoteImage(urlString: item.logo, contentMode: .fit, fallbackText: item.title)
                    .padding(28)
                LinearGradient(
                    colors: [.clear, .black.opacity(0.88)],
                    startPoint: .center,
                    endPoint: .bottom
                )

                VStack(alignment: .leading, spacing: 4) {
                    Spacer()
                    Text(item.title)
                        .font(.system(size: 18, weight: .bold, design: .rounded))
                        .foregroundStyle(.white)
                        .lineLimit(1)
                    Text(app.displayGroup(item.group))
                        .font(.system(size: 12, weight: .medium))
                        .foregroundStyle(.white.opacity(0.58))
                        .lineLimit(1)
                }
                .padding(14)

                VStack {
                    HStack {
                        Spacer()
                        HStack(spacing: 5) {
                            Circle().fill(Theme.red).frame(width: 6, height: 6)
                            Text(L.t("live.live"))
                                .font(.system(size: 10, weight: .heavy))
                        }
                        .foregroundStyle(.white)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(.black.opacity(0.45), in: Capsule())
                    }
                    Spacer()
                }
                .padding(10)
            }
            .frame(width: width, height: width * 0.56)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(focused ? Color.white.opacity(0.9) : Theme.border, lineWidth: focused ? 2.5 : 1)
            )
            .scaleEffect(focused ? 1.04 : 1)
            .animation(.easeOut(duration: 0.14), value: focused)
            .padding(5)
        }
        .buttonStyle(.plain)
        .focused($focused)
    }
}

struct ContinueCard: View {
    @EnvironmentObject private var app: AppModel
    let entry: WatchProgress
    var width: CGFloat = 380
    @FocusState private var focused: Bool

    var body: some View {
        Button {
            Task { await app.resume(entry) }
        } label: {
            ZStack(alignment: .bottomLeading) {
                RemoteImage(urlString: entry.artwork, fallbackText: entry.title)
                    .frame(width: width, height: width * 0.56)
                    .clipped()
                LinearGradient(
                    colors: [.clear, .black.opacity(0.2), .black.opacity(0.92)],
                    startPoint: .top,
                    endPoint: .bottom
                )

                VStack(alignment: .leading, spacing: 5) {
                    Spacer()
                    Text(entry.title)
                        .font(.system(size: 19, weight: .bold, design: .rounded))
                        .foregroundStyle(.white)
                        .lineLimit(1)
                    if let subtitle = entry.subtitle {
                        Text(subtitle)
                            .font(.system(size: 12, weight: .medium))
                            .foregroundStyle(.white.opacity(0.6))
                            .lineLimit(1)
                    }
                    HStack(spacing: 7) {
                        Image(systemName: "play.fill")
                            .font(.system(size: 9, weight: .bold))
                        Text(L.f("time.remaining", TimeText.clock(entry.remaining)))
                            .font(.system(size: 12, weight: .semibold))
                        Spacer()
                        Text("\(Int(entry.fraction * 100))%")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundStyle(Theme.accentLight)
                    }
                    .foregroundStyle(.white.opacity(0.72))
                    ProgressBadge(fraction: entry.fraction)
                        .frame(height: 4)
                }
                .padding(14)
                .frame(width: width, height: width * 0.56, alignment: .bottomLeading)
            }
            .frame(width: width, height: width * 0.56)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .stroke(focused ? Color.white.opacity(0.9) : Theme.border, lineWidth: focused ? 2.5 : 1)
            )
            .scaleEffect(focused ? 1.035 : 1)
            .animation(.easeOut(duration: 0.14), value: focused)
            .padding(5)
        }
        .buttonStyle(.plain)
        .focused($focused)
    }
}

// MARK: - Rows

struct PosterRow: View {
    let title: String
    let items: [MediaItem]
    var subtitle: String? = nil
    var onSeeAll: (() -> Void)? = nil

    var body: some View {
        if !items.isEmpty {
            VStack(alignment: .leading, spacing: 12) {
                SectionHeader(
                    title: title,
                    subtitle: subtitle,
                    actionTitle: onSeeAll != nil ? L.t("common.seeAll") : nil,
                    action: onSeeAll
                )
                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHStack(spacing: 20) {
                        ForEach(items) { item in
                            PosterCard(item: item)
                        }
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 18)
                }
                .scrollClipDisabled()
            }
        }
    }
}

// MARK: - Chips

struct SelectChip: View {
    let title: String
    var selected: Bool = false
    var count: Int? = nil
    let action: () -> Void
    @FocusState private var focused: Bool

    var body: some View {
        Button(action: action) {
            HStack(spacing: 7) {
                Text(title)
                    .font(.system(size: 17, weight: .semibold))
                if let count {
                    Text("\(count)")
                        .font(.system(size: 12, weight: .bold))
                        .padding(.horizontal, 6)
                        .padding(.vertical, 2)
                        .background(focused ? Color.black.opacity(0.12) : Color.white.opacity(0.10), in: Capsule())
                }
            }
            .padding(.horizontal, 18)
            .padding(.vertical, 10)
            .background(
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .fill(focused ? Color.white : (selected ? Theme.accent.opacity(0.20) : Theme.surface2))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .stroke(selected && !focused ? Theme.accent.opacity(0.75) : Color.clear, lineWidth: 1.5)
            )
            .foregroundStyle(focused ? Color.black : (selected ? Theme.accentLight : Theme.text))
        }
        .buttonStyle(.plain)
        .focused($focused)
    }
}

// MARK: - Layout

struct SettingsCard<Content: View>: View {
    let title: String
    let symbol: String?
    let subtitle: String?
    private let content: () -> Content

    init(
        title: String,
        symbol: String? = nil,
        subtitle: String? = nil,
        @ViewBuilder content: @escaping () -> Content
    ) {
        self.title = title
        self.symbol = symbol
        self.subtitle = subtitle
        self.content = content
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 10) {
                if let symbol {
                    Image(systemName: symbol)
                        .font(.system(size: 21, weight: .semibold))
                        .foregroundStyle(Theme.accent)
                }
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 25, weight: .bold, design: .rounded))
                        .foregroundStyle(Theme.text)
                    if let subtitle {
                        Text(subtitle)
                            .font(.system(size: 15))
                            .foregroundStyle(Theme.textDim)
                    }
                }
            }
            content()
        }
        .padding(24)
        .cardSurface(corner: 20)
    }
}

struct LabeledTextField: View {
    let title: String
    let placeholder: String
    @Binding var text: String
    var isSecure = false

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            Text(title)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(Theme.textDim)
            Group {
                if isSecure {
                    SecureField(placeholder, text: $text)
                } else {
                    TextField(placeholder, text: $text)
                }
            }
            .textFieldStyle(.plain)
            .font(.system(size: 19))
            .foregroundStyle(Theme.text)
            .padding(.horizontal, 14)
            .padding(.vertical, 11)
            .background(Theme.surface2, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .stroke(Theme.border, lineWidth: 1)
            )
            .autocorrectionDisabled()
        }
    }
}

struct InfoPill: View {
    let symbol: String
    let text: String
    var tint: Color = Theme.textDim

    var body: some View {
        HStack(spacing: 6) {
            Image(systemName: symbol)
                .font(.system(size: 13, weight: .semibold))
            Text(text)
                .font(.system(size: 15, weight: .medium))
        }
        .foregroundStyle(tint)
    }
}
