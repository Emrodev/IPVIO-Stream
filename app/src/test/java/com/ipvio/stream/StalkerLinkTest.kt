package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StalkerLinkTest {
    private val ep = "http://p:8080/portal.php"
    private val mac = "00:1A:79:AB:CD:EF"

    @Test fun channelsKeepOneRequest() {
        val r = StalkerLink.requests(ep, "itv", "ffrt http://localhost/ch/1_")
        assertEquals(1, r.size)
        assertTrue(r[0].contains("forced_storage=undefined"))
    }

    @Test fun moviesTrySeveralForms() {
        val r = StalkerLink.requests(ep, "vod", "eyJ0eXBlIjoibW92aWUifQ==")
        assertEquals(3, r.size)
        assertTrue(r[0].contains("forced_storage=undefined"))
        assertTrue(r[1].contains("forced_storage=&"))
        assertEquals("$ep?type=vod&action=create_link&cmd=eyJ0eXBlIjoibW92aWUifQ%3D%3D&JsHttpRequest=1-xml", r[2])
        // Episode number goes along in every form
        assertTrue(StalkerLink.requests(ep, "vod", "/media/5.mpg", "3").all { it.contains("&series=3") })
        // Bare file id: also as the video club's own form
        assertTrue(StalkerLink.requests(ep, "vod", "123").last().contains("cmd=%2Fmedia%2Ffile_123.mpg"))
    }

    @Test fun readsTheAddress() {
        assertEquals("http://s/movie/u/p/1.mkv", StalkerLink.address(ep, mac, mapOf("cmd" to "ffmpeg http://s/movie/u/p/1.mkv"), "vod"))
        assertEquals("https://s/a.mp4", StalkerLink.address(ep, mac, mapOf("url" to "https://s/a.mp4", "cmd" to ""), "vod"))
        assertEquals("http://p:8080/play/movie.php?stream=1.mkv&play_token=t",
            StalkerLink.address(ep, mac, mapOf("cmd" to "ffmpeg /play/movie.php?stream=1.mkv&play_token=t"), "vod"))
        // Only id + play token
        assertEquals("http://p:8080/play/movie.php?mac=00%3A1A%3A79%3AAB%3ACD%3AEF&stream=55.mp4&play_token=abc&type=movie",
            StalkerLink.address("http://p:8080/stalker_portal/c/", mac, mapOf("id" to "55", "play_token" to "abc", "cmd" to ""), "vod"))
    }

    @Test fun nothingPlayable() {
        assertNull(StalkerLink.address(ep, mac, mapOf("cmd" to "/media/5.mpg"), "vod"))
        assertNull(StalkerLink.address(ep, mac, mapOf("cmd" to "ffmpeg ", "error" to "nothing_to_play"), "vod"))
        assertNull(StalkerLink.address(ep, mac, mapOf("id" to "55", "play_token" to "abc"), "itv"))
        assertNull(StalkerLink.address(ep, mac, emptyMap(), "vod"))
    }
}
