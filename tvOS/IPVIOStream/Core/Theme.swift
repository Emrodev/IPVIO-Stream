import SwiftUI
import UIKit

enum Theme {
    static let background = Color(hex: 0x0A0B0F)
    static let backgroundElevated = Color(hex: 0x101116)
    static let surface = Color(hex: 0x16171D)
    static let surface2 = Color(hex: 0x1E2027)
    static let surface3 = Color(hex: 0x2A2C35)
    static let accent = Color(hex: 0xFF5A1F)
    static let accentLight = Color(hex: 0xFF8A4C)
    static let accentDark = Color(hex: 0xB93E12)
    static let text = Color.white.opacity(0.94)
    static let textDim = Color.white.opacity(0.64)
    static let textFaint = Color.white.opacity(0.42)
    static let red = Color(hex: 0xFF5A5F)
    static let gold = Color(hex: 0xFFC857)
    static let border = Color.white.opacity(0.10)
    static let focusBackground = Color.white

    static let cornerS: CGFloat = 10
    static let cornerM: CGFloat = 16
    static let cornerL: CGFloat = 24

    static let pagePadding: CGFloat = 60
}

extension Color {
    init(hex: UInt32, alpha: Double = 1) {
        let red = Double((hex >> 16) & 0xFF) / 255
        let green = Double((hex >> 8) & 0xFF) / 255
        let blue = Double(hex & 0xFF) / 255
        self.init(.sRGB, red: red, green: green, blue: blue, opacity: alpha)
    }
}

struct BrandMark: View {
    var size: CGFloat = 64

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: size * 0.26, style: .continuous)
                .fill(
                    LinearGradient(
                        colors: [Theme.accentLight, Theme.accent, Theme.accentDark],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
            Image(systemName: "play.fill")
                .font(.system(size: size * 0.42, weight: .black))
                .foregroundStyle(Theme.background)
                .offset(x: size * 0.03)
        }
        .frame(width: size, height: size)
        .shadow(color: Theme.accent.opacity(0.35), radius: size * 0.3, y: size * 0.08)
    }
}

struct TagBadge: View {
    let text: String
    var tint: Color = .white
    var background: Color = .white.opacity(0.12)

    var body: some View {
        Text(text)
            .font(.system(size: 17, weight: .semibold, design: .rounded))
            .foregroundStyle(tint)
            .padding(.horizontal, 10)
            .padding(.vertical, 4)
            .background(background, in: Capsule())
    }
}

struct SectionHeader: View {
    let title: String
    var subtitle: String? = nil
    var actionTitle: String? = nil
    var action: (() -> Void)? = nil

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 16) {
            VStack(alignment: .leading, spacing: 4) {
                Text(title)
                    .font(.system(size: 25, weight: .bold, design: .rounded))
                    .foregroundStyle(Theme.text)
                if let subtitle {
                    Text(subtitle)
                        .font(.system(size: 16))
                        .foregroundStyle(Theme.textDim)
                }
            }
            Spacer()
            if let actionTitle, let action {
                Button(action: action) {
                    HStack(spacing: 5) {
                        Text(actionTitle)
                        Image(systemName: "chevron.right")
                            .font(.system(size: 12, weight: .bold))
                    }
                    .font(.system(size: 15, weight: .semibold))
                }
                .buttonStyle(.plain)
                .foregroundStyle(Theme.accentLight)
            }
        }
    }
}

struct EmptyStateView: View {
    let symbol: String
    let title: String
    let message: String
    var actionTitle: String? = nil
    var action: (() -> Void)? = nil

    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: symbol)
                .font(.system(size: 62, weight: .light))
                .foregroundStyle(Theme.accent.opacity(0.85))
            Text(title)
                .font(.system(size: 32, weight: .bold, design: .rounded))
                .foregroundStyle(Theme.text)
            Text(message)
                .font(.system(size: 21))
                .foregroundStyle(Theme.textDim)
                .multilineTextAlignment(.center)
                .frame(maxWidth: 680)
            if let actionTitle, let action {
                Button(actionTitle, action: action)
                    .buttonStyle(.borderedProminent)
                    .tint(Theme.accent)
                    .foregroundStyle(Theme.background)
                    .font(.system(size: 20, weight: .semibold))
                    .padding(.top, 8)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding(60)
    }
}

