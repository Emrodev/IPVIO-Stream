package com.ipvio.stream

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay

/** Pure rules for the "What's new" popup after an update (W-004), without Android so they can be tested. */
object WhatsNewLogic {
    /**
     * Show once per version and only after an update: not on a fresh install ([freshInstall]), not when the version was
     * already seen, and not when there are no notes for it.
     */
    fun shouldShow(lastSeenCode: Long, currentCode: Long, freshInstall: Boolean, hasNotes: Boolean): Boolean =
        hasNotes && !freshInstall && currentCode > 0 && lastSeenCode < currentCode
}

/**
 * "What's new" popup (W-004): appears once after the app was updated to a new version. The notes are built in per
 * version name (German UI text, translated like every other text); a version without notes shows nothing.
 * Update [NOTES] for every release.
 */
object WhatsNew {
    private const val PREFS = "ipvio_whatsnew"

    val NOTES: Map<String, List<String>> = mapOf(
        "1.4.3" to listOf(
            "Filme und Serien: auf Wunsch direkt zu den Kategorien (Einstellungen > Allgemein)",
            "Player: Untertitel- und Ton-Verzögerung im Menü einstellbar",
            "Player: das Menü schließt sich nur noch mit Zurück",
            "Live TV: OK gedrückt halten entfernt einen Sender aus „Zuletzt gesehen“",
            "Jugendschutz: Inhalte für Erwachsene auf Wunsch nie auf den Startseiten"
        )
    )

    var show by mutableStateOf(false)
        private set
    var name by mutableStateOf("")
        private set

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Once at start: decides whether the popup appears. The version counts as seen when the popup is closed. */
    fun check(c: Context) {
        val code = AppUpdate.currentCode(c)
        // Test builds ("1.4.3-test") use the notes of their release version
        val n = AppUpdate.currentName(c).substringBefore('-')
        val fresh = runCatching {
            val pi = c.packageManager.getPackageInfo(c.packageName, 0)
            pi.firstInstallTime == pi.lastUpdateTime
        }.getOrDefault(true)
        val last = prefs(c).getLong("seen_code", 0L)
        show = WhatsNewLogic.shouldShow(last, code, fresh, NOTES[n].orEmpty().isNotEmpty())
        name = n
        // Fresh install or no notes: nothing to show, remember the version so a later update compares against it
        if (!show && last < code) prefs(c).edit().putLong("seen_code", code).apply()
    }

    fun dismiss(c: Context) {
        show = false
        prefs(c).edit().putLong("seen_code", AppUpdate.currentCode(c)).apply()
    }
}

@Composable
fun WhatsNewHost() {
    if (!WhatsNew.show) return
    val ctx = LocalContext.current
    val notes = WhatsNew.NOTES[WhatsNew.name].orEmpty()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(150); runCatching { focus.requestFocus() } }
    Dialog(onDismissRequest = { WhatsNew.dismiss(ctx) }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xE6000000)), contentAlignment = Alignment.Center) {
            Column(
                Modifier.widthIn(max = 620.dp).clip(RoundedCornerShape(20.dp)).background(Tok.GlassStrong).padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Was ist neu", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text(Lang.tr("Version") + " " + WhatsNew.name, color = Tok.TextMid, fontSize = 18.sp, raw = true)
                for (n in notes) Text("•  " + Lang.tr(n), color = Color.White, fontSize = 17.sp, lineHeight = 24.sp, raw = true)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PillButton("OK", light = true, modifier = Modifier.focusRequester(focus)) { WhatsNew.dismiss(ctx) }
                }
            }
        }
    }
}
