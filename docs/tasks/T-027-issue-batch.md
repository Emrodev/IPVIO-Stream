# T-027: GitHub issue batch (19 Nov 2026)

**Status:** all items below are implemented or analysed. **Nothing was built with Gradle and nothing was tested on a device.** Pure logic was run on the JVM (Kotlin 2.0.21 compiler, JUnit 4.13.2). Device tests are pending for the maintainer.

Branch: `claude/jolly-ride-mr9tgm`.

## Status per issue

| Issue | Topic | Status | Commit | JVM tests |
|---|---|---|---|---|
| #69 | No silent http fallback on certificate mismatch | Implemented: consent dialog, stored agreement | `0bad579`, `7fdda3c` | `TlsFallbackLogicTest` 8/8 |
| #90 | Per-playlist user-agent | Implemented: "Kennung: VLC / IPVIO" in the add screen | `40faa51` | `PlaylistUaLogicTest` 4/4 |
| #111 | Counts per playlist | Implemented: stored at load, shown in settings | `ff33a00` | `PlaylistCountLogicTest` 4/4 |
| #15 | Trailer inside the app | Implemented: WebView embed of the TMDB trailer | `06030c9` | `TrailerLogicTest` 3/3 |
| #95 | Keep the screen awake while browsing | Implemented as an option (default off) | `a8c752b` | none (flag logic reused) |
| #102 | Live channels in a movie group | Implemented: live address stays live | `9086e1b` | `LiveUrlLogicTest` 4/4 |
| #112 | Playlist vanishes | Partly: unreadable list is no longer overwritten | `bc9e0a9` | none |
| #28 | Resume after leaving a film | No code change: progress is saved and the detail page offers resume | — | — |
| #73 | "Content too large" with one language | Analysed, no change (see below) | — | — |
| #108, #104 | "Connection lost" during playback | Analysed, no change (see below) | — | — |

## Open points and risks

- **#112:** the cause is a hypothesis from the code (key lost, the encrypted list cannot be read). A device log would confirm it. Recovery is not automatic: the kept copy (`lists_enc_unreadable`) is only saved, not restored.
- **#90:** series episodes (`LazySeries` → `PlaylistStore.xtreamApi()`) still send the VLC user-agent. A playlist that needs the IPVIO identifier may still fail when opening a series.
- **#102:** this changes the IPTV classifier (protected area, CLAUDE.md section 5). A live channel with an episode pattern in its name (S01E01) is now a live channel, not a series.
- **#111:** counts show only after the next load of a playlist with this version. They reflect what was loaded (language and mode filters).
- **#15:** D-pad control of the embedded YouTube player on Fire TV is unverified. YouTube may refuse embedding for some videos (error page).
- **#95:** unverified that `FLAG_KEEP_SCREEN_ON` also blocks the Fire TV screensaver (not only the screen timeout).
- **#69 and #108/#104:** the consent dialog appears after a certificate error. For a movie or episode the player retries a few times; if the user agrees during those retries, playback continues. Otherwise the error remains. Check whether a provider's episode host has a certificate mismatch (see logs).
- **#73:** the message "Die gewählten Inhalte sind zu groß für den Speicher dieses Geräts" comes from the memory budget of the app (`LoadLogic.budget`, based on the maximum heap), not from free storage. The check cannot be judged without the device's memory limit and the playlist type. Needed: the device model, the playlist type (Xtream, Stalker, M3U) and the number of languages selected.
- **#108 and #104:** no logs yet. Possible causes to check: the change in `TlsFallback` (#69, a certificate mismatch on a stream host), the R8 build of 1.4.3 (T-024, not confirmed), the retry rules (`IptvLoadErrorPolicy`, `PlayerScreen.onPlayerError`). Logcat from the moment of the error is needed.

## Test steps for the maintainer

1. **#69:** see `docs/tasks/T-026-tls-consent.md`.
2. **#90:** add an Xtream or M3U playlist with "Kennung: VLC" (default) and check it loads as before. Then add a second one with "Kennung: IPVIO" and check it loads (the provider that gave 403 is the test case).
3. **#111:** open Settings > Playlists after a load: each playlist shows "Live TV: n · Filme: n · Serien: n".
4. **#15:** open a movie with a trailer (TMDB key set): Trailer plays in the app; Back closes it; no sound after closing. A movie without a trailer opens the YouTube search as before.
5. **#95:** Settings > "Bildschirm beim Stöbern wach halten" on, leave the app idle on a screen for longer than the screensaver delay: the screen stays on. Off: as before.
6. **#102:** in a playlist with a "Movies" group that contains live channels (e.g. HBO Comedy): the channel is in Live TV, not under Movies.
7. **#112:** no test possible without the broken state. Check that a normal load, saving and restarting still keeps the playlists.

## Not done

- Automatic restore of an unreadable playlist list.
- Series episodes with the IPVIO user-agent (#90, see above).
- Logs for #108, #104, #112 and the device memory data for #73.
