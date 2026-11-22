package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadLogicTest {
    private val movie = Item("42", Kind.MOVIE, "A Movie", 2020, "http://x/m.mkv", null, "Group")

    @Test fun keyIsStablePerMovie() = assertEquals("vod:42", DownloadLogic.keyFor(movie))

    @Test fun progressiveFilesAreSupported() {
        assertTrue(DownloadLogic.supported("http://host/movie/u/p/42.mp4"))
        assertTrue(DownloadLogic.supported("https://host/movie/u/p/42.mkv?token=abc"))
    }

    @Test fun playlistsAndOtherSchemesAreNotSupportedYet() {
        assertFalse(DownloadLogic.supported("http://host/movie/u/p/42.m3u8"))
        assertFalse(DownloadLogic.supported("http://host/movie/u/p/42.mpd?x=1"))
        assertFalse(DownloadLogic.supported("file:///storage/42.mp4"))
    }

    @Test fun roomIsCheckedAgainstTheFloor() {
        assertFalse(DownloadLogic.hasRoom(DownloadLogic.MIN_FREE_BYTES - 1))
        assertTrue(DownloadLogic.hasRoom(DownloadLogic.MIN_FREE_BYTES))
    }
}
