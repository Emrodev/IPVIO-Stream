package com.ipvio.stream

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch

private const val VOD_PREFS = "ipvio_vod"

/** T-052a: the notice about the provider terms is shown once, before the first download. */
fun noticeAccepted(c: Context): Boolean =
    runCatching { c.getSharedPreferences(VOD_PREFS, Context.MODE_PRIVATE).getBoolean("notice_ok", false) }.getOrDefault(false)

fun acceptNotice(c: Context) {
    runCatching { c.getSharedPreferences(VOD_PREFS, Context.MODE_PRIVATE).edit().putBoolean("notice_ok", true).apply() }
}

/**
 * Download button of a movie on the detail page (T-052a). Single files download at once. HLS playlists first show their
 * qualities (from the master playlist) and the user picks one. Pressed again when finished or failed, it removes the download.
 */
@Composable
fun VodDownloadButton(item: Item, url: String, h: Int) {
    if (item.kind != Kind.MOVIE || !DownloadLogic.supported(url)) return
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var askNotice by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var variants by remember { mutableStateOf<List<HlsLogic.Variant>>(emptyList()) }
    var picking by remember { mutableStateOf(false) }
    val st = DownloadStore.progress[DownloadLogic.keyFor(item)]

    fun start(variant: String?) {
        message = if (DownloadStore.enqueue(ctx, item, url, variant)) "" else "Nicht genug freier Speicher für einen Download."
    }

    // HLS: load the master playlist and offer its qualities; a single file or a media playlist starts at once
    fun prepare() {
        if (!HlsLogic.isHls(url)) { start(null); return }
        busy = true
        scope.launch {
            val text = EpgNet.fetch(url)
            busy = false
            if (text == null) { message = "Die Playlist konnte nicht geladen werden."; return@launch }
            val v = HlsLogic.variants(text, url)
            if (v.isEmpty()) start(null) else { variants = v; picking = true }
        }
    }

    val label = when {
        busy -> "Qualitäten werden geladen …"
        st == null -> "Herunterladen"
        st == DownloadStore.DONE || st == DownloadStore.FAILED -> "Download entfernen"
        else -> Lang.tr("Lädt {0} %").replace("{0}", st.toString())
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HeroButton(label, AIcon.Download, false, Modifier, h) {
            when {
                busy -> Unit
                st == DownloadStore.DONE || st == DownloadStore.FAILED -> DownloadStore.remove(ctx, item)
                st != null -> Unit
                noticeAccepted(ctx) -> prepare()
                else -> askNotice = true
            }
        }
        if (message.isNotEmpty()) Text(message, color = DS.Danger, fontSize = 14.sp)
    }
    if (askNotice) Dialog(onDismissRequest = { askNotice = false }) {
        Column(
            Modifier.width(560.dp).clip(RoundedCornerShape(DS.RadiusL)).background(Tok.GlassStrong).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Downloads sind nur für Inhalte gedacht, die du rechtmäßig nutzen darfst. Die Bedingungen deines Anbieters gelten. Die Dateien bleiben auf diesem Gerät.",
                color = Color.White, fontSize = 17.sp)
            PillButton("Verstanden", light = true) {
                acceptNotice(ctx)
                askNotice = false
                prepare()
            }
        }
    }
    if (picking) Dialog(onDismissRequest = { picking = false }) {
        Column(
            Modifier.width(560.dp).clip(RoundedCornerShape(DS.RadiusL)).background(Tok.GlassStrong).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Qualität wählen", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            variants.forEach { v ->
                PillButton(v.label, light = true, raw = true) {
                    picking = false
                    start(v.uri)
                }
            }
            PillButton("Abbrechen") { picking = false }
        }
    }
}

/**
 * T-055: "Staffel laden" on a series. The one-time notice of the provider terms is asked here too, because a series
 * never shows the movie button that asks it (before, the episodes were refused with a message about a movie).
 */
