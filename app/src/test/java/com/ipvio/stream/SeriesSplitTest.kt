package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-067: series of the same name that are different series are no longer merged into one. */
class SeriesSplitTest {
    private fun series(m3u: String) = M3uParser.parse(m3u.trimIndent()).filter { it.kind == Kind.SERIES }

    @Test fun placeholdersInDifferentCategoriesStaySeparate() {
        val s = series("""
            #EXTM3U
            #EXTINF:-1 group-title="Netflix DE",Dark S01E01
            http://h/series/u/p/5.xtser
            #EXTINF:-1 group-title="Netflix EN",Dark S01E01
            http://h/series/u/p/6.xtser
        """)
        assertEquals(2, s.size)
        assertEquals(setOf("Netflix DE", "Netflix EN"), s.map { it.group }.toSet())
        // the first one keeps the old ID, so resume and My list stay with it
        assertEquals("s_dark", s.first { it.group == "Netflix DE" }.id)
        assertTrue(s.all { it.title == "Dark" })
    }

    @Test fun differentYearsStaySeparateWithTheirYear() {
        val s = series("""
            #EXTM3U
            #EXTINF:-1 group-title="Serien",Battlestar Galactica (1978) S01E01
            http://h/series/u/p/3.xtser
            #EXTINF:-1 group-title="Serien",Battlestar Galactica (2004) S01E01
            http://h/series/u/p/4.xtser
        """)
        assertEquals(2, s.size)
        // the first one stays exactly as before (ID, title, no year); the one split off gets its year
        assertEquals("s_battlestar galactica", s[0].id)
        assertNull(s[0].year)
        assertEquals("Battlestar Galactica", s[0].title)
        assertEquals(2004, s[1].year)
    }

    @Test fun differentCountriesStaySeparateWithTheCountryInTheTitle() {
        val s = series("""
            #EXTM3U
            #EXTINF:-1 group-title="Serien",The Office (US) S01E01
            http://h/series/u/p/1.xtser
            #EXTINF:-1 group-title="Serien",The Office (UK) S01E01
            http://h/series/u/p/2.xtser
        """)
        assertEquals(listOf("The Office", "The Office (UK)"), s.map { it.title })
        assertEquals("s_the office", s[0].id)
    }

    @Test fun episodesOfOneSeriesInSeveralCategoriesStillMerge() {
        // Plain M3U with episodes: some providers put each season into its own category; that stays one series
        val s = series("""
            #EXTM3U
            #EXTINF:-1 group-title="Dark Staffel 1",Dark S01E01
            http://h/dark/s1e1.mp4
            #EXTINF:-1 group-title="Dark Staffel 2",Dark S02E01
            http://h/dark/s2e1.mp4
        """)
        assertEquals(1, s.size)
        assertEquals("s_dark", s[0].id)
        assertEquals(2, s[0].episodes.size)
        assertNull(s[0].year)
    }

    @Test fun qualityVariantsWithoutYearStillMerge() {
        val s = series("""
            #EXTM3U
            #EXTINF:-1 group-title="Serien",Dark S01E01
            http://h/dark/hd/s1e1.mp4
            #EXTINF:-1 group-title="Serien",Dark 4K S01E01
            http://h/dark/4k/s1e1.mp4
        """)
        assertEquals(1, s.size)
        assertEquals(2, s[0].episodes[0].variants.size)
    }

    @Test fun onlyPlaceholdersAreSplit() {
        assertFalse(SeriesKeyLogic.differs("G", "2004", true, "G", null, true))
        assertTrue(SeriesKeyLogic.differs("G", "2004", true, "G", "1978", true))
        assertTrue(SeriesKeyLogic.differs("A", null, true, "B", null, true))
        // episode lines (plain M3U): never split, as before
        assertFalse(SeriesKeyLogic.differs("A", null, false, "B", null, false))
        assertFalse(SeriesKeyLogic.differs("G", "2004", false, "G", "1978", false))
    }

    @Test fun plainM3uWithTheSeasonYearInTheNameStaysOneSeries() {
        val s = series("""
            #EXTM3U
            #EXTINF:-1 group-title="Serien",The Crown (2016) S01E01
            http://h/crown/s1e1.mp4
            #EXTINF:-1 group-title="Serien",The Crown (2017) S02E01
            http://h/crown/s2e1.mp4
        """)
        assertEquals(1, s.size)
        assertEquals("s_the crown", s[0].id)
        assertEquals(2, s[0].episodes.size)
    }

    @Test fun distinguisherAndPlaceholder() {
        assertEquals("2004", SeriesKeyLogic.distinguisher("Battlestar Galactica (2004)"))
        assertEquals("UK", SeriesKeyLogic.distinguisher("The Office [uk]"))
        assertNull(SeriesKeyLogic.distinguisher("Dark (DE)"))
        assertTrue(SeriesKeyLogic.isPlaceholder("http://h/series/u/p/1.xtser"))
        assertTrue(SeriesKeyLogic.isPlaceholder("stalker://1/series/abc.ser"))
        assertFalse(SeriesKeyLogic.isPlaceholder("http://h/series/u/p/1.mp4"))
        assertEquals("dark~2004~g" + Integer.toHexString("netflix en".hashCode()), SeriesKeyLogic.altKey("dark", "Netflix EN", "2004", true))
    }

    @Test fun tmdbSearchIgnoresTheAddedCountry() {
        assertEquals("The Office", MatchLogic.searchTitle("The Office (UK)"))
        assertEquals("Doctor Who", MatchLogic.searchTitle("Doctor Who"))
    }

    @Test fun parseReportCountsWhatIsLeftOut() {
        val p = M3uParser.parseStream("""
            #EXTM3U
            #EXTINF:-1 group-title="DE | Sport",### SPORT ###
            http://h/live/u/p/1.ts
            #EXTINF:-1 group-title="DE | Sport",Sky Sport 1
            http://h/live/u/p/2.ts
            #EXTINF:-1 group-title="EN | Sport",Sky Sports Main Event
            http://h/live/u/p/3.ts
            #EXTINF:-1 group-title="DE | Filme",Film (2020)
            http://h/movie/u/p/4.mp4
        """.trimIndent().reader(), hidden = setOf("en", LangTagLogic.NO_VOD))
        assertEquals(4, p.report.entries)
        assertEquals(1, p.report.separators)
        assertEquals(1, p.report.hiddenLang)
        assertEquals(1, p.report.hiddenMode)
        assertEquals(1, p.items.size)
        val line = LoadReportLogic.line("h", p.report, 1, 0, 0, 1, 1, listOf("en"))
        assertTrue(line, line.contains("4 entries") && line.contains("1 by the language filter (en)") && line.contains("1 by the content mode"))
    }
}
