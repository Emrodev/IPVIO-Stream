package com.ipvio.stream

import com.ipvio.stream.ProgSearchLogic.Mode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgSearchLogicTest {
    private val now = 10_000L

    private fun hit(title: String, start: Long, end: Long, channel: String = "Sport 1") =
        ProgSearchLogic.Hit("id-$channel", channel, title, start, end)

    @Test fun spacesAndCaseAreIgnored() {
        assertTrue(ProgSearchLogic.matches("Formula 1: Practice", "formula1"))
        assertTrue(ProgSearchLogic.matches("Formula 1: Practice", "Formula 1"))
        assertTrue(ProgSearchLogic.matches("Formula 1: Practice", "FORMULA"))
        assertFalse(ProgSearchLogic.matches("Tennis Open", "formula"))
    }

    @Test fun tooShortOrBlankQueryNeverMatches() {
        assertFalse(ProgSearchLogic.matches("Formula 1", "f"))
        assertFalse(ProgSearchLogic.matches("Formula 1", "   "))
        assertFalse(ProgSearchLogic.matches("Formula 1", ""))
    }

    @Test fun liveKeepsOnlyShowsRunningNow() {
        val hits = listOf(hit("Running", 9_000, 11_000), hit("Starts later", 11_000, 12_000), hit("Ended", 1_000, 9_000))
        assertEquals(listOf("Running"), ProgSearchLogic.select(hits, now, Mode.LIVE).map { it.title })
    }

    @Test fun upcomingKeepsLaterShowsWithinTheNext24Hours() {
        val hits = listOf(
            hit("Soon", 11_000, 12_000),
            hit("Too late", now + ProgSearchLogic.UPCOMING_SPAN_SEC + 1, now + ProgSearchLogic.UPCOMING_SPAN_SEC + 2),
            hit("Running", 9_000, 11_000)
        )
        assertEquals(listOf("Soon"), ProgSearchLogic.select(hits, now, Mode.UPCOMING).map { it.title })
    }

    @Test fun resultsAreSortedByStartThenChannel() {
        val hits = listOf(hit("B", 11_000, 12_000, "Sport 2"), hit("A", 10_500, 12_000, "Sport 1"), hit("C", 11_000, 12_000, "Sport 1"))
        assertEquals(listOf("A", "C", "B"), ProgSearchLogic.select(hits, now, Mode.UPCOMING).map { it.title })
    }

    @Test fun resultsAreLimited() {
        val hits = (1..60).map { hit("Show $it", 11_000L + it, 12_000L) }
        assertEquals(ProgSearchLogic.LIMIT, ProgSearchLogic.select(hits, now, Mode.UPCOMING).size)
    }
}
