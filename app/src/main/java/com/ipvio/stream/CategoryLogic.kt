package com.ipvio.stream

/**
 * Which titles a category shows (T-054). Pure logic, no Android, testable.
 *
 * The provider's categories must list every title the provider puts into them, like the provider's own list does.
 * Before, the movie list was reduced to one entry per title, year and language *before* it was split into categories.
 * A film that appears in two categories (for example "Action" and "All films 2026") was then only in the first of them,
 * and the second category showed fewer titles than the provider (100 instead of 200).
 */
object CategoryLogic {
    /** Most further categories remembered for one merged series (bounds memory with odd lists). */
    const val MAX_MORE_GROUPS = 16

    /**
     * One entry per movie title and language for "All movies", genres, home rows and search: the same movie in several
     * versions (HD, 4K, …) or in several categories is shown once; the version is chosen on the detail page.
     */
    fun uniqueMovies(movies: List<Item>): List<Item> = PlaylistLogic.firstPerKey(movies) { it.mk + "|" + it.lang }

    /**
     * Titles per provider category, in list order. Every title is in its own category and, for a series that the list
     * merged from several categories, in each of them ([Item.moreGroups]). Nothing is merged or dropped here.
     */
    fun byGroup(list: List<Item>): Map<String, List<Item>> {
        val out = LinkedHashMap<String, ArrayList<Item>>()
        for (i in list) {
            out.getOrPut(i.group) { ArrayList() }.add(i)
            for (g in i.moreGroups) if (g != i.group) out.getOrPut(g) { ArrayList() }.add(i)
        }
        return out
    }
}
