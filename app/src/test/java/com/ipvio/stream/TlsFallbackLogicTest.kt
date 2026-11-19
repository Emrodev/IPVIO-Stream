package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** #69: no silent downgrade to http; only for hosts the user agreed to (pure, no Android). */
class TlsFallbackLogicTest {

    @Test
    fun httpsHost_isLowerCaseHostOnlyForHttps() {
        assertEquals("provider.example", TlsFallbackLogic.httpsHost("https://Provider.Example/get.php?x=1"))
        assertNull(TlsFallbackLogic.httpsHost("http://provider.example/"))
        assertNull(TlsFallbackLogic.httpsHost("not a url"))
    }

    @Test
    fun downgrade_keepsPathAndQuery_andDropsDefaultPort() {
        assertEquals(
            "http://p.example/a/b?u=1&p=2",
            TlsFallbackLogic.downgrade("https://p.example:443/a/b?u=1&p=2"),
        )
        assertEquals("http://p.example:8443/x", TlsFallbackLogic.downgrade("https://p.example:8443/x"))
        assertNull(TlsFallbackLogic.downgrade("http://p.example/x"))
    }

    @Test
    fun matchingCertificate_isUsedAsIs() {
        assertEquals(
            TlsFallbackLogic.Decision.USE_AS_IS,
            TlsFallbackLogic.decide("https://p.example/", mismatch = false, agreed = emptySet()),
        )
    }

    @Test
    fun mismatch_withoutAgreement_asksAndNeverGoesToHttp() {
        assertEquals(
            TlsFallbackLogic.Decision.ASK_CONSENT,
            TlsFallbackLogic.decide("https://p.example/", mismatch = true, agreed = emptySet()),
        )
    }

    @Test
    fun mismatch_withAgreementForThisHost_usesHttp() {
        assertEquals(
            TlsFallbackLogic.Decision.USE_HTTP,
            TlsFallbackLogic.decide("https://P.example/", mismatch = true, agreed = setOf("p.example")),
        )
    }

    @Test
    fun agreement_isPerHost() {
        assertEquals(
            TlsFallbackLogic.Decision.ASK_CONSENT,
            TlsFallbackLogic.decide("https://other.example/", mismatch = true, agreed = setOf("p.example")),
        )
    }

    @Test
    fun plainHttp_isNeverAskedAbout() {
        assertEquals(
            TlsFallbackLogic.Decision.USE_AS_IS,
            TlsFallbackLogic.decide("http://p.example/", mismatch = true, agreed = emptySet()),
        )
    }

    @Test
    fun agree_addsOnlyHttpsHosts() {
        assertEquals(setOf("a.example", "b.example"), TlsFallbackLogic.agree(setOf("a.example"), "https://B.example/x"))
        assertEquals(setOf("a.example"), TlsFallbackLogic.agree(setOf("a.example"), "http://c.example/"))
    }
}
