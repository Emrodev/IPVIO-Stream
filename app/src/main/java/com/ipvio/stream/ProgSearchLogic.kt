package com.ipvio.stream

/**
 * T-051a: programme search on the phone (pure logic, no Android imports).
 * A typed title is matched against the loaded TV guide (XMLTV) of the visible live channels.
 * Live = running now; Upcoming = starts within the next 24 hours. Times are in seconds since 1970.
 */
object ProgSearchLogic {
    enum class Mode { LIVE, UPCOMING }

    /** One programme of one channel. */
    class Hit(val channelId: String, val channel: String, val title: String, val start: Long, val end: Long)

    /** Most results sent to the phone per request. */
    const val LIMIT = 50

    /** Xtream channels without guide in the playlist: at most this many short guides are fetched per request (time limit of the phone request). */
    const val XTREAM_FETCH_PER_REQUEST = 40

    /** Upcoming = starts after now and within this many seconds. */
    const val UPCOMING_SPAN_SEC = 24L * 3600L

    /** Lower case, letters and digits only: "Formula 1" and "formula1" give the same text. */
    fun norm(s: String): String = buildString { for (c in s.lowercase()) if (c.isLetterOrDigit()) append(c) }

    /** A programme title matches when the normalized query is part of it (at least two letters or digits). */
    fun matches(title: String, query: String): Boolean {
        val q = norm(query)
        return q.length >= 2 && norm(title).contains(q)
    }

    /** Keeps the shows of one [mode] at time [now] (seconds), sorted by start and channel, at most [LIMIT]. */
    fun select(hits: List<Hit>, now: Long, mode: Mode): List<Hit> {
        val picked = hits.filter { h ->
            when (mode) {
                Mode.LIVE -> h.start <= now && now < h.end
                Mode.UPCOMING -> h.start > now && h.start <= now + UPCOMING_SPAN_SEC
            }
        }
        return picked.sortedWith(compareBy<Hit>({ it.start }, { it.channel })).take(LIMIT)
    }
}
