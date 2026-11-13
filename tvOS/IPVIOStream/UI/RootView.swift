import SwiftUI

struct RootView: View {
    @EnvironmentObject private var app: AppModel
    @Environment(\.scenePhase) private var scenePhase
    @State private var showIntro = true

    var body: some View {
        Group {
            if !app.bootstrapped {
                SplashView()
            } else if app.playlists.isEmpty {
                OnboardingView()
                    .id(app.interfaceLanguage)
            } else {
                MainShell()
                    .id(app.interfaceLanguage)
            }
        }
        .environment(\.layoutDirection, app.interfaceLanguage.layoutDirection)
        .environment(\.locale, app.interfaceLanguage.locale)
        .task {
            await app.bootstrap()
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .background || phase == .inactive {
                app.saveNow()
            }
        }
        .overlay(alignment: .top) {
            if let toast = app.toast {
                ToastView(message: toast)
                    .padding(.top, 22)
                    .transition(.move(edge: .top).combined(with: .opacity))
            }
        }
        .animation(.easeInOut(duration: 0.25), value: app.toast)
        .overlay {
            if let job = app.job {
                LoadingOverlay(job: job)
            }
        }
        .overlay {
            if showIntro {
                LaunchIntroView {
                    withAnimation(.easeInOut(duration: 0.65)) {
                        showIntro = false
                    }
                }
                .transition(.opacity)
                .zIndex(20)
            }
        }
        .fullScreenCover(item: $app.playback) { request in
            PlayerContainer(request: request)
                .environmentObject(app)
        }
        .preferredColorScheme(.dark)
    }
}

struct LaunchIntroView: View {
    var onFinished: () -> Void

    @State private var started = false
    @State private var finished = false

    @State private var glowOpacity: Double = 0
    @State private var logoScale: CGFloat = 0.62
    @State private var logoOpacity: Double = 0
    @State private var logoRotation: Double = -10
    @State private var haloRotation: Double = 0
    @State private var ringScale: CGFloat = 0.92
    @State private var ringOpacity: Double = 0.55
    @State private var ring2Scale: CGFloat = 0.92
    @State private var ring2Opacity: Double = 0.42
    @State private var titleOpacity: Double = 0
    @State private var titleOffset: CGFloat = 26
    @State private var titleTracking: CGFloat = 22
    @State private var subtitleOpacity: Double = 0
    @State private var subtitleOffset: CGFloat = 16
    @State private var chipsOpacity: Double = 0
    @State private var progress: CGFloat = 0

    var body: some View {
        ZStack {
            Theme.background.ignoresSafeArea()

            RadialGradient(
                colors: [Theme.accent.opacity(0.24), Theme.accent.opacity(0.06), .clear],
                center: .center,
                startRadius: 30,
                endRadius: 640
            )
            .opacity(glowOpacity)
            .ignoresSafeArea()
            .blur(radius: 20)

            VStack(spacing: 24) {
                ZStack {
                    Circle()
                        .stroke(Theme.accent.opacity(0.30), lineWidth: 1)
                        .frame(width: 308, height: 308)
                        .scaleEffect(ringScale)
                        .opacity(ringOpacity)

                    Circle()
                        .stroke(Theme.accentLight.opacity(0.22), lineWidth: 1)
                        .frame(width: 308, height: 308)
                        .scaleEffect(ring2Scale)
                        .opacity(ring2Opacity)

                    Circle()
                        .stroke(
                            AngularGradient(
                                colors: [
                                    .clear,
                                    Theme.accent.opacity(0.85),
                                    Theme.accentLight.opacity(0.25),
                                    .clear,
                                    Theme.accent.opacity(0.45),
                                    .clear
                                ],
                                center: .center
                            ),
                            lineWidth: 2
                        )
                        .frame(width: 286, height: 286)
                        .rotationEffect(.degrees(haloRotation))
                        .opacity(logoOpacity * 0.9)

                    BrandMark(size: 202)
                        .scaleEffect(logoScale)
                        .rotationEffect(.degrees(logoRotation))
                        .opacity(logoOpacity)
                        .shadow(color: Theme.accent.opacity(0.55), radius: 60, y: 12)
                }
                .frame(height: 320)
                .padding(.bottom, 6)

                VStack(spacing: 10) {
                    Text("IPVIO")
                        .font(.system(size: 78, weight: .black, design: .rounded))
                        .tracking(titleTracking)
                        .foregroundStyle(Theme.text)
                        .opacity(titleOpacity)
                        .offset(y: titleOffset)

                    Text("STREAM")
                        .font(.system(size: 20, weight: .bold, design: .rounded))
                        .tracking(13)
                        .foregroundStyle(Theme.accent)
                        .opacity(subtitleOpacity)
                        .offset(y: subtitleOffset)
                }

                HStack(spacing: 15) {
                    introLabel("LIVE TV")
                    introDot
                    introLabel("4K HDR")
                    introDot
                    introLabel("MOVIES & SERIES")
                }
                .opacity(chipsOpacity)

                Capsule()
                    .fill(Color.white.opacity(0.10))
                    .frame(width: 300, height: 4)
                    .overlay(alignment: .leading) {
                        Capsule()
                            .fill(
                                LinearGradient(
                                    colors: [Theme.accent, Theme.accentLight],
                                    startPoint: .leading,
                                    endPoint: .trailing
                                )
                            )
                            .frame(width: max(4, 300 * progress), height: 4)
                    }
                    .padding(.top, 10)
            }
        }
        .onAppear(perform: playIntro)
    }

