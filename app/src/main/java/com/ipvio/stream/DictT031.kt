package com.ipvio.stream

// T-031 steps 4 and 5: Trakt sync texts (Settings > Import & Trakt).
// Format: key¦English. Other languages show the English text until they are translated.
val DictT031 = """
Mit Trakt verbunden¦Connected to Trakt
Gesehene Titel bekommen ihr Häkchen, Verlauf und Merkliste erscheinen als Kategorien „Trakt“ bei Filme und Serien. Die App ändert nichts in deinem Trakt-Konto.¦Watched titles get their check mark, and your history and watchlist appear as "Trakt" categories in Movies and Series. The app changes nothing in your Trakt account.
Wird synchronisiert …¦Syncing …
Zuletzt synchronisiert: {0}¦Last synced: {0}
Jetzt synchronisieren¦Sync now
Trakt trennen¦Disconnect Trakt
Nicht mit Trakt verbunden.¦Not connected to Trakt.
Erst eine Playlist laden, dann synchronisieren.¦Load a playlist first, then sync.
Trakt hat die Anmeldung abgelehnt. Bitte neu verbinden.¦Trakt refused the sign-in. Please connect again.
Trakt ist gerade nicht erreichbar. Später erneut versuchen.¦Trakt cannot be reached right now. Try again later.
{#0} gesehen · {#1} auf der Merkliste¦{#0} watched · {#1} on the watchlist
""".trimIndent()
