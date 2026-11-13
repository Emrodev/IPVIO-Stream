# TV test plan

A repeatable end-to-end check of IPVIO Stream on an Android TV device or the Android TV emulator. Run it before every release and after any change to Live TV, Movies/Series, search, the player or playlist loading. Unit tests (`gradlew testDebugUnitTest`) do not cover focus, scrolling, playback or real portals; this plan does.

The scripts in [scripts/](scripts/) drive the remote over adb, take a screenshot at every checkpoint and write a report with the expected result next to each screenshot. A person (or an assistant reading the screenshots) then ticks pass or fail.

## 1. Two ways to run it

| | Real device + real playlist | Emulator + mock server |
|---|---|---|
| What it proves | Works with the real provider (Stalker on-demand loading, portal search, real streams, TMDB) | Works with known data on a clean install; no provider or account needed |
| Data | Your playlist | `mock_xtream.py`: 240 channels in 40 categories, 300 movies in one category, the same 12 movies in English and French, a series with 2 seasons |
| Use when | Before a release, after player or loader changes | After every merge or UI change, in a clean state |

Run both before a release.

## 2. Prerequisites

- adb on the computer; macOS or Linux shell (`sh`, `perl`, `curl`, `python3`).
- TV resolution 1920x1080, sidebar on the left (default). The scripts tap fixed positions for the sidebar, the header search pill and the on-screen keyboard.
- App language: English (the expected texts below are English).
- For playback on the emulator: a video file for the mock server (the command is in the header of `mock_xtream.py`).
- Real device: enable network debugging and connect once with `adb connect <ip>:5555`.

## 3. Setup

**Real device.** The playlist must already be added and loaded. Build and pass the APK, or install it yourself:

```sh
DEVICE=<ip>:5555 APK=app/build/outputs/apk/debug/app-debug.apk OUT=tv-test-out/device sh docs/testing/scripts/run_tv_suite.sh
```

**Emulator.** Start an Android TV emulator (1080p), then:

```sh
export DEVICE=emulator-5554 OUT=tv-test-out/emulator APK=app/build/outputs/apk/debug/app-debug.apk
VIDEO=/path/test.mkv sh docs/testing/scripts/setup_emulator.sh
SEARCH_WORD=batman SERIES_WORD=mock sh docs/testing/scripts/run_tv_suite.sh
kill $(cat $OUT/mock.pid)
```

Useful variables: `CASES="T02 T04"` runs only some cases; `DEEP_ROWS` sets how far down the Live TV list goes (default 30); `SEARCH_WORD` / `SERIES_WORD` / `HOME_WORD` set the search terms. Output goes to `$OUT` (screenshots, `report.md`, `health.txt`). Do not commit it: screenshots of a real device show the provider name and may show channel names.

## 4. Test cases

Each case lists the bug or feature it protects. "Back" is the remote's Back key. Before every case (except T01 and T12) the script presses Back until a screen with the sidebar is shown (`ensure_main` in `lib.sh`), so a case that ends in the player or on a detail page does not break the next one.

