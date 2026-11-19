package com.ipvio.stream

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** #15: trailer ids are checked before they go into the embed page (pure, no Android). */
class TrailerLogicTest {

    @Test
    fun validYoutubeIds_areAccepted() {
        assertTrue(TrailerLogic.validKey("dQw4w9WgXcQ"))
        assertTrue(TrailerLogic.validKey("abc-_123XYZ"))
    }

    @Test
    fun invalidIds_areRejected() {
        assertFalse(TrailerLogic.validKey(null))
        assertFalse(TrailerLogic.validKey(""))
        assertFalse(TrailerLogic.validKey("short"))
        assertFalse(TrailerLogic.validKey("x\"><script>alert(1)</script>"))
        assertFalse(TrailerLogic.validKey("https://youtu.be/dQw4w9WgXcQ"))
    }

    @Test
    fun embedHtml_onlyForValidIds_andUsesEmbedHost() {
        assertNull(TrailerLogic.embedHtml("bad id!"))
        val html = TrailerLogic.embedHtml("dQw4w9WgXcQ")!!
        assertTrue(html.contains("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ?"))
        assertTrue(html.contains("allowfullscreen"))
    }
}
