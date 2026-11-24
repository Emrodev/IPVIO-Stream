package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

/** English rows of the new search screen (T-051b): the watchlist button showed the German key. */
class DictEn4Test {
    private fun en(s: String) = run {
        I18nCore.load(DictA, DictB, DictC, DictD, DictE, DictF, DictG, DictH, DictI, DictJ, DictK, DictL, DictM, DictN, DictO, DictP, DictEn1, DictEn2, DictEn3, DictEn4)
        I18nCore.translate(s, 1)
    }

    @Test fun watchlistButton() = assertEquals("Watchlist", en("Merkliste"))

    @Test fun onWatchlistButton() = assertEquals("On your watchlist", en("Auf der Merkliste"))

    @Test fun clearAllKey() = assertEquals("Clear all", en("Alles löschen"))
}
