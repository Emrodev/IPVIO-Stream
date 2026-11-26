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
  screens). Keeping it composed under the detail page is an architectural change with memory and focus effects.
- Every screen fades in (`screenEnter`): 380 ms with Settings > "Design and color" > "Animations" set to "Normal" (the default), 220 ms
  with "Subtle", none with "Off". Shorter times are a visible design change: maintainer decision.
- Play from a home row runs `Sources.alternatives(item, all)` (a pass over the playlist) on the main thread in
  `MainActivity.start` (play path, not changed).
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
- Syntax check of the 10 changed Kotlin files with the same compiler: no syntax errors. Android and Compose classes are
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

## Status

Implemented on branch `claude/nifty-pasteur-ubd26z` (based on `dev` bc952a1), open for build and device test.
