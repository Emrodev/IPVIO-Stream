Last update: 2026-11-26 (T-069 screen opening, start and search typing speed). Read `CLAUDE.md` first, then this file.
Last update: 2026-11-25 (T-067 missing content and EPG matching, T-068 playlist switch and parser speed). Read `CLAUDE.md` first, then this file.
Last update: 2026-11-23 (T-051 to T-054, analysis and plans for four user issues). Read `CLAUDE.md` first, then this file.
Last update: 2026-11-23 (T-051 stage 3, fixes after the test build). Read `CLAUDE.md` first, then this file.

The former log is kept unchanged in `docs/tasks/WORK_STATE-history.md`. Do not add new entries there.

## Current task

Last update: 2026-11-26, fourth round. T-070 (`docs/tasks/T-070-device-performance-setting.md`): setting "Device performance" (Automatic / Weak device / Strong device) in Settings > General, maintainer's choices recorded in the task file. New files `DevicePerfLogic.kt`, `DevicePerf.kt`, `DictT070.kt`; `Look.motion` is now the level in use (stored choice `Look.motionChoice`, same key). New key `device_perf` in `ipvio_general`. JVM tests 15/15 (with T-069d/e). Not built, not device tested. Next: maintainer runs the 7 steps in the task file.

Last update: 2026-11-26. Branch `claude/nifty-pasteur-ubd26z` (from `dev` bc952a1). Maintainer's request: start, screen changes and scrolling as fast and smooth as possible without breaking anything.

- T-069 (`docs/tasks/T-069-screen-open-and-start-speed.md`): programme search off the main thread and without a map of the whole playlist; Live TV, its start view, the player's channel list, title lists (language choices) and category pages reuse the work of the last visit (`ScreenCacheLogic.kt`, `LastResults`); search index 3 s after the list is shown; 18 more settings files opened in parallel at start. New caches are emptied by `ScreenCaches.clear()` on low memory and on every playlist change.
- Changed files: `ScreenCacheLogic.kt` (new), `SearchScreen.kt`, `LiveTab.kt`, `PlayerLive.kt`, `BrowseScreen.kt`, `CategoriesScreen.kt`, `MainActivity.kt`, `StartupLogic.kt`, `Profiles.kt`, `Mem.kt`; test `ScreenCacheLogicTest.kt` (new); docs `CHANGELOG.md`, `docs/TESTING.md`, this file.
- Checks: `ScreenCacheLogicTest` + `StartupLogicTest` 10/10 (Kotlin 2.0.21 compiler bundled with Gradle 8.14.3, JUnit 4.13.2); syntax check of the 10 changed Kotlin files: no syntax errors. Not run: Gradle, type check of Android files, device test, measurements.
- 26 Nov, second round (maintainer asked to work on points 2 and 3 of the open decisions): T-069b home screen kept composed, hidden and not laid out, under a detail or person page (`HomeKeepLogic`, `KeepAliveBox` in `MainActivity`, `hidden` in `HomeScreen`, `FocusReturn` in `Nav.kt`; rollback `HomeKeepLogic.ENABLED = false`); tile images and "Because you watched" kept for the next build (`HomeLookCache`). T-069c movie source index for Play (`MovieSourcesLogic.kt`, `Sources` in `Playback.kt`). JVM tests 17/17 (`HomeKeepLogicTest`, `MovieSourcesLogicTest`, `ScreenCacheLogicTest`, `StartupLogicTest`); syntax check without errors.
- Highest risk: D-pad focus with the hidden home screen (test steps 11 to 16 in the task file). Not built, not device tested.
- 26 Nov, third round (maintainer: start with points 3 and 4 of the second idea list): T-069d fade-in 160/110 ms (before 380/220), scrolling 200/260 ms (before 280/380), `focusSoon` instead of fixed focus waits on the navigation path, Live TV panels follow sooner; T-069e start buffer 1.0/1.25/1.5 s (before 1.5/2/2.5 s), automatic buffer steps down after 5 clean playbacks (`BufferLogic.afterPlayback`, new key `buf_clean` in `ipvio_settings`, `PlayerScreen` reports each playback). JVM tests 7/7 (`MotionTimingTest`, `BufferRelaxTest`); syntax check without errors.
- Still open: Drive token decryption at start; idea list items 1 (prefetch series episodes), 5 (start snapshot), 6 (parallel home data), 7 (image prefetch), 8 (recorded baseline profile), 9 (jank counter); hint text of the buffer setting (new dictionary rows).
- Next: maintainer builds, runs the 26 device test steps in the task file, sends the "Start:" and "Opened …: N ms" lines from Settings > Diagnostics > Log.

Earlier entry, 2026-11-25:

