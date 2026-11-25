package com.ipvio.stream

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-068: the current list stays on screen during a playlist switch only when memory has room for both. */
class SwitchLogicTest {
    private val mb = 1024L * 1024

    @Test fun smallStoredListOnABigHeapKeepsTheScreen() = assertTrue(SwitchLogic.roomForBoth(20 * mb, 400 * mb))

    @Test fun largeStoredListOnASmallHeapEmptiesFirst() = assertFalse(SwitchLogic.roomForBoth(60 * mb, 200 * mb))

    @Test fun noStoredCopyNeverKeepsTheScreen() = assertFalse(SwitchLogic.roomForBoth(0, 4096 * mb))
}
