package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-053: when the "Because you watched" row is shown. */
class BecauseLogicTest {
    @Test fun noMatch_noRow() {
        assertFalse(BecauseLogic.show(0))
        assertFalse(BecauseLogic.show(1))
    }

    @Test fun row_is_built_from_the_last_three_titles() {
        assertEquals(3, BecauseLogic.SOURCES)
    }

    @Test fun twoOrMore_showsRow() {
        assertEquals(2, BecauseLogic.MIN_MATCHES)
        assertTrue(BecauseLogic.show(2))
        assertTrue(BecauseLogic.show(20))
    }
}
