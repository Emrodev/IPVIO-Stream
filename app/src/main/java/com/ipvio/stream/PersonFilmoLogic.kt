package com.ipvio.stream

/**
 * One title of a person (T-053b): TMDB data, and the playlist item when the title is in the own playlist (null = not in the playlist).
 */
class FilmoEntry(val title: String, val year: Int?, val isTv: Boolean, val poster: String?, val item: Item?)

/** Filmography rules (pure logic, no Android imports). */
object PersonFilmoLogic {
    /** Titles of one kind: 0 all, 1 movies, 2 series. Newest first; titles without a year last. */
    fun visible(entries: List<FilmoEntry>, kind: Int): List<FilmoEntry> =
        entries.filter { kind == 0 || (kind == 1 && !it.isTv) || (kind == 2 && it.isTv) }
            .sortedWith(compareByDescending<FilmoEntry> { it.year ?: Int.MIN_VALUE })

    /** How many of the titles are in the own playlist. */
    fun inPlaylist(entries: List<FilmoEntry>): Int = entries.count { it.item != null }
}
