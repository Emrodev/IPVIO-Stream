package com.ipvio.stream

/**
 * Playlist selection per section (T-051): which of the shown playlists a section (Live TV, Movies, Series) uses.
 * Pure logic, no Android imports, testable on the JVM.
 *
 * Empty selection = "All playlists" (every eligible playlist). A selection never ends up empty on screen:
 * if none of the selected playlists is eligible for the section, the section falls back to "All playlists".
 *
 * Content modes and sections use the same numbers as [PlaylistMode] and [AreaSource]:
 * mode 0 = all, 1 = Live TV only, 2 = movies and series only; section 1 = movies, 2 = series, 3 = Live TV.
 */
object SectionSourceLogic {
    const val MODE_ALL = 0
    const val MODE_LIVE_ONLY = 1
    const val MODE_VOD_ONLY = 2

    const val SECTION_MOVIES = 1
    const val SECTION_SERIES = 2
    const val SECTION_LIVE = 3

    /** Can a playlist with content mode [mode] supply the section [section]? Live TV needs live channels, movies and series need VOD. */
    fun eligible(mode: Int, section: Int): Boolean = when (section) {
        SECTION_LIVE -> mode != MODE_VOD_ONLY
        SECTION_MOVIES, SECTION_SERIES -> mode != MODE_LIVE_ONLY
        else -> false
    }

    /**
     * The playlist indices a section really uses: the selected ones that are eligible.
     * An empty selection, or one without any eligible playlist, means all eligible playlists.
     *
     * @param modes content mode per shown playlist, index = position in the shown list
     * @param selected selected indices (may contain indices that are no longer shown or no longer eligible)
     */
    fun effective(modes: List<Int>, section: Int, selected: Set<Int>): Set<Int> {
        val allEligible = modes.indices.filter { eligible(modes[it], section) }.toSet()
        if (selected.isEmpty()) return allEligible
        val picked = allEligible.filter { it in selected }.toSet()
        return if (picked.isEmpty()) allEligible else picked
    }

    /** Selection after a click on one playlist: add it when missing, remove it when present. */
    fun toggle(selected: Set<Int>, index: Int): Set<Int> = if (index in selected) selected - index else selected + index

    /**
     * Stored selection "no playlist ticked" (maintainer, 23 Nov 2026: every playlist can be unticked, also the last one).
     * Not a playlist key: the hash of a playlist address is never this value in practice, and [effective] ignores it.
     */
    const val NONE = Int.MIN_VALUE

    /** True when every playlist was unticked. */
    fun isNone(selected: Set<Int>): Boolean = NONE in selected && selected.size == 1

    /**
     * Is a playlist ticked in the menu? Only a loaded (shown) playlist that fits the section can be ticked;
     * with "All playlists" (nothing selected) every one of them is ticked, with [NONE] none.
     */
    fun checked(shown: Boolean, eligible: Boolean, key: Int, selected: Set<Int>): Boolean =
        shown && eligible && (selected.isEmpty() || key in selected)

    /** Result of a click on a playlist: the new selection, and whether the playlist must be loaded first. */
    data class Click(val selection: Set<Int>, val load: Boolean)

    /**
     * Click on the playlist [key] in the menu of a section (maintainer decision, 23 Nov 2026: ticking loads it).
     * - Not loaded yet: it is loaded in addition and ticked. From "All" every loaded playlist stays ticked; from [NONE]
     *   only this one is ticked.
     * - Loaded: ticked or unticked. Unticking the last one gives [NONE] (nothing shown);
     *   all loaded playlists ticked is stored as "All" (empty selection).
     * @param eligibleShown keys of the loaded playlists that fit the section
     */
    fun click(selected: Set<Int>, eligibleShown: Set<Int>, key: Int, shown: Boolean): Click {
        val base = when {
            selected.isEmpty() -> eligibleShown
            else -> selected - NONE
        }
        if (!shown) return Click(if (selected.isEmpty()) selected else base + key, load = true)
        val next = toggle(base, key)
        return Click(
            when {
                next.isEmpty() -> setOf(NONE)
                next.containsAll(eligibleShown) -> emptySet()
                else -> next
            },
            load = false
        )
    }

    /** Click on "All playlists": from all ticked to none ticked, otherwise to all (the caller loads the missing ones). */
    fun clickAll(allTicked: Boolean): Set<Int> = if (allTicked) setOf(NONE) else emptySet()

    /** True when the section shows all eligible playlists (nothing selected). */
    fun isAll(selected: Set<Int>): Boolean = selected.isEmpty()

    /**
     * Selection saved by playlist key (hash of the address) instead of the index, so a reordered playlist list
     * keeps the right playlists. Keys of playlists that are gone are dropped when the selection is resolved.
     */
    fun encode(keys: Collection<Int>): String = MultiLogic.encodeKeys(keys)
    fun decode(s: String?): Set<Int> = MultiLogic.decodeKeys(s)
}
