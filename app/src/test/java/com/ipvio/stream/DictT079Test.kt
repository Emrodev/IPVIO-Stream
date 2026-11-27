package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

/** T-079: translations of the per-profile PIN texts. */
class DictT079Test {
    private fun tr(s: String, code: String): String {
        I18nCore.load(DictT079)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("nl"), DictT079Nl)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("ar"), DictT079Ar)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("pl"), DictT079Pl)
        return I18nCore.translate(s, I18nCore.CODES.indexOf(code))
    }

    @Test fun english_rows() {
        assertEquals("Add PIN", tr("PIN hinzufügen", "en"))
        assertEquals("Change PIN", tr("PIN ändern", "en"))
    }

    @Test fun dutch_and_polish_rows() {
        assertEquals("PIN toevoegen", tr("PIN hinzufügen", "nl"))
        assertEquals("Zmień PIN", tr("PIN ändern", "pl"))
    }
}
