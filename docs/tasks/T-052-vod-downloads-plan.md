# T-052 – Downloads for movies and series (plan, not started)

Status: **T-052a started (22 Nov 2026): engine, service, manifest entry, detail button for movies, rules and JVM test. Not built, not device tested. Playback from the download and the download list are open (step T-052a, second part, needs approval: player core).**
Source: user request, 22 Nov 2026. Reference the user saw: another IPTV app (UltraTV). Goal: download VOD (movies and series episodes) to the device, so that 4K titles play smoothly later, with weak internet or a slow device.

## Why
- 4K streams can stutter on weak connections or slow devices. A downloaded copy plays from local storage.
- Users can prepare titles before a trip or at night on Wi-Fi.

## What already exists (read in the code)
- `Recorder.kt`: live-TV recordings run as a foreground service, store `.ts` files with a small `.json` next to them (title, channel, times), list them and delete them. Recordings are played again through `Recorder.itemOf` as local `file://` items. The same pattern fits downloads.
- Storage folders: `Recorder.dirs` / `Recorder.dir` (external app folders, selectable).
- Media3 `1.11.1` is already a dependency (`media3-exoplayer`, `-hls`, `-dash`). The `offline` package (DownloadManager, DownloadService) belongs to `media3-exoplayer`; **to be verified in the library before the decision**.
- No download code exists yet (grep: none).

## Two ways to build it

**Option A – own downloader (OkHttp, like the recorder)**
- Plain HTTP range download to a file, resume with `Range`, one foreground service.
- Pros: no new service in the manifest needed if the recorder service pattern is reused (still a manifest change for a second service, to check); small code.
- Cons: we write retry, resume and queue logic ourselves. HLS streams (many Xtream VOD are `.m3u8`) need segment handling, which is a lot of code.

**Option B – Media3 offline (`DownloadManager` + `DownloadService`)** (recommended)
- Handles progressive files (mp4, mkv) and HLS/DASH, resume after network loss, retries, queue, pause and remove.
- Pros: robust on weak connections (the main reason for the feature); no new library.
- Cons: a `DownloadService` must be declared in `AndroidManifest.xml` (protected area, section 5 and 6 approval); a notification is required while downloading; more API to learn.

**Recommendation: Option B.** Resume after a dropped connection is the core value, and Media3 already does it well. It needs the manifest change, which is part of the approval.

## Scope in steps (each step testable)
1. **T-052a Movies, progressive files only:** a "Download" button on the movie detail page, a Downloads list (Settings or Library), progress, play from the file, delete. Storage: app folder on external storage, free-space check before the start.
2. **T-052b Series:** download per episode and "all episodes of a season". Play from local file with the same resume position logic as now (watch position stored per item).
3. **T-052c Queue and settings:** one or two downloads at a time, Wi-Fi only (default on), pause and resume, a storage limit warning.
4. **T-052d HLS VOD:** only if the provider sends `.m3u8` for VOD and step 1 is working.

## Acceptance criteria (step 1)
- A movie downloads, the progress is shown, the download survives a short Wi-Fi drop (resumes, not restarts).
- The downloaded movie plays with no network (airplane mode).
- Deleting a download frees the space and removes the entry.
- Not enough free space: a clear message, nothing starts.
- The backup (`Backup.kt`) is **not** changed: downloads are large and are not part of the backup (decision needed, see questions).

## Risks
- **Storage:** a 4K movie can be 10–30 GB. Fire TV internal storage is small; external storage or an SD card is needed for many titles.
- **Expiring links:** Xtream VOD addresses carry the login and work for later use; Stalker portal links may need a fresh lookup before a download (**unverified**, needs a test with the portal).
- **Provider terms:** downloading provider content may be against the terms of the provider. The app is free software (GPL-3.0); the feature should be opt-in and documented. The maintainer decides whether to include it.
- **Battery and load:** downloads run in the background; the notification has to be visible, and downloads stop when the device sleeps unless the service keeps them running.
- **Manifest change:** the `DownloadService` declaration is a protected change (section 5); it needs the maintainer's approval.
- **Dependencies:** none new if Option B uses the existing Media3 version.

## Test steps the user can run (offered by the user)
1. Download one 4K movie over a normal connection; time and size of the download.
2. Download the same movie, switch Wi-Fi off for 30 s in the middle, switch back on: the download resumes.
3. Play the downloaded file with Wi-Fi off.
4. Remove the download; check that the space is free again.
5. Fill the storage (or use a nearly full device): the start is refused with a message.
6. On an older Fire TV stick: start two downloads at the same time, check playback of a third title during the downloads.

