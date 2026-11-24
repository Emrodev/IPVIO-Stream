package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

class PersonFilmoLogicTest {
    private fun e(title: String, year: Int?, tv: Boolean, owned: Boolean) =
        FilmoEntry(title, year, tv, null, if (owned) Item(title, if (tv) Kind.SERIES else Kind.MOVIE, title, year, "u", null, "g") else null)

    private val all = listOf(
        e("Old film", 1990, false, true),
        e("New film", 2020, false, false),
        e("Show", 2015, true, true),
        e("No year", null, false, false)
    )

    @Test fun allTitlesAreShownNewestFirst() {
        assertEquals(listOf("New film", "Show", "Old film", "No year"), PersonFilmoLogic.visible(all, 0).map { it.title })
    }

    @Test fun kindFilterMoviesAndSeries() {
        assertEquals(listOf("New film", "Old film", "No year"), PersonFilmoLogic.visible(all, 1).map { it.title })
        assertEquals(listOf("Show"), PersonFilmoLogic.visible(all, 2).map { it.title })
    }

    @Test fun countsTitlesInThePlaylist() {
        assertEquals(2, PersonFilmoLogic.inPlaylist(all))
    }
}
