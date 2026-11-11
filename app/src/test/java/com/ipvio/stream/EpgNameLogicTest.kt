package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class EpgNameLogicTest {
    @Test fun providerTagsAndQuality_areIgnored() {
        val k = EpgNameLogic.key("Das Erste")
        assertEquals("daserste", k)
        assertEquals(k, EpgNameLogic.key("DE: Das Erste HD"))
        assertEquals(k, EpgNameLogic.key("|DE| Das Erste FHD"))
        assertEquals(k, EpgNameLogic.key("Das Erste (DE)"))
        assertEquals(k, EpgNameLogic.key("DE - Das Erste 1080p"))
    }

    @Test fun spacesAndCase_doNotMatter() {
        assertEquals(EpgNameLogic.key("Sky Sport 1"), EpgNameLogic.key("SKY SPORT1 HD"))
        assertEquals(EpgNameLogic.key("ProSieben"), EpgNameLogic.key("Pro Sieben"))
    }

    @Test fun differentChannels_stayDifferent() {
        assertNotEquals(EpgNameLogic.key("Sky Sport 1"), EpgNameLogic.key("Sky Sport 2"))
        assertNotEquals(EpgNameLogic.key("RTL"), EpgNameLogic.key("RTL2"))
    }

    @Test fun empty_givesEmptyKey() {
        assertEquals("", EpgNameLogic.key(null))
        assertEquals("", EpgNameLogic.key("  "))
        assertEquals("", EpgNameLogic.key("HD"))
    }
}
