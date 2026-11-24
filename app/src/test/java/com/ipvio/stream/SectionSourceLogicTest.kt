package com.ipvio.stream

import com.ipvio.stream.SectionSourceLogic.MODE_ALL
import com.ipvio.stream.SectionSourceLogic.MODE_LIVE_ONLY
import com.ipvio.stream.SectionSourceLogic.MODE_VOD_ONLY
import com.ipvio.stream.SectionSourceLogic.SECTION_LIVE
import com.ipvio.stream.SectionSourceLogic.SECTION_MOVIES
import com.ipvio.stream.SectionSourceLogic.SECTION_SERIES
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SectionSourceLogicTest {
    // Five playlists: 0 and 1 are "Live TV only", 2 and 3 are "movies and series only", 4 has everything.
    private val modes = listOf(MODE_LIVE_ONLY, MODE_LIVE_ONLY, MODE_VOD_ONLY, MODE_VOD_ONLY, MODE_ALL)

    @Test fun liveTvOnlyPlaylistIsNotEligibleForMovies() {
        assertFalse(SectionSourceLogic.eligible(MODE_LIVE_ONLY, SECTION_MOVIES))
        assertFalse(SectionSourceLogic.eligible(MODE_LIVE_ONLY, SECTION_SERIES))
        assertTrue(SectionSourceLogic.eligible(MODE_LIVE_ONLY, SECTION_LIVE))
    }

    @Test fun vodOnlyPlaylistIsNotEligibleForLiveTv() {
        assertFalse(SectionSourceLogic.eligible(MODE_VOD_ONLY, SECTION_LIVE))
        assertTrue(SectionSourceLogic.eligible(MODE_VOD_ONLY, SECTION_MOVIES))
        assertTrue(SectionSourceLogic.eligible(MODE_VOD_ONLY, SECTION_SERIES))
    }

    @Test fun allModePlaylistIsEligibleEverywhere() {
        assertTrue(SectionSourceLogic.eligible(MODE_ALL, SECTION_LIVE))
        assertTrue(SectionSourceLogic.eligible(MODE_ALL, SECTION_MOVIES))
        assertTrue(SectionSourceLogic.eligible(MODE_ALL, SECTION_SERIES))
    }

    @Test fun emptySelectionMeansAllEligiblePlaylists() {
        assertEquals(setOf(0, 1, 4), SectionSourceLogic.effective(modes, SECTION_LIVE, emptySet()))
        assertEquals(setOf(2, 3, 4), SectionSourceLogic.effective(modes, SECTION_MOVIES, emptySet()))
    }

    @Test fun selectionIsLimitedToEligiblePlaylists() {
        // Selecting a movies-only playlist for Live TV is ignored; the Live TV one stays
        assertEquals(setOf(0), SectionSourceLogic.effective(modes, SECTION_LIVE, setOf(0, 2)))
    }

    @Test fun selectionOfOnlyIneligiblePlaylistsFallsBackToAll() {
        assertEquals(setOf(0, 1, 4), SectionSourceLogic.effective(modes, SECTION_LIVE, setOf(2, 3)))
    }

    @Test fun indicesNoLongerShownAreIgnored() {
        assertEquals(setOf(2, 3, 4), SectionSourceLogic.effective(modes, SECTION_MOVIES, setOf(9)))
    }

    @Test fun toggleAddsAndRemoves() {
        val s1 = SectionSourceLogic.toggle(emptySet(), 1)
        assertEquals(setOf(1), s1)
        assertEquals(setOf(1, 4), SectionSourceLogic.toggle(s1, 4))
        assertEquals(emptySet<Int>(), SectionSourceLogic.toggle(s1, 1))
    }

    @Test fun isAllOnlyWhenNothingSelected() {
        assertTrue(SectionSourceLogic.isAll(emptySet()))
        assertFalse(SectionSourceLogic.isAll(setOf(0)))
    }

    @Test fun selectionRoundTripsThroughKeys() {
        val text = SectionSourceLogic.encode(listOf(42, -7, 1000))
        assertEquals(setOf(42, -7, 1000), SectionSourceLogic.decode(text))
        assertEquals(emptySet<Int>(), SectionSourceLogic.decode(null))
    }

    // Menu clicks (maintainer decision 23 Nov 2026: ticking a playlist that is not loaded loads it in addition)

    @Test fun allSelectedTicksEveryLoadedFittingPlaylist() {
        assertTrue(SectionSourceLogic.checked(shown = true, eligible = true, key = 5, selected = emptySet()))
        assertFalse(SectionSourceLogic.checked(shown = false, eligible = true, key = 5, selected = emptySet()))
        assertFalse(SectionSourceLogic.checked(shown = true, eligible = false, key = 5, selected = emptySet()))
        assertFalse(SectionSourceLogic.checked(shown = true, eligible = true, key = 5, selected = setOf(6)))
    }

    @Test fun clickOnPlaylistNotLoadedLoadsItAndKeepsAll() {
        assertEquals(SectionSourceLogic.Click(emptySet(), load = true), SectionSourceLogic.click(emptySet(), setOf(1), key = 2, shown = false))
    }

    @Test fun clickOnPlaylistNotLoadedAddsItToAPartialSelection() {
        assertEquals(SectionSourceLogic.Click(setOf(1, 2), load = true), SectionSourceLogic.click(setOf(1), setOf(1, 3), key = 2, shown = false))
    }

    @Test fun untickFromAllKeepsTheOthers() {
        assertEquals(SectionSourceLogic.Click(setOf(1), load = false), SectionSourceLogic.click(emptySet(), setOf(1, 3), key = 3, shown = true))
    }

    @Test fun lastTickedPlaylistCanBeUntickedToNone() {
        val none = setOf(SectionSourceLogic.NONE)
        assertEquals(SectionSourceLogic.Click(none, load = false), SectionSourceLogic.click(emptySet(), setOf(1), key = 1, shown = true))
        assertEquals(SectionSourceLogic.Click(none, load = false), SectionSourceLogic.click(setOf(3), setOf(1, 3), key = 3, shown = true))
        assertTrue(SectionSourceLogic.isNone(none))
        assertFalse(SectionSourceLogic.isNone(emptySet()))
    }

    @Test fun fromNoneTickingOneLoadedPlaylistSelectsOnlyIt() {
        val none = setOf(SectionSourceLogic.NONE)
        assertEquals(SectionSourceLogic.Click(setOf(3), load = false), SectionSourceLogic.click(none, setOf(1, 3), key = 3, shown = true))
    }

    @Test fun fromNoneTickingAPlaylistNotLoadedSelectsOnlyItAndLoadsIt() {
        val none = setOf(SectionSourceLogic.NONE)
        assertEquals(SectionSourceLogic.Click(setOf(2), load = true), SectionSourceLogic.click(none, setOf(1), key = 2, shown = false))
    }

    @Test fun noneTicksNothing() {
        assertFalse(SectionSourceLogic.checked(shown = true, eligible = true, key = 1, selected = setOf(SectionSourceLogic.NONE)))
    }

    @Test fun allButtonTogglesBetweenAllAndNone() {
        assertEquals(setOf(SectionSourceLogic.NONE), SectionSourceLogic.clickAll(allTicked = true))
        assertEquals(emptySet<Int>(), SectionSourceLogic.clickAll(allTicked = false))
    }

    @Test fun noneIsIgnoredWhenResolvingIndices() {
        // effective() never sees NONE as a playlist; AreaSource turns NONE into "no source" before
        assertEquals(setOf(0, 1, 4), SectionSourceLogic.effective(modes, SECTION_LIVE, setOf(SectionSourceLogic.NONE)))
    }

    @Test fun tickingTheLastMissingPlaylistBecomesAll() {
        assertEquals(SectionSourceLogic.Click(emptySet(), load = false), SectionSourceLogic.click(setOf(1), setOf(1, 3), key = 3, shown = true))
    }
}
