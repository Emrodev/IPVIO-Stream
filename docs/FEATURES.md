# Features

Feature register for version 1.4.8 (`versionName` in `app/build.gradle.kts`). Last checked against the code on 2026-11-23.

This file is the contract for what the app must keep doing. A feature is removed or hidden only after a decision by the maintainer, and the pull request must say so (see CLAUDE.md, section 6).

## How to read this table

- **Code anchor:** the main file or object that implements the feature. Line numbers are left out on purpose, because they go stale. Paths are relative to `app/src/main/java/com/ipvio/stream/`.
- **Code present:** checked that the anchor file exists and was read in this review. It does not mean the feature works.
- **Device tested:** `yes` only if the maintainer confirmed it on a device. `no` means the release notes say "not device tested yet". `unknown` means nobody has recorded a test. Nothing marked `no` or `unknown` is confirmed to work.
- Items marked **unverified** were not read in detail in this review.

## Playback and sources

| Feature | Code anchor | Code present | Device tested | Notes |
|---|---|---|---|---|
| Xtream Codes playlists | `XtreamApi.kt`, `XtreamLogic.kt` | yes | unknown | Falls back to `player_api.php` when `get.php` fails |
| Xtream backup servers with failover | `HostFailover.kt` | yes | no | T-009. See `docs/tasks/T-009-xtream-backup-servers.md` |
| M3U playlists (URL and file) | `Models.kt`, `CatchupLogic.kt` | yes | unknown | |
| Stalker/Ministra portals | `StalkerApi.kt`, `StalkerLogic.kt`, `StalkerMore.kt` | yes | unknown | Series load on open |
| Several playlists at once (multi view, "All playlists") | `MultiLogic.kt`, `AreaSource.kt`, `SectionPlaylistUi.kt`, `MultiView.kt`, `Header.kt`, `LiveTab.kt` | yes | no | Per section (T-051): Live TV, Movies and Series each pick any number of playlists, "All playlists" on top; ticking a playlist that is not loaded loads it next to the main list. Start page and search still use the shown set. The earlier "All playlists" entry depended on two or more shown playlists, see Known issues |
| Playlist edit window and per-playlist refresh plan | `PlaylistStore.kt`, `PlaylistRefresh.kt`, `PlaylistRefreshLogic.kt` | yes | no | 1.4.8 |
| Sublist filter | `SublistStore.kt`, `SublistLogic.kt` | yes | unknown | Settings page "Sublists" removed in T-035. Saved filters stay on the device |
| Live TV and TV guide (XMLTV) | `Epg.kt`, `EpgFull.kt` | yes | unknown | |
| Catch-up | `Catchup.kt`, `CatchupLogic.kt` | yes | unknown | Works only if the provider supplies it |
| Live rewind with buffer | `LiveTimeshift.kt` | yes | unknown | |
| Recording | `Recorder.kt` | yes | unknown | |
| Second stream (picture-in-picture or split) | `DualView.kt` | yes | unknown | |
| Lazy series loading | `LazySeries.kt` | yes | unknown | |
| Live quality bundles | `LiveBundle.kt` | yes | unknown | |
| Technical stream info on the detail page | `MediaProbe.kt`, `MediaProbeLogic.kt` | yes | no | T-033. See `docs/tasks/T-033-media-probe.md` |
| Trailer playback | `TrailerDialog.kt`, `TrailerLogic.kt` | yes | no | T-032 |
| Player | `PlayerScreen.kt` | yes | unknown | Protected area (CLAUDE.md, section 5) |

## Browsing, favorites and organisation

| Feature | Code anchor | Code present | Device tested | Notes |
|---|---|---|---|---|
| Home screen | `HomeScreen.kt`, `HomeParts.kt` | yes | unknown | Reported as lost after updates, see Known issues |
| Favorites for channels, movies and series | `Store.kt` (`Library`), `CatFavs.kt` | yes | unknown | |
| Manual order of favorites | `FavOrderLogic.kt`, `FavReorder.kt` | yes | no | T-014 and T-018 |
| Hide categories, ignore groups, language filter | `HiddenCats.kt`, `IgnoredGroups.kt`, `LangFilter.kt` | yes | unknown | |
| Private categories | `PrivateLogic.kt`, `PrivateCats.kt` | yes | no | T-028 |
| Genres and categories settings (tabs) | `SettingsHub.kt`, `CategoriesScreen.kt` | yes | no | 1.4.8 |
| Search and voice search | `SearchScreen.kt`, `SearchLogic.kt`, `VoiceSearch.kt` | yes | unknown | |
| Recently added | `RecentSortLogic.kt` | yes | unknown | |
| Profiles (max. 5) | `Profiles.kt` | yes | unknown | |
| Parental control | `Parental.kt`, `ParentalLogic.kt` | yes | unknown | 4-digit PIN, SHA-256 with salt, lock after 5 failed attempts for 60 s |
| Boomer mode (simplified interface) | `Boomer.kt` | yes | unknown | |
| Demo mode ("Continue without playlist") | `DemoMode.kt` | yes | no | Memory only, nothing saved |
| Live quality bundles, reminders, screen-edge calibration | `Live.kt`, `FitCalibration.kt` | yes | unknown | |

