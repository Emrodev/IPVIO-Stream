# T-036 Start-up speed (handoff)

Status: **analysis only, no startup code changed.** Target from the maintainer: the app opens at once, about 0.1 s to the first usable screen, no visible lag and no long loading at start. The target is a goal, not a measured value. Nothing here is built or measured (no Gradle, no device in the assistant's environment).

Read first: `CLAUDE.md`, `docs/tasks/WORK_STATE.md`, this file, `docs/tasks/T-034-perf-1.4.4.md` (not on this branch; it lives on `claude/confident-heisenberg-ulq8q2`).

## Goal and acceptance
- Cold start to the first screen: measured on a Fire TV Stick and one phone, with `adb logcat`, not guessed.
- Settings and colours still correct in the first frame (existing comment in `MainActivity.onCreate`).
- No change to stored data, keys or file names (CLAUDE.md section 5).
- A large playlist: the start screen appears before the list is parsed; the list fills in afterwards.

## What the code does at start (read, not measured)
`app/src/main/java/com/ipvio/stream/MainActivity.kt`, `onCreate` runs synchronously on the main thread before `setContent`:
- `Profiles.init`, `SublistStore.clearActiveOnce`, `Profiles.prewarm`, `TlsFallback.init`, `Profiles.needsPicker`, `AppSettings.init`, `General.init`, `Boomer.init`, `HomeLayout.init`, `BoomerLayout.init`, `SidebarPrefs.init`, `Config.init`, `Config.initSubs`, `LiveSettings.init`, `Recorder.attach`, `MobilePrefs.apply`, `ScreenAwake.attach`, `CrashLog.install`.
- Coil `ImageLoader` built here.
- `Updater.apply` already runs on its own thread (good).
- `Lang`/`I18nCore`: the dictionaries are parsed on first `translate` (T-034 item 4 changes this; not on this branch).
- `PlaylistStore`: the stored list is read in `fromLocal` (`PlaylistStore.kt`, around line 1410) and parsed/shown during `App` composition. Its duration is written to Settings > Diagnostics as "Start: stored list N items read in X ms, shown in Y ms, translations Z ms" (`Diag.note`). **Use these numbers first.**

Suspected costs (all **unverified**):
1. Synchronous init calls above, each may read SharedPreferences or files.
2. Stored playlist read and built on the main thread when the first screen is composed.
3. Home screen composition: many card rows, per-card effects (T-019/T-020 notes in `docs/tasks/`).
4. T-034 items: age-rating requests per focused title, Xtream lists loaded one after another, all dictionary columns parsed at start.

## Plan (suggested order, smallest first)
1. **Measure.** Add nothing new first: read `Diag` start note and `adb logcat -d -s IPVIO-PL`; add a `Choreographer`/`onCreate`-to-first-frame timing only if the existing notes are not enough. Record numbers in this file.
2. **Show the shell before data.** Keep `onCreate` to what the first frame needs (colours, size, language selection). Move the rest to a background coroutine or run it after the first frame; the first screen shows a skeleton while the playlist loads. Must keep the existing order rules (`Profiles` before `AppSettings`).
3. **Playlist load off the first frame.** Read and build the stored list on `Dispatchers.IO`; the UI reads it when ready (the skeleton already exists for loading states).
4. **Take T-034 in** (separate branch, its own device test) if the measurements point at the Xtream/age-rating/dictionary items.
5. Re-measure with the same steps; keep the change only if the number drops.

## Constraints for the implementer
- Refactor, do not rewrite (CLAUDE.md principle 1). One change per commit, each with its measured number in the message body (English).
- Do not change SharedPreferences names or keys, file names or backup keys.
- Player and IPTV core (`PlayerScreen.kt`, playlist sources, EPG) are protected: a larger change there needs the maintainer's approval first (section 6).
- Any change of start behaviour is user-visible: ask the maintainer before merging step 2 or 3.
- Tests: move pure decision logic into `*Logic.kt` and cover it in `app/src/test/java/com/ipvio/stream/`. Timing itself is device-only: say so in the PR and list the device steps.
- Write the CHANGELOG entry in English, under `## Unreleased`.

## Device test steps (maintainer)
1. Force-stop the app, start it from the launcher, and note the time until the first screen is usable (stopwatch is fine, then `adb logcat -d -s IPVIO-PL`).
2. Repeat 5 times; also with a large playlist (more than 100,000 titles if available).
3. Check the Diagnostics start note before and after.
4. Check that colours, language and text size are right in the first frame (no flash of default colours).
5. Check that "Resume last watched at start" still works.

## Open questions for the maintainer
- Which device is the reference for the 0.1 s target (Fire TV Stick or phone)? A 0.1 s cold start may not be reachable on a Fire TV Stick with a large list; the realistic target should be fixed after the first measurement.
- Is a skeleton at start acceptable (visible change), or must the last list appear at once?

## Test results
- None. Nothing built, nothing measured, no device test.

## Status
Open. Next step: measurement (step 1), then step 2 after the maintainer's approval.
