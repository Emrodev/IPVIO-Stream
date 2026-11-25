package com.ipvio.stream

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-031 steps 4 and 5: read-only Trakt sync (history and watchlist into the app). */
class TraktSyncLogicTest {
    private val watchedMovies = """[
      {"plays":2,"last_watched_at":"2026-11-20T20:00:00.000Z","movie":{"title":"Inception","year":2010,"ids":{"trakt":1,"tmdb":27205}}},
      {"plays":1,"last_watched_at":"2026-10-01T20:00:00.000Z","movie":{"title":"Not In Playlist","year":2001,"ids":{"trakt":2}}},
      {"plays":1,"movie":{"title":"","year":2001}}
    ]"""
    private val watchedShows = """[
      {"plays":3,"last_watched_at":"2026-11-24T21:00:00.000Z","show":{"title":"Dark","year":2017,"ids":{"trakt":3}},
       "seasons":[{"number":1,"episodes":[{"number":1,"plays":1},{"number":2,"plays":2}]},{"number":2,"episodes":[{"number":1,"plays":1}]}]}
    ]"""
    private val watchlistMovies = """[{"listed_at":"2026-11-01T10:00:00.000Z","type":"movie","movie":{"title":"Dune","year":2021,"ids":{"trakt":4}}}]"""
    private val watchlistShows = """[{"listed_at":"2026-11-02T10:00:00.000Z","type":"show","show":{"title":"The Office","year":2005,"ids":{"trakt":5}}}]"""

    private fun movie(id: String, title: String, year: Int?) = Item(id, Kind.MOVIE, title, year, "http://h/movie/$id.mp4", null, "Filme")
    private fun series(id: String, title: String) = Item(id, Kind.SERIES, title, null, "", null, "Serien")
    private val items = listOf(movie("m1", "Inception", 2010), movie("m2", "Dune", 2021), series("s_dark", "Dark"), series("s_the office", "The Office"))

    @Test fun parsesWatchedMoviesAndSkipsEntriesWithoutTitle() {
        val m = TraktSyncLogic.parseWatchedMovies(watchedMovies)
        assertEquals(listOf("Inception", "Not In Playlist"), m.map { it.title })
        assertEquals(2010, m[0].year)
    }

    @Test fun parsesWatchedEpisodesAsKeys() {
        val s = TraktSyncLogic.parseWatchedShows(watchedShows).single()
        assertEquals(setOf(ResumeLogic.key(1, 1), ResumeLogic.key(1, 2), ResumeLogic.key(2, 1)), s.episodes)
    }

    @Test fun parsesWatchlistOfMoviesAndShows() {
        val l = TraktSyncLogic.parseWatchlist(watchlistMovies, false) + TraktSyncLogic.parseWatchlist(watchlistShows, true)
        assertEquals(listOf("Dune", "The Office"), l.map { it.title })
        assertTrue(l.all { it.status == ListImportLogic.Status.WATCHLIST })
        assertEquals(true, l[1].series)
    }

    @Test fun brokenJsonGivesNothing() {
        assertTrue(TraktSyncLogic.parseWatchedMovies("{oops").isEmpty())
        assertTrue(TraktSyncLogic.parseWatchedShows("").isEmpty())
        assertTrue(TraktSyncLogic.parseWatchlist("null", true).isEmpty())
    }

    private fun plan(existing: Map<String, Set<Long>> = emptyMap(), stored: Int = 0, capacity: Int = 400) = TraktSyncLogic.plan(
        TraktSyncLogic.parseWatchedMovies(watchedMovies), TraktSyncLogic.parseWatchedShows(watchedShows),
        TraktSyncLogic.parseWatchlist(watchlistMovies, false) + TraktSyncLogic.parseWatchlist(watchlistShows, true),
        items, { existing[it] }, stored, capacity,
    )

    @Test fun matchesHistoryAndWatchlistWithThePlaylist() {
        val p = plan()
        assertEquals(setOf("m1", "s_dark"), p.watchedIds.toSet())
        assertEquals(setOf("m2", "s_the office"), p.watchlistIds.toSet())
        assertEquals(setOf(ResumeLogic.key(0, 0)), p.marks["m1"])
        assertEquals(3, p.marks["s_dark"]?.size)
        assertEquals(5, p.read)
    }

