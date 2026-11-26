package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenCacheLogicTest {
    private data class Ch(val id: String, val name: String)

    @Test
    fun sameInputReturnsStoredResultWithoutBuildingAgain() {
        val cache = LastResults<List<String>>()
        val input = listOf("a", "b")
        var builds = 0
        val first = cache.get(input) { builds++; input.map { it.uppercase() } }
        val second = cache.get(input) { builds++; input.map { it.uppercase() } }
        assertSame(first, second)
        assertEquals(1, builds)
    }

    @Test
    fun equalButOtherListObjectIsBuiltAgain() {
        val cache = LastResults<List<String>>()
        val a = arrayListOf("x")
        val b = arrayListOf("x")
        var builds = 0
        cache.get(a) { builds++; listOf("1") }
        cache.get(b) { builds++; listOf("2") }
        assertEquals(2, builds)
    }

    @Test
    fun extraKeyIsComparedByEquals() {
        val cache = LastResults<String>(max = 3)
        val input = listOf(1)
        var builds = 0
        cache.get(input, setOf(1, 2) to 0) { builds++; "a" }
        cache.get(input, setOf(2, 1) to 0) { builds++; "b" }
        assertEquals(1, builds)
        assertEquals("c", cache.get(input, setOf(1) to 0) { builds++; "c" })
        assertEquals(2, builds)
        assertEquals("a", cache.peek(input, setOf(1, 2) to 0))
    }

    @Test
    fun oldestEntryIsDroppedAndClearEmpties() {
        val cache = LastResults<String>(max = 2)
        val a = listOf("a"); val b = listOf("b"); val c = listOf("c")
        cache.get(a) { "A" }; cache.get(b) { "B" }; cache.get(c) { "C" }
        assertNull(cache.peek(a))
        assertEquals("B", cache.peek(b))
        assertEquals("C", cache.peek(c))
        cache.clear()
        assertNull(cache.peek(c))
    }

    @Test
    fun inOrderMatchesAssociateByLookup() {
        val items = listOf(Ch("1", "one"), Ch("2", "two"), Ch("3", "three"), Ch("2", "two again"))
        val ids = listOf("3", "9", "2", "1", "3")
        val expected = ids.mapNotNull { id -> items.associateBy { it.id }[id] }
        assertEquals(expected, ScreenCacheLogic.inOrder(ids, items) { it.id })
        // An id that occurs twice in the items gives its last item, as associateBy does
        assertEquals("two again", ScreenCacheLogic.inOrder(listOf("2"), items) { it.id }.single().name)
    }

    @Test
    fun byIdsKeepsOnlyWantedIds() {
        val items = (1..1000).map { Ch(it.toString(), "n$it") }
        val m = ScreenCacheLogic.byIds(setOf("5", "500", "nope"), items) { it.id }
        assertEquals(setOf("5", "500"), m.keys)
        assertEquals("n500", m["500"]?.name)
        assertTrue(ScreenCacheLogic.byIds(emptyList(), items) { it.id }.isEmpty())
        assertTrue(ScreenCacheLogic.inOrder(emptyList(), items) { it.id }.isEmpty())
    }
}
