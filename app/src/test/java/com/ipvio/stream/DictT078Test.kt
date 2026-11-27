package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

/** T-078: translations of the PIN texts for a new profile. */
class DictT078Test {
    private fun tr(s: String, code: String): String {
        I18nCore.load(DictT078)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("nl"), DictT078Nl)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("ar"), DictT078Ar)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("pl"), DictT078Pl)
        return I18nCore.translate(s, I18nCore.CODES.indexOf(code))
    }

    @Test fun english_rows() {
        assertEquals("Repeat the PIN", tr("PIN wiederholen", "en"))
    }

    @Test fun dutch_polish_arabic_rows() {
        assertEquals("PIN herhalen", tr("PIN wiederholen", "nl"))
        assertEquals("Powtórz PIN", tr("PIN wiederholen", "pl"))
        assertEquals("أعد إدخال رمز PIN", tr("PIN wiederholen", "ar"))
    }
}
