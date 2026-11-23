package com.ipvio.stream

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * Playlists used per section (T-051): movies (1), series (2) and Live TV (3), each with its own selection.
 *
 * The selection is stored as playlist keys ([PlaylistStore.key]), so it survives a change of the shown list.
 * [sources] turns it into source indices (position in [PlaylistStore.sourceNames]) once per change, not per item.
 * An empty selection means "All playlists". Performance: the selection only filters playlists that are already
 * loaded; it never loads one. Nothing is read from the device per title.
 */
object AreaSource {
    /** Raised on every change of a selection (screens that depend on it recompute). */
    var version by mutableIntStateOf(0)
        private set

    private const val PREFS = "ipvio_section"
    /** Result of [sources] when every playlist is unticked: no title belongs to source -1. */
    val NO_SOURCE: Set<Int> = setOf(-1)
    private val keys = HashMap<Int, Set<Int>>()
    private var loaded = false

    private fun load(c: Context) {
        if (loaded) return
        loaded = true
        val p = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        for (t in 1..3) {
            keys[t] = SectionSourceLogic.decode(runCatching { p.getString("sec_$t", null) }.getOrNull())
        }
    }

    /** Playlist keys selected for the section [tab]; empty = all playlists. */
    fun selectedKeys(c: Context, tab: Int): Set<Int> {
        load(c)
        return keys[tab] ?: emptySet()
    }

    /** Saves the selection of the section [tab] (playlist keys; empty = all playlists). */
    fun setKeys(c: Context, tab: Int, newKeys: Set<Int>) {
        load(c)
        keys[tab] = newKeys
        runCatching {
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("sec_$tab", SectionSourceLogic.encode(newKeys)).apply()
        }
        version++
    }

    /** One shown source (k = source index) for the section, or all of them (k < 0). Used by the Live TV "Quelle" row. */
    fun selectSource(c: Context, tab: Int, k: Int) {
        val url = if (k < 0) null else PlaylistStore.shownUrls(c).getOrNull(k)
        setKeys(c, tab, if (url == null) emptySet() else setOf(PlaylistStore.key(url)))
    }

    /**
     * Source indices the section [tab] uses. Empty = no restriction (every shown source counts).
     * Only call it when the shown playlists or the selection change (the screens keep the result in remember).
     */
    fun sources(c: Context, tab: Int): Set<Int> {
        val selected = selectedKeys(c, tab)
        // Every playlist unticked: a source index that no title has, so the section shows nothing (as the menu says)
        if (SectionSourceLogic.isNone(selected)) return NO_SOURCE
        if (PlaylistStore.sourceNames.size < 2) return emptySet()
        if (selected.isEmpty()) return emptySet()
        val urls = PlaylistStore.shownUrls(c)
        if (urls.size != PlaylistStore.sourceNames.size) return emptySet()
        val modes = urls.map { PlaylistMode.get(c, it) }
        val chosen = SectionSourceLogic.effective(modes, tab, urls.indices.filter { PlaylistStore.key(urls[it]) in selected }.toSet())
        return if (chosen.size >= urls.size) emptySet() else chosen
    }

    /**
     * Only titles of the sources [ks] for the area [tab] (1 movies, 2 series); other kinds stay unchanged.
     * Duplicate movies are replaced by the version of one of the sources (like in the title list filter).
     * [ks] empty = no restriction.
     */
    fun filter(items: List<Item>, tab: Int, ks: Set<Int>): List<Item> {
        if (ks.isEmpty() || tab !in 1..2) return items
        val kind = if (tab == 1) Kind.MOVIE else Kind.SERIES
        val seen = HashSet<String>()
        return items.mapNotNull { i ->
            if (i.kind != kind) i
            else (if (ks.any { PlaylistStore.inSource(i, it) }) i else ks.firstNotNullOfOrNull { PlaylistStore.twin(i, it) })
                ?.takeIf { seen.add(it.id) }
        }
    }
}
