package com.ipvio.stream

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Private categories (T-028), per profile. Content of a private category shows only inside that category: it is not
 * recorded ([History], [WatchStore], [EpProgress]) and not shown in "All …", genre lists, search or the home rows.
 * Rules: [PrivateLogic].
 */
object PrivateCats {
    private const val PREFS = "ipvio_privatecats"
    /** Bumped on every change; screens use it as a key to rebuild their lists. */
    var version by mutableIntStateOf(0)
        private set
    /** Parental-flagged categories count as private too (off by default). */
    var parental by mutableStateOf(false)
        private set
    /** Read from background threads, so plain immutable snapshots instead of Compose state lists. */
    @Volatile var keys: Set<String> = emptySet()
        private set
    @Volatile private var ids: Set<String> = emptySet()
    @Volatile private var lastAll: List<Item>? = null
    @Volatile private var lastVer = -1
    @Volatile private var lastVisible: List<Item> = emptyList()
    private var inited = false

    private fun prefs(c: Context) = Profiles.sp(c, PREFS)

    fun init(c: Context) {
        if (inited) return
        inited = true
        val p = prefs(c)
        keys = PrivateLogic.decode(p.getString("k", null))
        parental = p.getBoolean("parental", false)
    }

    val active: Boolean get() = keys.isNotEmpty() || parental

    fun isPrivate(item: Item): Boolean = active && PrivateLogic.isPrivate(item, keys, parental)
    fun isPrivate(kind: Kind, group: String): Boolean = PrivateLogic.key(kind, group) in keys
    /** For writers that only know the id (history, resume); filled by [refresh] after each playlist load. */
    fun isPrivateId(id: String): Boolean = id in ids

    /** [all] without private items. The result is cached, so callers that key caches by list identity keep working. */
    fun visible(all: List<Item>): List<Item> {
        if (!active) return all
        val v = version
        if (all === lastAll && v == lastVer) return lastVisible
        return PrivateLogic.visible(all, keys, parental).also { lastAll = all; lastVer = v; lastVisible = it }
    }

    /** Recomputes the private ids for the loaded playlist (background thread). */
    fun refresh(all: List<Item>) { ids = PrivateLogic.ids(all, keys, parental) }

    /** Marks a category private (or not). Marking also removes its titles from history and continue watching. */
    fun set(c: Context, kind: Kind, group: String, on: Boolean) {
        init(c)
        val k = PrivateLogic.key(kind, group)
        keys = if (on) keys + k else keys - k
        prefs(c).edit().putString("k", PrivateLogic.encode(keys)).apply()
        changed(c, on)
    }

    fun setParental(c: Context, on: Boolean) {
        init(c)
        parental = on
        prefs(c).edit().putBoolean("parental", on).apply()
        changed(c, on)
    }

    private fun changed(c: Context, purge: Boolean) {
        refresh(PlaylistStore.channels)
        version++
        if (purge && ids.isNotEmpty()) {
            History.removeAll(c, ids)
            WatchStore.removeAll(c, ids)
        }
    }
}

/** Menu action "Privat machen" / "Nicht mehr privat" for one category, with a short confirmation. */
internal fun privateToggle(c: Context, kind: Kind, group: String) {
    val on = !PrivateCats.isPrivate(kind, group)
    PrivateCats.set(c, kind, group, on)
    android.widget.Toast.makeText(
        c, Lang.tr(if (on) "Kategorie ist privat: kein Verlauf, nur in dieser Kategorie sichtbar." else "Kategorie ist nicht mehr privat."),
        android.widget.Toast.LENGTH_LONG
    ).show()
}
