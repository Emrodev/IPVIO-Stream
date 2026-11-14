package com.ipvio.stream

import org.junit.Assert.*
import org.junit.Test

class DriveLogicTest {
    @Test fun fileNames() {
        assertEquals("ipvio_sync_ab12cd34.json", DriveLogic.fileName("ab12cd34"))
        assertEquals("ab12cd34", DriveLogic.deviceIdOf("ipvio_sync_ab12cd34.json"))
        assertNull(DriveLogic.deviceIdOf("ipvio_backup.json"))
        assertTrue(DriveLogic.isSyncFile("ipvio_sync_ab12cd34.json"))
        assertTrue(DriveLogic.isSyncFile("ipvio_backup.json"))
        assertFalse(DriveLogic.isSyncFile("ipvio_sync_.json"))
        assertFalse(DriveLogic.isSyncFile("ipvio_sync_a'b.json"))
        assertFalse(DriveLogic.isSyncFile("other.json"))
    }

    @Test fun badDeviceIdIsRefused() {
        var thrown = false
        try { DriveLogic.fileName("x' or name='y") } catch (e: IllegalArgumentException) { thrown = true }
        assertTrue(thrown)
    }

    @Test fun driveTimes() {
        assertEquals(1794652530123L, DriveLogic.parseTime("2026-11-14T10:35:30.123Z"))
        assertEquals(1794652530000L, DriveLogic.parseTime("2026-11-14T10:35:30Z"))
        assertEquals(0L, DriveLogic.parseTime("yesterday"))
        assertEquals(0L, DriveLogic.parseTime(null))
    }

    @Test fun pollAnswers() {
        assertEquals(DriveLogic.Poll.WAIT, DriveLogic.pollAnswer("authorization_pending"))
        assertEquals(DriveLogic.Poll.SLOWER, DriveLogic.pollAnswer("slow_down"))
        assertEquals(DriveLogic.Poll.DENIED, DriveLogic.pollAnswer("access_denied"))
        assertEquals(DriveLogic.Poll.EXPIRED, DriveLogic.pollAnswer("expired_token"))
        assertEquals(DriveLogic.Poll.FAILED, DriveLogic.pollAnswer("invalid_client"))
    }
}
