package com.ipvio.stream

/**
 * "Neu hinzugefügt" (#22): newest first by the provider's added date. Without any date (older caches, M3U lists)
 * the list keeps the old order (last entry of the playlist first), so nothing changes for those playlists.
 * Pure logic, no Android, testable.
 */
object RecentSortLogic {
    fun <T> newestFirst(items: List<T>, added: (T) -> Long): List<T> =
        if (items.any { added(it) > 0L }) items.sortedByDescending(added) else items.asReversed()

    /**
     * The [n] newest items, in the same order as [newestFirst] (#125: the home row must show the titles that the
     * "Neu hinzugefügt" page lists first). One pass with a small heap, because the list can hold tens of thousands of titles.
     * Without any date: the last [n] entries, last first (as before).
     */
    fun <T> newestN(items: List<T>, n: Int, added: (T) -> Long): List<T> {
        if (n <= 0) return emptyList()
        if (items.none { added(it) > 0L }) return items.takeLast(n).asReversed()
        // The head of the queue is the weakest candidate: the oldest one, and among equal dates the later one in the list
        val heap = java.util.PriorityQueue<Int>(n + 1) { a, b ->
            val c = added(items[a]).compareTo(added(items[b]))
            if (c != 0) c else b.compareTo(a)
        }
        for (i in items.indices) { heap.add(i); if (heap.size > n) heap.poll() }
        return heap.sortedWith { a, b ->
            val c = added(items[b]).compareTo(added(items[a]))
            if (c != 0) c else a.compareTo(b)
        }.map { items[it] }
    }
}
