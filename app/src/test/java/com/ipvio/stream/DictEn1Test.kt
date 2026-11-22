package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

/** Stage 1 English rows (T-041): exactly two columns per row, unique German keys, English lookup works. */
class DictEn1Test {
    private fun rawRows(): List<List<String>> = DictEn1.lineSequence()
        .filter { it.isNotBlank() }
        .map { it.split('¦') }
        .toList()

    @Test fun every_row_has_exactly_german_key_and_english_text() {
        val bad = rawRows().filter { it.size != 2 || it[0].isBlank() || it[1].isBlank() }
        assertEquals("rows that are not German¦English: $bad", 0, bad.size)
    }

    @Test fun german_keys_are_unique() {
        val keys = rawRows().map { it[0] }
        assertEquals("duplicate keys", keys.size, keys.toSet().size)
    }

    @Test fun english_lookup_returns_the_english_text() {
        I18nCore.load(DictA, DictB, DictC, DictD, DictE, DictF, DictG, DictH, DictI, DictJ, DictK, DictL, DictM, DictN, DictO, DictP, DictEn1)
        assertEquals("Nothing found", I18nCore.translate("Nichts gefunden", 1))
        assertEquals("Error", I18nCore.translate("Fehler", 1))
    }
}
