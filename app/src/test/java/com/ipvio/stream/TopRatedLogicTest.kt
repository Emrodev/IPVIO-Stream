package com.ipvio.stream

import org.junit.Assert.*
import org.junit.Test

class TopRatedLogicTest {
    @Test fun sameTmdbIdKeepsOnlyTheFirstAndBestEntry() {
        val sorted = listOf("The Godfather" to 238, "Der Pate" to 238, "Other" to 1)
        val out = TopRatedLogic.distinctByKey(sorted) { TopRatedLogic.tmdbKey(it.second, false) }
        assertEquals(listOf("The Godfather", "Other"), out.map { it.first })
    }

    @Test fun entriesWithoutTmdbIdAreAllKept() {
        val sorted = listOf("a" to 0, "b" to 0, "c" to 5)
        val out = TopRatedLogic.distinctByKey(sorted) { TopRatedLogic.tmdbKey(it.second, false) }
        assertEquals(listOf("a", "b", "c"), out.map { it.first })
    }

    @Test fun movieAndSeriesWithSameNumberAreNotMerged() {
        assertNotEquals(TopRatedLogic.tmdbKey(42, false), TopRatedLogic.tmdbKey(42, true))
        assertNull(TopRatedLogic.tmdbKey(-1, false))
    }
}
