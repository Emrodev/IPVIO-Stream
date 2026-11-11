package com.ipvio.stream

/**
 * Order for "recently added" (issue #22). M3U and Xtream lists run oldest -> newest (Xtream movies are ordered by their
 * "added" time while loading), so the newest titles are at the end. Stalker portals are asked with sortby=added and
 * send the newest first. A list that only holds Stalker titles is therefore read from the front.
 */
object RecentLogic {
    fun isNewestFirst(urls: Iterable<String>): Boolean {
        var any = false
        for (u in urls) { if (!u.startsWith("stalker://")) return false; any = true }
        return any
    }

    /** All titles, newest first. */
    fun <T> newestFirst(l: List<T>, url: (T) -> String): List<T> =
        if (isNewestFirst(l.asSequence().map(url).asIterable())) l else l.asReversed()

    /** The [n] newest titles, newest first. */
    fun <T> newest(l: List<T>, n: Int, url: (T) -> String): List<T> =
        if (isNewestFirst(l.asSequence().map(url).asIterable())) l.take(n) else l.takeLast(n).reversed()
}
