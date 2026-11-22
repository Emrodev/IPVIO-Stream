package com.ipvio.stream

// T-041, stage 2a: English rows for German text patterns with placeholders that no row covered.
// Placeholders {0}, {1} match any text; the German key must equal the runtime text around them.
// Other languages fall back to English (I18nCore.fill), as for DictEn1.
val DictEn2 = """
OpenSubtitles: Fehler {0}¦OpenSubtitles: error {0}
Server-Fehler {0}¦Server error {0}
Serien · {0}¦Series · {0}
Filme · {0}¦Movies · {0}
Sender werden geladen ({0}) …¦Loading channels ({0}) …
Kategorien werden geladen ({0}/{1}) …¦Loading categories ({0}/{1}) …
Suche „{0}“ · Filme¦Search „{0}“ · Movies
Suche „{0}“ · Serien¦Search „{0}“ · Series
Suche „{0}“ · Live TV¦Search „{0}“ · Live TV
"""
