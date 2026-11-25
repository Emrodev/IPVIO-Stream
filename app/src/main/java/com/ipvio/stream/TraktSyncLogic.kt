package com.ipvio.stream

import org.json.JSONArray
import org.json.JSONObject

/**
 * Trakt sync, read-only (T-031 steps 4 and 5): what the app takes from the user's Trakt account. Pure logic, JVM-testable
 * (org.json is a test dependency; on Android it is part of the platform).
 *
 * API facts used here (from the open-source clients trakt.tv 8.3.0 for Node and trakt.py 4.4.0, the official
 * documentation could not be reached from the build session):
 * - base https://api.trakt.tv, headers trakt-api-version 2, trakt-api-key = client ID, Authorization: Bearer
 * - GET /sync/watched/movies: [{plays, last_watched_at, movie{title, year, ids}}]
 * - GET /sync/watched/shows: [{last_watched_at, show{title, year, ids}, seasons[{number, episodes[{number, plays}]}]}]
 * - GET /sync/watchlist/movies and /sync/watchlist/shows: [{listed_at, type, movie{…} or show{…}}]
 * - the token endpoint takes a JSON body; a refresh sends grant_type refresh_token
 *
 * Rules: nothing is ever written to Trakt; nothing in the app is unmarked or removed; existing watch progress is never
 * lowered; watched marks only fill free places of the progress store, so the user's own progress is never pushed out.
 */
object TraktSyncLogic {
    const val API_URL = "https://api.trakt.tv"
    /** Page size for the paginated lists; Trakt returns at most this many entries per page. */
    const val PAGE_LIMIT = 1000
    /** Pages read at most per list (safety stop against a broken page count). */
    const val MAX_PAGES = 20
    /** Automatic sync at most this often (app start); "Sync now" ignores it. */
    const val AUTO_INTERVAL_MS = 6 * 3600_000L

    fun watchedPath(shows: Boolean, page: Int) = "/sync/watched/" + (if (shows) "shows" else "movies") + "?page=$page&limit=$PAGE_LIMIT"
    fun watchlistPath(shows: Boolean, page: Int) = "/sync/watchlist/" + (if (shows) "shows" else "movies") + "?page=$page&limit=$PAGE_LIMIT"

    /** Request headers for an authenticated API call. */
    fun headers(clientId: String, access: String, userAgent: String): Map<String, String> = linkedMapOf(
        "Content-Type" to "application/json",
        "trakt-api-version" to "2",
        "trakt-api-key" to clientId,
        "Authorization" to "Bearer $access",
        "User-Agent" to userAgent,
    )

    /** JSON body for renewing the access token (PKCE app: no client secret). */
    fun refreshBody(refresh: String, clientId: String, redirectUri: String): String = JSONObject()
        .put("refresh_token", refresh)
        .put("client_id", clientId)
        .put("redirect_uri", redirectUri)
        .put("grant_type", "refresh_token")
        .toString()

    /** JSON body for the code exchange (same fields as [TraktLogic.tokenBody]). */
    fun exchangeBody(fields: Map<String, String>): String = JSONObject().also { o -> fields.forEach { (k, v) -> o.put(k, v) } }.toString()

    /** More pages to read? [pageCount] from the X-Pagination-Page-Count header (null = not paginated). */
    fun hasNextPage(page: Int, pageCount: Int?, itemsOnPage: Int): Boolean =
        pageCount != null && page < pageCount && page < MAX_PAGES && itemsOnPage > 0

    /** A show the user watched, with the watched episodes as [ResumeLogic.key] values. */
    data class WatchedShow(val title: String, val year: Int?, val episodes: Set<Long>, val lastWatched: String)

    /** A watched movie, with the time it was last watched (ISO text, sorts by time). */
    data class WatchedMovie(val title: String, val year: Int?, val lastWatched: String)

    private fun yearOf(o: JSONObject): Int? = o.optInt("year", 0).takeIf { it in 1870..2200 }

    fun parseWatchedMovies(json: String): List<WatchedMovie> {
        val a = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        val out = ArrayList<WatchedMovie>(a.length())
        for (i in 0 until a.length()) {
            val e = a.optJSONObject(i) ?: continue
            val m = e.optJSONObject("movie") ?: continue
            val title = m.optString("title").trim()
            if (title.isEmpty()) continue
            out.add(WatchedMovie(title, yearOf(m), e.optString("last_watched_at")))
        }
        return out
    }

