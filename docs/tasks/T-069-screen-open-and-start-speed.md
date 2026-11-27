# T-069 – Faster screen opening, smoother start and search typing

## Goal

Maintainer request (26 Nov 2026): start, screen changes and scrolling as fast and smooth as possible, without breaking
anything. Nothing in the app may be slow.

## Analysis (read-through, no profiler, nothing measured on a device)

Earlier rounds (T-007, T-034, T-047, T-056, T-068) already moved most work off the main thread: home data
(`HomeCache`), Browse lists and filters, Live TV rows with fixed height and delayed guide data, the billboard without a
constant zoom, the search index, settings files opened in parallel (`Profiles.prewarm`). Card composables, focus
modifiers, translation lookups (memoized) and the Coil setup were checked and left as they are.

Found on the main thread, ordered by expected effect (**unverified**, not measured):

1. **Search, every typing pause** (`SearchScreen.kt`, `progHits`): the programme search ran on the main thread: one pass
   over the whole playlist to find Xtream channels without a guide, then one pass over every live channel with
   `SearchSport.isSport` (string building and lowercase per channel) and every guide entry. This ran 250 ms after each
   typing pause, so the keyboard could stall while typing.
2. **Search, on opening**: `all.associateBy { it.id }` built a map of the whole playlist (all channels, movies and series)
   on the main thread every time the search opened. It was only used to find the channels of programme hits.
3. **Live TV with a playlist filter or private categories** (`LiveTab.kt`): the filtered channel list was a new list object
   on every visit, so `LiveGenre.split` (pattern matching over all channels for six genres) missed its identity cache and ran
   again on the main thread each time Live TV opened. The filtered categories were rebuilt too.
4. **Live TV start view** (`LiveHome`): a map of all channels on every visit, only to resolve "Recently watched".
5. **Player channel list** (`PlayerLive.kt`, `ChannelOverlay`, Left in the live player): a map and a grouping of all
   channels on every opening.
6. **Movie/series lists** (`BrowseScreen.kt`): the language choices counted every title of the list on the main thread
   on every opening and on every return from a detail page (the list is rebuilt after the detail page closes).
7. **Category pages** (`CategoriesScreen.kt`): the folder pattern pass over all category names and the copy of every folder's
   titles ran again on every return from a title list.
8. **Start**: the search index (a pass over every title with many allocations) was built in the same moment as the home
   rows, so both competed for the CPU while the first screen appeared and the user started to scroll.
9. **Start**: `Profiles.prewarm` opened 11 settings files in parallel; 18 more files on the start path were still read one
   after the other on the main thread.

Seen, not changed (needs a decision or touches a protected area):

- The home screen is disposed when a detail page opens and composed again on Back (`MainActivity.kt`, `when` on the
  screens). Done in T-069b (maintainer decision 26 Nov 2026, below).
- Every screen fades in (`screenEnter`): 380 ms with Settings > "Design and color" > "Animations" set to "Normal" (the default), 220 ms
  with "Subtle", none with "Off". Done in T-069d (maintainer decision 26 Nov 2026, below).
- Play from a home row runs `Sources.alternatives(item, all)` (a pass over the playlist) on the main thread in
  `MainActivity.start`. Done in T-069c (maintainer decision 26 Nov 2026, below).
- `DriveSync.init` decrypts the Drive token with the Android keystore on the main thread at start (only with Drive
  connected). Secrets handling: needs approval (CLAUDE.md section 6).

## Changes

- `ScreenCacheLogic.kt` (new, pure, JVM tests): `LastResults` keeps the last results of a pass over a large list, found
  again by the identity of the input list and an optional extra key; `ScreenCacheLogic.byIds` / `inOrder` resolve a few ids
  in one pass without a map of every item (same result as `associateBy`, the last item of a duplicate id wins).
- `SearchScreen.kt`: both passes of the programme search run on `Dispatchers.Default` (the guide caches they read are
  thread safe: `ConcurrentHashMap`, `LruCache`, volatile maps). The search for Xtream channels without a guide stops after
  the first ones it needs (same channels as before). The channels of the hits come with the result (`ProgState.items`);
  the map of the whole playlist is gone.
