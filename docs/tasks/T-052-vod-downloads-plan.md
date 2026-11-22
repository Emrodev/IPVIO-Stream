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