## Sports

| Feature | Code anchor | Code present | Device tested | Notes |
|---|---|---|---|---|
| Sports hub (ESPN, leagues, tables) | `SportHub.kt`, `SportLeague.kt`, `SportTable.kt`, `SportTab.kt`, `SportUi.kt` | yes | no | Competitions added in 1.4.8 are off by default and their ESPN addresses are not verified |
| Sports alerts | `SportAlerts.kt`, `SportWatch.kt` | yes | no | 1.4.8 |
| Sports Hub settings | `SportSettings.kt`, `SportHubPrefs.kt` | yes | no | 1.4.8 |

## Network, privacy and connections

| Feature | Code anchor | Code present | Device tested | Notes |
|---|---|---|---|---|
| Own DNS server (plain and DNS over HTTPS, system fallback) | `AppDns.kt`, `DnsLogic.kt` | yes | no | T-049f |
| VPN with WireGuard (import a `.conf`, encrypted storage) | `AppVpn.kt`, `VpnLogic.kt`, `VpnSettings.kt` | yes | no | T-049e. No kill switch |
| Certificate consent for plain HTTP (TLS fallback) | `TlsFallback.kt`, `TlsFallbackLogic.kt` | yes | no | Open security item: silent HTTPS-to-HTTP downgrade, see `docs/SECURITY.md` |
| Phone import, transfer, crash report | `PhoneImport.kt` | yes | unknown | Ports 8765-8775. Plain HTTP on the local network |
| Phone remote and phone search | `RemoteHub.kt`, `RemoteLogic.kt`, `RemoteUi.kt`, `RemoteMore.kt` | yes | unknown | Routes `/r` and `/b/CODE` are protected (CLAUDE.md, section 5) |
| List import (Letterboxd, MAL, Trakt) | `ListImport.kt`, `ListImportUi.kt`, `ListImportApply.kt` | yes | unknown | |
| Trakt sync | `ListImportApply.kt`, `HomeScreen.kt`, `SettingsHub.kt` | yes | no | T-030 and T-031. **Unverified** how far the sync goes |
| TMDB / OMDb metadata | `Meta.kt`, `Config.kt` | yes | unknown | Only with the user's own keys. IMDb values come through OMDb |

## Backup, sync and updates

| Feature | Code anchor | Code present | Device tested | Notes |
|---|---|---|---|---|
| Backup and restore (`ipvio_backup.json`) | `Backup.kt` | yes | unknown | Backup JSON keys are protected (CLAUDE.md, section 5) |
| Google Drive backup and sync | `DriveSync.kt` (329 lines), `DriveLogic.kt`, `DriveUi.kt` | yes | no | Back in the code since 1.4.2 according to CLAUDE.md. Scope `drive.file`. See [GOOGLE_DRIVE.md](GOOGLE_DRIVE.md) |
| Updates via GitHub Releases | `AppUpdate.kt`, `Updater.kt`, `UpdateCard.kt`, `UpdateVerify.kt` | yes | unknown | Verified by Android signature. No hash check |
| Update card on first start | `UpdateCard.kt`, `UpdateCardLogic.kt` | yes | unknown | T-014, T-025 |

## Languages

| Feature | Code anchor | Code present | Device tested | Notes |
|---|---|---|---|---|
| UI languages | `I18nCore.kt`, `Dict*.kt` | yes | unknown | 12 codes: de, en, tr, bs, hr, sr, es, fr, it, nl, ar, pl. Arabic with right-to-left layout (T-029) |

## Removed or not in the repository

| Feature | Status | Notes |
|---|---|---|
| Licence system | not in the repository | Removed on purpose when the project became open source |
| IMDb dataset import | removed | Only clean-up of old files remains |
| Sublists page in Settings | removed (T-035) | Saved sublist definitions stay on the device |

## Known issues (open, from the review of 2026-11-23)

- **"All playlists" can disappear.** The entry depends on several playlists being shown together. The list of shown playlists is stored as hashes of the playlist addresses (`PlaylistStore.kt`, `key()`). The cause is not confirmed. Needs a device check.
- **Home screen lost after updates** was reported by the maintainer. No cause found yet.
- **Google Drive status conflicts with older notes.** This file said "removed in 1.2" until now. CLAUDE.md says Drive is back since 1.4.2. The code confirms `DriveSync.kt`. Its device status is unknown.

## Planned

- No new features until the known issues above are resolved and checked on a device.
