package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

/** #146: tunneled playback is only asked for in Live TV, without a second picture, when the user switched it on. */
class TunnelingLogicTest {
    @Test fun offByDefaultSetting_neverTunnels() {
        for (live in listOf(true, false)) for (second in listOf(true, false))
            assertEquals(false, TunnelingLogic.wanted(false, live, second))
    }

    @Test fun on_onlyInLiveWithoutSecondPicture() {
        assertEquals(true, TunnelingLogic.wanted(true, live = true, secondPictureOn = false))
        assertEquals(false, TunnelingLogic.wanted(true, live = false, secondPictureOn = false))   // movies and episodes
        assertEquals(false, TunnelingLogic.wanted(true, live = true, secondPictureOn = true))
    }
}
