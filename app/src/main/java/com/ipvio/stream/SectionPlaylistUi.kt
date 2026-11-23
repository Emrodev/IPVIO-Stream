package com.ipvio.stream

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * Playlist selection of one section (T-051): the pill shows the choice, the popup has a checkbox per playlist.
 * Playlists that do not fit the section (content mode) are shown greyed out with the reason.
 * A playlist that is not shown yet is activated as before (it is loaded), the selection is then cleared.
 */
@Composable
fun SectionPlaylistPill(tab: Int) {
    val ctx = LocalContext.current
    var open by remember { mutableStateOf(false) }
    val ver = AreaSource.version
    val sel = remember(ver) { AreaSource.selectedKeys(ctx, tab) }
    FilterPill(
        Lang.tr("Playlist") + ": " + sectionPlaylistValue(ctx, tab, sel),
        active = PlaylistStore.sourceNames.size > 1 && sel.isNotEmpty(),
        icon = FIcon.Grid, caret = true, raw = true
    ) { open = true }
    if (open) SectionPlaylistPopup(tab) { open = false }
}

/** Text of the pill: the selected playlist names, or "All playlists". Single playlist: its name. */
fun sectionPlaylistValue(c: Context, tab: Int, sel: Set<Int>): String {
    val lists = PlaylistStore.lists.toList()
    if (PlaylistStore.sourceNames.size < 2) return lists.getOrNull(PlaylistStore.active)?.name ?: ""
    val names = lists.filter { PlaylistStore.key(it.url) in sel }.map { it.name }
    return if (sel.isEmpty() || names.isEmpty()) Lang.tr("Alle Playlists") else names.joinToString(", ")
}

@Composable
fun SectionPlaylistPopup(tab: Int, onChanged: () -> Unit = {}, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val ver = AreaSource.version
    val sel = remember(ver) { AreaSource.selectedKeys(ctx, tab) }
    val lists = PlaylistStore.lists.toList()
    val shownUrls = PlaylistStore.shownUrls(ctx)
    val shownKeys = shownUrls.map { PlaylistStore.key(it) }.toSet()
    val eligibleShown = shownUrls
        .filter { SectionSourceLogic.eligible(PlaylistMode.get(ctx, it), tab) }
        .map { PlaylistStore.key(it) }.toSet()
    // Nothing selected = "All": a checkbox click starts from all shown playlists that fit the section
    val base = if (sel.isEmpty()) eligibleShown else sel
    Popup(popupPositionProvider = belowAnchor, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        val shape = RoundedCornerShape(DS.RadiusM)
        Column(
            Modifier.width(300.dp).heightIn(max = 380.dp).clip(shape)
                .background(Surf.copy(alpha = 0.97f)).border(1.dp, DS.Overlay3, shape).padding(8.dp)
        ) {
            LazyColumn {
                item {
                    OptionRow(Lang.tr("Alle Playlists"), sel.isEmpty()) {
                        AreaSource.setKeys(ctx, tab, emptySet()); onChanged()
                    }
                }
                itemsIndexed(lists) { i, pl ->
                    val key = PlaylistStore.key(pl.url)
                    val mode = PlaylistMode.get(ctx, pl.url)
                    when {
                        !SectionSourceLogic.eligible(mode, tab) ->
                            DisabledRow(pl.name + " · " + Lang.tr(PlaylistMode.labels.getOrElse(mode) { "" }))
                        key in shownKeys -> OptionRow(pl.name, key in base) {
                            val next = SectionSourceLogic.toggle(base, key)
                            // The last playlist cannot be unticked (that would mean "All"); all ticked = "All"
                            if (next.isNotEmpty()) {
                                AreaSource.setKeys(ctx, tab, if (next.containsAll(eligibleShown)) emptySet() else next)
                                onChanged()
                            }
                        }
                        else -> OptionRow(pl.name, false) {
                            AreaSource.setKeys(ctx, tab, emptySet())
                            PlaylistStore.switchAsync(ctx, i)
                            onChanged()
                            onDismiss()
                        }
                    }
                }
            }
        }
    }
}

/** A playlist that does not fit the section: visible, but not selectable. */
@Composable
private fun DisabledRow(text: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = Color.White.copy(alpha = 0.4f), fontSize = 16.sp, fontWeight = FontWeight.Normal, maxLines = 1)
    }
}
