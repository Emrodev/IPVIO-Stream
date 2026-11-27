package com.ipvio.stream

/**
 * T-073: "Top rated" shows one title per TMDB entry. The provider sends the same film several times
 * (German and original title, 4K/FHD/HD), and each copy matches the same TMDB id.
 */
object TopRatedLogic {
    /**
     * Keeps the first entry per key. [sorted] must already be sorted best first, so the best copy stays.
     * A null key (no TMDB id known) keeps the entry, because there is nothing to compare it with.
     */
    fun <T> distinctByKey(sorted: List<T>, key: (T) -> String?): List<T> {
        val seen = HashSet<String>()
        return sorted.filter { t ->
            val k = key(t)
            k == null || seen.add(k)
        }
    }

    /**
     * T-076: TMDB key of a title. The id the provider sent wins (exact, no network); the id from the metadata cache is the fallback
     * for titles without one. 0 for both = unknown, no key.
     */
    fun keyOf(providerId: Int, cachedId: Int, isTv: Boolean): String? = tmdbKey(if (providerId > 0) providerId else cachedId, isTv)

    /** T-076: keeps the first title per TMDB key (input sorted best first where it matters). Titles without a key stay. */
    fun <T> collapse(items: List<T>, providerId: (T) -> Int, cachedId: (T) -> Int, isTv: (T) -> Boolean): List<T> =
        distinctByKey(items) { keyOf(providerId(it), cachedId(it), isTv(it)) }

    /** Key for the TMDB id; movies and series are separate TMDB ids, so the kind is part of the key. */
    fun tmdbKey(tmdbId: Int, isTv: Boolean): String? = if (tmdbId > 0) (if (isTv) "tv:" else "movie:") + tmdbId else null
}