- `LiveTab.kt`: `LiveListCache` returns the same filtered channel list and categories on every visit (key: channel list
  object, playlist filter, private-category version), so the genre rows and bundles are found in their caches. The channel
  map for own channel lists is kept too. "Recently watched" is resolved in one pass.
- `PlayerLive.kt`: `ChannelOverlayCache` keeps the channel map and the categories of the last channel list.
- `BrowseScreen.kt`: the language choices are counted in the background and kept in `BrowseState` for the return from the
  detail page. The "Language" pill appears with the result, like the genre and country filters.
- `CategoriesScreen.kt`: `folderViewOf` (same code as before, moved out of the composable) and `CatFolderCache` keep the
  folder view per category page.
- `MainActivity.kt`, `StartupLogic.kt`: the search index of a newly shown list is built 3 s later
  (`SEARCH_INDEX_DELAY_MS`). Opening the search earlier builds it itself, as before T-056.
- `Profiles.kt`: `prewarm` also opens the other settings files read on the start path (names unchanged, nothing read or
  written by the prewarm itself).
- `Mem.kt`: `ScreenCaches.clear()` empties the new caches; called from `Mem.trim` (memory low) and from `MainActivity`
  whenever the playlist changes (load, refresh, switch), so they never keep the titles of a previous playlist in memory.

## Not changed

Storage format, settings names and keys, backup, player core (`PlayerScreen.kt`), playlist parsing, guide loading,
animations, dependencies, Gradle.

## Acceptance criteria

1. Typing in the search does not stall, also with a large playlist and a TV guide loaded; programme hits appear as before.
2. Opening Search, Live TV (also with a playlist filter or private categories), the player's channel list, a movie/series
   list and a category page is at least as fast as before; the content is the same.
3. The first seconds after a start (home rows, first scrolling) are smoother; search still finds titles right after the start.

## Risks

- Memory: the caches keep a few derived lists and maps of the current playlist (Live TV filtered list and categories,
  channel maps of Live TV and the player list, two category folder views). They reference the same items as the playlist
  itself, so the extra memory is the containers only (about 1 to 3 MB for 30,000 channels, estimate). They are emptied when
  Android reports low memory and when the playlist changes; after a change the first visit of each screen computes once.
- A cache entry would be stale if a list object were changed in place. The lists used as keys are not changed after they
  are built (new list on every playlist load, filter or private-category change).
- Search opened within 3 s of a start or a playlist change builds the index itself (short "Searching …" as before T-056).
- The "Language" pill of a title list appears a moment after the list opens the first time.
- `SearchScreen.kt` reads the guide caches from a background thread; `LiveTab` already does the same (`Epg.now`).

## Documentation

`CHANGELOG.md` (Unreleased), `docs/TESTING.md` (T15), `docs/tasks/WORK_STATE.md`.

## Test results

- Ran here (JDK 21, Kotlin 2.0.21 compiler bundled with Gradle 8.14.3, JUnit 4.13.2): `ScreenCacheLogicTest` (6 new
  tests) and `StartupLogicTest`: 10/10 passed.
- T-069b/c: `HomeKeepLogicTest` (3 new), `MovieSourcesLogicTest` (4 new, compiled with `Models.kt` and the 13 pure files it
  needs), `ScreenCacheLogicTest`, `StartupLogicTest`: 17/17 passed.
- T-069d/e: `MotionTimingTest` (2 new), `BufferRelaxTest` (5 new): 7/7 passed. `PlayerBufferTest` and `Round17Test` were not
  run here (they need Android-only files); `BufferLogic.byteLimit`, `bump` and `scale` are unchanged.
- Syntax check of every changed Kotlin file with the same compiler: no syntax errors. Android and Compose classes are
  not available here, so there was no type check.
- **Not run:** Gradle build, Android unit tests, device test, time measurement on a device.

## Device test steps (maintainer)

