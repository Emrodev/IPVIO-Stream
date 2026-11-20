package com.ipvio.stream

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** #102: live addresses are never movies (pure, no Android). */
class LiveUrlLogicTest {

    @Test
    fun xtreamLiveAddress_isLive() {
        assertTrue(LiveUrlLogic.isLiveUrl("http://provider.example:8080/live/user/pass/123.ts"))
    }

    @Test
    fun stalkerLiveAddress_isLive() {
        assertTrue(LiveUrlLogic.isLiveUrl("stalker://portal.example/live/abc"))
    }

    @Test
    fun movieAndSeriesAddresses_areNotLive() {
        assertFalse(LiveUrlLogic.isLiveUrl("http://provider.example:8080/movie/user/pass/45.mkv"))
        assertFalse(LiveUrlLogic.isLiveUrl("http://provider.example:8080/series/user/pass/9.mp4"))
    }

    @Test
    fun plainM3uAddress_withoutLiveSegment_isNotLive() {
        assertFalse(LiveUrlLogic.isLiveUrl("http://provider.example/stream/123"))
    }
}
