# Behaviour register

This file lists user-visible behaviours that must not disappear when code changes. It is the record the maintainer asked for after a
Left-key regression (25 Nov 2026): a change to one screen silently broke a behaviour that had worked before.

## Rules

1. **Every new feature or changed behaviour gets an entry** here, in the same change. Entry = expected behaviour, the code marker that
   must stay in the code, and the manual test steps for the device.
2. **Every change to code that has an entry is checked against it.** Run `.claude/scripts/behaviour-check.sh` (read only). It fails when a
   marker is no longer in its file. Then go through the test steps of the touched entries before the change is reported as done.
3. **Removing a behaviour** needs the maintainer's approval (CLAUDE.md section 6). Remove its entry in the same change, and say so in the CHANGELOG.
4. The markers are a tripwire, not a proof. They show that the code is still there; only the device test shows that it works.
   Mark entries that have no device test yet as **not device tested**.

## Entries

Format used by `behaviour-check.sh`: one line `- Marker: \`text\` in \`path\`` per marker. The path is relative to the repository root.

### B-001 Movies and Series, list view: Left from the grid returns to the same category

- Expected: in a category (e.g. "Romance") press Down into the titles grid, then Left in the first column. Focus goes back to the
  category "Romance", not to a neighbour such as "Mystery". Fixed in 25 Nov 2026 with an explicit handler (the spatial focus
  search picked the row at the same height).
- Marker: `fun backToCategory()` in `app/src/main/java/com/ipvio/stream/CategoriesScreen.kt`
- Marker: `ev.key == Key.DirectionLeft && i % cols == 0` in `app/src/main/java/com/ipvio/stream/CategoriesScreen.kt`
- Test steps: Movies > Categories (list view). Move to "Romance", Down into the grid, Left. Expect "Romance". Repeat with Series and with a category that is not first.
- Status: fixed 25 Nov 2026, not device tested.

### B-002 Movies and Series: the selected category is remembered by name