1. Install over the current version (data stays). Use the largest playlist, with a TV guide (XMLTV or Xtream).
2. Search: type a word quickly, letter by letter (on-screen keyboard and remote). The keys follow without stalling. Results,
   "Live" and "Sport" programme hits are the same as before.
3. Search right after a cold start (within 3 s): titles are found (a short "Searching …" is fine).
4. Live TV: open it several times from the menu. With several playlists, let Live TV show only one of them, and mark one
   category private: Live TV still opens quickly, the genre rows are right, private channels stay hidden.
   Change the source or the private category: the lists follow at once.
5. Live TV start view: "Recently watched" shows the same channels in the same order.
6. Live player: press Left several times (channel list): it opens quickly, favorites, own lists and categories are right.
7. Movies > a large category (or "All movies"): the "Language" pill appears (with several languages), choosing a language
   works; open a title, Back: the list, the focus and the pill are there at once.
8. Movies > Categories with folders ("FR ›"): open a category, Back: the category page is back at once; folders open.
9. Cold start with a stored playlist: home rows appear, scroll right away. Settings > Diagnostics > Log: send the "Start:"
   line and the "Opened …: N ms" lines (Search, Tab 3, Browse, Categories) so the next round can use device numbers.
10. Low memory (Fire TV Stick with a large list): no crash; after the system frees memory the screens still open.

T-069b (home screen kept under the detail page), most important, please test with the remote only:

11. Home: open a title from a row, press Back: the home screen is there at once, the same card is focused, the rows did not
    move. Same from Movies, Series, Favorites and My list, and from the big picture ("Watch now" / the info button).
12. On the detail page press Up, Down, Left and Right at every edge, also on a series while "Loading episodes …" shows and on a
    person page: the focus never disappears (no invisible movement), OK never opens something that is not on screen.
13. Detail page > a person > a title > Back three times: each page comes back, then the home screen with the focused card.
14. Detail page > Play > Back from the player > Back: the home screen comes back with the focus on the card (built again, as before).
15. Home key on the remote while a detail page is open, then open the app again: the home screen, focus on the content.
16. Back on the home screen after returning: it behaves as before (up to "Watch now", then the menu, then "Press Back again to exit" if set).
17. Favorites: on the detail page add or remove a favorite, Back: the Favorites row shows the change.
18. "Because you watched": after Search > Back the row is there at once, it does not appear late.
19. Settings > Diagnostics > Log: "Opened Tab 0: N ms" after Back from a detail page; please send the number (before: the full build).

T-069c (movie sources):

20. A movie that the playlist has in several qualities: Play from a home row and from the detail page; the quality choice
    (automatic, chosen source, chosen quality) works as before; "Source" on the detail page lists the same entries.

T-069d (fade-in and focus):

21. Switch Home, Movies, Series, Live TV through the menu: the new screen is there almost at once (short fade), the focus is
    in the content right away. With "Animations" = "Off" nothing fades, as before.
22. Back from a title list, from "Show all", from a person page and from the search: the focus lands on the same element as
    before, only sooner. Hold Down on the home screen for a few seconds: the rows follow without lagging behind.
23. Live TV: move through the categories with Up/Down, single steps and held: the channel list follows sooner; holding does
    not stutter.

T-069e (player start):

24. Settings > Player > "Network and frame rate" > "Buffer (for slow internet)": note the level in the hint. Zap through 10 channels and start 3 movies:
    the picture comes sooner than in the previous version.
25. On a slow line or a weak provider: if a channel stutters right after the start more often than before, report it (the
    automatic mode raises the buffer again).
26. "Automatic" with level 1 or 2: watch 5 times for at least 2 minutes without stutter, then the hint shows one level less.

## T-069b – Home screen kept under the detail page

Maintainer decision (26 Nov 2026): work on points 2 and 3 of the open decisions (this section and T-069c).

### Analysis

`AppContent` showed exactly one screen (`when`): opening a detail or person page disposed `HomeScreen`, Back composed it
again: every row, card, image request and focus step, and the 380 ms fade-in (`screenEnter`). The data came from `HomeCache`,
so the cost is the composition and layout of the whole screen, plus two background lookups that started again from empty
(tile images, "Because you watched" row), so the row appeared late and pushed the rows below it down.

