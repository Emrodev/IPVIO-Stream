package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchVersionsTest {
    private fun movie(id: String, t: String, year: Int?, lang: String, group: String = "G") =
        Item(id, Kind.MOVIE, t, year, "u$id", null, group, lang = lang)

    @Test
    fun `titles that occur once get no label`() {
        val list = listOf(movie("1", "The Batman", 2022, "fr"), movie("2", "Batman Begins", 2005, "en"))
        assertTrue(SearchVersions.labels(list).isEmpty())
    }

    @Test
    fun `language versions of the same movie are labelled with year and language`() {
        val list = listOf(movie("1", "Merry Little Batman", 2023, "fr"), movie("2", "Merry Little Batman", 2023, "en"))
        val l = SearchVersions.labels(list)
        assertEquals("2023 · Français", l["1"])
        assertEquals("2023 · English", l["2"])
    }

    @Test
    fun `untagged version shows its category and punctuation does not matter`() {
        val list = listOf(
            movie("1", "Batman: Knightfall Part 1: Knightfall", 2026, "es", "SPANISH | KIDS"),
            movie("2", "Batman: Knightfall - Part 1: Knightfall", 2026, LangTagLogic.UNKNOWN, "KIDS | MOVIES")
        )
        val l = SearchVersions.labels(list)
        assertEquals("2026 · Español", l["1"])
        assertEquals("2026 · KIDS | MOVIES", l["2"])
    }

    @Test
    fun `different years are different movies`() {
        val list = listOf(movie("1", "Dune", 1984, "en"), movie("2", "Dune", 2021, "en"))
        assertTrue(SearchVersions.labels(list).isEmpty())
    }

    @Test
    fun `live channels are never labelled`() {
        val list = listOf(
            Item("1", Kind.LIVE, "CNN", null, "u1", null, "US"),
            Item("2", Kind.LIVE, "CNN", null, "u2", null, "UK")
        )
        assertTrue(SearchVersions.labels(list).isEmpty())
    }
}