- Expected: leaving a list and coming back restores the category that was selected, also after the list was rebuilt (#65).
- Marker: `CatMemory.selName` in `app/src/main/java/com/ipvio/stream/CategoriesScreen.kt`
- Test steps: select a category other than the first, open a title, go back. The same category is selected and the focus is in it.

### B-003 Live TV: Left from the grid returns to the same category

- Expected: same as B-001 for Live TV. Works since 1.4.x; checked on 25 Nov 2026.
- Expected (wrap, 25 Nov 2026): Up on the first category ("Playlist" or "Home") goes to the last category; Down on the last goes to the first. Same as the channel list and TV guide.
- Marker: `backToCategory()` in `app/src/main/java/com/ipvio/stream/LiveTab.kt`
- Marker: `GuideWrapLogic.wrapTarget(catFocusIdx, names.size` in `app/src/main/java/com/ipvio/stream/LiveTab.kt`
- Marker: `onFocus = { catFocusIdx = i }, onRight` in `app/src/main/java/com/ipvio/stream/LiveTab.kt`
- Test steps: Live TV, category "Sport", Down, Left. Expect "Sport". Wrap: on the first category press Up, expect the last one; on the last press Down, expect the first. Not device tested.

### B-004 Search: picking a recent search keeps the focus in the search screen

- Expected: pick a recent search chip; focus stays on the search screen, not in the sidebar (#101).
- Marker: `Same as #101 above` in `app/src/main/java/com/ipvio/stream/SearchScreen.kt`
- Test steps: Search, empty field, pick a chip. Focus stays in the search screen.

### B-005 Search: "Show all hits" is reachable on an empty category

- Expected: on a category with no hits, Down from the category tiles goes to "Show all hits"; OK shows all hits.
- Marker: `noHitsBtn` in `app/src/main/java/com/ipvio/stream/SearchScreen.kt`
- Test steps: Search "xyzabc"-type word with hits only in another category; select the empty category; Down; OK.

### B-006 Home: "Top rated" shows each TMDB title once

- Expected: the same film from several provider entries appears once, with its best-rated copy (T-073).
- Marker: `TopRatedLogic.distinctByKey` in `app/src/main/java/com/ipvio/stream/HomeScreen.kt`
- Test steps: Home, "Top rated" row: no title twice (e.g. The Godfather).

### B-007 Lists: the focused row stays in the middle of the screen

- Expected: moving Up/Down keeps the focused row centred until the list ends (T-074).
- Marker: `FocusCenterLogic.scrollDistance` in `app/src/main/java/com/ipvio/stream/MainActivity.kt`
- Test steps: any long vertical list; move Down and Up; no row jumps or is cut off.

### B-008 Home and Billboard: the kicker above the title shows only the picture quality

- Expected: FHD, 2K or SD above the title, no "Movie"/"Series" word (25 Nov 2026).
- Marker: `qualityOf(c)?.let { Kicker(it.uppercase()) }` in `app/src/main/java/com/ipvio/stream/HomeScreen.kt`
- Marker: `qualityOf(c)?.let { Kicker(it.uppercase()) }` in `app/src/main/java/com/ipvio/stream/Billboard.kt`
- Test steps: Home hero and Billboard with a title of known quality.

### B-009 Trakt: automatic sync interval setting

- Expected: "Automatically sync" in the Trakt panel with six choices; default every 6 hours; "Only manually" never syncs at app start (T-075).
- Marker: `TraktSyncLogic.autoDue(System.currentTimeMillis(), lastSyncMs(app), autoInterval(app))` in `app/src/main/java/com/ipvio/stream/TraktSync.kt`
- Marker: `TraktSync.setAutoInterval(ctx, ms)` in `app/src/main/java/com/ipvio/stream/ListImportUi.kt`
- Test steps: T-075 in `docs/tasks/T-073-T-074-user-issues-batch.md`.

### B-012 Movies and Category grids: the same film is shown once (T-076)

- Expected: the same film from several provider entries (e.g. "Der Pate" and "The Godfather", both TMDB 238) appears once in a
  category grid and in "Top rated". The provider's TMDB id is used when it is sent; the metadata cache is the fallback.
  Titles without any id stay as they are. Series are not covered yet (see T-076).
- Marker: `TopRatedLogic.collapse(l, { it.tmdb }` in `app/src/main/java/com/ipvio/stream/CategoriesScreen.kt`
- Marker: `TopRatedLogic.keyOf(t.first.tmdb` in `app/src/main/java/com/ipvio/stream/HomeScreen.kt`
- Marker: `(f["tmdb"] ?: f["tmdb_id"])` in `app/src/main/java/com/ipvio/stream/XtreamLogic.kt`
- Marker: `const val VERSION = 7` in `app/src/main/java/com/ipvio/stream/PlaylistCacheIO.kt`
- Test steps: reload the Xtream playlist once (the cache is rewritten in version 7). A category with a film in German and English
  shows it once. Old caches (version 6) still load; the titles are then shown as before until the playlist is reloaded.
- Status: JVM tests of the logic pass (T-076). Not device tested.

### B-013 Search: Up from the number row goes to the recent searches; no kind word on result covers (T-077)

- Expected: pressing Up on the on-screen number row (the keyboard's top row) focuses the first recent search, not the category tiles.
  Result covers under a category heading show no "Movie"/"Series" word (the heading already says it).
- Marker: `onUp: () -> Boolean = { false }` in `app/src/main/java/com/ipvio/stream/SearchScreen.kt`
- Marker: `PosterCard(x, { onOpen(x) }, Modifier.width(sk(168)).restoreFocus(x.id).then(fr), kindBadge = false)` in `app/src/main/java/com/ipvio/stream/SearchScreen.kt`
- Marker: `else if (kindBadge) Badge(kindLabel(item.kind), DS.Scrim)` in `app/src/main/java/com/ipvio/stream/Components.kt`
- Test steps: Search with recent searches; on "1" press Up: the first recent search is focused. Open a category (e.g. Movies) with hits: the covers show no kind word.
- Status: not device tested.

### B-010 Trakt sign-in on the phone: the login link opens in its own tab

- Expected: the code page stays open; the paste works after the login (not "The sign-in was not accepted").
- Marker: `target="_blank" rel="noopener"` in `app/src/main/java/com/ipvio/stream/PhoneImport.kt`
- Test steps: scan the Trakt QR code on the phone, sign in, paste the code.

### B-011 Sports Hub settings: one "Live results" switch

- Expected: one switch "Live results" on the Sports Hub settings page (25 Nov 2026).
- Marker: `ToggleRow("Live-Ergebnisse"` in `app/src/main/java/com/ipvio/stream/SportSettings.kt`
- Test steps: Settings > Sports Hub: the switch appears once.
