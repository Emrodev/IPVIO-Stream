# T-051 – Handoff: Live TV search on the phone, and the TV search screen redesign

Status: **not started. Handoff for a follow-up session (Sonnet).** Two parts:
- **T-051a** Phone search: find a live programme (e.g. "Formula 1") and see which channel shows it now, with tabs "Live" and "Upcoming".
- **T-051b** TV search screen: new layout from the maintainer's sketch (see "Layout" below).

The maintainer's sketch (three states, 1920 x 1080: typing, empty field, empty category) was uploaded in the chat on 23 Nov 2026. It is not in the repository because it has German labels (CLAUDE.md section 4); the measurements are recorded in "third round" below.

## Rules (from `CLAUDE.md`, read it first)
- Repository text in **English** (comments, names, docs, commit text). The maintainer writes in German; do not put German into the repository. UI labels: use the English column of the `Dict*.kt` dictionaries.
- Section 5 (protected): playlist sources, player and IPTV core, phone-remote routes (`/r`, `/b/CODE`) and their parameters, UI strings. The phone search lives under the existing `/r` page: **extend it, do not rename routes**.
- Section 6: large UI changes need the maintainer's approval of the plan. Write the plan first, then wait.
- Refactor, do not rewrite. Reuse existing components. The search logic, ViewModel, data sources and navigation are **not changed**; only the UI layer and derived UI states.
- Tests: pure logic goes into `*Logic.kt` without Android imports, with a JVM test in `app/src/test/java/com/ipvio/stream/`.
- The assistant cannot run Gradle. Report tests as not run unless CI or the maintainer shows results.

## T-051a Phone live TV search

### Read first (existing code, verify before use)
- `RemoteLogic.kt`: the phone page `/r` and its JSON API `/api/<name>` (answered by `PhoneImport.api`, see `PhoneImport.kt` `handle()`).
- `RemoteHub.kt`: what the TV side answers for the phone API.
- `Epg.kt`, `EpgFull.kt`, `LiveNow.kt`: where programme data (title, start, end, channel) comes from today. Check which fields exist before designing anything.
- `SearchScreen.kt`, `ChannelSearch.kt`, `GuideSearch.kt`: existing search on the TV; reuse the matching if possible.

### Goal
- On the phone (`/r`): a field "Search programmes". Typing "formula1" lists programmes whose title matches, with the channel name and the time.
- Two tabs: **Live** (on air now) and **Upcoming** (starts later; the list is sorted by start time).
- Each result: programme title, channel name, time (start–end), and a live bar for "Live".

### Acceptance criteria
- Search works with partial words and ignores case and punctuation (test with "formula1" and "Formula 1").
- "Live" shows only programmes whose start ≤ now < end; "Upcoming" shows start > now, next 24 h by default (**decision to confirm with the maintainer**).
- No programme data → an empty-state text, not an error.
- The phone API answers JSON; the route and parameter names of existing pages stay as they are.
- Limit of results (for example 50 per tab) to keep the phone page fast.

### Open questions (ask the maintainer, do not guess)
1. Window for "Upcoming": 24 h, or the whole guide?
2. Does the search include channels that are hidden or in ignored categories?
3. Should a result open the channel on the TV? (That is a new phone-to-TV action; needs approval per section 6.)

## T-051b TV search screen redesign

### Rules for the visual design
- **Only the arrangement and structure change.** Do not change colours, fonts, font sizes (use the existing size steps), corner radii, shadows, icons or the focus style (border/glow). Use the theme and the existing components. No new hex colours, no new fonts.
- Reuse existing components: poster card, channel tile, chips, buttons, on-screen keyboard, navigation bar, hero/backdrop. Create new ones only where none exists.
- The on-screen keyboard keeps its keys, logic, "Phone" input, umlaut keys and symbol keys. Only its placement changes.
- Keep the UI technology the screen already uses (Compose or XML, whichever `SearchScreen.kt` uses).
- Derived UI state is allowed: hit counts per category, suggestion list.