@Composable
fun SeasonDownloadButton(item: Item, eps: List<Episode>, urlOf: (Episode) -> String, h: Int) {
    val ctx = LocalContext.current
    var askNotice by remember { mutableStateOf(false) }
    fun start() {
        eps.forEach { ep -> startEpisodeDownload(ctx, item, ep, urlOf(ep), announce = false) }
        announceQueued(ctx, eps.size)
    }
    HeroButton("Staffel laden", AIcon.Download, false, Modifier, h) {
        if (noticeAccepted(ctx)) start() else askNotice = true
    }
    if (askNotice) Dialog(onDismissRequest = { askNotice = false }) {
        Column(
            Modifier.width(560.dp).clip(RoundedCornerShape(DS.RadiusL)).background(Tok.GlassStrong).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Downloads sind nur für Inhalte gedacht, die du rechtmäßig nutzen darfst. Die Bedingungen deines Anbieters gelten. Die Dateien bleiben auf diesem Gerät.",
                color = Color.White, fontSize = 17.sp)
            PillButton("Verstanden", light = true) {
                acceptNotice(ctx)
                askNotice = false
                start()
            }
        }
    }
}

/** Button "Pick episodes" on a series: opens [EpisodePickDialog] with the episodes of the shown season. */
@Composable
fun EpisodePickButton(item: Item, eps: List<Episode>, urlOf: (Episode) -> String, h: Int) {
    val ctx = LocalContext.current
    var open by remember { mutableStateOf(false) }
    HeroButton("Folgen wählen", AIcon.Download, false, Modifier, h) { open = true }
    if (open) EpisodePickDialog(ctx, item, eps, urlOf) { open = false }
}

/**
 * Episode list with check boxes: the checked episodes are queued together (one notice, the same rules as "Season download").
 * Nothing else on the detail page changes; this dialog is only opened by its own button.
 */