    private var introDot: some View {
        Circle()
            .fill(Theme.textFaint)
            .frame(width: 3, height: 3)
    }

    private func introLabel(_ text: String) -> some View {
        Text(text)
            .font(.system(size: 14, weight: .semibold, design: .rounded))
            .tracking(1.6)
            .foregroundStyle(Theme.textDim)
    }

    private func playIntro() {
        guard !started else { return }
        started = true

        withAnimation(.easeOut(duration: 0.6)) {
            glowOpacity = 1
        }
        withAnimation(.spring(response: 0.85, dampingFraction: 0.68)) {
            logoScale = 1
            logoOpacity = 1
            logoRotation = 0
        }
        withAnimation(.linear(duration: 6).repeatForever(autoreverses: false)) {
            haloRotation = 360
        }
        withAnimation(.easeOut(duration: 1.5).delay(0.15)) {
            ringScale = 1.55
            ringOpacity = 0
        }
        withAnimation(.easeOut(duration: 1.5).delay(0.55)) {
            ring2Scale = 1.72
            ring2Opacity = 0
        }
        withAnimation(.easeOut(duration: 0.9).delay(0.28)) {
            titleOpacity = 1
            titleOffset = 0
            titleTracking = 7
        }
        withAnimation(.easeOut(duration: 0.8).delay(0.52)) {
            subtitleOpacity = 1
            subtitleOffset = 0
        }
        withAnimation(.easeOut(duration: 0.8).delay(0.78)) {
            chipsOpacity = 1
        }
        withAnimation(.easeInOut(duration: 1.9).delay(0.22)) {
            progress = 1
        }

        Task {
            try? await Task.sleep(nanoseconds: 2_750_000_000)
            finish()
        }
    }

    private func finish() {
        guard !finished else { return }
        finished = true
        onFinished()
    }
}

struct SplashView: View {
    var body: some View {
        ZStack {
            Theme.background.ignoresSafeArea()
            RadialGradient(
                colors: [Theme.accent.opacity(0.16), .clear],
                center: .center,
                startRadius: 20,
                endRadius: 520
            )
            .ignoresSafeArea()
            VStack(spacing: 24) {
                BrandMark(size: 96)
                Text("IPVIO Stream")
                    .font(.system(size: 54.3, weight: .heavy, design: .rounded))
                    .foregroundStyle(Theme.text)
                ProgressView()
                    .tint(Theme.accent)
                    .scaleEffect(1.2)
            }
        }
    }
}

final class Router: ObservableObject {
    @Published var path = NavigationPath()

    func reset() {
        path = NavigationPath()
    }

    func detail(_ id: String) {
        path.append(Route.detail(id))
    }
}

struct MainShell: View {
    @EnvironmentObject private var app: AppModel
    @StateObject private var router = Router()

    var body: some View {
        VStack(spacing: 0) {
            TopBar()
            ZStack {
                Theme.background.ignoresSafeArea()
                NavigationStack(path: $router.path) {
                    sectionContent
                        .focusSection()
                        .navigationDestination(for: Route.self) { route in
                            switch route {
                            case let .detail(id):
                                DetailView(itemID: id)
                            }
                        }
                }
            }
        }
        .environmentObject(router)
        .background(Theme.background)
    }

