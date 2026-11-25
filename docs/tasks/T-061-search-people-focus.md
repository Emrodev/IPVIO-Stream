# T-061 Search: Down reaches the people results

- **Issue:** none yet (reported by the maintainer on 24 Nov 2026)
- **Status:** implemented, not built, not device tested

## Problem

In the search, with the filter "Persons" (or when only people match), the focus could not reach the results. Down from the keyboard or the filter did nothing; the user had to switch to "All" first.

## Cause (from the code)

`SearchScreen.kt`: `firstKey` (the row that receives the focus when Down is pressed, `firstRes`) was chosen from movies, series, live and sport only. With only people hits it was empty, so `firstRes.requestFocus()` found no target. `PeopleRow` also had no way to take that focus.

## Change

- `firstKey` falls back to `"pe"` when only people match (people come last, as in `rowOrder`, so other rows keep winning).
- `PeopleRow` and its `PersonCard` take an optional focus requester for the first card.

## Risks

Low. Only the focus entry of one row. Other result rows are unchanged.

## Tests

- Unit test: not applicable (Compose focus). Syntax check of the changed file: no syntax errors. Not built, not device tested.

### Manual test steps (maintainer)

1. Search for an actor name (TMDB key set). Pick the filter "Persons". Press Down from the keyboard or the filter: the focus lands on the first person.
2. Same with the filter "All" when movies and people both match: Down goes to the first movie row, as before.
3. Filter "Movies" with hits: unchanged.
