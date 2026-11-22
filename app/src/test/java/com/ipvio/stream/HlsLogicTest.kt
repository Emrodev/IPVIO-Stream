package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HlsLogicTest {
    private val base = "http://host/movie/u/p/42.m3u8"
    private val master = """
        #EXTM3U
        #EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080
        high/index.m3u8
        #EXT-X-STREAM-INF:BANDWIDTH=1200000,RESOLUTION=854x480
        https://cdn.example.net/low.m3u8
    """.trimIndent()

    @Test fun variantsKeepFileOrderWithLabelsAndAbsoluteAddresses() {
        val v = HlsLogic.variants(master, base)
        assertEquals(2, v.size)
        assertEquals("1080p · 5.0 Mbit/s", v[0].label)
        assertEquals("http://host/movie/u/p/high/index.m3u8", v[0].uri)
        assertEquals("480p · 1.2 Mbit/s", v[1].label)
        assertEquals("https://cdn.example.net/low.m3u8", v[1].uri)
    }

    @Test fun mediaPlaylistHasNoVariants() {
        assertTrue(HlsLogic.variants("#EXTM3U\n#EXTINF:10,\nseg1.ts\n", base).isEmpty())
    }

    @Test fun bitrateOnlyWhenResolutionIsMissing() {
        assertEquals("2.5 Mbit/s", HlsLogic.label(null, 2_500_000))
    }

    @Test fun hlsIsDetectedWithoutQuery() {
        assertTrue(HlsLogic.isHls("http://h/x.m3u8?token=1"))
        assertFalse(HlsLogic.isHls("http://h/x.mp4"))
    }
}
