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

    @Test
    fun diagLine_acceptsOnlyKnownConsoleLines() {
        assertTrue(TrailerLogic.diagLine("IPVIO_TRAILER_ERR 153") == "Trailer: player error 153")
        assertTrue(TrailerLogic.diagLine("IPVIO_TRAILER_STATE -1") == "Trailer: player state -1")
        assertTrue(TrailerLogic.diagLine("IPVIO_TRAILER_LOAD") == "Trailer: iframe loaded")
        assertNull(TrailerLogic.diagLine(null))
        assertNull(TrailerLogic.diagLine("IPVIO_TRAILER_ERR abc"))
        assertNull(TrailerLogic.diagLine("IPVIO_TRAILER_ERR 12345"))
        assertNull(TrailerLogic.diagLine("Uncaught TypeError: x"))
    }

    @Test
    fun embedHtml_enablesPlayerReports() {
        val html = TrailerLogic.embedHtml("dQw4w9WgXcQ")!!
        assertTrue(html.contains("enablejsapi=1"))
        assertTrue(html.contains("IPVIO_TRAILER_ERR"))
    }

    @Test
    fun attempts_useDifferentOrigins_andRejectBadInput() {
        val a0 = TrailerLogic.attempt(0, "dQw4w9WgXcQ")!!
        val a1 = TrailerLogic.attempt(1, "dQw4w9WgXcQ")!!
        val a2 = TrailerLogic.attempt(2, "dQw4w9WgXcQ")!!
        assertTrue(a0.baseUrl == "https://www.youtube-nocookie.com")
        assertTrue(a1.baseUrl == "https://com.ipvio.stream" && a1.html.contains("origin=https://com.ipvio.stream"))
        assertTrue(a1.html.contains("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ?"))
        assertTrue(a2.html.contains("https://www.youtube.com/embed/dQw4w9WgXcQ?"))
        assertNull(TrailerLogic.attempt(TrailerLogic.ATTEMPTS, "dQw4w9WgXcQ"))
        assertNull(TrailerLogic.attempt(-1, "dQw4w9WgXcQ"))
        assertNull(TrailerLogic.attempt(0, "bad id!"))
    }

    @Test
    fun consoleLines_playingAndBlockedErrors() {
        assertTrue(TrailerLogic.isPlaying("IPVIO_TRAILER_STATE 1"))
        assertFalse(TrailerLogic.isPlaying("IPVIO_TRAILER_STATE 3"))
        assertFalse(TrailerLogic.isPlaying(null))
        assertTrue(TrailerLogic.playerError("IPVIO_TRAILER_ERR 153") == 153)
        assertNull(TrailerLogic.playerError("IPVIO_TRAILER_STATE 1"))
        assertTrue(TrailerLogic.isVideoBlocked(101))
        assertTrue(TrailerLogic.isVideoBlocked(150))
        assertFalse(TrailerLogic.isVideoBlocked(153))
        assertFalse(TrailerLogic.isVideoBlocked(null))
    }
}