### Layout (1920×1080, left to right)
Text is written in English. The German labels of the maintainer's sketch are replaced by the English dictionary labels; check each label in `Dict*.kt` before use.

1. **Navigation bar** on the left, as today. The search icon is marked as active.
2. **Left search column** (fixed width about 600 dp-equivalent, slightly separated background from the theme), top to bottom:
   a. Heading "Search".
   b. Search field across the full column width: magnifier icon, text with cursor, clear button (X).
   c. Small line below: "X results in Y categories".
   d. A fixed-height area (same height in both states, so nothing jumps):
      - Field **not empty** → heading "Suggestions": vertical list of at most 4 rows, each row focusable: magnifier icon, the typed part in bold + the rest normal, a small category label on the right (All / Series / Live TV / Sport / Movies). Choosing a suggestion puts it into the search field.
      - Field **empty** → heading "Recent searches" as chips + a "Clear history" button.
   e. The on-screen keyboard in an even grid (all keys the same width, special keys wider), 5 rows.
3. **Right area** (rest of the width):
   a. **Hero** at the top: backdrop of the currently focused result fills the upper part, with a gradient to the left and downwards into the background colour. On it: type badge ("SERIES") + "Best match", large title, meta line (year • genre • seasons/episodes + IMDb badge), description (max. 3 lines), buttons "Watch", "Details", "Watchlist".
   b. Below: a row of 5 equal-width category tiles: All · Series · Live TV · Sport · Movies. Each tile: icon, large result count, category name. The selected tile is highlighted; tiles with 0 results are dimmed but still focusable. Selecting a tile filters the results below.
   c. Below: scrollable result rows, one per category that has results (categories with 0 results are hidden under "All"). Each row: title + count badge on the left, "Show all ›" on the right, then a horizontal row of cards:
      - Series / Movies: portrait poster with rating; below it title + seasons/episodes.
      - Live TV: landscape tile with channel logo, LIVE badge and progress bar; below it programme + time.
      - Sport: landscape card with league + event; below it date/time + channel.
      Order of the rows: the category of the best match first.
   d. If the selected category has 0 results: a notice box "No [category] for "[search term]"" with a "Show all results" button.

### Focus / D-pad
- Start focus on the keyboard.
- Right from the keyboard or suggestions → category tiles; down from the tiles → result rows; up from the tiles → hero buttons.
- The hero always shows the currently focused result (the best match at start).

### Acceptance criteria
- Every element of the layout exists; order and alignment match the sketch once the image is available.
- No new colour, font or size values in the diff (grep the diff for `Color(0x` and `sp` values not already used by the theme).
- D-pad order as above, tested on a device.
- The search results are the same as before the change (same query, same count).

### Test steps for the maintainer (after the build)
1. Open Search, type "formula", check the suggestions list and the hit count.
2. Press Right from the keyboard, then Down through the rows; press Up back to the hero buttons.
3. Select the "Live TV" tile: only Live TV rows remain; a category with 0 hits shows the notice.
4. Clear the field: "Recent searches" and "Clear history" appear in the same space.
5. Phone search (T-051a): open the phone page from Settings, search "formula1", switch between "Live" and "Upcoming".

## Files likely affected (to verify when starting)
- T-051a: `RemoteLogic.kt`, `RemoteHub.kt`, `PhoneImport.kt` (only the route list), `Epg.kt` / `EpgFull.kt`.
- T-051b: `SearchScreen.kt` (layout), `ChannelSearch.kt`, `GuideSearch.kt` (only if the derived counts live there), `ViewSettings.kt` (only if a tile is reused).

## Status of the parts
- T-051a: **implemented** on the phone search page (new "TV guide" tab), not built, not device tested. Defaults used, since the open questions were not answered: "Upcoming" = starts within 24 hours; hidden channels and ignored categories are left out (only the visible live channels, as the TV search does); a result starts the channel on the TV through the existing play action. Source: the XMLTV guide of the playlist, and for Xtream channels without XMLTV the short guide (`Epg.now`, cached 2 minutes, at most 40 channels fetched per search, 9 s time limit). The first search can therefore be partial. A full search would need a background guide index (open). Files: `ProgSearchLogic.kt` (pure logic), `ProgSearchLogicTest.kt`, `RemoteHub.kt` (route `prog`), `RemoteLogic.kt` (tab and page script; the script was not executed here).
- T-051b: **layout implemented** from the maintainer's three screenshots (22 Nov 2026), in `SearchScreen.kt`. Not built with Gradle, not device tested. Details below.