    @Test fun neverTouchesEpisodesThatAlreadyHaveProgress() {
        // S01E01 is in the middle (any progress counts): only the two others are added
        val p = plan(existing = mapOf("s_dark" to setOf(ResumeLogic.key(1, 1))))
        assertEquals(setOf(ResumeLogic.key(1, 2), ResumeLogic.key(2, 1)), p.marks["s_dark"])
        // Everything already there: no mark at all
        val q = plan(existing = mapOf("m1" to setOf(ResumeLogic.key(0, 0))))
        assertNull(q.marks["m1"])
    }

    @Test fun newTitlesOnlyFillFreePlacesMostRecentFirst() {
        // One free place: the show watched on 24 Nov wins over the movie of 20 Nov
        val p = plan(stored = 399, capacity = 400)
        assertEquals(setOf("s_dark"), p.marks.keys)
        // Full store: no new title, but a title already in the store still gets its missing episodes
        val q = plan(existing = mapOf("s_dark" to setOf(ResumeLogic.key(1, 1))), stored = 400, capacity = 400)
        assertEquals(setOf("s_dark"), q.marks.keys)
    }

    @Test fun paginationStopsAtThePageCountAndOnEmptyPages() {
        assertTrue(TraktSyncLogic.hasNextPage(1, 3, 1000))
        assertFalse(TraktSyncLogic.hasNextPage(3, 3, 1000))
        assertFalse(TraktSyncLogic.hasNextPage(1, null, 1000))
        assertFalse(TraktSyncLogic.hasNextPage(1, 5, 0))
        assertFalse(TraktSyncLogic.hasNextPage(TraktSyncLogic.MAX_PAGES, 999, 10))
    }

    @Test fun requestsCarryTheTraktHeadersAndAJsonRefresh() {
        val h = TraktSyncLogic.headers("cid", "tok", "IPVIO Stream/1.5.1")
        assertEquals("2", h["trakt-api-version"])
        assertEquals("cid", h["trakt-api-key"])
        assertEquals("Bearer tok", h["Authorization"])
        val r = JSONObject(TraktSyncLogic.refreshBody("ref", "cid", TraktLogic.REDIRECT_URI))
        assertEquals("refresh_token", r.getString("grant_type"))
        assertEquals("ref", r.getString("refresh_token"))
        assertFalse(r.has("client_secret"))
        assertEquals("/sync/watched/shows?page=2&limit=1000", TraktSyncLogic.watchedPath(true, 2))
        assertEquals("/sync/watchlist/movies?page=1&limit=1000", TraktSyncLogic.watchlistPath(false, 1))
    }

    @Test fun autoSyncAtMostEverySixHours() {
        assertTrue(TraktSyncLogic.autoDue(10_000L, 0L))
        assertFalse(TraktSyncLogic.autoDue(TraktSyncLogic.AUTO_INTERVAL_MS - 1, 0L + 1))
        assertTrue(TraktSyncLogic.autoDue(TraktSyncLogic.AUTO_INTERVAL_MS + 5, 5))
    }

    @Test fun syncTextsHaveEnglishRowsWithMatchingPlaceholders() {
        I18nCore.load(DictA, DictT031)
        val en = I18nCore.CODES.indexOf("en")
        assertEquals("Sync now", I18nCore.translate("Jetzt synchronisieren", en))
        assertEquals("3 watched · 2 on the watchlist", I18nCore.translate("3 gesehen · 2 auf der Merkliste", en))
        assertEquals("Last synced: 25.11. 18:30", I18nCore.translate("Zuletzt synchronisiert: 25.11. 18:30", en))
    }

    @Test fun whatsNewLinesOf151HaveEnglishRows() {
        I18nCore.load(DictA, DictT031)
        val en = I18nCore.CODES.indexOf("en")
        val rows = DictT031.lineSequence().filter { it.contains('¦') && !it.startsWith("#") && !it.contains('{') }.toList()
        assertTrue(rows.size >= 20)
        for (r in rows) {
            val (de, eng) = r.split('¦')
            assertTrue(de, eng.isNotBlank())
            assertEquals(eng, I18nCore.translate(de, en))
        }
    }
}
