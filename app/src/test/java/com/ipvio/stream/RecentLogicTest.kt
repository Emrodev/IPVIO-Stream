package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Issue #22: "recently added" order for M3U/Xtream (oldest first in the list) and Stalker (newest first). */
class RecentLogicTest {
    private val m3u = listOf("http://h/1.mp4", "http://h/2.mp4", "http://h/3.mp4")
    private val stalker = listOf("stalker://a", "stalker://b", "stalker://c")

    @Test fun stalkerOnlyListIsNewestFirst() {
        assertTrue(RecentLogic.isNewestFirst(stalker))
        assertFalse(RecentLogic.isNewestFirst(m3u))
        assertFalse(RecentLogic.isNewestFirst(stalker + "http://h/x.mp4"))
        assertFalse(RecentLogic.isNewestFirst(emptyList()))
    }

    @Test fun newestReadsFromTheEndForM3u() {
        assertEquals(listOf("http://h/3.mp4", "http://h/2.mp4"), RecentLogic.newest(m3u, 2) { it })
        assertEquals(m3u.reversed(), RecentLogic.newestFirst(m3u) { it })
    }

    @Test fun newestReadsFromTheFrontForStalker() {
        assertEquals(listOf("stalker://a", "stalker://b"), RecentLogic.newest(stalker, 2) { it })
        assertEquals(stalker, RecentLogic.newestFirst(stalker) { it })
    }

    @Test fun xtreamAddedTimeOrdersOldestFirst() {
        assertEquals(1700000000L, XtreamLogic.addedOf(mapOf("added" to " 1700000000 ")))
        assertEquals(0L, XtreamLogic.addedOf(mapOf("added" to "abc")))
        assertEquals(0L, XtreamLogic.addedOf(mapOf("added" to "-5")))
        assertEquals(0L, XtreamLogic.addedOf(emptyMap()))
        val sorted = XtreamLogic.byAdded(listOf(30L to "c", 10L to "a", 0L to "none", 20L to "b"))
        assertEquals(listOf("none", "a", "b", "c"), sorted.map { it.second })
        // Stable: equal times keep the provider order
        assertEquals(listOf("x", "y"), XtreamLogic.byAdded(listOf(5L to "x", 5L to "y")).map { it.second })
    }
}
