package com.ipvio.stream

/**
 * When two playlist entries with the same cleaned series name are one series, and when they are two (T-058).
 * Pure logic, no Android, testable.
 *
 * The parser merges entries by the cleaned name. That merged different shows into one: "The Office (US)" and
 * "The Office (UK)", "Battlestar Galactica (1978)" and "(2004)", and, for Xtream and Stalker logins, the same name in
 * two categories ("Dark" in "Netflix DE" and "Netflix EN"), although the provider lists them as separate series with
 * their own IDs. The second one then existed only as a hidden variant and was missing from its category.
 *
 * The first entry of a name keeps the old key, so its ID ("s_" + key) and the user's progress and lists stay the same.
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
     * Different years or countries: yes. Two placeholders in different categories: yes (the provider has two series).
     * Missing information on one side merges as before (episode lines of one series often differ in their tags).
     */
    fun differs(prevGroup: String, prevDist: String?, prevPlaceholder: Boolean, group: String, dist: String?, placeholder: Boolean): Boolean =
        (prevDist != null && dist != null && prevDist != dist) ||
            (placeholder && prevPlaceholder && prevGroup != group)

    /** Key for a series that differs from the first one of its name; letters, digits and "~" only besides the name. */
    fun altKey(base: String, group: String, dist: String?, byGroup: Boolean): String {
        val sb = StringBuilder(base)
        if (dist != null) sb.append('~').append(dist.lowercase())
        if (byGroup) sb.append("~g").append(Integer.toHexString(group.lowercase().hashCode()))
        return sb.toString()
    }
}
