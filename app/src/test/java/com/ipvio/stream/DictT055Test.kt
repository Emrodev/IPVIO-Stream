package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

/** T-055: translations of the UI texts that had no row before (search, sports, VPN, downloads). */
class DictT055Test {
    private fun tr(s: String, code: String): String {
        I18nCore.load(DictT055)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("nl"), DictT055Nl)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("ar"), DictT055Ar)
        I18nCore.loadColumn(I18nCore.CODES.indexOf("pl"), DictT055Pl)
        return I18nCore.translate(s, I18nCore.CODES.indexOf(code))
    }

    @Test fun english_rows() {
        assertEquals("Last searches", tr("Last searches", "en"))
        assertEquals("Upload file", tr("Datei hochladen", "en"))
        assertEquals("Type a programme title, e.g. Formula 1.", tr("Tippe einen Sendungstitel ein, z. B. Formula 1.", "en"))
    }

    @Test fun dutch_arabic_polish_rows() {
        assertEquals("Recente zoekopdrachten", tr("Last searches", "nl"))
        assertEquals("عمليات البحث الأخيرة", tr("Last searches", "ar"))
        assertEquals("Wybierz ligę", tr("Liga wählen", "pl"))
    }
}