    fun parseWatchedShows(json: String): List<WatchedShow> {
        val a = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        val out = ArrayList<WatchedShow>(a.length())
        for (i in 0 until a.length()) {
            val e = a.optJSONObject(i) ?: continue
            val s = e.optJSONObject("show") ?: continue
            val title = s.optString("title").trim()
            if (title.isEmpty()) continue
            val eps = HashSet<Long>()
            val seasons = e.optJSONArray("seasons")
            if (seasons != null) for (j in 0 until seasons.length()) {
                val se = seasons.optJSONObject(j) ?: continue
                val sn = se.optInt("number", -1)
                if (sn !in 0..99) continue
                val list = se.optJSONArray("episodes") ?: continue
                for (k in 0 until list.length()) {
                    val ep = list.optJSONObject(k) ?: continue
                    val n = ep.optInt("number", -1)
                    if (n !in 0..9999 || ep.optInt("plays", 1) <= 0) continue
                    eps.add(ResumeLogic.key(sn, n))
                }
            }
            out.add(WatchedShow(title, yearOf(s), eps, e.optString("last_watched_at")))
        }
        return out
    }

    /** Watchlist entries as import rows ([shows] = the list of shows, else movies). */
    fun parseWatchlist(json: String, shows: Boolean): List<ListImportLogic.Row> {
        val a = runCatching { JSONArray(json) }.getOrNull() ?: return emptyList()
        val key = if (shows) "show" else "movie"
        val out = ArrayList<ListImportLogic.Row>(a.length())
        for (i in 0 until a.length()) {
            val o = a.optJSONObject(i)?.optJSONObject(key) ?: continue
            val title = o.optString("title").trim()
            if (title.isNotEmpty()) out.add(ListImportLogic.Row(title, yearOf(o), ListImportLogic.Status.WATCHLIST, shows))
        }
        return out
    }

    /** What one sync does in the app. */
    class Plan(
        /** IDs for the category "Trakt · watched" (movies and series found in the playlist). */
        val watchedIds: List<String>,
        /** IDs for the category "Trakt · watchlist". */
        val watchlistIds: List<String>,
        /** Watched marks to add: item ID → episode keys (a movie uses key (0, 0)). Only marks that are new. */
        val marks: Map<String, Set<Long>>,
        /** Titles read from Trakt (watched movies + shows + watchlist), for the status line. */
        val read: Int,
    )

    /**
     * Matches the Trakt lists with the loaded playlist and decides which watched marks to add.
     * [existing] = item ID → keys already marked as watched (never touched), [stored] = number of titles in the progress
     * store, [capacity] = its size: new titles are only added while there is room, most recently watched first.
     */
    fun plan(
        movies: List<WatchedMovie>,
        shows: List<WatchedShow>,
        watchlist: List<ListImportLogic.Row>,
        items: List<Item>,
        existing: (String) -> Set<Long>?,
        stored: Int,
        capacity: Int,
    ): Plan {
        val rowsWatched = movies.map { ListImportLogic.Row(it.title, it.year, ListImportLogic.Status.WATCHED, false) } +
            shows.map { ListImportLogic.Row(it.title, it.year, ListImportLogic.Status.WATCHED, true) }
        val watchedHits = ListImportLogic.match(rowsWatched, items)
        val watchlistHits = ListImportLogic.match(watchlist, items)

        // Which Trakt entry each hit came from, to know its episodes and its time
        val byItem = HashMap<String, Pair<Set<Long>, String>>()
        for (m in movies) {
            val hit = ListImportLogic.match(listOf(ListImportLogic.Row(m.title, m.year, ListImportLogic.Status.WATCHED, false)), items).firstOrNull() ?: continue
            byItem.putIfAbsent(hit.first.id, setOf(ResumeLogic.key(0, 0)) to m.lastWatched)
        }
        for (s in shows) {
            if (s.episodes.isEmpty()) continue
            val hit = ListImportLogic.match(listOf(ListImportLogic.Row(s.title, s.year, ListImportLogic.Status.WATCHED, true)), items).firstOrNull() ?: continue
            byItem.putIfAbsent(hit.first.id, s.episodes to s.lastWatched)
        }

        var free = (capacity - stored).coerceAtLeast(0)
        val marks = LinkedHashMap<String, Set<Long>>()
        // Most recently watched first, so the titles a user is most likely to continue get their marks
        for ((id, v) in byItem.entries.sortedByDescending { it.value.second }) {
            val have = existing(id)
            val add = if (have == null) v.first else v.first - have
            if (add.isEmpty()) continue
            if (have == null) {
                if (free <= 0) continue
                free--
            }
            marks[id] = add
        }
        return Plan(
            watchedIds = watchedHits.map { it.first.id }.distinct(),
            watchlistIds = watchlistHits.map { it.first.id }.distinct(),
            marks = marks,
            read = movies.size + shows.size + watchlist.size,
        )
    }

    /** Is an automatic sync due? Never before the first successful sync of a connection is wanted again too early. */
    fun autoDue(nowMs: Long, lastSyncMs: Long): Boolean = lastSyncMs <= 0L || nowMs - lastSyncMs >= AUTO_INTERVAL_MS
}
