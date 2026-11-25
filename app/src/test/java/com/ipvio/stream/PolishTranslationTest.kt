package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

class PolishTranslationTest {
    private val pl = I18nCore.CODES.indexOf("pl")
    private val en = I18nCore.CODES.indexOf("en")

    @Test fun soundLevellingTextsAreTranslated() {
        // DictAudio existed but was not loaded: "Ton angleichen" stayed German in every language
        I18nCore.load(DictAudio, DictPhone)
        I18nCore.loadColumn(pl, DictPl9)
        assertEquals("Sound levelling", I18nCore.translate("Ton angleichen", en))
        assertEquals("Wyrównywanie dźwięku", I18nCore.translate("Ton angleichen", pl))
    }

    @Test fun phonePageTextsHaveADictionaryRow() {
        I18nCore.load(DictAudio, DictPhone)
        I18nCore.loadColumn(pl, DictPl9)
        assertEquals("Pobierz kopię zapasową", I18nCore.translate("Sicherung herunterladen", pl))
    }
}
