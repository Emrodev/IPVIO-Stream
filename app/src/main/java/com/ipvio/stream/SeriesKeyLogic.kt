package com.ipvio.stream

/**
 * When two playlist entries with the same cleaned series name are one series, and when they are two (T-067).
 * Pure logic, no Android, testable.
 *
 * The parser merges entries by the cleaned name. On Xtream and Stalker logins (one placeholder per provider series) that
 * merged separate provider series into one: the same name in two categories ("Dark" in "Netflix DE" and "Netflix EN"),
 * "The Office (US)" and "(UK)", "Battlestar Galactica (1978)" and "(2004)". The second one then existed only as a hidden
 * variant and was missing from its category.
 *
 * The first entry of a name keeps its old key, title and year, so its ID ("s_" + key), the user's progress and lists and
 * its stored TMDB data stay the same. Only the series split off get a new key and their year or country.
 */
object SeriesKeyLogic {
    private val DIST = Regex("""[\(\[]\s*((?:19|20)\d{2}|US|UK|AU|NZ|CA)\s*[\)\]]""", RegexOption.IGNORE_CASE)
    private val YEAR = Regex("""^(?:19|20)\d{2}$""")

    /** Year or country in brackets in the original name ("(2004)", "[UK]"), upper case; null = none. */
    fun distinguisher(rawName: String): String? = DIST.find(rawName)?.groupValues?.get(1)?.uppercase()

    /** The year if [dist] is one, otherwise null. */
    fun yearOf(dist: String?): Int? = dist?.takeIf { YEAR.matches(it) }?.toInt()

    /** Placeholder of a series whose episodes are loaded on opening: one entry per provider series (Xtream or Stalker). */
    fun isPlaceholder(url: String): Boolean =
        url.endsWith("." + XtreamLogic.LAZY_EXT) || (url.startsWith("stalker://") && url.contains("/series/"))

    /**
     * Is the new entry a different series than the one already collected under the same name?
     * Only placeholders are split: each one is a whole provider series. Two of them in different categories, or with a
     * different year or country, are two series. Lists with episode lines (plain M3U) merge exactly as before: their
     * episodes may spread over season categories or carry the year of the season ("Show (2019) S03E01").
     */
    fun differs(prevGroup: String, prevDist: String?, prevPlaceholder: Boolean, group: String, dist: String?, placeholder: Boolean): Boolean {
        if (!placeholder || !prevPlaceholder) return false
        return prevGroup != group || (prevDist != null && dist != null && prevDist != dist)
    }

    /** Key for a series that differs from the first one of its name; letters, digits and "~" only besides the name. */
    fun altKey(base: String, group: String, dist: String?, byGroup: Boolean): String {
        val sb = StringBuilder(base)
        if (dist != null) sb.append('~').append(dist.lowercase())
        if (byGroup) sb.append("~g").append(Integer.toHexString(group.lowercase().hashCode()))
        return sb.toString()
    }
}
