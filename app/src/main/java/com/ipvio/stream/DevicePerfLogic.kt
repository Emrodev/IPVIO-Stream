package com.ipvio.stream

/**
 * T-070: setting "Device performance" (Settings > General). What each choice switches, from the choice and the memory tier
 * the app detects ([Mem.tier]: 0 = low, 1 = medium, 2 = high). Pure logic, no Android imports, covered by JVM tests.
 *
 * - Automatic (default): as before, except that on devices with little memory (tier 0, most Fire TV sticks) the work nobody
 *   sees starts later (genre detection, search index). The look does not change.
 * - Weak device: no animations, no background picture behind the home rows, no running slideshow bar, no poster preview,
 *   no focus shadow, smaller card images, a small image cache, search index only when the search opens, the home screen is
 *   not kept under a detail page, genre detection later.
 * - Strong device: everything on, also on devices with little memory, larger image cache and more images decoded at once.
 *
 * The player buffer always follows the detected memory ([Mem.bufferPlan]), whatever is chosen here.
 */
object DevicePerfLogic {
    const val AUTO = 0
    const val WEAK = 1
    const val STRONG = 2

    /** Wait before the automatic genre detection starts after a list change (before T-070 always 8 s). */
    const val GENRE_DELAY_MS = 8_000L
    const val GENRE_DELAY_LIGHT_MS = 30_000L

    /** Wait before the search index is prepared on devices with little memory in "Automatic". */
    const val SEARCH_DELAY_LIGHT_MS = 8_000L

    class Profile(
        /** No animations, background picture, slideshow bar, poster preview or focus shadow. */
        val reduceVisuals: Boolean,
        /** Posters and landscape images of the cards in a lower resolution. */
        val smallImages: Boolean,
        /** The search index is built only when the search opens. */
        val searchOnDemand: Boolean,
        /** Wait before the search index is prepared after a list is shown (unless [searchOnDemand]). */
        val searchDelayMs: Long,
        /** The home screen may stay composed under a detail page (T-069b). */
        val keepHome: Boolean,
        /** Wait before the automatic genre detection starts after a list change. */
        val genreDelayMs: Long,
        /** Memory tier for the image cache ([Mem.imageCachePercent]). */
        val imageTier: Int,
        /** Images decoded at the same time. */
        val decoders: Int
    )

    fun profile(mode: Int, detectedTier: Int): Profile {
        val tier = detectedTier.coerceIn(0, 2)
        return when (mode) {
            WEAK -> Profile(
                reduceVisuals = true, smallImages = true, searchOnDemand = true, searchDelayMs = StartupLogic.SEARCH_INDEX_DELAY_MS,
                keepHome = false, genreDelayMs = GENRE_DELAY_LIGHT_MS, imageTier = 0, decoders = 2
            )
            STRONG -> Profile(
                reduceVisuals = false, smallImages = false, searchOnDemand = false, searchDelayMs = StartupLogic.SEARCH_INDEX_DELAY_MS,
                keepHome = true, genreDelayMs = GENRE_DELAY_MS, imageTier = maxOf(tier, 1), decoders = 4
            )
            else -> {
                val low = tier == 0
                Profile(
                    reduceVisuals = false, smallImages = false, searchOnDemand = false,
                    searchDelayMs = if (low) SEARCH_DELAY_LIGHT_MS else StartupLogic.SEARCH_INDEX_DELAY_MS,
                    keepHome = true, genreDelayMs = if (low) GENRE_DELAY_LIGHT_MS else GENRE_DELAY_MS,
                    imageTier = tier, decoders = if (tier >= 2) 4 else 2
                )
            }
        }
    }

    /** TMDB poster address for a card: w342 (as before), w185 with [small]. */
    fun cardPoster(url: String, small: Boolean): String =
        url.replace("/t/p/w500/", if (small) "/t/p/w185/" else "/t/p/w342/")

    /** TMDB landscape address for a small card (landscape cards, tiles): w780 (as before), w300 with [small]. */
    fun smallBackdrop(url: String, small: Boolean): String =
        url.replace("/t/p/w1280/", if (small) "/t/p/w300/" else "/t/p/w780/")
}
