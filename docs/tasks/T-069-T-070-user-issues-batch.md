# T-069 and T-070: user issues batch (25 Nov 2026)

Status: done in code, not built with Gradle, not device tested. Branch `claude/confident-ritchie-byuqg1`.

## Changes

| Item | Files | What |
|---|---|---|
| Sports Hub, duplicate toggle | `SportSettings.kt` | Hint and "Live results" toggle were listed twice; one copy left |
| Trakt sign-in on the phone | `PhoneImport.kt` | Trakt login link opens in its own tab; the code page stays open |
| Search placeholder | `DictG.kt`, `DictAr3.kt`, `DictNl4.kt`, `DictPl3.kt`, `DictPt4.kt`, `DictZh.kt` | English "Search..." and short translations; German key unchanged |
| Search, recent searches | `SearchScreen.kt` | Picking a recent search keeps focus in the search screen (same fix as #101) |
| Search, "Show all hits" | `SearchScreen.kt` | Down from the category tiles goes to the button when the category has no hits (`noHitsBtn`) |
| Cover kicker | `HomeScreen.kt`, `Billboard.kt` | Only the quality (FHD, 2K, SD) above the title; no kind word; nothing when quality is unknown |
| Top rated duplicates (T-069) | `TopRatedLogic.kt`, `HomeScreen.kt` | One entry per TMDB id (movie and series kept apart), best-rated copy first |
| Focus stays centred (T-070) | `FocusCenterLogic.kt`, `MainActivity.kt` | Default `BringIntoViewSpec` centres the focused item; list ends stop the scroll |

## Checks run

- JVM tests, run here with the Kotlin 2.0.21 compiler and JUnit 4.13.2: `TopRatedLogicTest` (3), `FocusCenterLogicTest` (4). 7/7 OK.
- Not run: Gradle, the full test suite, device. The Android files (`SearchScreen.kt`, `HomeScreen.kt`, `Billboard.kt`, `MainActivity.kt`, `PhoneImport.kt`, `SportSettings.kt`) were not compiled.

## Open

- **Live TV: Up from the first category** should wrap to the last one. The wrap already exists in `LiveTab.kt` (`wrapCat`). The cause is not found by reading the code; needs a device check and the exact steps.
- **Top rated duplicates:** the merge works on titles that have a TMDB match in the cache. Titles without a match stay separate.
- **Focus centring (T-070)** changes every list that uses the default spec, horizontal rows too. Pivot and smooth specs in `HomeScreen.kt`, `HomeParts.kt` and `Billboard.kt` are not changed.
- **Translations** of the placeholder are drafts, not reviewed by native speakers.

## Test steps for the maintainer

1. Settings > Sports Hub: "Live results" appears once.
2. Phone: scan the Trakt QR code, tap "Sign in to Trakt". It opens in a new tab; after login paste the code in the first tab. Expect "Signed in to Trakt".
3. Search: the placeholder is one line, "Search...".
4. Search: type a word, pick a recent search. Focus stays in the search screen, not the sidebar.
5. Search: choose a category with no hits (e.g. Series for a movie word). Press Down from the category tiles: focus lands on "Show all hits" and OK opens all hits.
6. Home hero and Billboard: the kicker above the title shows only FHD/2K/SD (or nothing).
7. Home "Top rated": the same film appears once (e.g. The Godfather from several provider entries).
8. Any vertical list (e.g. Live TV channels, Movies grid, Settings): move Down; the focused row stays in the middle until the list ends, then the focus moves towards the bottom edge. Also move Up the same way. Check that no row jumps or is cut off; report any screen where it breaks.

## T-071: automatic Trakt sync interval (follow-up, 25 Nov 2026)

- Setting in the Trakt panel (`ListImportUi.kt`): "Sync automatically" with six choices: on every app start, every 6 hours (default, as before), 12 hours, daily, weekly, only manually.
- Stored as a number in the Trakt preferences (`ipvio_trakt`, key `autoInterval`). It is not in the backup, like the other Trakt state.
- Logic in `TraktSyncLogic.autoDue` (5 new tests in `TraktSyncLogicTest.kt`, run here on an extracted copy of the function; the full test file needs Android classes). Translations in `DictT071.kt` (`DictT071Test`, 2 tests, run here).
- The check happens at app start only. A sync while the app stays open for days is not done; a background job (WorkManager) would be a new decision.
- Test steps: connect Trakt; pick "Only manually", restart the app, check that no sync starts; pick "Every 12 hours", sync once, restart within 12 hours (no sync), after 12 hours (sync).
