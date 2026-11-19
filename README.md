# IPVIO Stream

**A free, open-source IPTV player for Android TV, Google TV and Amazon Fire TV.** Built for the TV remote: live TV with TV guide and rewind, movies, series, sports and favourites, from your own playlists.

![IPVIO Stream home screen](docs/screenshots/homescreen.jpg)

> **IPVIO Stream does not provide any content.** It contains no channels, playlists, films or series. It is only a player for playlists that **you** add (Xtream Codes login, M3U address or file, or Stalker portal). Only use sources you are legally allowed to use.

---

## Quick start

**Downloader code (Fire TV / Android TV):** `4200542`

1. Install the **Downloader** app on your device, open it, enter the code `4200542` and download the APK.
2. Allow **Install unknown apps** for Downloader when asked, then install.
3. Start the app, choose **Add playlist** and enter your Xtream login, M3U address or Stalker portal.

Or download the APK directly: [IPVIO-Stream.apk (latest release)](https://github.com/Emrodev/IPVIO-Stream/releases/latest/download/IPVIO-Stream.apk)

**Requirements:** Android 7.0 (API 24) or newer. No account or registration needed.

**Tip:** no TV remote with a keyboard? Under *Playlists > Enter from phone* the app shows an address. Open it on your phone in the same Wi-Fi and type the login there.

---

## Key features

- **Live TV with TV guide.** Categories, favourites and your own channel lists. Now and next at a glance, a timeline TV guide with preview, and reminders with notification.
- **Rewind live TV and record.** Pause and rewind the running channel with an on-device buffer (256 MB to 2 GB). Record the current show to internal storage or a USB stick.
- **Catch-up.** Watch from the beginning or the archive, if your provider supplies it.
- **Movies and series.** Rotating header, rows like *Continue watching*, *Top 10 today* and *Because you watched*, filters by genre, year, rating and country. Series with seasons, episodes and "go to episode" for very long series.
- **Several playlists at once.** Combine playlists with *Also show*. Movies and series appear together, with the same title only once. Filter by source.
- **Sports hub.** Sports on now and coming up, tables and all sports channels.
- **Player that works hard for you.** Audio and subtitle choice (remembered per title), AC3/E-AC3/DTS sound (also over Bluetooth), skip intro and credits, next episode automatically, quality choice with fallback to another source on errors. HLS, MPEG-DASH and MPEG-TS.
- **Built for the remote.** Navigation is designed around the D-pad, OK and the menu key. *Boomer mode* is a simplified player: left and right switch channels, nothing else opens.
- **Profiles and parental controls.** "Who's watching?" with a separate history and favourites per person. Parental controls with a PIN.
- **Your data stays on your device.** Keys, logins and favourites are stored only on the device, unless you turn on the optional Google Drive backup. The app is ad-free.
- **Optional metadata.** Posters, descriptions, cast and ratings from TMDB and OMDb, with your own free keys. Without keys, Xtream providers supply the data.
- **Backup, transfer and updates.** Back up and restore settings and favourites, move them to another device over Wi-Fi, optional Google Drive backup, and in-app updates from GitHub Releases.
- **Look and feel.** IPVIO green on navy plus colour themes, backgrounds, custom colours, three font choices, adjustable text and display size. Sidebar left, right or top. 10 languages.

---

## Good to know: remote control tips

Some things are not obvious at first. This is what the buttons do.

### Live TV (playing a channel)

| Button | What it does |
|---|---|
| **Left** | Opens the channel list |
| **Up / Down** | Zap to the previous / next channel |
| **Right** | Opens the TV guide |
| **OK** | Shows the player controls |
| **Number keys** | Type a channel number and press OK |
| **0** | Jumps back to the last channel |
| **Red button** | Records the current show (until its end in the TV guide) |
| **Menu key** or **long OK** (in the channel list) | Opens the menu for the channel |

### Lists and cards

- **Long OK** (hold about 0.35 s) or the **menu key** on a card opens a menu. A short OK still opens the title or channel.
- On **favourites** (channels, movies and series) the menu holds Move up, Move down and Move to top.
- On **Favourites**, the menu also holds *Remove from favourites*. Some remotes (Fire TV) have no menu key. On those, use long OK.
- Long OK also marks an episode as seen on a series page.

### Movies and series (playing a title)

- **Left / Right** seek back and forth (step length in *Settings > Player*).
- **Number keys 0 to 9** jump to 0 % to 90 % of the movie.
- **OK** after a seek confirms the target position and keeps playing.

### Favourites, My list and History

- **Favourites** (heart in the sidebar): channels, movies and series you starred. Add them with long OK or the menu key on a card, or with the favourite button on the detail page. In Favourites and in *Live TV > Favourites* you can sort them by hand: long OK or menu key, then move them up, down or to the top. The order also decides the zapping order.
- **My list** (bookmark in the sidebar): movies and series you want to watch later. Add them on the title's detail page with "My list". It is not used for live channels.
- **History** (clock): what you watched recently, movies and series separately.

### Customise the sidebar

- **Settings > View > Sidebar**: put it on the left, right or top, with icons only or with labels.
- **Settings > Menu**: change the order of the entries and hide the ones you do not use.
- **Search** and the **TV guide (EPG)** are hidden in the sidebar by default. Search is in the header. The TV guide is reachable from Live TV, and you can show it again under *Settings > Menu*.

### Playlists

- **Several playlists:** *Settings > Playlists > Also show* adds a second playlist to the active one.
- **Content mode:** *Settings > Playlists* can limit a playlist to live TV only or movies and series only.
- **Refresh:** the playlist is refreshed automatically in the background, not during playback. You can choose the interval or a fixed time.
- **Xtream with blocked `get.php`:** the app loads the account through `player_api.php` automatically.

### Live TV rewind and recordings

- *Settings > Live TV & guide > Rewind live TV* keeps the running channel in a buffer. Press **Back to live** to return to the live picture.
- Recordings are listed under *Live TV > Recordings*. You can choose internal storage or a USB stick in the settings.

### Settings and resets

- Every settings page has its own reset button at the bottom. It resets only that page.
- *Settings > Reset all* resets all settings. Playlists, favourites, history, resume points, API keys, the parental-control PIN, the custom TV guide address and reminders are **not** touched.

### Try the app before you add a playlist

- On the first start, choose the option to continue without a playlist. The app opens a demo mode with sample channels, movies and series so you can explore the interface. Nothing is saved, nothing plays, and the samples disappear as soon as you add a real playlist.

### On a phone

- The app runs in landscape. Tap the video to show the controls, double-tap left or right to skip, double-tap in the middle to pause.

---

## Screenshots

| | |
|---|---|
| ![Home screen](docs/screenshots/homescreen.jpg)<br>**Home**: rotating header, quick tiles, live TV now | ![Series details](docs/screenshots/details_series.jpg)<br>**Series details**: seasons, episodes with ratings, director |
| ![Categories](docs/screenshots/categories_list.jpg)<br>**Automatic genres**: detected in the background | ![Director page](docs/screenshots/producer.jpg)<br>**Cast and crew pages**: with the titles from your playlist |
| ![Player](docs/screenshots/player.jpg)<br>**Player**: skip intro, audio and subtitles, quality | ![TV guide](docs/screenshots/tvguide.jpg)<br>**TV guide**: timeline with preview |
| ![Live TV](docs/screenshots/channel_list.jpg)<br>**Live TV**: now and next at a glance | ![Settings](docs/screenshots/settings.jpg)<br>**Settings**: grouped, remote-friendly |

<sub>Screenshots taken on a TV. Film artwork, channel logos and programme data belong to their respective owners; provider categories are blurred.</sub>

---

## Your own API keys (optional)

The app ships **without any API keys**. You can add your own free keys under **Settings > API keys**:

| Service | Used for | Key |
|---|---|---|
| TMDB | posters, descriptions in your language, cast, trailers, automatic genres, episode info, similar titles | free, see below |
| OMDb | IMDb and Rotten Tomatoes ratings | free, see below |
| TheIntroDB | intro, recap and credits times | no key needed |

**Without keys** the app still works. With an Xtream Codes playlist it takes description, cover, rating, genres, running time and trailer **directly from your provider**. With a TMDB key it uses TMDB first and falls back to the provider when a title is not found. Keys are stored only on the device.

### Get a free TMDB key
1. Create an account at https://www.themoviedb.org/signup and confirm the e-mail address.
2. **Log in.** The API page shows "restricted" as long as you are not logged in.
3. Open https://www.themoviedb.org/settings/api (or: profile picture > Settings > API) and click **Request an API key**.
4. Choose **Developer**, accept the terms and fill in the form (application name e.g. "IPVIO Stream", type "Personal use", a short description; the URL can be your GitHub page).
5. Copy the **API Key** (the 32-character one, not the "API Read Access Token").

### Get a free OMDb key
1. Open https://www.omdbapi.com/apikey.aspx, choose **FREE** (1,000 requests per day), enter your e-mail address and a short reason (e.g. "personal media player").
2. You receive an e-mail with an 8-character key and an **activation link**. Click the link, otherwise the key does not work.

### Enter the keys in the app
*Settings > API keys*: paste the key, press **Save and check** (a green "Key valid" confirms it) and restart the app once. Tip: type long keys with the phone import or a USB keyboard instead of the remote.

*This product uses the TMDB API but is not endorsed or certified by TMDB.*

---

## Documentation

| Document | For |
|---|---|
| [FAQ](docs/FAQ.md) | Common questions and problems |
| [SETTINGS](docs/SETTINGS.md) | Overview of every settings page |
| [FEATURES](docs/FEATURES.md) | Feature list with the code locations |
| [ARCHITECTURE](docs/ARCHITECTURE.md) | How the app is structured |
| [GOOGLE_DRIVE](docs/GOOGLE_DRIVE.md) | Optional Google Drive backup and live sync |
| [RELEASE](docs/RELEASE.md) | Signing, releases and in-app updates |
| [SECURITY](docs/SECURITY.md) | Security notes and known open items |
| [CHANGELOG](CHANGELOG.md) | What changed in each version |

---

## Building

Requirements: Android Studio (current version) with the Android SDK. The Gradle wrapper downloads everything else.

```
git clone <this repository>
cd IPVIO-Stream
./gradlew assembleDebug        # Windows: gradlew.bat assembleDebug
```

The APK is then in `app/build/outputs/apk/debug/`. For releases and in-app updates see [docs/RELEASE.md](docs/RELEASE.md). Every release must be signed with the same key (`keystore.properties`), otherwise users lose their data when updating.

Optional Google Drive backup: [docs/GOOGLE_DRIVE.md](docs/GOOGLE_DRIVE.md).

Developers can optionally copy `secrets.properties.example` to `secrets.properties` (ignored by Git) and put their keys there. They are only used when no key was entered in the app.

Unit tests for the pure logic: `./gradlew test`

## Project layout

```
app/src/main/java/com/ipvio/stream/   all Kotlin sources (one package)
  *Logic.kt                           pure, Android-free logic (covered by unit tests)
  Dict*.kt                            translations (one row per text: de¦en¦tr¦bs¦hr¦sr¦es¦fr¦it)
app/src/test/java/com/ipvio/stream/   JUnit tests
app/src/main/res/font/                Outfit and Inter (SIL Open Font License)
licenses/                             font licences
```

## Contributing

Changes go through feature branches and pull requests into `dev`. See [docs/RELEASE.md](docs/RELEASE.md) for releases and [CLAUDE.md](CLAUDE.md) for the working rules.

## Support the project

Hello, I'm Claude. If you enjoy using this project and want to help this wannabe developer keep building things (and occasionally understand what he's doing), feel free to support the project.

- Ko-fi: https://ko-fi.com/ipviodude
- PayPal: https://www.paypal.com/ncp/payment/EUSU4WAGRJNXY

IPVIO Stream stays free, ad-free and open source. Donations are voluntary and do not unlock any features.

## License

Copyright (C) 2026 IPVIO Stream contributors

IPVIO Stream is free software: you can redistribute it and/or modify it under the terms of the **GNU General Public License version 3** (see [LICENSE](LICENSE)). The GPL is required because the app includes the GPL-3.0 licensed FFmpeg audio decoder. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for the libraries, fonts and services used.

The name "IPVIO" and the IPVIO logo identify the original project. If you publish a modified version, please use your own name and logo so that users are not confused.

## Disclaimer

This software is provided "as is", without warranty of any kind. The authors do not provide, host, sell or link to any streams or playlists and are not responsible for how the app is used. Using playlists without the rights holders' permission may be illegal in your country.
