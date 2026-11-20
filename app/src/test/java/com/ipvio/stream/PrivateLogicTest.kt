package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateLogicTest {
    private fun item(id: String, kind: Kind, group: String, title: String = "T$id") = Item(id, kind, title, null, "u$id", null, group)

    private val liveNews = item("1", Kind.LIVE, "UK| NEWS")
    private val movieNews = item("2", Kind.MOVIE, "UK| NEWS")
    private val liveSport = item("3", Kind.LIVE, "UK| SPORTS")
    private val all = listOf(liveNews, movieNews, liveSport)
    private val keys = setOf(PrivateLogic.key(Kind.LIVE, "UK| NEWS"))

    @Test fun sameGroupNameOfAnotherKindStaysVisible() {
        assertTrue(PrivateLogic.isPrivate(liveNews, keys, false))
        assertFalse(PrivateLogic.isPrivate(movieNews, keys, false))
    }

    @Test fun visibleDropsOnlyPrivateItems() =
        assertEquals(listOf(movieNews, liveSport), PrivateLogic.visible(all, keys, false))

    @Test fun visibleKeepsSameListWhenNothingIsPrivate() =
        assertSame(all, PrivateLogic.visible(all, emptySet(), false))

    @Test fun idsOfPrivateItems() {
        assertEquals(setOf("1"), PrivateLogic.ids(all, keys, false))
        assertEquals(emptySet<String>(), PrivateLogic.ids(all, emptySet(), false))
    }

    @Test fun parentalOptionUsesParentalCheck() {
        val flagged = all.filter { ParentalLogic.isAdult(it.group, it.title) }
        // None of the sample groups is flagged, so the option alone hides nothing here
        assertEquals(all - flagged.toSet(), PrivateLogic.visible(all, emptySet(), true))
    }

    @Test fun groupNamesWithSeparatorsRoundTrip() {
        val k = setOf(PrivateLogic.key(Kind.SERIES, "|EN| ASIAN SERIES"), PrivateLogic.key(Kind.LIVE, "ASIA | INDIA"))
        assertEquals(k, PrivateLogic.decode(PrivateLogic.encode(k)))
        assertEquals(emptySet<String>(), PrivateLogic.decode(null))
    }
}
