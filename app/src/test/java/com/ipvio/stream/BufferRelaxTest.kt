package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-069e: the automatic buffer steps down again after clean playbacks. */
class BufferRelaxTest {
    private fun after(mode: Int, learned: Int, clean: Int, ok: Boolean) = BufferLogic.afterPlayback(mode, learned, clean, ok).toList()

    @Test
    fun fiveCleanPlaybacksLowerTheLearnedLevelByOne() {
        var learned = 2
        var count = 0
        repeat(BufferLogic.RELAX_AFTER - 1) {
            val r = BufferLogic.afterPlayback(0, learned, count, true)
            learned = r[0]; count = r[1]
        }
        assertEquals(2, learned)
        assertEquals(BufferLogic.RELAX_AFTER - 1, count)
        assertEquals(listOf(1, 0), after(0, learned, count, true))
    }

    @Test
    fun shortPlaybacksNeitherCountNorReset() {
        assertEquals(listOf(2, 3), after(0, 2, 3, false))
    }

    @Test
    fun levelZeroAndFixedModesKeepTheirLevel() {
        assertEquals(listOf(0, 0), after(0, 0, 4, true))
        for (mode in 1..3) assertEquals(listOf(1, 0), after(mode, 1, 2, true))
    }

    @Test
    fun cleanNeedsReadyNoStutterAndTwoMinutes() {
        val t0 = 1_000_000L
        assertTrue(BufferLogic.isClean(t0, t0 + BufferLogic.CLEAN_MS, false))
        assertFalse(BufferLogic.isClean(t0, t0 + BufferLogic.CLEAN_MS - 1, false))
        assertFalse(BufferLogic.isClean(t0, t0 + 10 * BufferLogic.CLEAN_MS, true))
        assertFalse(BufferLogic.isClean(0L, t0, false))
    }

    @Test
    fun stutterStillRaisesTheLevel() {
        assertEquals(1, BufferLogic.bump(0, 0))
        assertEquals(2, BufferLogic.bump(0, 2))
        // A learned level adds 2 s before the start, as before
        val base = intArrayOf(10_000, 25_000, 1_000, 5_000, -1)
        assertEquals(3_000, BufferLogic.scale(base, 1)[2])
    }
}
