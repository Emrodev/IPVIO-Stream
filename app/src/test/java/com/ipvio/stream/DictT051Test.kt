package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

/** T-051 to T-054: translations of the Live TV channel key setting (German key in the code). */
class DictT051Test {
    private fun tr(s: String, code: String): String {
        I18nCore.load(DictT051)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("nl"), DictT051Nl)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("ar"), DictT051Ar)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("pl"), DictT051Pl)
        return I18nCore.translate(s, I18nCore.CODES.indexOf(code))
    }

    @Test fun english_rows() {
        assertEquals("Channel keys", tr("Kanaltasten", "en"))
        assertEquals("Up goes to the next channel", tr("Hoch führt zum nächsten Sender", "en"))
    }

    @Test fun dutch_arabic_polish_rows() {
        assertEquals("Zenderknoppen", tr("Kanaltasten", "nl"))
        assertEquals("أزرار القنوات", tr("Kanaltasten", "ar"))
        assertEquals("Klawisze kanałów", tr("Kanaltasten", "pl"))
    }
}
