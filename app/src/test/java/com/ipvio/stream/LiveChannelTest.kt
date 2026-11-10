package com.ipvio.stream

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveChannelTest {
    private fun live(url: String, name: String = "x", group: String = "g") = M3uParser.isLiveChannel(url, name, group)

    @Test fun liveStreamsStayLive() {
        assertTrue(live("stalker://1/live/abcd.ts", group = "24/7 MOVIES"))
        assertTrue(live("http://h:80/live/u/p/12.ts", name = "Friends S01E01", group = "SERIES 24/7"))
        assertTrue(live("http://h/u/p/12", name = "Friends 24/7"))
        assertTrue(live("http://h/u/p/12.ts", group = "24x7 Movies"))
        assertTrue(live("http://h/u/p/12.m3u8", name = "Seinfeld 24-7"))
    }

    @Test fun filmsAndEpisodesStayVod() {
        assertFalse(live("stalker://1/movie/abcd.mp4", group = "24/7 MOVIES"))
        assertFalse(live("http://h/movie/u/p/5.mkv", group = "24/7"))
        assertFalse(live("http://h/series/u/p/5.mp4", name = "Show 24/7 S01E01"))
        assertFalse(live("http://h/files/Film 24-7 (2020).mp4"))
        assertFalse(live("http://h/u/p/12", name = "Film 1247", group = "VOD"))
        assertFalse(live("http://h/u/p/12", name = "Top 24", group = "Movies 7"))
    }
}
