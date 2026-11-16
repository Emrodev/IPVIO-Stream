package com.ipvio.stream

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Persistence of sublist definitions per playlist URL.
 *
 * Each playlist URL (identified by its hash, same as [PlaylistStore.key]) may have several
 * named sublists (e.g. "UK channels", "4K movies"). Only the filter definition is stored;
 * the actual filtered list is rebuilt on every playlist load.
 *
 * Stored under the key "sublists" in SharedPreferences "ipvio", as a JSON object mapping
 * urlHash → JSON array of SublistDef encoded forms.
 */
object SublistStore {
    private const val PREFS = "ipvio"
    private const val KEY = "sublists"

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ------------------------------------------------------------------ Pure JSON encoding (testable without Android)

    /** Encode all sublists to the JSON string stored in SharedPreferences. */
    fun encodeAll(all: Map<Int, List<SublistDef>>): String {
        val o = JSONObject()
        all.forEach { (hash, list) ->
            val arr = JSONArray()
            list.forEach { arr.put(it.encode()) }
            o.put(hash.toString(), arr)
        }
        return o.toString()
    }

    /** Decode the JSON string back into per-hash sublists. Invalid JSON yields an empty map. */
    fun decodeAll(json: String?): Map<Int, List<SublistDef>> {
        return runCatching {
            val o = JSONObject(json ?: "{}")
            o.keys().asSequence().mapNotNull { k ->
                val hash = k.toIntOrNull() ?: return@mapNotNull null
                val a = o.optJSONArray(k) ?: return@mapNotNull null
                val list = (0 until a.length()).mapNotNull { i -> SublistDef.decode(a.optString(i)) }
                hash to list
            }.toMap()
        }.getOrDefault(emptyMap())
    }

    // ------------------------------------------------------------------ SharedPreferences persistence

    /** All sublists for one playlist (urlHash), in saved order. */
    fun allFor(c: Context, urlHash: Int): List<SublistDef> {
        return decodeAll(prefs(c).getString(KEY, null)).getOrDefault(urlHash, emptyList())
    }

    /** Save (replace) all sublists for one playlist. */
    fun saveFor(c: Context, urlHash: Int, sublists: List<SublistDef>) {
        val all = decodeAll(prefs(c).getString(KEY, null)).toMutableMap()
        all[urlHash] = sublists
        prefs(c).edit().putString(KEY, encodeAll(all)).apply()
    }

    /** Add or replace one named sublist for a playlist. */
    fun upsert(c: Context, urlHash: Int, def: SublistDef) {
        val list = allFor(c, urlHash)
        val idx = list.indexOfFirst { it.name == def.name }
        val next = if (idx >= 0) list.toMutableList().apply { set(idx, def) } else list + def
        saveFor(c, urlHash, next)
    }

    /** Remove a named sublist. */
    fun remove(c: Context, urlHash: Int, name: String) {
        val next = allFor(c, urlHash).filter { it.name != name }
        saveFor(c, urlHash, next)
    }

    /** The currently active sublist name for a playlist, or null if none. */
    fun active(c: Context, urlHash: Int): String? {
        return prefs(c).getString("sublist_active_$urlHash", null)
    }

    /** Set the active sublist name (null = full playlist). */
    fun setActive(c: Context, urlHash: Int, name: String?) {
        val e = prefs(c).edit()
        if (name == null) e.remove("sublist_active_$urlHash")
        else e.putString("sublist_active_$urlHash", name)
        e.apply()
    }
}
