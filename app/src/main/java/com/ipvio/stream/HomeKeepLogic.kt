package com.ipvio.stream

/**
 * T-069b: the home screen (Home, Movies, Series, Live TV, Favorites, My list and the other areas drawn by `HomeScreen`) stays
 * composed, hidden and not laid out, while a detail or person page opened from it lies on top. Back then shows it at once
 * with its rows, scroll positions and images, instead of building it again. Pure logic, no Android imports, JVM tests.
 */
object HomeKeepLogic {
    /** Switch for the whole feature. Rollback: false (the home screen is built again after every detail page, as before). */
    const val ENABLED = true

    /** Share of the app's memory above which the home screen is not kept under a page (it is built again on Back instead). */
    const val HEAP_LIMIT = 0.7

    /**
     * `HomeScreen` is the screen under the top page: no player, title list, category page, add screen, Boomer Mode, search or
     * downloads (each of those replaces it, as before).
     */
    fun homeIsBase(
        playing: Boolean, browse: Boolean, categories: Boolean, adding: Boolean,
        boomer: Boolean, search: Boolean, downloads: Boolean
    ): Boolean = !playing && !browse && !categories && !adding && !boomer && !search && !downloads

    /** The home screen stays composed but hidden: it is the base, a detail or person page lies on top, and memory allows it. */
    fun keepHidden(base: Boolean, pageOnTop: Boolean, heapTight: Boolean, enabled: Boolean = ENABLED): Boolean =
        enabled && base && pageOnTop && !heapTight

    /** The home screen is composed at all: shown (base without a page on top) or kept hidden under a page. */
    fun composeHome(base: Boolean, pageOnTop: Boolean, keepHidden: Boolean): Boolean = base && (!pageOnTop || keepHidden)
}
