# T-053: "Because you watched" row shows nothing

Status: implemented in the branch (23 Nov 2026), not built, not device tested. Threshold 2 is a decision, see below.
Source: user issue. "The widget that offers movies in relation to what I watched doesn't seem to work. I'm on my 4th movie and still no proposal."

## Goal

Either show the row when there are matches, or show a clear reason when there are none. Do not change the silent behaviour without a decision.

## Current behaviour (read only)

- `HomeScreen.kt:292-306`:
  - Source: `becauseSrc = histHome.firstOrNull { it.kind != Kind.LIVE }`, the last started movie or series.
  - Needs `MetaRepo.extended(...)` with a TMDB id (`tmdbId > 0`).
  - Needs `CreditsRepo.credits(...)` with `similar` titles from TMDB.
  - Matching against the own playlist: `LibMatch.matches(PlaylistStore.channels, c.similar, 40)`, excluding titles already watched.
  - **Row shown only if at least 4 matches remain** (`if (l.size >= 4)`), at most 20 titles.
- If TMDB is off (`Meta.kt:166`: `DemoMode.active` or empty `Config.TMDB_KEY`), `MetaRepo.extended` falls back to `providerMeta`, which normally has no `tmdbId`. The row then never appears.
- The row is a `produceState` keyed on `becauseSrc?.id` and `byId.size`. It does not retry on error.

## Likely cause (unverified)

1. The playlist contains few of the TMDB "similar" titles, so fewer than 4 matches (most likely with small or regional playlists).
2. TMDB metadata is off or the key is missing in this build (check the build, not the APK the user installed).
3. The history source is a live channel or an item without TMDB id (the source is the last non-live title; check that the user's last movie has an id).

Cannot be decided without the user's playlist size and whether movie details show TMDB data (poster, rating, description).

## Plan

1. Get data: does the movie detail page show TMDB poster and description? How many movies are in the playlist? Which item was the source?
2. Only then decide: lower the 4-match threshold, or show a short hint in the row (not in the home screen by default). Decision for the maintainer.
3. Pure logic for the threshold and the source choice in `HomeLogic` (no Android imports), JUnit test in `app/src/test/java/com/ipvio/stream/`.

## Acceptance criteria (after plan approval)

- With at least 1 usable match, the user sees the row or a short reason.
- Existing row behaviour for playlists with many matches unchanged.

## Risks

- Home screen layout (protected? no, but visible to all users).
- Load on TMDB (extra calls): none added by the plan.

## Test steps (maintainer)

1. Open a movie detail page of the last watched title. Does it show TMDB poster and description?
2. Count movies in the playlist (Playlists page).
3. Watch movie 4 and go Home. Is the "Because you watched" row there? Note the title shown in the row before it appears.
4. Send the answers to the analysis (T-053 follow-up) so the threshold decision can be made.

## Documentation

- `CHANGELOG.md` only with a fix.

## Test results

- Static read of the code only. No device test, no Gradle build.
