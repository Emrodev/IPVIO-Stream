# T-059 Genre pill shows the opened genre category

- **Issue:** #27 "Movies filter: selected category still shows All and filters combine"
- **Status:** partly implemented (display), not built, not device tested

## Goal

Movies > Categories > OK on an IPVIO genre category opens a list that is already limited to that genre. The Genre pill still showed "All", so it looked as if no category was applied. Picking another genre then filtered inside the first list, with no sign of the combination.

## Change

- `BrowseReq` has a new optional field `genre` (default null). Existing callers are unchanged.
- `CategoriesScreen`: when an IPVIO genre category is opened (not "Other", not a provider category), its name is passed as `genre` (`onPick` has a fourth argument; the only caller is `MainActivity`).
- `BrowseScreen`: once the genre choices of the list are known, `state.genre` is set to that genre, only if it is one of the real genres of the list. The list does not change, because `ipvioGroups` and the Genre pill both use `Genres.categoriesOf`. It runs once per view (`genreInit`), so "Reset" and a later choice are not overridden.

## Not done

The reporter also expects that picking another genre in the pill switches to that category. The pill only sees the titles of the opened list, so it can only combine. Switching needs the full list of the section passed along (`BrowseReq` plus a second list). That is a larger change to the browse flow and is left open. The combination is now visible in the pill.

## Risks

Low. Display only: `state.genre` is set to a value that every title in the list already has. The list is recomputed once when the pill is set, which can cause one short reload on a very large list.

## Tests

- Unit test: not applicable. Compose state in `BrowseScreen` / `CategoriesScreen`; no pure logic was added.
- Syntax check of the changed files with the Kotlin compiler bundled in Gradle: no syntax errors (type errors are expected here, no Android classes).
- Build: not run. Device: not tested.

### Manual test steps (maintainer)

1. Movies > Categories (IPVIO genres): OK on "Action". The Genre pill shows "Action"; the list is the same as before.
2. Open the Genre pill, pick "Comedy": the list shows titles that are both Action and Comedy. The pill shows "Comedy".
3. Pill > "All" shows the whole opened category again.
4. Provider categories (the other tab of the categories): OK on a category. The Genre pill shows "All", as before.
5. "Other": the Genre pill shows "All".
6. Open a category, press Back from a detail page: the pill still shows the same genre.
