# T-064 M3U: categories written in other ways than group-title="..."

- **Issue:** #145 "categories from provider not detected" (everything shows as "Other" / other)
- **Status:** implemented as a likely cause, not confirmed. Not built, not device tested.

## What is known

The reporter sent two screenshots (not readable here) and no details about the provider. "Other" is what the app uses when a channel has no category: M3U entries without `group-title="..."`, Xtream channels whose `category_id` is not in the category list, and Stalker channels without a genre.

## Change (M3U only)

`M3uGroupLogic` (pure) is used by the parser when `group-title="..."` is missing:

- `group-title` in other letter case, or with single quotes (`group-title='X'`)
- `tvg-group="X"`
- a line `#EXTGRP:X` (after or before the `#EXTINF` line of that entry; it applies to one entry)

Lists that use `group-title="..."` behave as before, with the same text (no trimming: category names are keys for favorites, hidden categories and the order). Lists without any group stay in "Other".

## Not changed

- Xtream: if the category list of the server cannot be read or the `category_id` does not match, channels still land in "Other". Not changed without a sample.
- Stalker: unchanged.

## Needed from the reporter (draft comment, not posted)

Which kind of playlist is it (M3U link, Xtream login, Stalker portal)? For an M3U link: the first lines of the list, with address, user name and password removed (an `#EXTINF` line is enough). With Settings > Diagnostics, if available, the playlist size.

## Risks

Low. Only used when the usual spelling is missing. A list with `group-title` is parsed the same way, and the first try is the same exact match as before.

## Tests

- JVM: `M3uGroupLogicTest` 6/6 (spellings, `#EXTGRP` before and after, `group-title` wins, nothing found stays "Other") and the existing `CatchupLogicTest` 8/8 (it parses lists) ran with the Kotlin compiler bundled in Gradle. Not built, not device tested.

### Manual test steps (maintainer)

1. Load an M3U list that uses `tvg-group` or `#EXTGRP`: the categories appear with their names instead of "Other".
2. Load a normal list with `group-title`: the categories are the same as before the update.
3. Favorite categories, hidden categories and the saved order of an existing playlist are still applied after the update.
