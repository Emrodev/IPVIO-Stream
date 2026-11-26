package com.ipvio.stream

/** Timing rules for the first seconds after a cold start (T-047). Pure logic, no Android imports, covered by JVM tests. */
object StartupLogic {
    /** Wait before the background refresh of the playlist starts after the stored list is already on screen. */
    const val REFRESH_DELAY_MS = 4_000L

    /** Wait before the archive details of an Xtream server are fetched (they are only needed for catch-up). */
    const val CATCHUP_DELAY_MS = 5_000L

    /**
     * T-069: wait before the search index of a newly shown list is built in the background. The home rows are built from the
     * same list in the same moment and the first scrolling happens then; the index (a pass over every title with many
     * allocations) no longer competes with them for the CPU. Opening the search earlier builds the index itself, as before T-056.
     */
    const val SEARCH_INDEX_DELAY_MS = 3_000L

    /** T-069c: wait before the movie source index of a newly shown list is built (after the home rows, before the search index). */
    const val SOURCES_INDEX_DELAY_MS = 2_000L

    /**
     * [shownFromStore] = the stored list is on screen now. [userStarted] = not the silent refresh at the app start: the user
     * reloaded, switched or added the list (no waiting). Only the silent refresh at a start is moved back, so the
     * first screens get the CPU and the network connection.
     */
    fun refreshDelayMs(shownFromStore: Boolean, userStarted: Boolean): Long =
        if (shownFromStore && !userStarted) REFRESH_DELAY_MS else 0L
}
