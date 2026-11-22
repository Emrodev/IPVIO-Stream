package com.ipvio.stream

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * T-048a: all settings of one playlist in one window (opened with "Edit" on the playlist row).
 * Name, content mode, "shown additionally" and backup servers are a draft: they are stored only with "Apply" (Cancel and Back drop them).
 * "Make main list" and "Delete" are actions, not settings: they run at once.
 */
@Composable
internal fun PlaylistEditDialog(ctx: Context, index: Int, scope: CoroutineScope, onAdd: () -> Unit, onClose: () -> Unit) {
    val pl = PlaylistStore.lists.getOrNull(index) ?: return
    val url = pl.url
    val isActive = index == PlaylistStore.active
    val xtream = XtreamLogic.fromUrl(url) != null
    val orig = remember(url) {
        PlaylistManageLogic.Draft(pl.name, PlaylistMode.get(ctx, url), PlaylistStore.isShown(index) && !isActive, PlaylistStore.backups(ctx, url))
    }
    var name by remember(url) { mutableStateOf(orig.name) }
    var mode by remember(url) { mutableStateOf(orig.mode) }
    var shown by remember(url) { mutableStateOf(orig.shown) }
    val backups = remember(url) { backupList(orig.backups) }
    var backupErr by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.width(760.dp).clip(RoundedCornerShape(DS.RadiusL)).background(Tok.GlassStrong).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(Lang.tr("Playlist bearbeiten"), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(PlaylistStore.hostOf(url), color = Muted, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Column(Modifier.weight(1f, fill = false).heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Field(name, { name = it }, "Name der Playlist")
                SelectRow("Inhalt der Playlist", PlaylistMode.labels[mode], PlaylistMode.labels, mode, active = mode != PlaylistMode.ALL) { mode = it }
                if (!isActive && orig.shown) {
                    SelectRow("Zusätzlich angezeigt", if (shown) "An" else "Aus", listOf("An", "Aus"), if (shown) 0 else 1) { shown = it == 0 }
                }
                if (xtream) {
                    XtreamLogic.fromUrl(url)?.base?.let { main ->
                        Text("Hauptserver: $main", color = Color.White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        HostHealthLine(main)
                    }
                    BackupFields(backups)
                    PillButton("+ Backup-Server") { backups.add("") }
                    Hint(BACKUP_HINT)
                    StatusLine(backupErr, color = Color(0xFFFF8A80), fontSize = 15.sp)
                }
                // Actions (not part of the draft)
                if (!isActive) PillButton("Zur Hauptliste machen", modifier = Modifier.fillMaxWidth(), light = true) {
                    PlaylistStore.switchAsync(ctx, index); onClose()
                }
                ConfirmPill("Löschen") {
                    onClose()
                    PlaylistStore.remove(ctx, index)
                    if (PlaylistStore.lists.isEmpty()) onAdd()
                    else if (isActive) {
                        val u = PlaylistStore.lists[PlaylistStore.active].url
                        scope.launch { PlaylistStore.load(ctx, u, false) }
                    }
                }
            }
            Hint("Änderungen gelten erst, wenn du „Übernehmen“ wählst.")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PillButton("Übernehmen", light = true) {
                    val now = PlaylistManageLogic.Draft(name, mode, shown, backups.toList())
                    val ch = PlaylistManageLogic.changes(orig, now)
                    if (ch.backups && !backupsValid(backups)) { backupErr = BACKUP_INVALID; return@PillButton }
                    if (ch.rename) PlaylistStore.rename(ctx, index, name)
                    if (ch.backups) PlaylistStore.setBackups(ctx, url, backups.toList())
                    if (ch.mode) PlaylistMode.set(ctx, url, mode)
                    if (ch.shown && !shown) PlaylistStore.setShown(ctx, index, false)
                    if (ch.mode || ch.shown) scope.launch {
                        // The mode is applied while parsing the stored list again (no download); then the extra lists are rebuilt
                        if (ch.mode && !PlaylistStore.reapplyLanguages(ctx)) PlaylistStore.applyMode(ctx)
                        if (ch.shown) PlaylistStore.applyMulti(ctx)
                    }
                    onClose()
                }
                PillButton("Abbrechen", light = false) { onClose() }
            }
        }
    }
}
