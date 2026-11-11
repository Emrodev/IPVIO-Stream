package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
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

    private fun movies(count: Int) = List(count) { item(it.toString(), Kind.MOVIE, "movie $it") }

    @Test fun exactScanChecksCancellationPeriodicallyAndAborts() {
        var calls = 0
        try {
            SearchIndex(List(1000) { item(it.toString(), Kind.LIVE, "channel $it") }).find("zz") {
                calls++
                if (calls == 2) throw java.util.concurrent.CancellationException()
            }
            fail("expected cancellation")
        } catch (_: java.util.concurrent.CancellationException) {
        }
        assertEquals(2, calls)
    }

    @Test fun fuzzyScanChecksCancellationToo() {
        var calls = 0
        // No exact hit, so the fuzzy scan runs: 4 checks in each of the two scans for 1000 items.
        SearchIndex(movies(1000)).find("gladiatr", fuzzyBudgetMs = 1_000_000L) { calls++ }
        assertEquals(8, calls)
    }

    @Test fun fuzzyFindsTyposWithinBudget() {
        val items = movies(10) + item("g", Kind.MOVIE, "Gladiator")
        val found = SearchIndex(items).find("gladiatr", fuzzyBudgetMs = 1_000_000L).first
        assertEquals(listOf("g"), found.map { it.id })
    }

    @Test fun fuzzyStopsWhenBudgetIsSpentButExactSearchStillWorks() {
        val items = movies(10) + item("g", Kind.MOVIE, "Gladiator")
        var t = 0L
        val clock = { t += 1_000_000_000L; t }
        val index = SearchIndex(items)
        assertEquals(0, index.find("gladiatr", fuzzyBudgetMs = 0L, nanoClock = clock).first.size)
        assertEquals(listOf("g"), index.find("gladiator", fuzzyBudgetMs = 0L, nanoClock = clock).first.map { it.id })
    }

    @Test fun suggestionsReturnMostFrequentWordsFirst() {
        val titles = listOf("alfa" to 3, "alpine" to 2, "algae" to 1, "alibi" to 1, "almanac" to 1, "aloft" to 1, "alone" to 1)
        var id = 0
        val items = titles.flatMap { (title, count) -> List(count) { item((id++).toString(), Kind.MOVIE, title) } }
        assertEquals(listOf("alfa", "alpine", "algae", "alibi", "almanac", "aloft"), WordSuggest(items).suggest("al"))
    }

    @Test fun suggestionsSkipTheExactWordAndHandleZeroLimit() {
        val items = listOf(item("1", Kind.MOVIE, "alpha"), item("2", Kind.MOVIE, "alphabet"))
        assertEquals(listOf("alphabet"), WordSuggest(items).suggest("alpha"))
        assertEquals(emptyList<String>(), WordSuggest(items).suggest("al", n = 0))
    }

    @Test fun suggestScanCanBeCancelled() {
        val items = listOf(item("1", Kind.MOVIE, "alfa"), item("2", Kind.MOVIE, "alpine"))
        try {
            WordSuggest(items).suggest("al") { throw java.util.concurrent.CancellationException() }
            fail("expected cancellation")
        } catch (_: java.util.concurrent.CancellationException) {
        }
    }
}
