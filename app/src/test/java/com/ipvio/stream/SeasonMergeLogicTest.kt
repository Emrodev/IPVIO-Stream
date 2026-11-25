package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/** #151: seasons that a provider lists as separate series (pure, no Android). */
class SeasonMergeLogicTest {

    @Test fun splitsTheUsualMarkers() {
        assertEquals("Show" to 1, SeasonMergeLogic.split("Show S01"))
        assertEquals("Show" to 2, SeasonMergeLogic.split("Show S2"))
        assertEquals("Show" to 3, SeasonMergeLogic.split("Show - Staffel 3"))
        assertEquals("Show" to 4, SeasonMergeLogic.split("Show Season 04"))
        assertEquals("Show" to 5, SeasonMergeLogic.split("Show (Saison 5)"))
        assertEquals("Show" to 6, SeasonMergeLogic.split("Show.S06"))
        assertEquals("The Walking Dead" to 10, SeasonMergeLogic.split("The Walking Dead | Season 10"))
        assertEquals("Show" to 7, SeasonMergeLogic.split("Show Temporada 7"))
    }

    @Test fun leavesOtherTitlesAlone() {
        assertNull(SeasonMergeLogic.split("Show"))
        assertNull(SeasonMergeLogic.split("Show 2"))
        assertNull(SeasonMergeLogic.split("Show2"))
        assertNull(SeasonMergeLogic.split("Season 2"))
        assertNull(SeasonMergeLogic.split("S02"))
        assertNull(SeasonMergeLogic.split("Show S00"))
        assertNull(SeasonMergeLogic.split("Show S02 Special"))
        assertNull(SeasonMergeLogic.split("Dallas"))
        assertNull(SeasonMergeLogic.split("Boys"))
    }

    @Test fun seasonsOfOneShowGetTheSameBase() {
        val names = listOf("Show S01", "Show S02", "show - Staffel 3")
        val bases = names.map { SeasonMergeLogic.split(it)!!.first.lowercase() }.toSet()
        assertEquals(setOf("show"), bases)
    }

    private fun ep(season: Int, num: Int) = Episode(season, num, "E$num", "http://x/$season/$num", emptyList(), null)

    @Test fun remap_movesEpisodesNumberedFromSeasonOne() {
        val r = SeasonMergeLogic.remap(listOf(ep(1, 1), ep(1, 2)), 3)
        assertEquals(listOf(3, 3), r.map { it.season })
        assertEquals(listOf(1, 2), r.map { it.num })
    }

    @Test fun remap_keepsEpisodesWithTheirOwnSeasonNumbers() {
        val l = listOf(ep(3, 1), ep(3, 2))
        assertSame(l, SeasonMergeLogic.remap(l, 3))
        val mixed = listOf(ep(1, 1), ep(2, 1))
        assertSame(mixed, SeasonMergeLogic.remap(mixed, 2))
        val one = listOf(ep(1, 1))
        assertSame(one, SeasonMergeLogic.remap(one, 1))
    }
}
