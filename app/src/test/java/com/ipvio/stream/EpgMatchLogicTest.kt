package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-067: guide channels for playlist channels with a missing or slightly different tvg-id. */
class EpgMatchLogicTest {
    private fun ch(id: String, name: String, tvg: String? = null, lang: String? = null) = EpgMatchLogic.Channel(id, name, tvg, lang)

    @Test fun namesCompareWithoutTagsAndQuality() {
        assertEquals(EpgMatchLogic.normName("Das Erste"), EpgMatchLogic.normName("DE: Das Erste HD"))
        assertEquals(EpgMatchLogic.normName("ZDF"), EpgMatchLogic.normName("|DE| ZDF FHD"))
        assertEquals("prosieben", EpgMatchLogic.normName("ProSieben"))
        // a time-shift channel is another channel
        assertTrue(EpgMatchLogic.normName("RTL +1") != EpgMatchLogic.normName("RTL"))
        // "RTL - Television" keeps its name (RTL is no language)
        assertEquals("rtltelevision", EpgMatchLogic.normName("RTL - Television"))
    }

    @Test fun idsCompareWithoutCountryAndVariant() {
        assertEquals("daserste", EpgMatchLogic.normId("DasErste.de"))
        assertEquals("daserste", EpgMatchLogic.normId("daserste.de@HD"))
        assertEquals("de", EpgMatchLogic.idCountry("ard.de@SD"))
        assertNull(EpgMatchLogic.idCountry("ard"))
    }

    @Test fun matchesBySimilarIdThenByName() {
        val guide = mapOf("daserste.de@HD" to listOf("Das Erste"), "zdf.de" to listOf("ZDF"), "rtl.de" to listOf("RTL"))
        val r = EpgMatchLogic.match(
            listOf(
                ch("1", "DE: Das Erste HD", "DasErste.de"),   // similar ID
                ch("2", "ZDF FHD"),                            // no ID: by name
                ch("3", "RTL", "rtl.de"),                      // exact: untouched
                ch("4", "Unknown channel")
            ),
            guide
        )
        assertEquals("daserste.de@hd", r.redirect["daserste.de"])
        assertEquals("zdf.de", r.byItem["2"])
        assertNull(r.redirect["rtl.de"])
        assertNull(r.byItem["4"])
    }

    @Test fun ambiguousNameNeedsAFittingCountry() {
        val guide = mapOf("skysport1.de" to listOf("Sky Sport 1"), "skysport1.uk" to listOf("Sky Sport 1"))
        val r = EpgMatchLogic.match(listOf(ch("a", "DE: Sky Sport 1", lang = "de"), ch("b", "Sky Sport 1", lang = "?")), guide)
        assertEquals("skysport1.de", r.byItem["a"])
        // no language: no program rather than a wrong one
        assertNull(r.byItem["b"])
    }

    @Test fun alreadyMatchedChannelsAreLeftAlone() {
        val r = EpgMatchLogic.match(listOf(ch("2", "ZDF")), mapOf("zdf.de" to listOf("ZDF")), done = setOf("2"))
        assertTrue(r.byItem.isEmpty())
    }
}