struct ProgressBadge: View {
    let fraction: Double

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .leading) {
                Capsule().fill(Color.white.opacity(0.22))
                Capsule()
                    .fill(Theme.accent)
                    .frame(width: max(4, proxy.size.width * min(max(fraction, 0), 1)))
            }
        }
        .frame(height: 5)
    }
}

struct RemoteImage: View {
    let urlString: String?
    var contentMode: ContentMode = .fill
    var fallbackSymbol: String = "film"
    var fallbackText: String? = nil

    var body: some View {
        Group {
            if let urlString, let url = URL(string: urlString), !urlString.isEmpty {
                AsyncImage(url: url, transaction: Transaction(animation: .easeInOut(duration: 0.25))) { phase in
                    switch phase {
                    case let .success(image):
                        image
                            .resizable()
                            .aspectRatio(contentMode: contentMode)
                    default:
                        placeholder
                    }
                }
            } else {
                placeholder
            }
        }
        .clipped()
    }

    private var placeholder: some View {
        ZStack {
            LinearGradient(
                colors: [Theme.surface2, Theme.surface],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            if let fallbackText, !fallbackText.isEmpty {
                Text(fallbackText.prefix(1).uppercased())
                    .font(.system(size: 44, weight: .bold, design: .rounded))
                    .foregroundStyle(Theme.textFaint)
            } else {
                Image(systemName: fallbackSymbol)
                    .font(.system(size: 38, weight: .light))
                    .foregroundStyle(Theme.textFaint)
            }
        }
    }
}

struct ToastView: View {
    let message: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "checkmark.circle.fill")
                .foregroundStyle(Theme.accent)
            Text(message)
                .font(.system(size: 20, weight: .semibold))
                .foregroundStyle(.white)
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 14)
        .background(.ultraThinMaterial, in: Capsule())
        .overlay(Capsule().stroke(Theme.border, lineWidth: 1))
        .shadow(color: .black.opacity(0.35), radius: 20, y: 8)
    }
}

struct LoadingOverlay: View {
    let job: LoadJob
    var onCancel: (() -> Void)? = nil

    var body: some View {
        ZStack {
            Color.black.opacity(0.72).ignoresSafeArea()
            VStack(spacing: 22) {
                BrandMark(size: 72)
                Text(L.t("load.title"))
                    .font(.system(size: 32, weight: .bold, design: .rounded))
                    .foregroundStyle(Theme.text)
                Text(job.playlistName)
                    .font(.system(size: 20))
                    .foregroundStyle(Theme.textDim)
                ProgressView(value: min(max(job.progress, 0), 1))
                    .progressViewStyle(.linear)
                    .tint(Theme.accent)
                    .frame(width: 520)
                Text(job.status)
                    .font(.system(size: 18))
                    .foregroundStyle(Theme.textDim)
                    .lineLimit(2)
                    .frame(width: 560)
                if let onCancel {
                    Button(L.t("common.cancel"), action: onCancel)
                        .buttonStyle(.bordered)
                        .font(.system(size: 18, weight: .medium))
                        .padding(.top, 4)
                }
            }
            .padding(48)
            .background(Theme.surface.opacity(0.96), in: RoundedRectangle(cornerRadius: 28, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 28, style: .continuous)
                    .stroke(Theme.border, lineWidth: 1)
            )
            .shadow(color: .black.opacity(0.5), radius: 40, y: 20)
        }
        .transition(.opacity)
    }
}

struct ScreenTitle: View {
    let title: String
    var subtitle: String? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(title)
                .font(.system(size: 42, weight: .heavy, design: .rounded))
                .foregroundStyle(Theme.text)
            if let subtitle {
                Text(subtitle)
                    .font(.system(size: 21))
                    .foregroundStyle(Theme.textDim)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

extension View {
    func ipvioPage() -> some View {
        self.padding(.horizontal, Theme.pagePadding)
    }

    func cardSurface(corner: CGFloat = Theme.cornerM) -> some View {
        background(Theme.surface.opacity(0.82), in: RoundedRectangle(cornerRadius: corner, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: corner, style: .continuous)
                    .stroke(Theme.border, lineWidth: 1)
            )
    }
}