Risks found in the code before the change: `ContentFocus.req` and other focus requesters live in the home screen; its effects
request the focus at composition (menu switch, return fallback, first "Watch now"); it has five Back handlers.

### Changes

- `HomeKeepLogic.kt` (new, pure, JVM tests): when `HomeScreen` is the base screen, when it is kept hidden (a detail or person
  page on top, memory below 70 % of the app's limit), switch `ENABLED` for a rollback.
- `MainActivity.kt`: `HomeScreen` is drawn before the other screens in `KeepAliveBox`. While a detail or person page opened from
  it is on top, the box keeps the screen composed but neither measures, places nor draws it. Content that is not placed gets
  no D-pad focus and no touch (the way Compose treats list items composed ahead). If the focus is still on a home card when the
  page opens, it is cleared, so no key reaches a hidden card. Title lists, categories, search, downloads, Boomer Mode, the add
  screen and the player still replace the home screen as before.
- `HomeScreen.kt`: parameter `hidden`. While hidden: no Back handler, no focus request of its own, no write of the shared
  navigation state. When shown again it applies the same focus rules as after a rebuild (card of the opened title, otherwise
  "Watch now" after 1.2 s at the top, or the content after a menu switch). Tile images and the "Because you watched" row are
  kept for the next build of the screen (`HomeLookCache`, found again by the objects they were computed from).
- `Nav.kt`: `FocusReturn` lists the cards with `restoreFocus` that are composed, so the kept screen can focus the card of the
  opened title again.

### Effect (expected, not measured)

Back from a detail or person page shows the home screen in the next frame with its rows, scroll positions and images, without
the fade-in. After the player, a title list or the search the screen is still built again (as before), but without the late
"Because you watched" row and tile images.

### Risks

- **Focus**: the safety relies on Compose not offering content that is not placed to D-pad search (used for prefetched list
  items). Not verified on a device. If a key ever moves the focus into the hidden screen, the focus seems lost on the page;
  Back still works. Rollback: `HomeKeepLogic.ENABLED = false`.
- **Memory**: the home screen's state and the images of its visible cards stay in memory while the page is open (a few MB,
  estimate). Not kept when the app's memory is above 70 % of its limit; checked when a page opens and again whenever Android
  reports low memory (`Mem.trimTick`), then the hidden screen is let go and built again on Back, as before.
- While hidden, the home screen still follows changes (favorites, history, a playlist refresh) and its slideshow timer runs;
  nothing is laid out or drawn for it.
- Back from the player to a detail page: the home screen under it is composed hidden but its rows are built only when it is
  shown, so this path gains less.

## T-069c – Movie sources for Play without a pass over the playlist

### Analysis

`Sources.alternatives(item, all)` compares kind, language and key text of every title (`all.filter`). `MainActivity.start` runs it
on the main thread for every movie that starts (also Play from a home row); the detail page runs it in the background.

### Changes

- `MovieSourcesLogic.kt` (new, pure, JVM tests): `scan` is the old rule; `MovieSourceIndex` keeps one number per movie
  (key text and language) and compares numbers first, the texts only on a number match. Same result and order as `scan`
  (tested against it on 5,000 random titles).
- `Playback.kt` (`Sources`): uses the index when it was built for the same list, otherwise the old pass.
- `MainActivity.kt`, `StartupLogic.kt`: the index is built 2 s after a list is shown, in the background; a list change cancels
  it. `ScreenCaches.clear()` frees it with the other caches (new playlist, low memory).

### Risks

- Memory: one number and one reference per movie (about 1 MB for 150,000 movies, estimate).
- Play within 2 s of a list change, or after Android reported low memory, uses the old pass (same result, as before).

## T-069d – Shorter fade-in and no fixed waits on the navigation path

Maintainer decision (26 Nov 2026): start with points 3 and 4 of the second idea list (this section and T-069e).

### Changes (visible: every screen change and every focus step is quicker)

