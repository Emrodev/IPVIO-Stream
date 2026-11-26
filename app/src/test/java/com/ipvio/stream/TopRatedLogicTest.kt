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

    @Test fun providerIdWinsOverCachedId() {
        assertEquals(TopRatedLogic.tmdbKey(238, false), TopRatedLogic.keyOf(238, 999, false))
    }

    @Test fun cachedIdIsTheFallbackWithoutProviderId() {
        assertEquals(TopRatedLogic.tmdbKey(238, false), TopRatedLogic.keyOf(0, 238, false))
        assertNull(TopRatedLogic.keyOf(0, 0, false))
    }

    @Test fun collapseKeepsOneGermanAndOneEnglishTitleOfTheSameFilm() {
        // (title, provider id, cached id): "Der Pate" and "The Godfather" are the same TMDB film
        val list = listOf(Triple("Der Pate", 238, 0), Triple("The Godfather", 238, 0), Triple("Unknown", 0, 0), Triple("Other", 0, 0))
        val out = TopRatedLogic.collapse(list, { it.second }, { it.third }, { false })
        assertEquals(listOf("Der Pate", "Unknown", "Other"), out.map { it.first })
    }
}
