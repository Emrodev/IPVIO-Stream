package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks every translation row: placeholders must be numbered uniquely and appear in every language. */
class DictConsistencyTest {
    private val ph = Regex("""\{#?(\d)\}""")

    @Test fun placeholders_eindeutig_und_in_allen_sprachen() {
        val problems = ArrayList<String>()
        for (block in listOf(DictA, DictB, DictC, DictD, DictE, DictF, DictG, DictH, DictI, DictJ, DictNl1, DictNl2, DictNl3, DictNl4, DictNl5, DictPl1, DictPl2, DictPl3, DictPl4, DictPl5)) {
            for (line in block.lineSequence()) {
                if (line.isBlank() || line.startsWith("#") || !line.contains('¦')) continue
                val cols = line.split('¦')
                val de = ph.findAll(cols[0]).map { it.groupValues[1] }.toList()
                if (de.size != de.toSet().size) problems.add("doppelt: " + cols[0])
                for (c in cols.drop(1)) {
                    if (c.isBlank()) continue
                    val ci = ph.findAll(c).map { it.groupValues[1] }.toSet()
                    if (ci != de.toSet()) problems.add("fehlt: " + cols[0] + " | " + c)
                }
            }
        }
        assertTrue(problems.joinToString("\n"), problems.isEmpty())
    }

    @Test fun niederlaendisch_deckt_alle_zeilen_ab() {
        I18nCore.load(DictA, DictB, DictC, DictD, DictE, DictF, DictG, DictH, DictI, DictJ)
        val nl = I18nCore.CODES.indexOf("nl")
        assertEquals(9, nl)
        I18nCore.loadColumn(nl, DictNl1, DictNl2, DictNl3, DictNl4, DictNl5)
        assertEquals("Playlist toevoegen", I18nCore.translate("Playlist hinzufügen", nl))
        assertEquals("8 afleveringen", I18nCore.translate("8 Folgen", nl))
        assertEquals("Nederlands", I18nCore.NATIVE[nl])
    }

    @Test fun zahlen_werden_richtig_eingesetzt() {
        I18nCore.load(DictA, DictB, DictC, DictD, DictE, DictF, DictG, DictH, DictI, DictJ)
        assertEquals("loading 5/10", I18nCore.translate("lädt 5/10", 1))
        assertEquals("8 episodes", I18nCore.translate("8 Folgen", 1))
        assertEquals("No connection: SocketException", I18nCore.translate("Keine Verbindung: SocketException", 1))
        assertEquals("Server not found (DNS)", I18nCore.translate("Server nicht gefunden (DNS)", 1))
    }

    @Test fun polishCoversEverySourceKey() {
        fun keys(blocks: List<String>) = blocks.flatMap { block ->
            block.lineSequence().filter { it.isNotBlank() && !it.startsWith("#") && it.contains('¦') }
                .map { it.substringBefore('¦').trim() }.toList()
        }.toSet()
        val base = listOf(DictA, DictB, DictC, DictD, DictE, DictF, DictG, DictH, DictI, DictJ)
        val polish = listOf(DictPl1, DictPl2, DictPl3, DictPl4, DictPl5)
        assertEquals(keys(base), keys(polish))
        assertTrue(polish.all { block ->
            block.lineSequence().filter { it.contains('¦') }.all { it.substringAfter('¦').isNotBlank() }
        })
    }

    @Test fun polishSupportsSystemLanguageAndDynamicText() {
        I18nCore.load(DictA, DictB, DictC, DictD, DictE, DictF, DictG, DictH, DictI, DictJ)
        val pl = I18nCore.CODES.indexOf("pl")
        assertTrue(pl > 0)
        assertEquals(pl, I18nCore.indexForSystem("pl"))
        assertEquals("Polski", I18nCore.NATIVE[pl])
        I18nCore.loadColumn(pl, DictPl1, DictPl2, DictPl3, DictPl4, DictPl5)
        assertEquals("Dodaj playlistę", I18nCore.translate("Playlist hinzufügen", pl))
        assertEquals("Pomiń czołówkę", I18nCore.translate("Intro überspringen", pl))
        assertEquals("Liczba odcinków: 8", I18nCore.translate("8 Folgen", pl))
        assertEquals("Następny odcinek: sezon 2, odcinek 3", I18nCore.translate("Nächste Folge: Staffel 2, Folge 3", pl))
        assertEquals("Brak wyników dla \"Test\"", I18nCore.translate("Keine Treffer für „Test“", pl))
        assertEquals("  Ustawienia ", I18nCore.translate("  Einstellungen ", pl))
    }

    @Test fun everyPolishRowTranslatesAtRuntime() {
        I18nCore.load(DictA, DictB, DictC, DictD, DictE, DictF, DictG, DictH, DictI, DictJ)
        val pl = I18nCore.CODES.indexOf("pl")
        val blocks = listOf(DictPl1, DictPl2, DictPl3, DictPl4, DictPl5)
        I18nCore.loadColumn(pl, *blocks.toTypedArray())
        val placeholder = Regex("""\{(#?)(\d)\}""")
        fun sample(text: String) = placeholder.replace(text.trim().replace("\\n", "\n")) {
            if (it.groupValues[1].isEmpty()) "Test" + it.groupValues[2]
            else (71 + it.groupValues[2].toInt()).toString()
        }
        for (block in blocks) for (line in block.lineSequence()) {
            if (!line.contains('¦') || line.startsWith("#")) continue
            val (source, translation) = line.split('¦')
            assertEquals(source, sample(translation), I18nCore.translate(sample(source), pl))
        }
    }
}
