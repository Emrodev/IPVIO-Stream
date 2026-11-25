package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

/** #151: the parser joins seasons that an Xtream provider lists as separate series, only when asked (pure, no Android). */
class SeasonMergeParseTest {

    private val list = """
        #EXTM3U
        #EXTINF:-1 group-title="Serien",Show S01 S01E01
        http://s.tv:80/series/u/p/11.xtser
        #EXTINF:-1 group-title="Serien",Show S02 S01E01
        http://s.tv:80/series/u/p/12.xtser
        #EXTINF:-1 group-title="Serien",Other Show S01E01
        http://s.tv:80/series/u/p/13.xtser
    """.trimIndent()

    @Test fun off_keepsEverySeasonAsItsOwnSeries_asBefore() {
        val s = M3uParser.parseLines(list.lineSequence()).items.filter { it.kind == Kind.SERIES }
        assertEquals(listOf("s_show s01", "s_show s02", "s_other show"), s.map { it.id })
    }

    @Test fun on_joinsTheSeasonsOfOneShow() {
        val s = M3uParser.parseLines(list.lineSequence(), setOf(LangTagLogic.MERGE_SEASONS)).items.filter { it.kind == Kind.SERIES }
        assertEquals(listOf("s_show", "s_other show"), s.map { it.id })
        val show = s.first()
        assertEquals("Show", show.title)
        assertEquals(listOf(1 to 1, 2 to 1), show.episodes.map { it.season to it.num })
        // Both placeholders are found for loading, each with its season
        assertEquals(listOf(1, 2), XtreamLogic.lazyParts(show).map { it.third })
        assertEquals(listOf("http://s.tv:80/series/u/p/11.xtser", "http://s.tv:80/series/u/p/12.xtser"), XtreamLogic.lazyParts(show).map { it.first })
    }

    @Test fun on_doesNotTouchSeriesWithoutAMarkerOrNormalEpisodes() {
        val m3u = """
            #EXTM3U
            #EXTINF:-1 group-title="Serien",Plain Show S01E01
            http://s.tv/series/u/p/1.mkv
            #EXTINF:-1 group-title="Serien",Plain Show S01E02
            http://s.tv/series/u/p/2.mkv
            #EXTINF:-1 group-title="Serien",Show S02 S01E01
            http://s.tv/series/u/p/3.mkv
        """.trimIndent()
        val s = M3uParser.parseLines(m3u.lineSequence(), setOf(LangTagLogic.MERGE_SEASONS)).items.filter { it.kind == Kind.SERIES }
        // Real files: no change, the title "Show S02" stays as it is
        assertEquals(listOf("s_plain show", "s_show s02"), s.map { it.id })
        assertEquals(2, s.first().episodes.size)
        assertEquals(emptyList<Triple<String, String, Int>>(), XtreamLogic.lazyParts(s.first()))
    }
}
