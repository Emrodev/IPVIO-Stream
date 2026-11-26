package com.ipvio.stream

/**
 * T-069: the last results of a pass over a large list (thousands of channels or titles), kept for the next visit of a
 * screen. An entry is found again by the identity of its input (the same list object, `===`) and an optional extra key
 * (compared with `equals`). A screen that is left and opened again gets the same result object back instead of
 * repeating the pass on the main thread, and caches further down that also compare by identity (for example
 * `LiveGenre.split`) keep working. Thread safe. Pure logic, no Android imports, covered by JVM tests.
 *
 * Holds at most [max] entries; the oldest one is dropped first. [clear] frees everything (called when memory is short).
 */
class LastResults<V : Any>(private val max: Int = 1) {
    private class Entry<V>(val input: Any, val extra: Any?, val value: V)

    private val entries = ArrayList<Entry<V>>(max)

    /** The stored result for [input] and [extra], or the result of [build], which is then stored. */
    fun get(input: Any, extra: Any? = null, build: () -> V): V {
        synchronized(this) { find(input, extra)?.let { return it } }
        // Built outside the lock: a long pass must not block other screens that only read
        val v = build()
        synchronized(this) {
            // Another thread was faster: keep one object, so identity checks further down still match
            find(input, extra)?.let { return it }
            entries.add(0, Entry(input, extra, v))
            while (entries.size > max) entries.removeAt(entries.size - 1)
        }
        return v
    }

    /** The stored result for [input] and [extra], without building it. */
    fun peek(input: Any, extra: Any? = null): V? = synchronized(this) { find(input, extra) }

    fun clear() = synchronized(this) { entries.clear() }

    private fun find(input: Any, extra: Any?): V? = entries.firstOrNull { it.input === input && it.extra == extra }?.value
}

object ScreenCacheLogic {
    /**
     * The items whose id is in [ids], by id. Same result as `items.associateBy(idOf)` restricted to [ids] (an id that occurs
     * twice in [items] gives its last item), but one pass that keeps only the wanted ids instead of a map of every item.
     */
    fun <T : Any> byIds(ids: Collection<String>, items: List<T>, idOf: (T) -> String): Map<String, T> {
        if (ids.isEmpty()) return emptyMap()
        val want = ids as? Set<String> ?: HashSet(ids)
        val found = HashMap<String, T>(want.size * 2)
        for (x in items) {
            val k = idOf(x)
            if (k in want) found[k] = x
        }
        return found
    }

    /** The items with the ids [ids], in the order of [ids]; ids without an item are skipped. Same as `ids.mapNotNull { items.associateBy(idOf)[it] }`. */
    fun <T : Any> inOrder(ids: List<String>, items: List<T>, idOf: (T) -> String): List<T> {
        if (ids.isEmpty()) return emptyList()
        val found = byIds(ids, items, idOf)
        return ids.mapNotNull { found[it] }
    }
}
