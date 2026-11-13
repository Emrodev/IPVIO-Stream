import SwiftUI

struct AddPlaylistView: View {
    @EnvironmentObject private var app: AppModel
    @Environment(\.dismiss) private var dismiss

    @State private var mode: PlaylistMode = .xtream
    @State private var name = ""
    @State private var server = ""
    @State private var username = ""
    @State private var password = ""
    @State private var m3uURL = ""
    @State private var busy = false
    @State private var errorMessage: String?

    var body: some View {
        ZStack {
            Theme.background.ignoresSafeArea()

            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    header

                    HStack(spacing: 10) {
                        ForEach(PlaylistMode.allCases) { option in
                            SelectChip(
                                title: L.t(option.titleKey),
                                selected: mode == option
                            ) {
                                mode = option
                                errorMessage = nil
                            }
                        }
                    }

                    VStack(alignment: .leading, spacing: 14) {
                        LabeledTextField(title: L.t("add.name"), placeholder: L.t("add.namePlaceholder"), text: $name)

                        if mode == .xtream {
                            LabeledTextField(title: L.t("add.server"), placeholder: L.t("add.serverPlaceholder"), text: $server)
                            LabeledTextField(title: L.t("add.username"), placeholder: "username", text: $username)
                            LabeledTextField(title: L.t("add.password"), placeholder: "••••••••", text: $password, isSecure: true)
                            Text(L.t("add.helpXtream"))
                                .font(.system(size: 14))
                                .foregroundStyle(Theme.textDim)
                        } else {
                            LabeledTextField(title: L.t("add.url"), placeholder: L.t("add.urlPlaceholder"), text: $m3uURL)
                            Text(L.t("add.helpM3u"))
                                .font(.system(size: 14))
                                .foregroundStyle(Theme.textDim)
                        }

                        Text(L.t("add.credentialsNote"))
                            .font(.system(size: 13))
                            .foregroundStyle(Theme.textFaint)
                    }
                    .padding(24)
                    .cardSurface(corner: 18)

                    if let errorMessage {
                        HStack(spacing: 10) {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundStyle(Theme.gold)
                            Text(errorMessage)
                                .font(.system(size: 16))
                                .foregroundStyle(Theme.text)
                        }
                        .padding(14)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Theme.surface2, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }

                    HStack(spacing: 14) {
                        Button {
                            Task { await importPlaylist() }
                        } label: {
                            HStack(spacing: 10) {
                                if busy {
                                    ProgressView()
                                        .tint(Theme.background)
                                }
                                Text(busy ? L.t("add.connecting") : L.t("add.test"))
                            }
                            .font(.system(size: 19, weight: .bold))
                            .padding(.horizontal, 26)
                            .padding(.vertical, 13)
                        }
                        .buttonStyle(.borderedProminent)
                        .tint(Theme.accent)
                        .foregroundStyle(Theme.background)
                        .disabled(busy)

                        Button(L.t("common.cancel")) {
                            dismiss()
                        }
                        .buttonStyle(.bordered)
                        .disabled(busy)
                    }

                    if busy {
                        VStack(alignment: .leading, spacing: 8) {
                            ProgressView(value: app.job?.progress ?? 0)
                                .tint(Theme.accent)
                            Text(app.job?.status ?? L.t("add.connecting"))
                                .font(.system(size: 14))
                                .foregroundStyle(Theme.textDim)
                        }
                        .padding(.top, 4)
                    }
                }
                .padding(48)
                .frame(maxWidth: 900, alignment: .leading)
                .frame(maxWidth: .infinity)
            }
        }
        .preferredColorScheme(.dark)
        .interactiveDismissDisabled(busy)
    }

    private var header: some View {
        HStack(alignment: .top) {
            VStack(alignment: .leading, spacing: 6) {
                Text(L.t("add.title"))
                    .font(.system(size: 42, weight: .heavy, design: .rounded))
                    .foregroundStyle(Theme.text)
                Text(L.t("welcome.legal"))
                    .font(.system(size: 15))
                    .foregroundStyle(Theme.textFaint)
            }
            Spacer()
            Button {
                dismiss()
            } label: {
                Image(systemName: "xmark")
                    .font(.system(size: 18, weight: .bold))
                    .frame(width: 42, height: 42)
            }
            .buttonStyle(.bordered)
            .disabled(busy)
        }
    }

    private func importPlaylist() async {
        guard !busy else { return }
        busy = true
        errorMessage = nil
        defer { busy = false }

        do {
            var source: PlaylistSource
            if mode == .xtream {
                if let detected = PlaylistInput.xtreamAccount(from: server) {
                    source = .xtream(detected)
                } else {
                    source = try PlaylistInput.makeSource(
                        mode: .xtream,
                        name: name,
                        url: "",
                        server: server,
                        username: username,
                        password: password
                    )
                }
            } else {
                source = try PlaylistInput.makeSource(
                    mode: .m3u,
                    name: name,
                    url: m3uURL,
                    server: "",
                    username: "",
                    password: ""
                )
            }
            try await app.addPlaylist(name: name, source: source)
            dismiss()
        } catch {
            errorMessage = (error as? LocalizedError)?.errorDescription ?? error.localizedDescription
        }
    }
}