Last update: 2026-11-25. Branch `claude/modest-johnson-415uuv` (from `main` f6270c3). Maintainer's priorities: 1. users see fewer channels, movies and series than the provider sends, and EPG matching; 2. speed (start, screen changes, playlist switch without loading screen).

- T-067 (`docs/tasks/T-067-missing-content-and-epg-matching.md`): same-name series no longer merged (first keeps its ID), partial Xtream `get.php` lists loaded through the API, load report in Settings > Diagnostics > Log, XMLTV matching by normalized ID and channel name.
- T-068 (`docs/tasks/T-068-instant-playlist-switch-and-parser-speed.md`): playlist switch keeps the screen when memory allows; parser about 30 % faster on the JVM.
- Checks: 491/491 tests of the 75 JVM-runnable test classes (Kotlin 2.0.21 compiler downloaded into the scratchpad, JUnit 4.13.2). Not run: Gradle, device. Android files not compiled here.
- Next: maintainer builds and runs the test steps in both task files; sends the "list has N entries" and "Playlist switch … ms" lines from the diagnostics log. Speed work after that only from device measurements ("Opened …: N ms" lines, T-056).
- ID note: T-055 to T-066 were taken by branch `claude/exciting-johnson-9gi5zn`; this work was renumbered from T-058/T-059 to T-067/T-068 on 25 Nov 2026.

Last update: 2026-11-23. Four user issues (Fire TV) analysed, no app code changed, plans only:

Last update: 2026-11-24 (issue review, branch `claude/exciting-johnson-9gi5zn`). Done: T-055 (#152, same cause as #67): a tap on a Live TV category or on "Search channels" now sets the category (`LiveTab.kt`, 2 lines). Not built, not device tested. Findings, no code change:
- **PR #40 and #70 (fork `browanoncoder`) were closed on 16 Nov without a merge.** Their issue comments say "fixed" for #67, #10, #25, #28, but `CuelessMkvExtractor`, `TapSeekLogic` and `MkvSeekLogic` do not exist in the code. #10 (MKV without Cues) and #25 (tap-to-seek) are therefore still open in the code. Porting touches the player core: needs approval (CLAUDE.md section 6).
- #109 is already fixed in the code (`HomeScreen.kt`, commits `fc824dc`, `494f00d`); close after a device check.
- #112 (playlist vanishes): `Secrets.key()` swallows every exception, so a short keystore failure at start looks like an empty list. T-027 only stops overwriting the unreadable list. Needs logcat from the Fire TV and approval for a storage change.
- No code yet for #125, #27, #23, #145, #131, #130, #151, #84, #146, #118 (a reply is enough).
- Done after that (branch `claude/exciting-johnson-9gi5zn`, all not built, not device tested):
  - T-056 (#112): `Secrets.key()` never replaces an existing key alias (`SecretsLogic`, 3/3 JVM tests); the empty home screen says when the saved playlist cannot be read (`PlaylistStore.unreadable`, row in `DictEn1`, 5/5).
  - T-057 (#130): alert delay options up to 120 s. The switch and delay already existed (Settings > Sports Hub). Per-team switch not done.
  - T-058 (#125): the home row "Recently added" uses the added date like the page (`RecentSortLogic.newestN`, 6/6).
  - T-059 (#27): the Genre pill shows the opened genre category. Switching category through the pill is not done.
  - Syntax check of the changed Kotlin files with the compiler bundled in Gradle: no syntax errors (no Android classes here, so no type check).
  - T-060 (#10, #25): ported from the closed PR #40: MKV seeking without Cues (`CuelessMkvExtractor`, `MkvSeekLogic`) and tap-to-seek on the timeline (`TapSeekLogic`); 10/10 JVM tests. Touches `PlayerScreen.kt`: needs a review. Only these two parts of #40 were taken over.
  - #131: no code change. On Live TV Left opens the channel list by design; Left/Right seeking is for movies and episodes. Decision for the maintainer.
  - T-061: search with the filter "Persons": Down reaches the people results (`firstKey` fallback "pe"). Not device tested.
  - T-062: implemented after the maintainer's decisions (24 Nov): fewer Live TV player buttons, rewind/forward buttons (seek step; archive for catch-up channels, buffer otherwise), own button list in Settings > Live TV (`ipvio_playerbtns`, in the backup). 11/11 JVM tests. Touches `PlayerScreen.kt`: needs a review. Several points are unverified on a device (see the file).
  - T-063 (#96): switch "Genre categories in Live TV" (Settings > Live TV, key `genres` in `ipvio_livetv`, default on). 3/3 JVM tests. Movies and Series already have "Automatic genres" (off by default).
  - T-064 (#145): M3U categories from `tvg-group`, other letter case, single quotes and `#EXTGRP:` lines (`M3uGroupLogic`, 6/6; parser tests 8/8). Cause not confirmed; Xtream/Stalker unchanged. Question for the reporter drafted in the file, not posted.
  - T-065 (#151): option "Combine seasons" (Settings > Playlists > Series, default off): Xtream series that a provider lists season by season become one series (`SeasonMergeLogic`, pseudo code `~merge` in the hidden set, `lazyParts`, `LazySeries`). Changes the series id while on. 26 JVM tests. Touches the IPTV core: needs a review. The reporter's provider type is unknown (hypothesis).
  - T-066 (#146): option "Tunneled playback (Live TV)" (Settings > Player > Source & quality, key `tunneling` in `ipvio_playback`, default off; `TunnelingLogic`; `applyTunneling()` in `PlayerScreen.kt`). Media3 API not compiled here. Needs a review and a device test.
  - #84: no code change. One sentence and a screenshot ("can't play series"); the maintainer asked on 22 Nov whether it works now, no answer. Needs the provider type and what is shown.
  - 25 Nov: replies posted on every open issue except #110 (28 comments, "update coming soon" where a fix is in the branch). Still no fix and no cause: #73 (memory estimate for a large Xtream list, persists on 1.4.9), #104/#84 (Strong8K: episodes of Xtream series do not show; look at `XtreamApi.walkEpisodes` / `episodeList` next), #108 (Connection lost). Open statements to check: #129 says the Play Protect review will be looked at.
  - 25 Nov (earlier): comment drafts for 15 issues in `docs/tasks/ISSUE-COMMENTS-2026-11-25.md` (not posted); device test plan in `docs/tasks/DEVICE-TEST-PLAN-2026-11-25.md`. 518 JVM tests ran green (15 test files need Android and were not run). English check: no German text in docs.
  - Comments posted on #131 and #118 (24 Nov 2026) as dictated by the maintainer.
- Open: #118 can be answered (the per-playlist reload button exists since 1.4.5); #131 not analysed; #130, #67, #109 can be closed after a device check.
- Next: maintainer decides on the PR #40 port (#10, #25) and tests T-055 to T-059 on the device.

Earlier entry, 2026-11-23. Four user issues (Fire TV) analysed, no app code changed, plans only:

- T-051 sound of the previous channel keeps playing: not reproduced, candidates listed (`docs/tasks/T-051-audio-after-channel-change.md`).
- T-052 setting for Up/Down in Live TV: plan, default proposed = current behaviour, maintainer decides (`docs/tasks/T-052-live-up-down-setting.md`).
- T-053 "Because you watched" row empty: needs playlist size and TMDB check from the user (`docs/tasks/T-053-because-you-watched.md`).
- T-054 Fire TV freeze while zapping: highest priority. Suspect is the per-channel 512 MB live buffer when zapping fast (unverified). Test steps for the maintainer in the file (`docs/tasks/T-054-freeze-when-zapping-history.md`).

Next: maintainer runs the T-054 test steps and answers the questions in T-051 and T-053. Then approve plans for the code changes (protected player core, CLAUDE.md section 5). Not built, not device tested.
T-051 stage 3 (branch `claude/adoring-noether-pf4asf`, based on `test/combined-2026-11-23`): fixes for the maintainer's test build of 23 Nov 2026 (missing movies on Xtream, playlist menu, edit window focus and scrolling, search layout). Details and test steps in `docs/tasks/T-051-section-playlists.md`, section "Stage 3". JVM tests 34/34 (run here with the compiler bundled in Gradle). Not built with Gradle, not device tested. Search screen rebuilt from the sketch uploaded in the chat (see `docs/tasks/T-051-search-handoff.md`, third round). **Open:** compare the search screen with the sketch on the device; confirm on the device whether the missing movies were the Xtream `get.php` case.


T-036 (done, committed `dd4c17a`): this file, the archive, `/start`, `/new-task`. Task file `docs/tasks/T-036-workflow-state-and-commands.md`.

T-037 (done, commit `5cdea91`): `/protected-check` and the release-notes skill, each with a check script in `.claude/scripts/`. Task file `docs/tasks/T-037-protected-check-release-notes.md`.

T-038 (done, in the branch commit after `5cdea91`): `/english-check`, report only, with `.claude/scripts/english-check.sh`. Task file `docs/tasks/T-038-english-check.md`. T-039 (done, in the branch commit after `7778f0f`): inventory of German text outside `Dict*.kt`, report `docs/quality/german-strings-2026-11-22.md`. About 700 lines outside the translation keys, by word-list estimate. T-040 (done, in the branch commit after `fff53a8`): option C of T-039 — 21 docs and the archive translated to the English dictionary labels. T-041 (stage 1 of option A, in the branch commit after `9638dba`): 16 English rows in `DictEn1.kt`, registered in `Lang.kt`, JVM test `DictEn1Test`, CHANGELOG entry. Not built, not device tested. T-042 (stage 2a, same branch, after the T-041 commit): 9 English patterns in `DictEn2.kt`, test `DictEn2Test`. T-043 (measurement, no code change): the lookup already covers nouns in templates and name-value fragments. Lookup port in `.claude/scripts/i18n-lookup.py`. T-044 (same branch): 2 rows for Stalker portal messages (`DictEn3.kt`), test `DictEn3Test`. T-045 (decided 22 Nov 2026, in the branch): `DictW` and `DictPlW` registered in `Lang.kt`, existing rows keep priority; no existing English value changes. Not built, not device tested. Decisions (22 Nov 2026): fallback for other languages stays English; the 7 CHANGELOG lines in released sections stay unchanged (section 18). T-046 (stage 3, in the branch): the 27 English-only rows are translated into 10 languages (drafted, not reviewed by a native speaker). Open: native review, build and device test of T-041 to T-046., and whether other languages should keep German as the fallback for new rows (currently English). And 7 CHANGELOG lines in released sections (section 18). No app code changed.

T-047 (faster start, branch `claude/pensive-galileo-i3tza8`): language dictionary parsed in a background thread, thread-safe `Lang` load, silent refresh 4 s and catch-up 5 s after the stored list is shown. Task file `docs/tasks/T-047-first-start-speed.md`. JVM tests ran (18/18). Not built with Gradle, not device tested, nothing measured. Next: maintainer builds, runs the steps in the task file, compares start time.

T-048a (playlist edit window, same branch): row with Reload + Edit, window with Apply/Cancel, Reload of other lists in the background. Task file `docs/tasks/T-048-playlist-manage-page.md`. T-048b (refresh plan per playlist) is in the same branch: new file `ipvio_plrefresh` (not in the backup, open decision), worker and start use the plan of each list. JVM tests 37/37. Not built, not device tested. Next: maintainer builds and runs the steps in the task file.

T-049 (sports alert jump, Sports Hub settings page, 26 more competitions off by default, own DNS) is in the same branch, see `docs/tasks/T-049-sports-vpn-dns-trakt-batch.md`. VPN and Trakt are not started. JVM tests 67/67, not built, not device tested.

Release 1.4.8 (versionCode 14) prepared on 22 Nov 2026 at the maintainer's request ("GO"): version bump, CHANGELOG section, `docs/release-notes/v1.4.8.md`, in-app What's new. **Open: Trakt** (T-049g, needs the maintainer's Trakt API app: client ID and secret; plan T-031). Not device tested.

## Release state (from git log, 2026-11-22)

- Latest release: **1.4.7** (`versionCode` 13), merged into `main` (PR #144, merge `ef3d17e`) and `dev` (PR #143, merge `09fcc2a`). Release notes: `docs/release-notes/v1.4.7.md`.
- Releases since the old log: 1.4.5 (category scrolling, Sublists page removed, T-034 performance), 1.4.6 (settings tabs, Reload button per playlist, refresh translation, Live TV addresses without `/live/`), 1.4.7 (Live TV playback over agreed http, instant playlist switch from memory, English diagnostics).
- The CHANGELOG marks these changes as "Not device tested yet". Device results are not recorded in the repository.

## Open device tests and decisions

The full list with test steps is in `docs/tasks/WORK_STATE-history.md`, section "Open problems". Its status is from 21 Nov 2026 and is not re-verified. Main items:

- Playlist mode switch, demo mode, favorites reorder (T-014/T-018), settings reset: device tests were pending in the old log.
- T-024 (R8 over the whole app): not built, not device tested when last recorded. Rollback is `enable = false`.
- T-015 (update check), T-014 (first-start update card), T-026 (playlist cache), T-028 (private categories), T-033 (stream info): device tests pending.
- T-006 and T-009 (backup): device checks pending.
- Maintainer decisions open: PR #40 (conflicts with `main`), T-010 and T-012 (Stalker, against on-demand loading), `ipvio_plmode` in the backup, T-004 (quality baseline: ktlint, detekt, Lint on the maintainer's machine), removal of the sublist load filter in the IPTV core (needs its own approval).

## Task IDs

- Highest ID in `docs/tasks/`: T-070. **Next free ID: T-071** (use `/new-task`, which checks this).
- IDs used by more than one file: T-009, T-014, T-026, T-027, T-032. The files are not renamed here, because the old log and other task files refer to them by name. Renaming needs a decision.

## Conventions

- One branch and one PR per change, into `dev`. Merge into `main` only through release PRs (`CLAUDE.md` section 16).
- Protected areas (`CLAUDE.md` section 5) and approval list (section 6) apply before any change.
- The assistant cannot run Gradle or test on a device. Every change ends with test steps for the maintainer.
