package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-069d: screen fade-in and scrolling times by animation level (0 = off, 1 = subtle, 2 = normal). */
class MotionTimingTest {
    @Test
    fun screenFadeInIsShortAndOffWithoutAnimations() {
        assertEquals(0, ThemeMath.screenEnterMs(0))
        assertEquals(110, ThemeMath.screenEnterMs(1))
        assertEquals(160, ThemeMath.screenEnterMs(2))
        // Before: 220 ms and 380 ms
        assertTrue(ThemeMath.screenEnterMs(1) < 220 && ThemeMath.screenEnterMs(2) < 380)
    }

    @Test
    fun scrollingIsShorterThanBefore() {
        assertTrue(ThemeMath.SCROLL_MS < 280)
        assertEquals(1, ThemeMath.pivotScrollMs(0))
        assertEquals(260, ThemeMath.pivotScrollMs(1))
        assertEquals(260, ThemeMath.pivotScrollMs(2))
    }

    @Test
    fun weakDeviceScrollsShorter() {
        assertEquals(ThemeMath.SCROLL_MS, ThemeMath.scrollMs(false))
        assertTrue(ThemeMath.scrollMs(true) < ThemeMath.SCROLL_MS)
    }
}