| ID | Area | Steps (what the script does) | Expected | Protects |
|---|---|---|---|---|
| T01 | Start | Install over the old version, force stop, cold start | Home with Continue watching and the Movies/Series counts; no new download when a cache exists; no crash | Upgrade path, cache format |
| T02 | Live TV focus | Live TV; `DEEP_ROWS` rows down; Right; Down 3; Left; Right | Opens on Home. Right focuses the first channel of that category; Left goes back to the selected category; Right returns to the 4th channel | Right did nothing after scrolling; Left switched category |
| T03 | Live playback | OK on the channel; Channel Up; Back | Plays; Channel Up switches to the next channel; Back returns to Live TV | Zapping |
| T04 | Movies categories | Movies; Categories; into the grid; 20 rows down; Left; Right | Search pill says "Search movies …". The grid scrolls past 40 titles and keeps loading; Left focuses the selected category; Right returns to the same title | Preview capped at 40 titles; Left switched category |
| T05 | Detail and playback | OK on the title; Play; skip forward twice; Back; Back; Back | Detail page with description/cast/rating (with a TMDB key); the movie plays; skipping moves the position; the detail page then offers Resume; Back lands on the same title in the grid | Movie playback (#20), resume (#28), focus restore |
| T06 | Full category view | OK on the category; Right ~230 times | Full view opens; near the end the title count grows (Stalker: next portions load) | On-demand paging |
| T07 | Series | Series; open a series; Play on the detail page | Seasons and episodes are listed; an episode plays | Series episodes (Stalker file lookup) |
| T08 | Search from Movies | Movies; search pill; type `SEARCH_WORD`; open "All" | Filter "Movies" selected; hint "Search movies …"; only movies; the same title in two versions shows "year · language" (or the category) under each card; "All N" lists N titles | Search scoped to the area; look-alike duplicates |
| T09 | Search from Series | Series; search pill; type `SERIES_WORD` | Filter "Series"; Recently watched shows only series; only series in the results | Search scoped to the area |
| T10 | Search from Home | Home; search pill; type `HOME_WORD` (default `news`) | Filter "All"; Movies, Series and Live TV rows | Unscoped search still works |
| T11 | Settings language | Settings; Playlists | No German text in English mode, e.g. the content-mode button reads "Live TV + movies & series" | Missing dictionary rows |
| T12 | Health | Read the log | `crash=0 anr=0 oom=0 http429=0 focusWarnings=0`; memory (PSS) noted for comparison between runs. On the mock server, focus warnings are expected (see below) | Crashes, rate limiting, focus loops |

**Known mock-only finding.** On the emulator with the mock server, T12 reports focus warnings ("FocusRequester is not initialized", bursts of 8 to 21 lines about 120 ms apart) after Back from a series detail page and after Back from search to Home. The mock has no artwork, so Home has no top picture and the start-up focus retries run without a target. On a real device with artwork the count was 0. Treat a non-zero count on the emulator as expected; on a real device it is a failure.

## 5. Manual checks (not scripted)

Do these by hand before a release; they need a person, a phone or a second device.

| Check | Expected |
|---|---|
| Add a playlist from the phone page (QR code on the add screen) | The playlist loads on the TV |
| Phone search: search on the phone, tap a result | It plays on the TV |
| Favorites: add a channel, a movie and a series; move one with the menu key | They appear in Favorites in the chosen order |
| Settings > Playlists: switch the content mode through all three values | The settings page stays open; Live TV or Movies/Series empty out and come back |
| TV guide and catch-up (provider with EPG) | Guide fills; a past programme plays |
| Multi view, second stream, live rewind | Two streams play; rewind and "Back to live" work |
| Playback longer than 10 minutes | No stall, no drift |
| Switch the app language to German and back | All texts change; nothing stays in the other language |
| Second device with the same account (one session per MAC on Stalker) | Only test one device at a time; the other shows a clear error, not a crash |

## 6. Rules

- Do not run the suite on two devices with the same Stalker MAC at the same time. The portal allows one session and answers HTTP 429 when asked too often; the scripts pause between scroll steps for this reason.
- Never put playlist addresses, MACs, API keys or IP addresses in the report you share, a PR or the repository.
- A failed case: keep the screenshots, note the step, then file or fix it. Rerun only the affected case with `CASES="Txx"`.
- When a feature changes the screen layout (sidebar position, header pills, keyboard), update the coordinates in `scripts/lib.sh`.
- Taps put the app in touch mode. In the full results view ("All N" in search, a category's full view), Back after a tap only moves between the filters and the grid and does not leave the view; the scripts leave it through the sidebar. Not yet checked with a real remote (key presses only).
- `open_search_in` checks that the search keyboard is shown and retries up to twice: a tap that lands while a page is still changing can open the focused card instead.

## 7. Last results

| Date | Build | Where | Result |
|---|---|---|---|
| 2026-11-12 | `a485b6f` + translation fix | NVIDIA SHIELD, Stalker portal | T01-T12 pass. Found and fixed: content-mode labels showed German. Note: two different series with the same long title prefix ("Trapped In A Dating Sim …") look alike because the card cuts the title |
| 2026-11-12 | `a485b6f` + translation fix | Android TV emulator, mock server | T01-T11 pass; T12: crash/ANR/OOM/429 = 0, focus warnings present (mock-only, see section 4). T06 shows no growth (expected without Stalker) |
