package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

/** #22: "Neu hinzugefügt" sorts by the provider's added date, falls back to the old order without dates. */
class RecentSortLogicTest {

    private data class T(val name: String, val added: Long)

    @Test
    fun withDates_newestFirst() {
        val list = listOf(T("a", 100), T("b", 300), T("c", 200))
        assertEquals(listOf("b", "c", "a"), RecentSortLogic.newestFirst(list) { it.added }.map { it.name })
    }

    @Test
    fun withoutDates_keepsOldOrderReversed() {
        val list = listOf(T("a", 0), T("b", 0), T("c", 0))
        assertEquals(listOf("c", "b", "a"), RecentSortLogic.newestFirst(list) { it.added }.map { it.name })
    }

    @Test
    fun mixedDates_unknownDatesGoLast() {
        val list = listOf(T("a", 0), T("b", 50), T("c", 0))
        // Stable sort: the entries without a date keep their order among themselves
        assertEquals(listOf("b", "a", "c"), RecentSortLogic.newestFirst(list) { it.added }.map { it.name })
    }

    @Test
    fun newestN_matchesTheFirstEntriesOfNewestFirst() {
        // Not sorted by date, as with a provider that does not list by added date (#125)
        val list = (1..200).map { T("t$it", ((it * 37) % 101).toLong() + 1) }
        for (n in listOf(1, 5, 40, 200, 500)) {
            assertEquals(
                RecentSortLogic.newestFirst(list) { it.added }.take(n).map { it.name },
                RecentSortLogic.newestN(list, n) { it.added }.map { it.name }
            )
        }
    }

    @Test
    fun newestN_equalDates_keepPlaylistOrder() {
        val list = listOf(T("a", 5), T("b", 5), T("c", 5), T("d", 9))
        assertEquals(listOf("d", "a"), RecentSortLogic.newestN(list, 2) { it.added }.map { it.name })
    }

    @Test
    fun newestN_withoutDates_isLastEntriesReversed() {
        val list = listOf(T("a", 0), T("b", 0), T("c", 0), T("d", 0))
        assertEquals(listOf("d", "c"), RecentSortLogic.newestN(list, 2) { it.added }.map { it.name })
        assertEquals(emptyList<String>(), RecentSortLogic.newestN(list, 0) { it.added }.map { it.name })
    }
}
