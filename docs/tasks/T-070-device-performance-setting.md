# T-070 – Setting "Device performance" (Automatic, Weak device, Strong device)

## Goal

Maintainer request (26 Nov 2026): a setting for weak and strong devices, so that people with weak devices can use the app
more smoothly.

Maintainer decisions (26 Nov 2026, in the chat):
- "Automatic" (default): on devices with little memory only the parts nobody sees are reduced; the look stays.
- "Weak device": animations, background picture and slideshow, smaller images, less background work (all four groups).
- "Strong device": everything on.

## Analysis (read-through, nothing measured)

- Before this task the app only adapted memory to the RAM size (`Mem.tier`: image cache share, images decoded at once, player
  buffer). Fire TV sticks with 1 to 1.5 GB land in tier 0. Every visual effect ran the same on every device.
- Costly effects found: the full-screen background picture behind the home rows (`BillboardLayer`), the slideshow progress bar
  of the big picture (redrawn on every frame while the home screen is open), the poster preview that expands after a rest, the
  shadow of the focused card (drawn on every frame of the focus animation), the screen fade-in and the scrolling animations.
- Background work at start: search index (T-069: 3 s after the list is shown), genre detection (8 s).
- Card images: posters `w342`, landscape cards `w780`. Large pictures (background, big picture at the top) use `w780` already.

## Changes

- `DevicePerfLogic.kt` (new, pure, JVM tests): what each choice switches (`Profile`), image sizes for cards.
  - Automatic: as before; with tier 0 the search index is prepared after 8 s (before 3 s) and genre detection starts after
    30 s (before 8 s).
  - Weak device: `reduceVisuals`, card posters `w185`, landscape cards and tiles `w300`, image cache of tier 0 with 2 decoders,
    search index only when the search opens, home screen not kept under a detail page (T-069b), genre detection after 30 s.
  - Strong device: no reductions, image cache at least tier 1, 4 decoders, search index after 3 s, genre detection after 8 s.
  - The player buffer always follows the detected memory (`Mem.bufferPlan`, unchanged).
- `DevicePerf.kt` (new): mode as Compose state, stored in `ipvio_general` under the new key `device_perf` (a device setting,
  the same for every profile; no key renamed or removed). A change rebuilds Coil's image loader with the new cache size.
- `Look.kt`, `Store.kt`, `SettingsHub.kt`: the stored animation choice is now `Look.motionChoice` (same key `look_motion`);
  `Look.motion` is the level in use: "Off" with "Weak device", otherwise the choice. The animation chips show the choice; a
  hint says that "Weak device" switches them off.
- `MainActivity.kt`: image loader from `DevicePerf`, search index and genre detection timing from the profile, T-069b only
  when the profile allows it, soft scrolling 120 ms with "Weak device" (`ThemeMath.scrollMs`).
- `HomeScreen.kt`: no background picture with "Weak device" (Home, Movies and Series then scroll like Favorites and My list,
  an existing code path); the slideshow bar does not run.
- `HomeParts.kt`: no poster preview with "Weak device"; landscape cards and tiles use `smallBackdrop`.
- `Components.kt`, `Billboard.kt`: `cardPoster` follows the profile; the full-screen poster fallback keeps `w342`.
- `Ui.kt`: no focus shadow with "Weak device".
- `SettingsHub.kt`: row "Device performance" with a hint in Settings > General, section of the remote-control settings.
- `DictT070.kt` (new): 5 new texts in all 13 languages (drafted, not reviewed by a native speaker), registered in `Lang.kt`.

## Not changed

Player buffer and player core, storage format of the playlist, backup list, the screens' layout in "Automatic" and "Strong
device".

## Acceptance criteria

1. "Automatic" looks and behaves as before on every device.
2. "Weak device" visibly reduces the effects listed above; the app stays fully usable; going back to "Automatic" restores the
   animations the user had chosen.
3. "Strong device" shows everything also on a device with little memory and does not crash.

## Risks

- "Strong device" on a device with very little memory uses a larger image cache and 4 decoders: more memory; the app still
  frees the cache when Android reports low memory (`Mem.trim`).
- "Weak device": the cards look a little softer (smaller images); the search opens with a short "Searching …" the first time.
- Switching the mode rebuilds the image loader: images reload once from the disk cache.
- Translations are drafts.

## Documentation

`docs/SETTINGS.md`, `CHANGELOG.md` (Unreleased), `docs/TESTING.md` (T16), `docs/tasks/WORK_STATE.md`.

## Test results

- Ran here (JDK 21, Kotlin 2.0.21 compiler bundled with Gradle 8.14.3, JUnit 4.13.2): `DevicePerfLogicTest` (5),
  `DictT070Test` (2), together with `MotionTimingTest` (3) and `BufferRelaxTest` (5): 15/15 passed.
- Syntax check of the 14 changed Kotlin files: no syntax errors. No type check (Android and Compose classes not available).
- **Not run:** Gradle build, `DictConsistencyTest` and other Android-dependent tests, device test, measurements.

## Device test steps (maintainer)

1. Install over the current version. Settings > General: "Device performance" shows "Automatic". Home, Movies, Series look
   as before (background picture, slideshow bar, preview, animations).
2. Choose "Weak device": at once no fade-in, no zoom or shadow on the focused card, no background picture behind the rows,
   the slideshow bar stands still, no poster preview after resting on a poster. Scroll Home, Movies, Series, Live TV: smooth,
   nothing missing. Settings > "Design and color" > "Animations": the choice is still marked, the hint below says they are off.
3. "Weak device": open a detail page and go back (the home screen is built again, as before T-069b); open the search after a
   start (short "Searching …", then results).
4. Choose "Automatic" again: the animations and the background picture come back as chosen before.
5. Choose "Strong device" on a Fire TV stick: everything on, scroll through a large list for a few minutes: no crash.
6. Switch the language to English, Polish, Arabic: the row, the choices and the hints are translated.
7. Restart the app: the choice is kept.

## Status

Implemented on branch `claude/nifty-pasteur-ubd26z`, open for build and device test.