- `ThemeMath.kt` (pure, JVM tests): one place for the times. Screen fade-in 160 ms with "Normal" (before 380 ms), 110 ms with
  "Subtle" (before 220 ms), none with "Off" (as before). Soft scrolling to the focused element 200 ms (before 280 ms), the
  scrolling that keeps the focused row at its height 260 ms (before 380 ms), at once with "Off" (as before). Used by
  `Visuals.screenEnter`, `SmoothSpec` (`MainActivity.kt`) and `PivotSpec` (`Billboard.kt`).
- `Nav.kt`: `focusSoon(maxMs)` asks for the focus right after the next frame and then every 40 ms, with a last try once the old
  fixed wait has passed. Used where the focus waited a fixed time before: card of the opened title on return
  (`restoreFocus`, 150 ms), menu switch into the content (`MainActivity`, 150 ms then every 120 ms), switch from the sidebar
  into the content and the T-069b return (`HomeScreen`, 150 ms), "Show all" back to the row link (250 ms), title list back to
  the grid (`BrowseScreen`, 200 ms), person page (`CastUi.kt`, 150 ms), header tab after Back (`Header.kt`, 100 ms).
- `HomeScreen.kt`: the fallback to "Watch now" when no card took the focus after a return waits 0.5 s (before 1.2 s); the
  background info follows the focused title after 280 ms of rest (before 450 ms).
- `LiveTab.kt`: the channel list follows the focused category after 140 ms (before 220 ms), the info panel follows the
  focused channel after 200 ms (before 300 ms). Both stay longer than the key repeat, so holding Up/Down still builds nothing on
  the way.
- Not changed: dialogs and pop-ups (they open their own window and need the short wait), the poster preview that opens after
  a longer rest (design), the metadata lookup delay of cards that fly past (protects the TMDB limit).

### Risks

- A focus request that succeeds earlier than before could land before a list has settled on its restored position; the list
  then scrolls to the card (as it did after 150 ms). Not verified on a device.
- Faster scrolling can look less soft on large TVs; the times are in one place (`ThemeMath`) to tune them.

## T-069e – Faster player start, the automatic buffer steps down again

### Analysis

- `Mem.bufferPlan`: playback starts when 1.5 s (low-memory devices, e.g. Fire TV sticks), 2 s or 2.5 s of the stream are
  buffered.
- Buffer mode "Automatic" (Settings > Player > "Network and frame rate" > "Buffer (for slow internet)") raises a learned level after stutter; each level
  adds 2 s before every start (`BufferLogic.scale`). The level never went down again, so after one bad evening every start and
  every channel change waited 2 or 4 s longer, for good (only choosing the mode again reset it).

### Changes

- `Mem.kt`: start buffer 1.0 s / 1.25 s / 1.5 s (before 1.5 / 2 / 2.5 s). Buffer after a stall, buffer sizes and byte limits unchanged.
- `Round17Logic.kt` (`BufferLogic`, pure, JVM tests): `afterPlayback` lowers the learned level by one after 5 clean playbacks
  in a row; `isClean` = ready, no stutter, at least 2 minutes. Short playbacks (zapping) neither count nor reset the count.
- `Store.kt` (`AppSettings`): new key `buf_clean` in `ipvio_settings` (count of clean playbacks; no key renamed or removed);
  `learnBuffer` and `setBufferMode` reset it.
- `PlayerScreen.kt`: remembers the time of the first "ready" and whether stutter raised the buffer; on closing the player it
  reports the playback to `AppSettings.bufferPlayback`.

### Risks

- A shorter start buffer means a slightly higher chance of a short stall right after the start on slow lines; the automatic
  mode then raises the level again as before (and lowers it after 5 clean playbacks).
- The hint under the setting still says the buffer grows when it stutters; it does not mention that it shrinks again. A new
  hint text needs new dictionary rows in all languages (maintainer decision, not done).

## Status

Implemented on branch `claude/nifty-pasteur-ubd26z` (based on `dev` bc952a1), open for build and device test.
T-069b and T-069c added on the same branch (26 Nov 2026), T-069d and T-069e later the same day.