### T-051b implementation notes
- Left column 600 dp: heading, field with clear button, hit count, fixed area (suggestions or last searches, 250 dp), keyboard at the bottom. The old filter chip row is replaced by the category tiles.
- Right side: hero of the focused hit (or best match), buttons "Ansehen", "Details", "Merkliste" (toggles the watchlist), 5 category tiles, then the result rows.
- Tile order: All · Series · Live TV · Sport · Movies. Tile counts: All = movies + series + live; Sport is a part of Live TV (it is counted in both, as in the code before).
- Row order: the category of the best match first. Empty selected category: notice with the other categories that have hits and "Show all hits".
- Suggestions are titles of the current hits (up to 3); the typed part is bold. Word suggestions from the earlier code stay as chips below them.
- Focus: keyboard → right → first tile; tiles → up → "Ansehen", tiles → down → first result card.
- Not done from the sketch: the magnifier icon (no search icon in the icon set `AppIcons.kt`), the "Best match" label next to the badge, the card subtitles for Sport ("league + event") and the Live TV progress bar check. The "Backdrop" label in the sketch is an annotation and is not shown.
- "Ansehen" and "Details" both open the detail page, as a card click does today. Whether "Ansehen" should start playback is **open**.
- New German UI keys need entries in the dictionaries (open): "Suche", "Vorschläge", "Letzte Suchen", "Alle Treffer zeigen", "Auf der Merkliste", "Merkliste", "Ansehen", "Details", the "Keine … zu" and "Treffer gibt es in" notices, the hit count line.

### T-051b second round (decided by the maintainer)
- No magnifier icon, no "Best match" label, no "Watch" button (OK on a marked title opens the details).
- Live TV row: channels whose name matches, and channels whose running show matches (progress bar, show title as subtitle).
- Sport row: sport channels whose show matches, running now or in the next 24 hours (league = channel group, event, time, channel).
- Counts: Live TV tile counts channel and show hits; Sport tile counts sport channel and sport show hits. Shows on sport channels are not counted as live.
- TV programme search uses the XMLTV guide, and the cached short guide of Xtream channels (at most 40 fetched per search, 9 s). Not built, not device tested.

### T-051b third round (23 Nov 2026, from the uploaded sketch)
- The maintainer found the second-round screen "not as expected" (test build: squeezed tiles, empty hero, cut cards). The sketch was uploaded and the layout rebuilt from it.
- Sizes are taken from the sketch in sketch pixels: `sk(px)` = `dpx(px * 1658 / 1920)` (the app's design scale is based on a 1658 px design), so the proportions match on every screen width. Left panel 601 px with 37 px margins; field 79 px; suggestion rows 53 px; keyboard keys 57 px; right side 64 px margins; hero buttons 64 px; tiles 91 px; posters 168 px, live cards 300 px, 22 px apart.
- Kept from the second round (maintainer, asked again on 23 Nov 2026): no "Watch" button, no "Best match" label, no magnifier icon. "People" stays as a sixth tile (not in the sketch).
- The keyboard of the sketch (QWERTY in every language, as the maintainer decided on 23 Nov 2026, not the QWERTZ of the sketch; backspace in the "a" row, "-" key, "Clear all") is a separate `SketchKeyboard`; `SearchKeyboard` of the TV guide and channel searches is unchanged.
- Suggestions: up to four rows, titles of the hits first, then completions of the typed word (badge "All"). The word chips are gone.
- The "Show all" tile at the end of each row is gone; "Show all ›" sits in the row heading as in the sketch.
- Not compiled (no Android SDK here; a syntax check with the Kotlin compiler passed), not device tested. A side-by-side check with the sketch on the device is open.
