package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TraktLogicTest {
    @Test fun challengeMatchesRfc7636Example() {
        // RFC 7636 appendix B test vector
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            TraktLogic.challenge("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"),
        )
    }

    @Test fun base64UrlHandlesAllPaddingCases() {
        assertEquals("", TraktLogic.base64UrlNoPad(ByteArray(0)))
        assertEquals("Zg", TraktLogic.base64UrlNoPad("f".toByteArray()))
        assertEquals("Zm8", TraktLogic.base64UrlNoPad("fo".toByteArray()))
        assertEquals("Zm9v", TraktLogic.base64UrlNoPad("foo".toByteArray()))
        assertEquals("-_8", TraktLogic.base64UrlNoPad(byteArrayOf(0xfb.toByte(), 0xff.toByte())))
    }

    @Test fun generatedVerifierIsValid() {
        repeat(20) {
            val v = TraktLogic.newVerifier()
            assertEquals(64, v.length)
            assertTrue(TraktLogic.isValidVerifier(v))
        }
        assertTrue(TraktLogic.isValidVerifier("a".repeat(43)))
        assertTrue(TraktLogic.isValidVerifier("a".repeat(128)))
        assertFalse(TraktLogic.isValidVerifier("a".repeat(42)))
        assertFalse(TraktLogic.isValidVerifier("a".repeat(129)))
        assertFalse(TraktLogic.isValidVerifier("a".repeat(43) + "+"))
    }

    @Test fun authorizeUrlHasPkceParameters() {
        val url = TraktLogic.authorizeUrl("client123", "https://example.org/cb", "CHALLENGE", "STATE1")
        assertTrue(url.startsWith("https://auth.trakt.tv/oauth/authorize?"))
        assertTrue(url.contains("response_type=code"))
        assertTrue(url.contains("client_id=client123"))
        assertTrue(url.contains("redirect_uri=https%3A%2F%2Fexample.org%2Fcb"))
        assertTrue(url.contains("code_challenge=CHALLENGE"))
        assertTrue(url.contains("code_challenge_method=S256"))
        assertTrue(url.contains("state=STATE1"))
        assertFalse(url.contains("client_secret"))
    }

    @Test fun tokenBodyHasNoClientSecret() {
        val body = TraktLogic.tokenBody("CODE", "VERIFIER", "client123", "https://example.org/cb")
        assertEquals("authorization_code", body["grant_type"])
        assertEquals("VERIFIER", body["code_verifier"])
        assertEquals("CODE", body["code"])
        assertFalse(body.containsKey("client_secret"))
    }

    @Test fun onlyHttpsRedirectsAreAccepted() {
        assertTrue(TraktLogic.isHttpsRedirect("https://example.org/cb"))
        assertFalse(TraktLogic.isHttpsRedirect("http://example.org/cb"))
        assertFalse(TraktLogic.isHttpsRedirect("https://"))
        assertFalse(TraktLogic.isHttpsRedirect("urn:ietf:wg:oauth:2.0:oob"))
        assertFalse(TraktLogic.isHttpsRedirect("http://localhost:5173/callback"))
        assertFalse(TraktLogic.isHttpsRedirect("myapp://callback"))
        assertFalse(TraktLogic.isHttpsRedirect("https://example.org/a b"))
    }

    @Test fun registeredRedirectIsHttps() {
        assertTrue(TraktLogic.isHttpsRedirect(TraktLogic.REDIRECT_URI))
        assertEquals("https://emrodev.github.io/IPVIO-Stream/trakt-callback/", TraktLogic.REDIRECT_URI)
    }

    @Test fun clientIdCheck() {
        assertTrue(TraktLogic.hasClientId("y_7Xynhs7CYGaW1huVVJGmd8ooTL0WQKTntnObBacfA"))
        assertFalse(TraktLogic.hasClientId(""))
        assertFalse(TraktLogic.hasClientId("   "))
        assertFalse(TraktLogic.hasClientId("abc def"))
    }

    @Test fun accessTokenHasSafetyMargin() {
        val expiresAt = TraktLogic.accessExpiresAt(issuedMs = 0L, expiresInSeconds = 7_776_000)
        assertEquals(7_776_000_000L - 60_000L, expiresAt)
        assertTrue(TraktLogic.isAccessValid(nowMs = 0L, expiresAtMs = expiresAt))
        assertFalse(TraktLogic.isAccessValid(nowMs = expiresAt, expiresAtMs = expiresAt))
    }
}
