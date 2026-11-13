import SwiftUI

enum Route: Hashable {
    case detail(String)
}

// MARK: - Cards

struct PosterCard: View {
    let item: MediaItem
    var width: CGFloat = 230

    var body: some View {
        NavigationLink(value: Route.detail(item.id)) {
            VStack(alignment: .leading, spacing: 8) {
                RemoteImage(urlString: item.logo, fallbackText: item.title)
                    .frame(width: width, height: width * 1.48)
                    .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    .overlay(
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .stroke(Color.white.opacity(0.08), lineWidth: 1)
                    )
                Text(item.title)
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundStyle(Theme.text)
                    .lineLimit(1)
                HStack(spacing: 6) {
                    if let quality = item.quality {
                        Text(quality)
                            .font(.system(size: 12, weight: .bold))
                            .foregroundStyle(Theme.greenBright)
                    }
                    if let year = item.year {
                        Text(String(year))
                            .font(.system(size: 14))
                            .foregroundStyle(Theme.textDim)
                    }
                    if let rating = item.rating, rating > 0 {
                        HStack(spacing: 3) {
                            Image(systemName: "star.fill")
                                .font(.system(size: 10))
                            Text(String(format: "%.1f", rating))
                                .font(.system(size: 13, weight: .semibold))
                        }
                        .foregroundStyle(Theme.gold)
                    }
                }
                .frame(height: 18)
            }
            .frame(width: width, alignment: .leading)
            .padding(10)
        }
        .buttonStyle(.card)
    }
}

struct LiveChannelCard: View {
    @EnvironmentObject private var app: AppModel
    let item: MediaItem
    var width: CGFloat = 300

    var body: some View {
        Button {
            app.play(item: item)
        } label: {
            VStack(alignment: .leading, spacing: 10) {
                ZStack {
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .fill(Theme.surface2)
                    RemoteImage(urlString: item.logo, contentMode: .fit, fallbackText: item.title)
                        .padding(14)
                    HStack {
                        Spacer()
                        Text(L.t("live.live"))
                            .font(.system(size: 11, weight: .heavy))
                            .foregroundStyle(.white)
                            .padding(.horizontal, 7)
                            .padding(.vertical, 3)
                            .background(Theme.red, in: Capsule())
                            .padding(8)
                    }
                }
                .frame(width: width, height: width * 0.56)
                Text(item.title)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundStyle(Theme.text)
                    .lineLimit(1)
                Text(app.displayGroup(item.group))
                    .font(.system(size: 14))
                    .foregroundStyle(Theme.textDim)
                    .lineLimit(1)
            }
            .frame(width: width, alignment: .leading)
            .padding(10)
        }
        .buttonStyle(.card)
    }
}

struct ContinueCard: View {
    @EnvironmentObject private var app: AppModel
    let entry: WatchProgress
    var width: CGFloat = 380

    var body: some View {
        Button {
            Task { await app.resume(entry) }
        } label: {
            VStack(alignment: .leading, spacing: 9) {
                ZStack(alignment: .bottom) {
                    RemoteImage(urlString: entry.artwork, fallbackText: entry.title)
                        .frame(width: width, height: width * 0.56)
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    LinearGradient(colors: [.clear, .black.opacity(0.68)], startPoint: .center, endPoint: .bottom)
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    VStack(alignment: .leading, spacing: 7) {
                        ProgressBadge(fraction: entry.fraction)
                        HStack {
                            Text(L.f("time.remaining", TimeText.clock(entry.remaining)))
                                .font(.system(size: 13, weight: .semibold))
                                .foregroundStyle(.white.opacity(0.9))
                            Spacer()
                            Image(systemName: "play.circle.fill")
                                .font(.system(size: 26))
                                .foregroundStyle(.white)
                        }
                    }
                    .padding(12)
                }
                Text(entry.title)
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundStyle(Theme.text)
                    .lineLimit(1)
                if let subtitle = entry.subtitle {
                    Text(subtitle)
                        .font(.system(size: 14))
                        .foregroundStyle(Theme.textDim)
                        .lineLimit(1)
                }
            }
            .frame(width: width, alignment: .leading)
            .padding(10)
        }
        .buttonStyle(.card)
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
                    .fill(focused ? Color.white : (selected ? Theme.green.opacity(0.20) : Theme.surface2))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .stroke(selected && !focused ? Theme.green.opacity(0.75) : Color.clear, lineWidth: 1.5)
            )
            .foregroundStyle(focused ? Color.black : (selected ? Theme.greenBright : Theme.text))
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
                        .foregroundStyle(Theme.green)
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
