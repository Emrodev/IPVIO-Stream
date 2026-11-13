package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

class TapSeekLogicTest {
    @Test fun tapMapsPositionToTime() {
        assertEquals(50_000L, TapSeekLogic.target(500f, 1000, 100_000L))
        assertEquals(0L, TapSeekLogic.target(0f, 1000, 100_000L))
    }

    @Test fun outsideTheBarIsClampedAndEndKeepsOneSecond() {
        assertEquals(0L, TapSeekLogic.target(-20f, 1000, 100_000L))
        assertEquals(99_000L, TapSeekLogic.target(1000f, 1000, 100_000L))
        assertEquals(99_000L, TapSeekLogic.target(1500f, 1000, 100_000L))
    }

    @Test fun nothingToSeekWithoutDurationOrWidth() {
        assertEquals(-1L, TapSeekLogic.target(10f, 1000, 0L))
        assertEquals(-1L, TapSeekLogic.target(10f, 0, 100_000L))
    }
}
