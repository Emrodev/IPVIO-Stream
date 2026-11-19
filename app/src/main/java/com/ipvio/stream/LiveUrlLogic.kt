package com.ipvio.stream

/**
 * A live channel is recognised by its address (#102). Xtream and Stalker live streams use a "/live/" path segment;
 * movies "/movie/" and series "/series/". A live address is never a movie, even if its group name says "Movies".
 * Pure logic, no Android, testable.
 */
object LiveUrlLogic {
    fun isLiveUrl(url: String): Boolean = url.contains("/live/", ignoreCase = true)
}