@Composable
private fun EpisodePickDialog(ctx: Context, item: Item, eps: List<Episode>, urlOf: (Episode) -> String, onClose: () -> Unit) {
    val picked = remember { mutableStateOf(setOf<Int>()) }
    var askNotice by remember { mutableStateOf(false) }

    fun startPicked() {
        val chosen = eps.filterIndexed { i, _ -> i in picked.value }
        chosen.forEach { ep -> startEpisodeDownload(ctx, item, ep, urlOf(ep), announce = false) }
        announceQueued(ctx, chosen.size)
        onClose()
    }

    Dialog(onDismissRequest = onClose) {
        // Fixed height: the list takes the space between title and buttons, so the buttons always stay visible
        Column(
            Modifier.width(720.dp).fillMaxHeight(0.9f).clip(RoundedCornerShape(DS.RadiusL)).background(Tok.GlassStrong).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (askNotice) {
                Text("Downloads sind nur für Inhalte gedacht, die du rechtmäßig nutzen darfst. Die Bedingungen deines Anbieters gelten. Die Dateien bleiben auf diesem Gerät.",
                    color = Color.White, fontSize = 17.sp)
                PillButton("Verstanden", light = true) {
                    acceptNotice(ctx)
                    startPicked()
                }
            } else {
                Text("Folgen auswählen", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                // Lazy list: the focused episode is scrolled into view automatically, and the remote scrolls it item by item
                LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    itemsIndexed(eps) { i, ep ->
                        val on = i in picked.value
                        // Focus = white frame and lighter background; selected = green box with a check mark and green text.
                        // Both at once are easy to tell apart on the TV.
                        var focused by remember { mutableStateOf(false) }
                        val rowShape = RoundedCornerShape(12.dp)
                        Row(
                            Modifier.fillMaxWidth().clip(rowShape)
                                .background(if (focused) Color(0x40FFFFFF) else if (on) Color(0x1F6BD39A) else Color(0x08FFFFFF))
                                .border(if (focused) 3.dp else 0.dp, if (focused) Color.White else Color.Transparent, rowShape)
                                .onFocusChanged { focused = it.isFocused }
                                .clickable { picked.value = if (on) picked.value - i else picked.value + i }
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(30.dp).clip(RoundedCornerShape(6.dp))
                                    .background(if (on) Color(0xFF6BD39A) else Color(0x33FFFFFF)),
                                contentAlignment = Alignment.Center
                            ) { if (on) AppIcon(AIcon.Check, Color(0xFF05070D), size = 20.dp) }
                            Text(raw = true, text = "S${ep.season}E${ep.num}  ${ep.title}",
                                color = if (on) Color(0xFF6BD39A) else Color.White,
                                fontSize = 18.sp, fontWeight = if (focused) FontWeight.Bold else FontWeight.Normal,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val n = picked.value.size
                    PillButton(if (n == 0) "Laden" else "Laden · $n", light = true) {
                        if (n == 0) return@PillButton
                        if (noticeAccepted(ctx)) startPicked() else askNotice = true
                    }
                    PillButton("Abbrechen") { onClose() }
                }
            }
        }
    }
}

/** T-052a: list of the downloads in Settings > Storage, with delete and the space they take. */
@Composable
fun DownloadsSettings(ctx: Context) {
    var wifi by remember { mutableStateOf(DownloadStore.wifiOnly(ctx)) }
    val dayChoices = listOf(0, 14, 30, 90)
    var autoIdx by remember { mutableStateOf(dayChoices.indexOf(DownloadStore.autoDeleteDays(ctx)).coerceAtLeast(0)) }
    SelectRow("Automatisch löschen", if (autoIdx == 0) "Aus" else Lang.tr("{0} Tage").replace("{0}", dayChoices[autoIdx].toString()),
        listOf("Aus", "14 Tage", "30 Tage", "90 Tage"), autoIdx, active = autoIdx > 0) { i ->
        autoIdx = i
        DownloadStore.setAutoDeleteDays(ctx, dayChoices[i])
    }
    Text("Nicht genutzte Downloads werden nach der gewählten Zeit gelöscht. Als genutzt gilt der Tag, an dem du den Download zuletzt abgespielt hast.", color = Muted, fontSize = 13.sp)
    ToggleRow("Nur im WLAN laden", "Downloads warten, wenn kein ungemessenes Netz (WLAN oder Ethernet) da ist.", wifi) {
        wifi = it
        DownloadStore.setWifiOnly(ctx, it)
    }
    DownloadList(ctx)
}

/** The list of all downloads with queue actions (used by the settings page and by the Downloads tab). */
@Composable
fun DownloadList(ctx: Context, onPlay: ((DownloadStore.Entry) -> Unit)? = null) {
    // Reading the progress map makes the list update while a download runs
    val rows = run { DownloadStore.progress.size; DownloadStore.entries(ctx) }
    if (rows.isEmpty()) {
        Text("Keine Downloads.", color = Muted, fontSize = 16.sp)
        return
    }
    Text("Gesamt: " + SettingsLogic.fmtBytes(rows.sumOf { it.bytes }), color = Accent2, fontSize = 15.sp)
    rows.forEach { r ->
        val status = when (r.state) {
            DownloadState.COMPLETED -> "Fertig"
            DownloadState.FAILED -> "Fehlgeschlagen"
            DownloadState.STOPPED -> "Pausiert"
            DownloadState.QUEUED -> "Wartet"
            else -> Lang.tr("Lädt {0} %").replace("{0}", (DownloadStore.progress[r.id] ?: 0).toString())
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.width(420.dp)) {
                Text(raw = true, text = r.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(status + "  ·  " + SettingsLogic.fmtBytes(r.bytes), color = Muted, fontSize = 13.sp)
            }
            // Queue actions: pause or continue, retry after a failure, delete
            if (r.state == DownloadState.COMPLETED && onPlay != null) PillButton("Abspielen", light = true) { onPlay(r) }
            if (r.state == DownloadState.STOPPED) PillButton("Fortsetzen", light = true) { DownloadStore.resume(ctx, r.id) }
            else if (r.state == DownloadState.DOWNLOADING || r.state == DownloadState.QUEUED) PillButton("Pausieren", light = true) { DownloadStore.pause(ctx, r.id) }
            if (r.state == DownloadState.FAILED) PillButton("Wiederholen", light = true) { DownloadStore.retry(ctx, r.id) }
            ConfirmPill("Löschen") { DownloadStore.removeId(ctx, r.id) }
        }
    }
    Text("Die Dateien liegen auf diesem Gerät. Löschen gibt den Speicher frei.", color = Muted, fontSize = 13.sp)
}

/** Episode as download item (T-052a): id from the series, season and number. */
fun episodeDownloadItem(item: Item, ep: Episode): Item =
    DownloadLogic.episodeItem(item, ep.season, ep.num, item.title + " S" + ep.season + "E" + ep.num)

/** Menu key on an episode (T-052a): starts its download in the best quality, after the one-time notice. */
fun startEpisodeDownload(c: Context, item: Item, ep: Episode, url: String, announce: Boolean = true) {
    val msg = when {
        !noticeAccepted(c) -> "Bestätige erst den Hinweis bei einem Film."
        !DownloadLogic.supported(url) -> "Dieser Stream lässt sich nicht herunterladen."
        !DownloadLogic.hasRoom(DownloadStore.freeBytes(c)) -> "Nicht genug freier Speicher für einen Download."
        else -> null
    }
    if (msg != null) {
        android.widget.Toast.makeText(c, Lang.tr(msg), android.widget.Toast.LENGTH_LONG).show()
        return
    }
    DownloadStore.startBest(c, episodeDownloadItem(item, ep), url)
    // Short confirmation for a single episode; batches show one message for all (see announceQueued)
    if (announce) {
        val title = episodeDownloadItem(item, ep).title
        android.widget.Toast.makeText(c, Lang.tr("Download gestartet: {0}").replace("{0}", title), android.widget.Toast.LENGTH_SHORT).show()
    }
}

/** One short message for a whole batch, so many queued episodes do not stack up as separate pop-ups. */
fun announceQueued(c: Context, count: Int) {
    android.widget.Toast.makeText(c, Lang.tr("{0} Folgen in der Warteschlange").replace("{0}", count.toString()), android.widget.Toast.LENGTH_SHORT).show()
}

/** Downloads tab in the sidebar (T-052a): the list with queue actions, with the sidebar beside it. */
@Composable
fun DownloadsTab(tab: Int, onTab: (Int) -> Unit, onPlay: (DownloadStore.Entry) -> Unit) {
    val ctx = LocalContext.current
    val sideFocus = remember { FocusRequester() }
    // The sidebar padding goes on the content only; on the box it would also push the sidebar to the right.
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().sidebarPad().padding(start = 48.dp, end = DS.PageX, top = 28.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Downloads", color = Color.White, fontSize = spx(36), fontWeight = FontWeight.ExtraBold)
            DownloadList(ctx, onPlay)
        }
        Sidebar(tab, onTab, Modifier.align(SidebarPrefs.align()), sideFocus) { }
    }
}

/** Media3 download states (Java constants of androidx.media3.exoplayer.offline.Download). */
private object DownloadState {
    val COMPLETED = androidx.media3.exoplayer.offline.Download.STATE_COMPLETED
    val FAILED = androidx.media3.exoplayer.offline.Download.STATE_FAILED
    val STOPPED = androidx.media3.exoplayer.offline.Download.STATE_STOPPED
    val QUEUED = androidx.media3.exoplayer.offline.Download.STATE_QUEUED
    val DOWNLOADING = androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING
}
