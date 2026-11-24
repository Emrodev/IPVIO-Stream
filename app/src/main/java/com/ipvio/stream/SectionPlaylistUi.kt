package com.ipvio.stream

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay

/**
 * Playlist selection of one section (T-051): the pill shows the choice, the popup has a checkbox per playlist.
 * Playlists that do not fit the section (content mode) are shown greyed out with the reason.
 * Ticking a playlist that is not loaded yet loads it in addition to the main list; "All playlists" loads every playlist
 * that fits the section (maintainer decision, 23 Nov 2026). Memory is guarded by PlaylistStore.loadExtras.
 */
@Composable
fun SectionPlaylistPill(tab: Int) {
    val ctx = LocalContext.current
    var open by remember { mutableStateOf(false) }
    val ver = AreaSource.version
    val sel = remember(ver) { AreaSource.selectedKeys(ctx, tab) }
    // Box: the popup is placed below the pill (a Popup is anchored to its parent layout, not to the pill)
    Box {
        FilterPill(
            Lang.tr("Playlist") + ": " + sectionPlaylistValue(ctx, tab, sel),
            active = (PlaylistStore.sourceNames.size > 1 && sel.isNotEmpty()) || SectionSourceLogic.isNone(sel),
            icon = FIcon.Grid, caret = true, raw = true
        ) { open = true }
        if (open) SectionPlaylistPopup(tab) { open = false }
    }
}

/** Text of the pill: the selected playlist names, or "All playlists". Single playlist: its name. */
fun sectionPlaylistValue(c: Context, tab: Int, sel: Set<Int>): String {
    if (SectionSourceLogic.isNone(sel)) return Lang.tr("Keine Playlist")
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
    // Read the shown set (state) so the rows update when a playlist was loaded in addition
    PlaylistStore.multi
    val shownKeys = lists.indices.filter { PlaylistStore.isShown(it) }.map { PlaylistStore.key(lists[it].url) }.toSet()
    val eligible = lists.indices.filter { SectionSourceLogic.eligible(PlaylistMode.get(ctx, lists[it].url), tab) }
    val eligibleShown = eligible.map { PlaylistStore.key(lists[it].url) }.filter { it in shownKeys }.toSet()
    val first = remember { FocusRequester() }
    Popup(popupPositionProvider = belowAnchor, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        // Without a focused row the remote cannot reach the menu (D-pad and OK did nothing); same as OptionPopup
        LaunchedEffect(Unit) { delay(50); runCatching { first.requestFocus() } }
        val shape = RoundedCornerShape(DS.RadiusM)
        Column(
            Modifier.width(320.dp).heightIn(max = 400.dp).clip(shape)
                .background(Surf.copy(alpha = 0.97f)).border(1.dp, DS.Overlay3, shape).padding(8.dp)
        ) {
            LazyColumn(Modifier.weight(1f, fill = false)) {
                item {
                    // "All playlists": every playlist that fits the section is ticked and loaded (maintainer decision, 23 Nov 2026)
                    // Second press while all are ticked: untick all, then tick only the wanted ones (maintainer, 23 Nov 2026)
                    val all = sel.isEmpty() && eligible.all { PlaylistStore.key(lists[it].url) in shownKeys }
                    OptionRow(Lang.tr("Alle Playlists"), all, Modifier.focusRequester(first)) {
                        val next = SectionSourceLogic.clickAll(all)
                        AreaSource.setKeys(ctx, tab, next)
                        if (next.isEmpty()) PlaylistStore.showAsync(ctx, eligible)
                        onChanged()
                    }
                }
                itemsIndexed(lists) { _, pl ->
                    val key = PlaylistStore.key(pl.url)
                    val mode = PlaylistMode.get(ctx, pl.url)
                    val fits = SectionSourceLogic.eligible(mode, tab)
                    if (!fits) DisabledRow(pl.name + " · " + Lang.tr(PlaylistMode.labels.getOrElse(mode) { "" }))
                    else {
                        val shown = key in shownKeys
                        OptionRow(pl.name, SectionSourceLogic.checked(shown, true, key, sel)) {
                            val click = SectionSourceLogic.click(sel, eligibleShown, key, shown)
                            if (click.selection != sel) AreaSource.setKeys(ctx, tab, click.selection)
                            if (click.load) PlaylistStore.showAsync(ctx, listOf(PlaylistStore.lists.indexOfFirst { it.url == pl.url }))
                            onChanged()
                        }
                    }
                }
            }
            if (PlaylistStore.loading) Text(Lang.tr("Wird geladen …"), color = Accent2, fontSize = 14.sp, raw = true, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
            PlaylistStore.extraError?.let {
                Text(Lang.tr(it), color = DS.Danger, fontSize = 14.sp, maxLines = 3, raw = true, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
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