## Questions for the maintainer (decisions needed before implementation)
1. Option B (Media3, recommended, manifest change) or Option A (own code, no manifest change)?
2. First step only movies, or movies and series together?
3. Downloads on the device's internal storage, on external storage, or the user chooses?
4. Wi-Fi only by default?
5. Are downloads per profile (`_p<ID>` like other data) or shared?
6. Should downloads be in the backup? (Recommendation: no, they are large.)
7. Is the feature acceptable for the project given the provider terms? (Maintainer's decision.)

## Documentation (when implemented)
`CHANGELOG.md`, `docs/FEATURES.md`, `docs/PLAYER.md` (playback from a file), `docs/SECURITY.md` (where the files are stored, no secrets in file names).

## Decisions (maintainer, 22 Nov 2026)
- Option B (Media3 offline). Movies and series together.
- Quality chosen per download when the provider offers several HLS variants (4K, 1080p, 720p). Re-encoding on the device is not planned: too slow and too heavy for a Fire TV stick, and it loses quality.
- Storage location and Wi-Fi-only: the user decides (no default set by us beyond what is needed for safety).
- Downloads belong to the profile (`_p<ID>`).
- No backup of downloads.
- First download shows a notice about the provider terms; the user confirms it once.
- Optional automatic deletion after X days without watching; off by default, the user sets it on and chooses the days.

## T-052a status
- Done (code, not built): `DownloadLogic.kt` (rules, test `DownloadLogicTest`), `DownloadStore.kt` (Media3 DownloadManager, SimpleCache in the app folder on external storage, max one download at a time), `VodDownloadService.kt` (foreground service, manifest entry `dataSync`), `DownloadUi.kt` (button and one-time notice), hook in `DetailScreen.kt`, English rows in `DictEn4.kt`.
- Open: playback of a finished download (the player must read the cache: `CacheDataSource` in front of the stream source, player core, approval needed); download list with delete and storage view; series episodes; Wi-Fi-only option; quality choice; auto-delete; per-profile folders.
- Media3 API used: `DownloadManager`, `DownloadRequest`, `DownloadService.sendAddDownload`, `DownloadNotificationHelper`, `StandaloneDatabaseProvider`, `SimpleCache`, `NoOpCacheEvictor`. **Compile check pending** (no Gradle here).

## T-052a second part (playback, code only)
- `DownloadStore.playbackFactory` puts a read-only cache source in front of the player source (`PlayerScreen.kt`, one line). Finished addresses read from the cache, every other address goes to the old source unchanged (`DownloadAwareDataSource`).
- The download key is now the stream address (no custom cache key), so the player finds the download by the address it plays. If the host failover changes the address, playback falls back to the network.
- Open: download list with delete and storage view; series; Wi-Fi-only; quality; auto-delete; per profile. Not built, not device tested. Player core change: needs the maintainer's review.

## T-052a third part (download list, code only)
- Settings > Storage > Downloads: list of all downloads with status, size, total, delete. The title is stored with the download request.
- Still open from the plan: series episodes, HLS and DASH, Wi-Fi-only, quality choice, automatic deletion after X days, per-profile folders. Not built, not device tested.

## T-052a fourth part (HLS and quality choice, code only)
- `HlsLogic.kt` (pure): reads the master playlist, lists variants with label (height and Mbit/s) and absolute address. Test `HlsLogicTest`.
- `DownloadStore`: HLS requests use the MIME type `application/x-mpegURL`. A finished download is played from its variant address (`localUrlFor`, used in the detail page). The player reads a file from the cache if the cache already holds its first byte (`isLocal`), so HLS segments come from the cache offline.
- Open: size estimate per quality (needs the duration), Wi-Fi-only, series episodes, automatic deletion, per-profile folders, DASH. Not built, not device tested. HLS offline playback is the riskiest part: check it first on a device.

## T-052a fifth part (Wi-Fi only, code only)
- Switch "Nur im WLAN laden" on Settings > Storage > Downloads (default off). Stored in `ipvio_vod`; applied through `DownloadManager.setRequirements` (unmetered network or any network).
- Still open: automatic deletion after X days, series episodes, size per quality, per-profile folders, DASH.

## T-052a sixth part (code only)
- Series episodes: menu key on an episode starts its download (best HLS quality, chosen automatically); the episode shows its state; playback uses the local copy when present. Episode id: series id, season, number.
- DASH (.mpd) downloads as a whole stream (no quality choice).
- Per profile: folder `downloads` for profile 0, `downloads_p<ID>` otherwise (same rule as the other profile files).
- Automatic deletion: off by default; 14, 30 or 90 days. "Used" = the day of the last playback (written at most once per hour); a download that was never played counts from its first check.
- Still open: size estimate per quality (needs the playlist duration), HLS offline playback is untested and the riskiest part.

## Queue (based on the review of another open-source app, ideas only, no code copied)
- Pause, resume, retry and delete per download; "Download season" on the series page.
- Open: a Downloads entry in the main menu (navigation, needs approval), the retry after removal relies on a 500 ms delay (unverified), season downloads start one toast per refused episode.

## Downloads tab in the sidebar
- Menu id 11 (`MenuLogic.DOWNLOADS`), label "Downloads", icon drawn in `Sidebar.kt`, page `DownloadsTab` in `DownloadUi.kt`, routing in `MainActivity.kt`.
- The list is shared by the settings page and the tab (`DownloadList`).
- Open: playing a download from the tab (the tab only manages), the sidebar order for users who already changed it.

## Sidebar tab: visibility and playback
- The Downloads entry is shown only while `DownloadStore.hasAny` is true (at least one download in the manager).
- Finished downloads have a "Play" button in the tab; it starts the playback with the download address, so the cache serves it.
- Open: the sidebar hides the entry while the tab is open and the last download is removed (the page stays until the next visit).
