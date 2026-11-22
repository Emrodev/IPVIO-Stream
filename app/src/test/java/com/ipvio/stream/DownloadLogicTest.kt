package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadLogicTest {
    private val movie = Item("42", Kind.MOVIE, "A Movie", 2020, "http://x/m.mkv", null, "Group")
    private val day = 24L * 60 * 60 * 1000

    @Test fun keyIsStablePerMovie() = assertEquals("vod:42", DownloadLogic.keyFor(movie))

    @Test fun episodeKeyUsesSeriesSeasonAndNumber() {
        val series = Item("7", Kind.SERIES, "Show", null, "", null, "Group")
        assertEquals("vod:7:S2E5", DownloadLogic.keyFor(DownloadLogic.episodeItem(series, 2, 5, "Episode 5")))
    }

    @Test fun progressiveHlsAndDashAreSupported() {
        assertTrue(DownloadLogic.supported("http://host/movie/u/p/42.mp4"))
        assertTrue(DownloadLogic.supported("https://host/movie/u/p/42.mkv?token=abc"))
        assertTrue(DownloadLogic.supported("http://host/movie/u/p/42.m3u8"))
        assertTrue(DownloadLogic.supported("http://host/movie/u/p/42.mpd?x=1"))
        assertTrue(DownloadLogic.isDash("http://host/x.mpd?y=1"))
        assertFalse(DownloadLogic.supported("file:///storage/42.mp4"))
    }

    @Test fun roomIsCheckedAgainstTheFloor() {
        assertFalse(DownloadLogic.hasRoom(DownloadLogic.MIN_FREE_BYTES - 1))
        assertTrue(DownloadLogic.hasRoom(DownloadLogic.MIN_FREE_BYTES))
    }

    @Test fun autoDeleteOnlyAfterTheChosenDays() {
        val now = 100 * day
        assertFalse(DownloadLogic.expired(now - 40 * day, now, 0))      // off
        assertFalse(DownloadLogic.expired(now - 20 * day, now, 30))     // within 30 days
        assertTrue(DownloadLogic.expired(now - 40 * day, now, 30))      // older than 30 days
    }
}
