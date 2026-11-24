package com.ipvio.stream

/**
 * T-062: which buttons the Live TV player shows, and in which order (pure logic, no Android).
 *
 * The rewind and fast-forward buttons and "Settings" are fixed and not part of this list. Everything here can be
 * hidden or moved in Settings > Live TV. Whether a shown button is also available (needs an archive, a buffer,
 * several qualities ...) is decided by the player when it draws the row.
 */
object PlayerButtonsLogic {
    const val BACKLIVE = "backlive"
    const val REC = "rec"
    const val FROMSTART = "fromstart"
    const val ARCHIVE = "archive"
    const val FAV = "fav"
    const val TRACKS = "tracks"
    const val QUALITY = "quality"
    const val SCORE = "score"
    const val ASPECT = "aspect"
    const val SECOND = "second"
    const val EXTERNAL = "external"

    /** Every button that can be shown or hidden, in the order used by the settings page. */
    val ALL = listOf(BACKLIVE, REC, FROMSTART, ARCHIVE, FAV, TRACKS, QUALITY, SCORE, ASPECT, SECOND, EXTERNAL)

    /** Shown by default. Aspect ratio, second picture and "Open externally" are in the player menu ("Settings"). */
    val DEFAULT = listOf(BACKLIVE, REC, FROMSTART, ARCHIVE, FAV, TRACKS, QUALITY, SCORE)

    /** Stored text to list. null = never changed = default. Unknown ids and repeats are dropped; an empty text means none. */
    fun decode(stored: String?): List<String> {
        if (stored == null) return DEFAULT
        return stored.split(',').map { it.trim() }.filter { it in ALL }.distinct()
    }

    fun encode(list: List<String>): String = list.filter { it in ALL }.distinct().joinToString(",")

    /** Shows a hidden button (at the end) or hides a shown one. */
    fun toggle(list: List<String>, id: String): List<String> =
        if (id !in ALL) list else if (id in list) list - id else list + id

    /** Puts a shown button at [index] (0 = first); other buttons keep their order. A hidden button is not changed. */
    fun move(list: List<String>, id: String, index: Int): List<String> {
        if (id !in list) return list
        val rest = list - id
        return rest.take(index.coerceIn(0, rest.size)) + id + rest.drop(index.coerceIn(0, rest.size))
    }
}
