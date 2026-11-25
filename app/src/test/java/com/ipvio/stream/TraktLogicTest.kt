package com.ipvio.stream

import com.ipvio.stream.TraktLogic.PollAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TraktLogicTest {
    @Test fun pollActionMapsStatusCodes() {
        assertEquals(PollAction.DONE, TraktLogic.pollAction(200))
        assertEquals(PollAction.WAIT, TraktLogic.pollAction(400))
        assertEquals(PollAction.SLOW_DOWN, TraktLogic.pollAction(429))
        assertEquals(PollAction.FAILED, TraktLogic.pollAction(410))
        assertEquals(PollAction.FAILED, TraktLogic.pollAction(418))
        assertEquals(PollAction.FAILED, TraktLogic.pollAction(404))
    }

    @Test fun slowDownAddsFiveSeconds() {
        assertEquals(10, TraktLogic.nextIntervalSeconds(5, PollAction.SLOW_DOWN))
        assertEquals(5, TraktLogic.nextIntervalSeconds(5, PollAction.WAIT))
    }

    @Test fun intervalNeverBelowOneSecond() {
        assertEquals(1, TraktLogic.nextIntervalSeconds(0, PollAction.WAIT))
        assertEquals(6, TraktLogic.nextIntervalSeconds(-3, PollAction.SLOW_DOWN))
    }

    @Test fun deviceCodeExpiry() {
        assertFalse(TraktLogic.codeExpired(startedMs = 1_000L, expiresInSeconds = 600, nowMs = 600_999L))
        assertTrue(TraktLogic.codeExpired(startedMs = 1_000L, expiresInSeconds = 600, nowMs = 601_000L))
    }

    @Test fun accessTokenHasSafetyMargin() {
        val expiresAt = TraktLogic.accessExpiresAt(issuedMs = 0L, expiresInSeconds = 7_776_000)
        assertEquals(7_776_000_000L - 60_000L, expiresAt)
        assertTrue(TraktLogic.isAccessValid(nowMs = 0L, expiresAtMs = expiresAt))
        assertFalse(TraktLogic.isAccessValid(nowMs = expiresAt, expiresAtMs = expiresAt))
    }

    @Test fun clientIdCheck() {
        assertTrue(TraktLogic.hasClientId("y_7Xynhs7CYGaW1huVVJGmd8ooTL0WQKTntnObBacfA"))
        assertFalse(TraktLogic.hasClientId(""))
        assertFalse(TraktLogic.hasClientId("   "))
        assertFalse(TraktLogic.hasClientId("abc def"))
    }
}
