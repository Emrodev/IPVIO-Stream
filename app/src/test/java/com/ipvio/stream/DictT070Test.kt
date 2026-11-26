package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-070: every text of "Device performance" is translated into every language of the app. */
class DictT070Test {
    private fun keys(block: String) = block.lineSequence().filter { it.contains('¦') }.map { it.substringBefore('¦') }.toList()

    @Test
    fun everyLanguageHasEveryRow() {
        val main = keys(DictT070)
        assertEquals(5, main.size)
        for (line in DictT070.lineSequence().filter { it.contains('¦') }) assertEquals(line.substringBefore('¦'), 9, line.split('¦').size)
        for (col in listOf(DictT070Nl, DictT070Ar, DictT070Pl, DictT070Zh, DictT070Pt)) assertEquals(main, keys(col))
    }

    @Test
    fun textsAreTranslated() {
        I18nCore.load(DictT070)
        for ((code, col) in listOf("nl" to DictT070Nl, "ar" to DictT070Ar, "pl" to DictT070Pl, "zh" to DictT070Zh, "pt" to DictT070Pt))
            I18nCore.loadColumn(I18nCore.CODES.indexOf(code), col)
        assertEquals("Device performance", I18nCore.translate("Geräteleistung", 1))
        assertEquals("Weak device", I18nCore.translate("Schwaches Gerät", 1))
        assertEquals("Strong device", I18nCore.translate("Starkes Gerät", 1))
        for (li in 1 until I18nCore.CODES.size) for (k in keys(DictT070)) {
            val t = I18nCore.translate(k, li)
            assertTrue(I18nCore.CODES[li] + ": " + k, t.isNotBlank())
            assertNotEquals(I18nCore.CODES[li] + ": " + k, k, t)
        }
    }
}
