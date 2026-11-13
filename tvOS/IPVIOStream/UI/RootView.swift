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
                    .tint(Theme.green)
                    .scaleEffect(1.2)
            }
        }
    }
}

struct MainShell: View {
    @EnvironmentObject private var app: AppModel
    @State private var path = NavigationPath()

    var body: some View {
        HStack(spacing: 0) {
            Sidebar(path: $path)
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

struct Sidebar: View {
    @EnvironmentObject private var app: AppModel
    @Binding var path: NavigationPath
    @FocusState private var focused: AppSection?

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 14) {
                BrandMark(size: 46)
                VStack(alignment: .leading, spacing: 1) {
                    Text("IPVIO")
                        .font(.system(size: 22, weight: .heavy, design: .rounded))
                        .foregroundStyle(Theme.text)
                    Text("STREAM")
                        .font(.system(size: 11, weight: .bold))
                        .tracking(3)
                        .foregroundStyle(Theme.green)
                }
            }
            .padding(.bottom, 40)

            VStack(spacing: 8) {
                ForEach(AppSection.allCases, id: \.self) { section in
                    navButton(section)
                }
            }
            .focusSection()

            Spacer()

            VStack(alignment: .leading, spacing: 7) {
                Text(L.t("nav.sources"))
                    .font(.system(size: 12, weight: .bold))
                    .tracking(1.5)
                    .foregroundStyle(Theme.textFaint)
                Text("\(app.playlists.filter(\.isEnabled).count) / \(app.playlists.count)")
                    .font(.system(size: 19, weight: .semibold, design: .rounded))
                    .foregroundStyle(Theme.text)
                Text(app.playlists.filter(\.isEnabled).map(\.name).joined(separator: " · "))
                    .font(.system(size: 13))
                    .foregroundStyle(Theme.textDim)
                    .lineLimit(3)
            }
            .padding(.bottom, 26)

            Text("v\(AppInfo.version)")
                .font(.system(size: 12, design: .monospaced))
                .foregroundStyle(Theme.textFaint)
        }
        .padding(.horizontal, 22)
        .padding(.top, 42)
        .padding(.bottom, 30)
        .frame(width: 280)
        .background(
            LinearGradient(
                colors: [Color(hex: 0x0D1422), Theme.background],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
        )
        .overlay(alignment: .trailing) {
            Rectangle()
                .fill(Theme.border)
                .frame(width: 1)
                .ignoresSafeArea()
        }
    }

    private func navButton(_ section: AppSection) -> some View {
        let selected = app.selectedSection == section
        let isFocused = focused == section
        return Button {
            path = NavigationPath()
            app.setSection(section)
        } label: {
            HStack(spacing: 14) {
                Image(systemName: section.symbol)
                    .font(.system(size: 19, weight: .semibold))
                    .frame(width: 26)
                Text(L.t(section.titleKey))
                    .font(.system(size: 19, weight: selected ? .bold : .medium))
                Spacer()
                if selected {
                    Circle()
                        .fill(isFocused ? Color.black.opacity(0.65) : Theme.green)
                        .frame(width: 7, height: 7)
                }
            }
            .padding(.horizontal, 15)
            .padding(.vertical, 13)
            .background(
                RoundedRectangle(cornerRadius: 13, style: .continuous)
                    .fill(isFocused ? Color.white : (selected ? Theme.green.opacity(0.13) : Color.clear))
            )
            .foregroundStyle(isFocused ? Color.black : (selected ? Theme.greenBright : Theme.textDim))
            .scaleEffect(isFocused ? 1.02 : 1)
            .animation(.easeOut(duration: 0.13), value: isFocused)
        }
        .buttonStyle(.plain)
        .focused($focused, equals: section)
    }
}

struct OnboardingView: View {
    @EnvironmentObject private var app: AppModel
    @State private var showAdd = false

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [Color(hex: 0x0B1220), Theme.background, Color(hex: 0x071B12)],
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
                    .tint(Theme.green)
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
                                .foregroundStyle(Theme.green)
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
                .foregroundStyle(Theme.green)
                .frame(width: 26)
            Text(text)
                .font(.system(size: 18))
                .foregroundStyle(Theme.textDim)
        }
    }
}
