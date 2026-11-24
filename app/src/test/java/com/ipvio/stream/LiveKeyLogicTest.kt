package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

/** T-052: Up / Down in Live TV, default and with the setting on. */
class LiveKeyLogicTest {
    @Test fun default_upIsPrevious_downIsNext() {
        assertEquals(-1, LiveKeyLogic.zapStep(up = true, upIsNext = false))
        assertEquals(1, LiveKeyLogic.zapStep(up = false, upIsNext = false))
    }

    @Test fun setting_on_upIsNext_downIsPrevious() {
        assertEquals(1, LiveKeyLogic.zapStep(up = true, upIsNext = true))
        assertEquals(-1, LiveKeyLogic.zapStep(up = false, upIsNext = true))
    }
}
