package com.ipvio.stream

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * T-062: the buttons of the Live TV player the user chose (Settings > Live TV), per profile, file `ipvio_playerbtns`
 * (new, additive). The list logic is in [PlayerButtonsLogic]; a broken stored value falls back to the default.
 */
object PlayerButtonPrefs {
    /** The shown buttons, in order. */
    var live by mutableStateOf(PlayerButtonsLogic.DEFAULT)
        private set
    private var inited = false
    private fun prefs(c: Context) = Profiles.sp(c, "ipvio_playerbtns")

    fun init(c: Context) {
        if (inited) return
        inited = true
        live = PlayerButtonsLogic.decode(prefs(c).getString("live", null))
    }

    fun set(c: Context, list: List<String>) {
        init(c)
        live = list
        prefs(c).edit().putString("live", PlayerButtonsLogic.encode(list)).apply()
    }

    fun reset(c: Context) {
        init(c)
        live = PlayerButtonsLogic.DEFAULT
        prefs(c).edit().remove("live").apply()
    }

    /** German UI text of a button (the translation key; same text as on the button in the player). */
    fun label(id: String): String = when (id) {
        PlayerButtonsLogic.BACKLIVE -> "Zurück zu Live"
        PlayerButtonsLogic.REC -> "Aufnehmen"
        PlayerButtonsLogic.FROMSTART -> "Von Anfang an"
        PlayerButtonsLogic.ARCHIVE -> "Archiv: verpasste Sendungen"
        PlayerButtonsLogic.FAV -> "Favoriten"
        PlayerButtonsLogic.TRACKS -> "Audio & Untertitel"
        PlayerButtonsLogic.QUALITY -> "Qualität"
        PlayerButtonsLogic.SCORE -> "Score-Overlay"
        PlayerButtonsLogic.ASPECT -> "Bildformat"
        PlayerButtonsLogic.SECOND -> "Zusatzbild"
        PlayerButtonsLogic.EXTERNAL -> "Extern öffnen"
        else -> id
    }
}

/** Settings > Live TV: which buttons the Live TV player shows and in which order. */
@Composable
internal fun PlayerButtonsSection(ctx: Context) {
    PlayerButtonPrefs.init(ctx)
    val shown = PlayerButtonPrefs.live
    SectionTitle("Player-Tasten (Live TV)")
    Hint("Wähle, welche Tasten der Live-TV-Player zeigt und in welcher Reihenfolge. Die Spultasten (bei Sendern mit Archiv oder Zeitversatz) und „Einstellungen“ sind immer da. Bildformat, Zusatzbild und Extern öffnen findest du auch im Player-Menü „Einstellungen“.")
    for (id in PlayerButtonsLogic.ALL) {
        ToggleRow(PlayerButtonPrefs.label(id), null, id in shown) { PlayerButtonPrefs.set(ctx, PlayerButtonsLogic.toggle(shown, id)) }
    }
    if (shown.size > 1) {
        SectionTitle("Reihenfolge der Tasten")
        val positions = shown.indices.map { (it + 1).toString() }
        for ((i, id) in shown.withIndex()) {
            SelectRow(PlayerButtonPrefs.label(id), (i + 1).toString(), positions, i) { PlayerButtonPrefs.set(ctx, PlayerButtonsLogic.move(shown, id, it)) }
        }
    }
}
