# T-010 Stalker: all movies and series

- **Goal:** Stalker portals with ~100,000 movies show all of them; series appear.
- **Cause:** `StalkerApi` capped movies at 6,000 and series at 4,000, and each category at 40-600 / 40-400 titles. Pages (14 titles each) were fetched one after another. If the series area failed, it failed silently.
- **Approved plan:** load per category on opening (maintainer, 2026-11-11). Split into two stages because stage B touches home rows, search, categories and the cache format:
  - **Stage A (done):** caps raised to safety limits (250,000 movies, 100,000 series, 20,000 pages per list); pages 2..n fetched by 4 workers in a window of 8, written in page order, failed page retried alone; portal without VOD categories → `category=*`; no series → one try with `category=*`; still none → short note in Diagnostics ("Stalker: keine Serien (Kategorien N, …)"), counts only.
  - **Stage B (open, needs plan):** categories first, titles of a category loaded when it is opened, kept on disk.
- **Files:** `StalkerApi.kt`.
- **Risks:** first load of a large portal is still long (about 7,000 requests at 4 in parallel, unmeasured). Portals that allow only one request per token: pages fail and are retried one by one (slow, not wrong). WorkManager stops a job after about 10 minutes: background refresh of very large portals may not finish (unverified). Memory is guarded while parsing (`ParseStop` → language selection).
- **Checks run:** pure-Kotlin compile of the logic files; CI build.
- **Device test:** add the Stalker portal again (or "Neu laden"); count of movies/series under Movies/Series; open a series → episodes; if still no series: Settings → Diagnostics, note the text after "Stalker:".
- **Status:** stage A on branch `fix/round28-major`; stage B open.
