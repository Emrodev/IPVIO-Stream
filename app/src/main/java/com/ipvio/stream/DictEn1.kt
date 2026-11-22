package com.ipvio.stream

// T-041, stage 1: English texts for German UI and message strings that had no dictionary row.
// Format: German key¦English. Other languages fall back to the English text (I18nCore.fill).
// The German key must equal the runtime text exactly; a row added here is not checked against the code.
val DictEn1 = """
Fehler¦Error
Zuletzt geschaut · Filme¦Recently watched · Movies
Zuletzt geschaut · Serien¦Recently watched · Series
Rechts/Links ändert den Rand seitlich, Hoch/Runter oben und unten, bis alle vier Ecken sichtbar sind. OK speichert.¦Left/Right moves the edge sideways, Up/Down at the top and bottom, until all four corners are visible. OK saves.
Links schließt das Menü  ·  Rechts wechselt zu den Untertiteln¦Left closes the menu  ·  Right switches to subtitles
Rechts / Vorspulen = 50 weiter · Zurückspulen = zurück · Links schließt¦Right / fast-forward = 50 ahead · Rewind = back · Left closes
Das Portal hat die Anmeldung abgelehnt. Adresse und MAC-Adresse prüfen.¦The portal rejected the login. Check the address and the MAC address.
Kategorien werden geladen …¦Loading categories …
Kategorien werden erneut versucht …¦Retrying categories …
Im Player wechselst du mit Links zu deinen Listen; Umschalten bleibt in der Liste.¦In the player, Left switches to your lists; switching stays in the list.
Welche Sender ein Archiv haben und wie viele Tage es reicht, bestimmt dein Anbieter.¦Which channels have an archive, and how many days it covers, is decided by your provider.
Nichts gefunden¦Nothing found
Zu viele Anfragen, bitte später erneut versuchen¦Too many requests, please try again later
Der Anbieter hat die Filmliste nicht geliefert. Die bisherige Liste bleibt erhalten – bitte später erneut versuchen.¦The provider did not deliver the movie list. The previous list is kept – please try again later.
Der Anbieter hat die Serienliste nicht geliefert. Die bisherige Liste bleibt erhalten – bitte später erneut versuchen.¦The provider did not deliver the series list. The previous list is kept – please try again later.
Der Anbieter hat die Senderliste nicht geliefert. Die bisherige Liste bleibt erhalten – bitte später erneut versuchen.¦The provider did not deliver the channel list. The previous list is kept – please try again later.
"""
