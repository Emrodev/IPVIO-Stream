package com.ipvio.stream

// T-031 steps 4 and 5: Trakt sync texts (Settings > Import & Trakt), and the "What's new" lines of 1.5.1.
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
# "What's new" in 1.5.1
Neu: Trakt verbinden. Einstellungen > Import & Trakt > „Mit Trakt verbinden“, die Anmeldung läuft bequem über das Handy¦New: connect Trakt. Settings > Import & Trakt > "Connect Trakt", you sign in comfortably on your phone
Trakt-Abgleich: gesehene Filme und Folgen bekommen ihr Häkchen, Verlauf und Merkliste erscheinen als Kategorien bei Filme und Serien¦Trakt sync: watched movies and episodes get their check mark, your history and watchlist appear as categories in Movies and Series
Die App liest nur von Trakt und ändert nichts an deinem Trakt-Konto¦The app only reads from Trakt and changes nothing in your Trakt account
Fehlende Serien: gleichnamige Serien bei Xtream und Stalker erscheinen jetzt alle, jede in ihrer Kategorie¦Missing series: series with the same name on Xtream and Stalker now all appear, each in its own category
Xtream: Filme und Serien werden vollständig geladen, auch wenn der M3U-Link nur einen Teil liefert. Einmal „Neu laden“ drücken¦Xtream: movies and series load completely, also when the M3U link delivers only part of them. Press "Reload" once
Ton angleichen: leise Live-Sender klingen so laut wie Streaming-Apps (Einstellungen > Ton & Untertitel)¦Sound levelling: quiet live channels sound as loud as streaming apps (Settings > Audio & subtitles)
Staffeln zusammenfassen: Serien, die dein Anbieter Staffel für Staffel führt, werden eine Serie (Einstellungen > Playlists)¦Combine seasons: series your provider lists season by season become one series (Settings > Playlists)
Live-TV-Player: weniger Knöpfe, Zurückspulen bei Sendern mit Archiv und eine eigene Knopf-Auswahl¦Live TV player: fewer buttons, rewind on channels with an archive and your own choice of buttons
TV-Guide: mehr Sender bekommen ihr Programm, auch ohne passende Sender-ID¦TV guide: more channels get their programme, also without a matching channel ID
Playlist wechseln ohne Ladebildschirm, große Playlists werden schneller gelesen¦Switch playlists without the loading screen, large playlists are read faster
Fernbedienung: P+ und P- schalten im Player den Sender um¦Remote: P+ and P- switch channels in the player
MKV-Filme ohne Index lassen sich wieder spulen und fortsetzen¦MKV movies without an index can be skipped and resumed again
Gespeicherte Playlists verschwinden auf manchen Boxen nach einem Neustart nicht mehr¦Saved playlists no longer vanish on some boxes after a restart
Hochladen über die Handy-Seite funktioniert wieder¦Uploading through the phone page works again
Inhaltsmodus „Live TV + Filme & Serien“ wirkt sofort, auch während die Playlist lädt¦The content mode "Live TV + movies & series" applies at once, also while the playlist loads
""".trimIndent()
