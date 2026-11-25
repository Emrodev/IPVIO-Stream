package com.ipvio.stream

/**
 * #151: some Xtream providers list every season of a series as its own series ("Show S01", "Show S02", "Show Staffel 3").
 * With the option "Combine seasons" the app treats them as one series (pure logic, no Android).
 *
 * The name is only split when the marker is at the end, after a separator, and something is left in front of it.
 * "Season 2" alone, "Show2" or "Show 2" are not touched.
 */
object SeasonMergeLogic {
    private val MARK = Regex(
        """^(.*?\S)[\s._-]+[(\[]?\s*(?:s|season|seasons|staffel|saison|temporada|stagione|sezon|sezona|seizoen|sæson)\s*\.?\s*0*(\d{1,2})\s*[)\]]?$""",
        RegexOption.IGNORE_CASE
    )

    /** Title without the season marker and the season number, or null if the title has no marker. */
    fun split(title: String): Pair<String, Int>? {
        val m = MARK.matchEntire(title.trim()) ?: return null
        val season = m.groupValues[2].toIntOrNull()?.takeIf { it in 1..99 } ?: return null
        val base = m.groupValues[1].trim().trim('-', '|', ':', '.', '_', ' ')
        if (base.isBlank()) return null
        return base to season
    }

    /**
     * Episodes loaded from a series that stands for [season] of a combined series. Many providers number the episodes of such a
     * series as season 1 again; then they are moved to [season]. If the provider already uses other season numbers, nothing changes.
     */
    fun remap(episodes: List<Episode>, season: Int): List<Episode> =
        if (season <= 1 || episodes.isEmpty() || episodes.any { it.season != 1 }) episodes
        else episodes.map { it.copy(season = season) }
}
