package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class MovieSourcesLogicTest {
    private fun movie(id: String, title: String, year: Int?, lang: String = "en", quality: String? = null) =
        Item(id, Kind.MOVIE, title, year, "http://x/$id", null, "Movies", quality = quality, lang = lang)

    private val list = listOf(
        movie("1", "Matrix", 1999, quality = "4K"),
        movie("2", "The Matrix", 1999),
        movie("3", "MATRIX", 1999, quality = "HD"),
        movie("4", "Matrix", 1999, lang = "de"),
        movie("5", "Matrix", 2021),
        Item("6", Kind.SERIES, "Matrix", 1999, "", null, "Series"),
        Item("7", Kind.LIVE, "Matrix", 1999, "http://x/7", null, "Live"),
        movie("8", "Matrix", 1999, quality = "FHD"),
        movie("9", "Other", null)
    )

    @Test
    fun indexGivesTheSameSourcesAsTheScan() {
        val idx = MovieSourceIndex.build(list)
        for (item in list) assertEquals(item.id, MovieSourcesLogic.scan(item, list), idx.alternatives(item))
        assertEquals(listOf("3", "1", "8"), idx.alternatives(list[2]).map { it.id })
    }

    @Test
    fun movieNotInTheListStillGetsTheSourcesWithItsKey() {
        // e.g. a downloaded copy: the playlist entries with the same key are offered after it
        val outside = movie("dl", "matrix", 1999)
        val idx = MovieSourceIndex.build(list)
        assertEquals(MovieSourcesLogic.scan(outside, list), idx.alternatives(outside))
        assertEquals(listOf("dl", "1", "3", "8"), idx.alternatives(outside).map { it.id })
        val alone = movie("x", "Nothing like it", 2000)
        assertEquals(listOf(alone), idx.alternatives(alone))
    }

    @Test
    fun otherKindsReturnOnlyThemselves() {
        val idx = MovieSourceIndex.build(list)
        assertEquals(listOf(list[5]), idx.alternatives(list[5]))
        assertSame(list, idx.all)
    }

    @Test
    fun largeRandomListMatchesTheScan() {
        val r = java.util.Random(7)
        val titles = (0 until 300).map { "Title ${it % 120}" }
        val langs = listOf("en", "de", "?")
        val big = (0 until 5000).map { i ->
            val kind = if (r.nextInt(10) == 0) Kind.SERIES else Kind.MOVIE
            Item("i$i", kind, titles[r.nextInt(titles.size)], 1990 + r.nextInt(5), "u$i", null, "G", lang = langs[r.nextInt(3)])
        }
        val idx = MovieSourceIndex.build(big)
        for (k in 0 until 400) {
            val it = big[r.nextInt(big.size)]
            assertEquals(MovieSourcesLogic.scan(it, big), idx.alternatives(it))
        }
    }
}
