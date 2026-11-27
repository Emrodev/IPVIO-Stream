package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

/** T-081: translations of the phone report texts on the diagnostics page. */
class DictT081Test {
    private fun tr(s: String, code: String): String {
        I18nCore.load(DictT081)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("nl"), DictT081Nl)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("ar"), DictT081Ar)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("pl"), DictT081Pl)
        return I18nCore.translate(s, I18nCore.CODES.indexOf(code))
    }

    @Test fun english_rows() {
        assertEquals("Open on phone", tr("Am Handy öffnen", "en"))
        assertEquals("Stop phone access", tr("Am Handy beenden", "en"))
    }

    @Test fun dutch_polish_arabic_rows() {
        assertEquals("Openen op telefoon", tr("Am Handy öffnen", "nl"))
        assertEquals("Zakończ dostęp z telefonu", tr("Am Handy beenden", "pl"))
        assertEquals("فتح على الهاتف", tr("Am Handy öffnen", "ar"))
    }
}
