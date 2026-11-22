package com.ipvio.stream

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color

private const val VOD_PREFS = "ipvio_vod"

/** T-052a: the notice about the provider terms is shown once, before the first download. */
private fun noticeAccepted(c: Context): Boolean =
    runCatching { c.getSharedPreferences(VOD_PREFS, Context.MODE_PRIVATE).getBoolean("notice_ok", false) }.getOrDefault(false)

private fun acceptNotice(c: Context) {
    runCatching { c.getSharedPreferences(VOD_PREFS, Context.MODE_PRIVATE).edit().putBoolean("notice_ok", true).apply() }
}

/**
 * Download button of a movie on the detail page (T-052a, step 1: single files only). Shown only when the address is a
 * downloadable file. Pressed again when finished or failed, it removes the download.
 */
@Composable
fun VodDownloadButton(item: Item, url: String, h: Int) {
    if (item.kind != Kind.MOVIE || !DownloadLogic.supported(url)) return
    val ctx = LocalContext.current
    var askNotice by remember { mutableStateOf(false) }
    var refused by remember { mutableStateOf(false) }
    val st = DownloadStore.progress[DownloadLogic.keyFor(item)]
    fun start() {
        refused = !DownloadStore.enqueue(ctx, item, url)
    }
    val label = when {
        st == null -> "Herunterladen"
        st == DownloadStore.DONE || st == DownloadStore.FAILED -> "Download entfernen"
        else -> Lang.tr("Lädt {0} %").replace("{0}", st.toString())
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HeroButton(label, AIcon.Download, false, Modifier, h) {
            when {
                st == DownloadStore.DONE || st == DownloadStore.FAILED -> DownloadStore.remove(ctx, item)
                st != null -> Unit
                noticeAccepted(ctx) -> start()
                else -> askNotice = true
            }
        }
        if (refused) Text("Nicht genug freier Speicher für einen Download.", color = DS.Danger, fontSize = 14.sp)
    }
    if (askNotice) Dialog(onDismissRequest = { askNotice = false }) {
        Column(
            Modifier.width(560.dp).clip(RoundedCornerShape(DS.RadiusL)).background(Tok.GlassStrong).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Downloads sind nur für Inhalte gedacht, die du rechtmäßig nutzen darfst. Die Bedingungen deines Anbieters gelten. Die Dateien bleiben auf diesem Gerät.",
                color = androidx.compose.ui.graphics.Color.White, fontSize = 17.sp)
            PillButton("Verstanden", light = true) {
                acceptNotice(ctx)
                askNotice = false
                start()
            }
        }
    }
}

/** T-052a: list of the downloads in Settings > Storage, with delete and the space they take. */
@Composable
fun DownloadsSettings(ctx: Context) {
    // Reading the progress map makes the list update while a download runs
    val rows = run { DownloadStore.progress.size; DownloadStore.entries(ctx) }
    if (rows.isEmpty()) {
        Text("Keine Downloads.", color = Muted, fontSize = 16.sp)
        return
    }
    Text("Gesamt: " + SettingsLogic.fmtBytes(rows.sumOf { it.bytes }), color = Accent2, fontSize = 15.sp)
    rows.forEach { r ->
        val st = DownloadStore.progress[r.id] ?: if (r.state == androidx.media3.exoplayer.offline.Download.STATE_COMPLETED) DownloadStore.DONE else 0
        val status = when (st) {
            DownloadStore.DONE -> "Fertig"
            DownloadStore.FAILED -> "Fehlgeschlagen"
            else -> Lang.tr("Lädt {0} %").replace("{0}", st.toString())
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.width(420.dp)) {
                Text(raw = true, text = r.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(status + "  ·  " + SettingsLogic.fmtBytes(r.bytes), color = Muted, fontSize = 13.sp)
            }
            ConfirmPill("Löschen") { DownloadStore.removeId(ctx, r.id) }
        }
    }
    Text("Die Dateien liegen auf diesem Gerät. Löschen gibt den Speicher frei.", color = Muted, fontSize = 13.sp)
}