    @ViewBuilder
    private var sectionContent: some View {
        switch app.selectedSection {
        case .home:
            HomeView()
        case .live:
            LiveView()
        case .guide:
            TVGuideView()
        case .favorites:
            FavoritesView()
        case .movies:
            BrowseView(kind: .movie)
        case .series:
            BrowseView(kind: .series)
        case .search:
            SearchView()
        case .settings:
            SettingsView()
        }
    }
}

/// Top navigation inspired by modern Apple TV IPTV apps: a compact brand mark,
/// pill-shaped tabs in the centre, and search/account controls on the right.
struct TopBar: View {
    @EnvironmentObject private var app: AppModel
    @EnvironmentObject private var router: Router
    @FocusState private var focusedTab: AppSection?

    private let tabs: [AppSection] = [.home, .live, .guide, .movies, .series, .settings]

    var body: some View {
        HStack(spacing: 18) {
            Button {
                router.reset()
                app.setSection(.home)
            } label: {
                HStack(spacing: 12) {
                    BrandMark(size: 38)
                    VStack(alignment: .leading, spacing: 0) {
                        Text("IPVIO")
                            .font(.system(size: 24.8, weight: .heavy, design: .rounded))
                            .foregroundStyle(Theme.text)
                        Text("STREAM")
                            .font(.system(size: 10.6, weight: .bold))
                            .tracking(2.6)
                            .foregroundStyle(Theme.accent)
                    }
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 6)
            }
            .buttonStyle(.plain)

            Spacer(minLength: 8)

            HStack(spacing: 4) {
                ForEach(tabs, id: \.self) { section in
                    tabButton(section)
                }
            }
            .padding(5)
            .background(Theme.surface.opacity(0.92), in: Capsule())
            .overlay(Capsule().stroke(Theme.border, lineWidth: 1))
            .focusSection()

            Spacer(minLength: 8)

            HStack(spacing: 10) {
                TopBarIcon(symbol: "magnifyingglass", label: L.t("nav.search"), active: app.selectedSection == .search) {
                    router.reset()
                    app.setSection(.search)
                }
                TopBarIcon(
                    symbol: app.favorites.isEmpty ? "heart" : "heart.fill",
                    label: L.t("nav.favorites"),
                    active: app.selectedSection == .favorites
                ) {
                    router.reset()
                    app.setSection(.favorites)
                }
                ZStack {
                    Circle()
                        .fill(
                            LinearGradient(
                                colors: [Theme.accent, Theme.accentDark],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                    Text("IP")
                        .font(.system(size: 15.3, weight: .heavy, design: .rounded))
                        .foregroundStyle(.black)
                }
                .frame(width: 42, height: 42)
                .overlay(Circle().stroke(Color.white.opacity(0.18), lineWidth: 1))
            }
        }
        .padding(.horizontal, 44)
        .frame(height: 92)
        .background(Theme.backgroundElevated)
        .overlay(alignment: .bottom) {
            Rectangle()
                .fill(Theme.border)
                .frame(height: 1)
        }
    }

    private func tabButton(_ section: AppSection) -> some View {
        let selected = app.selectedSection == section
        let isFocused = focusedTab == section
        return Button {
            router.reset()
            app.setSection(section)
        } label: {
            Text(L.t(section.titleKey))
                .font(.system(size: 20.1, weight: selected ? .bold : .medium))
                .padding(.horizontal, 19)
                .padding(.vertical, 9)
                .background(
                    Capsule()
                        .fill(isFocused ? Color.white : (selected ? Theme.accent.opacity(0.15) : Color.clear))
                )
                .overlay(
                    Capsule()
                        .stroke(selected && !isFocused ? Theme.accent.opacity(0.65) : Color.clear, lineWidth: 1)
                )
                .foregroundStyle(isFocused ? Color.black : (selected ? Theme.accentLight : Theme.textDim))
                .scaleEffect(isFocused ? 1.04 : 1)
                .animation(.easeOut(duration: 0.12), value: isFocused)
        }
        .buttonStyle(.plain)
        .focused($focusedTab, equals: section)
    }
}

private struct TopBarIcon: View {
    let symbol: String
    let label: String
    var active: Bool = false
    let action: () -> Void
    @FocusState private var focused: Bool

    var body: some View {
        Button(action: action) {
            Image(systemName: symbol)
                .font(.system(size: 22.4, weight: .semibold))
                .frame(width: 42, height: 42)
                .background(
                    focused ? Color.white : (active ? Theme.accent.opacity(0.16) : Theme.surface2),
                    in: Circle()
                )
                .overlay(
                    Circle().stroke(active && !focused ? Theme.accent.opacity(0.55) : Color.clear, lineWidth: 1)
                )
                .foregroundStyle(focused ? Color.black : (active ? Theme.accentLight : Theme.text))
                .scaleEffect(focused ? 1.06 : 1)
                .animation(.easeOut(duration: 0.12), value: focused)
        }
        .buttonStyle(.plain)
        .focused($focused)
        .accessibilityLabel(Text(label))
    }
}

struct OnboardingView: View {
    @EnvironmentObject private var app: AppModel
    @State private var showAdd = false

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(hex: 0x160D08), Theme.background, Color(hex: 0x1A0C05)],
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()

            HStack(spacing: 70) {
                VStack(alignment: .leading, spacing: 24) {
                    BrandMark(size: 76)
                    Text(L.t("welcome.title"))
                        .font(.system(size: 73.2, weight: .heavy, design: .rounded))
                        .foregroundStyle(Theme.text)
                        .fixedSize(horizontal: false, vertical: true)
                    Text(L.t("welcome.subtitle"))
                        .font(.system(size: 27.1))
                        .foregroundStyle(Theme.textDim)
                        .frame(maxWidth: 760, alignment: .leading)
                        .fixedSize(horizontal: false, vertical: true)

                    VStack(alignment: .leading, spacing: 14) {
                        feature("play.rectangle.on.rectangle", L.t("welcome.feature1"))
                        feature("appletvremote.gen4", L.t("welcome.feature2"))
                        feature("waveform", L.t("welcome.feature3"))
                        feature("lock.shield", L.t("welcome.feature4"))
                    }
                    .padding(.top, 6)

                    Button {
                        showAdd = true
                    } label: {
                        HStack(spacing: 12) {
                            Image(systemName: "plus")
                            Text(L.t("welcome.start"))
                        }
                        .font(.system(size: 24.8, weight: .bold))
                        .padding(.horizontal, 28)
                        .padding(.vertical, 15)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(Theme.accent)
                    .foregroundStyle(Theme.background)
                    .padding(.top, 8)

                    Text(L.t("welcome.legal"))
                        .font(.system(size: 16.5))
                        .foregroundStyle(Theme.textFaint)
                        .padding(.top, 12)
                }
                .frame(maxWidth: 900, alignment: .leading)

                VStack(spacing: 22) {
                    ZStack {
                        RoundedRectangle(cornerRadius: 28, style: .continuous)
                            .fill(Theme.surface.opacity(0.62))
                            .frame(width: 400, height: 500)
                            .overlay(
                                RoundedRectangle(cornerRadius: 28, style: .continuous)
                                    .stroke(Theme.border, lineWidth: 1)
                            )
                        VStack(spacing: 18) {
                            Image(systemName: "play.tv.fill")
                                .font(.system(size: 113.3, weight: .thin))
                                .foregroundStyle(Theme.accent)
                            Text("Xtream · M3U")
                                .font(.system(size: 26, weight: .bold, design: .rounded))
                                .foregroundStyle(Theme.text)
                            Text("4K · HDR · AC3")
                                .font(.system(size: 17.7, weight: .semibold))
                                .tracking(2)
                                .foregroundStyle(Theme.textDim)
                        }
                    }
                    Text(L.t("misc.poweredBy"))
                        .font(.system(size: 15.3, weight: .semibold))
                        .foregroundStyle(Theme.textFaint)
                }
            }
            .padding(70)
        }
        .sheet(isPresented: $showAdd) {
            AddPlaylistView()
                .environmentObject(app)
        }
    }

    private func feature(_ symbol: String, _ text: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: symbol)
                .font(.system(size: 20.1, weight: .semibold))
                .foregroundStyle(Theme.accent)
                .frame(width: 26)
            Text(text)
                .font(.system(size: 21.2))
                .foregroundStyle(Theme.textDim)
        }
    }
}
