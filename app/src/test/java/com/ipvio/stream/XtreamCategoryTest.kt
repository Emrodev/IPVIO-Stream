package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

/** Xtream: full list plus lists per category, each title once (providers that cut the full list short). */
class XtreamCategoryTest {
    private fun m(id: String, cat: String? = null) = HashMap<String, String>().apply { put("stream_id", id); if (cat != null) put("category_id", cat) }

    private fun run(full: List<Map<String, String>>, cats: Map<String, Any>, skippable: (Exception) -> Boolean = { it is IOException }): List<Map<String, String>> {
        val out = ArrayList<Map<String, String>>()
        XtreamLogic.eachUnique("stream_id", cats.keys,
            fetchAll = { each -> full.forEach(each) },
            fetchCategory = { cid, each ->
                when (val v = cats[cid]) {
                    is Exception -> throw v
                    is List<*> -> v.forEach { @Suppress("UNCHECKED_CAST") each(it as Map<String, String>) }
                }
            },
            skippable = skippable, sink = { out.add(it) })
        return out
    }

    @Test fun categoryListsAddWhatTheFullListMisses() {
        val out = run(listOf(m("1", "10"), m("2", "10")), linkedMapOf("10" to listOf(m("2", "10"), m("3")), "20" to listOf(m("4", "20"))))
        assertEquals(listOf("1", "2", "3", "4"), out.map { it["stream_id"] })
        // Entry without its own category gets the requested one
        assertEquals("10", out[2]["category_id"])
    }

    @Test fun entriesWithoutNumericIdAreDropped() {
        val out = run(listOf(m("x"), m("")), linkedMapOf("10" to listOf(m("5", "10"))))
        assertEquals(listOf("5"), out.map { it["stream_id"] })
    }

    @Test fun failedCategoryIsSkipped() {
        val out = run(emptyList(), linkedMapOf("10" to IOException("timeout"), "20" to listOf(m("7", "20"))))
        assertEquals(listOf("7"), out.map { it["stream_id"] })
    }

    @Test fun abortIsPassedOn() {
        try {
            run(emptyList(), linkedMapOf("10" to IllegalStateException("abgebrochen"), "20" to listOf(m("7", "20"))))
            fail("abort must not be swallowed")
        } catch (e: IllegalStateException) {
            assertEquals("abgebrochen", e.message)
        }
    }

    @Test fun providerIgnoringTheFilterStopsAfterOneCategory() {
        var calls = 0
        val all = listOf(m("1", "10"), m("2", "20"), m("3", "30"))
        val out = ArrayList<Map<String, String>>()
        XtreamLogic.eachUnique("stream_id", listOf("10", "20", "30"),
            fetchAll = { each -> all.forEach(each) },
            fetchCategory = { _, each -> calls++; all.forEach(each) },
            skippable = { false }, sink = { out.add(it) })
        assertEquals(1, calls)
        assertEquals(3, out.size)
    }

    @Test fun seriesUseTheirOwnKey() {
        val out = ArrayList<Map<String, String>>()
        XtreamLogic.eachUnique("series_id", listOf("5"),
            fetchAll = { each -> each(mapOf("series_id" to "1")) },
            fetchCategory = { _, each -> each(mapOf("series_id" to "1")); each(mapOf("series_id" to "2")) },
            skippable = { false }, sink = { out.add(it) })
        assertEquals(listOf("1", "2"), out.map { it["series_id"] })
        assertTrue(out[1]["category_id"] == "5")
    }
}
