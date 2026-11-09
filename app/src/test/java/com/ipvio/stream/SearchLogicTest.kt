package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchLogicTest {
    private fun item(id: String, k: Kind, t: String) = Item(id, k, t, null, "u$id", null, "g")
    private val idx = SearchIndex(listOf(
        item("1", Kind.MOVIE, "Die fabelhafte Welt der Amélie"),
        item("2", Kind.SERIES, "Straße der Lieder"),
        item("3", Kind.LIVE, "ZDF HD"),
        item("4", Kind.MOVIE, "Amelie 2")
    ))

    @Test fun findetOhneAkzenteUndGrossKlein() {
        val (m, s, l) = idx.find("amelie")
        assertEquals(listOf("1", "4"), m.map { it.id })
        assertEquals(0, s.size + l.size)
    }

    @Test fun eszettUndArten() {
        assertEquals(listOf("2"), idx.find("strasse").second.map { it.id })
        assertEquals(listOf("3"), idx.find("zdf").third.map { it.id })
    }

    @Test fun leer() = assertEquals(0, idx.find("  ").first.size + idx.find("xyz").first.size)

    @Test fun scanSearchCanBeCancelled() {
        var cancelled = false
        try {
            SearchIndex(List(600) { item(it.toString(), Kind.LIVE, "channel $it") }).find("zz") {
                throw java.util.concurrent.CancellationException()
            }
        } catch (_: java.util.concurrent.CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
    }

    @Test fun suggestionsReturnMostFrequentWordsFirst() {
        val titles = listOf("alfa" to 3, "alpine" to 2, "algae" to 1, "alibi" to 1, "almanac" to 1, "aloft" to 1, "alone" to 1)
        var id = 0
        val items = titles.flatMap { (title, count) -> List(count) { item((id++).toString(), Kind.MOVIE, title) } }
        assertEquals(listOf("alfa", "alpine", "algae", "alibi", "almanac", "aloft"), WordSuggest(items).suggest("al"))
    }
}
