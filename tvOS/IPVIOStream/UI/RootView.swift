import SwiftUI

struct RootView: View {
    @EnvironmentObject private var app: AppModel
    @Environment(\.scenePhase) private var scenePhase

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
        .fullScreenCover(item: $app.playback) { request in
            PlayerContainer(request: request)
                .environmentObject(app)
        }
        .preferredColorScheme(.dark)
    }
}

struct SplashView: View {
    var body: some View {
        ZStack {
            Theme.background.ignoresSafeArea()
            VStack(spacing: 24) {
                BrandMark(size: 96)
                Text("IPVIO Stream")
                    .font(.system(size: 46, weight: .heavy, design: .rounded))
                    .foregroundStyle(Theme.text)
                ProgressView()
                    .tint(Theme.accent)
                    .scaleEffect(1.2)
            }
        }
    }
}

struct MainShell: View {
    @EnvironmentObject private var app: AppModel
    @State private var path = NavigationPath()

    var body: some View {
        VStack(spacing: 0) {
            TopBar(path: $path)
            ZStack {
                Theme.background.ignoresSafeArea()
                NavigationStack(path: $path) {
                    sectionContent
                        .navigationDestination(for: Route.self) { route in
                            switch route {
                            case let .detail(id):
                                DetailView(itemID: id)
                            }
                        }
                }
            }
        }
        .background(Theme.background)
    }

    @ViewBuilder
    private var sectionContent: some View {
        switch app.selectedSection {
        case .home:
            HomeView(path: $path)
        case .live:
            LiveView()
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
    @Binding var path: NavigationPath
    @FocusState private var focusedTab: AppSection?

    private let tabs: [AppSection] = [.home, .live, .movies, .series, .settings]

    var body: some View {
        HStack(spacing: 18) {
            Button {
                path = NavigationPath()
                app.setSection(.home)
            } label: {
                HStack(spacing: 12) {
                    BrandMark(size: 38)
                    VStack(alignment: .leading, spacing: 0) {
                        Text("IPVIO")
                            .font(.system(size: 21, weight: .heavy, design: .rounded))
                            .foregroundStyle(Theme.text)
                        Text("STREAM")
                            .font(.system(size: 9, weight: .bold))
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
                    path = NavigationPath()
                    app.setSection(.search)
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
                        .font(.system(size: 13, weight: .heavy, design: .rounded))
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
            path = NavigationPath()
            app.setSection(section)
        } label: {
            Text(L.t(section.titleKey))
                .font(.system(size: 17, weight: selected ? .bold : .medium))
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
                .font(.system(size: 19, weight: .semibold))
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
                        .font(.system(size: 62, weight: .heavy, design: .rounded))
                        .foregroundStyle(Theme.text)
                        .fixedSize(horizontal: false, vertical: true)
                    Text(L.t("welcome.subtitle"))
                        .font(.system(size: 23))
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
                        .font(.system(size: 21, weight: .bold))
                        .padding(.horizontal, 28)
                        .padding(.vertical, 15)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(Theme.accent)
                    .foregroundStyle(Theme.background)
                    .padding(.top, 8)

                    Text(L.t("welcome.legal"))
                        .font(.system(size: 14))
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
                                .font(.system(size: 96, weight: .thin))
                                .foregroundStyle(Theme.accent)
                            Text("Xtream · M3U")
                                .font(.system(size: 22, weight: .bold, design: .rounded))
                                .foregroundStyle(Theme.text)
                            Text("4K · HDR · AC3")
                                .font(.system(size: 15, weight: .semibold))
                                .tracking(2)
                                .foregroundStyle(Theme.textDim)
                        }
                    }
                    Text(L.t("misc.poweredBy"))
                        .font(.system(size: 13, weight: .semibold))
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
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(Theme.accent)
                .frame(width: 26)
            Text(text)
                .font(.system(size: 18))
                .foregroundStyle(Theme.textDim)
        }
    }
}
