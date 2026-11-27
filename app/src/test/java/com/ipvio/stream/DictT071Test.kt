package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

/** T-075: translations of the automatic Trakt sync setting. */
class DictT071Test {
    private fun tr(s: String, code: String): String {
        I18nCore.load(DictT071)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("nl"), DictT071Nl)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("ar"), DictT071Ar)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("pl"), DictT071Pl)
        return I18nCore.translate(s, I18nCore.CODES.indexOf(code))
    }

    @Test fun english_rows() {
        assertEquals("Sync automatically", tr("Automatisch synchronisieren", "en"))
        assertEquals("Every 12 hours", tr("Alle 12 Stunden", "en"))
        assertEquals("Only manually", tr("Nur manuell", "en"))
    }

    @Test fun dutch_polish_arabic_rows() {
        assertEquals("Alleen handmatig", tr("Nur manuell", "nl"))
        assertEquals("Co tydzień", tr("Wöchentlich", "pl"))
        assertEquals("يوميًا", tr("Täglich", "ar"))
    }
}
