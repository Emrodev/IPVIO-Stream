package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

class StalkerEpgTest {
    @Test fun epgEntries_parsesSortsAndSkipsBrokenRows() {
        val rows = listOf(
            mapOf("name" to "News", "descr" to "Today", "start_timestamp" to "2000", "stop_timestamp" to "3000"),
            mapOf("name" to "Morning", "start_timestamp" to "1000", "stop_timestamp" to "2000"),
            mapOf("name" to "Broken", "start_timestamp" to "x", "stop_timestamp" to "2000"),
            mapOf("name" to "Backwards", "start_timestamp" to "5000", "stop_timestamp" to "4000"),
            mapOf("name" to "", "start_timestamp" to "6000", "stop_timestamp" to "7000")
        )
        val out = StalkerLogic.epgEntries(rows)
        assertEquals(listOf("Morning", "News"), out.map { it.title })
        assertEquals("Today", out[1].desc)
        assertEquals(2000L, out[1].start)
    }
}
