package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** T-062: rewinding a catch-up channel finds the show and the offset inside it (pure, no Android). */
class CatchupSeekLogicTest {

    private fun show(title: String, start: Long, end: Long) = EpgEntry(title, "", start, end)

    @Test
    fun target_isStepBeforeNow() {
        assertEquals(970L, CatchupSeekLogic.target(1000L, 30L))
        assertEquals(999L, CatchupSeekLogic.target(1000L, 0L))   // a step of zero still moves one second
    }

    @Test
    fun pick_findsTheRunningShowAndTheOffset() {
        val list = listOf(show("A", 0, 1800), show("B", 1800, 3600), show("C", 3600, 5400))
        val p = CatchupSeekLogic.pick(list, 2000L)
        assertNotNull(p)
        assertEquals("B", p!!.show.title)
        assertEquals(200L, p.offsetSec)
    }

    @Test
    fun pick_goesBackIntoThePreviousShow() {
        val list = listOf(show("A", 0, 1800), show("B", 1800, 3600))
        // Now is 1810 and the step is 300 s: the target 1510 is in the previous show
        val p = CatchupSeekLogic.pick(list, CatchupSeekLogic.target(1810L, 300L))
        assertEquals("A", p!!.show.title)
        assertEquals(1510L, p.offsetSec)
    }

    @Test
    fun pick_endOfShowBelongsToTheNext_andGapsGiveNull() {
        val list = listOf(show("A", 0, 1800), show("B", 2000, 3600))
        assertNull(CatchupSeekLogic.pick(list, 1800L))
        assertEquals("B", CatchupSeekLogic.pick(list, 2000L)!!.show.title)
        assertNull(CatchupSeekLogic.pick(emptyList(), 100L))
    }

    @Test
    fun pick_ignoresBrokenEntries_andOverlapTakesTheLaterStart() {
        val list = listOf(show("bad", 500, 500), show("A", 0, 2000), show("B", 1000, 3000))
        assertEquals("B", CatchupSeekLogic.pick(list, 1500L)!!.show.title)
        assertEquals("A", CatchupSeekLogic.pick(list, 600L)!!.show.title)
    }
}
