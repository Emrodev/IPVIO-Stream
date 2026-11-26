package com.ipvio.stream

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import kotlinx.coroutines.delay

object ContentFocus {
    val req = FocusRequester()
    var tab = -1
}

/**
 * Last focused element in the content (cards, buttons). Right from the sidebar goes back there
 * instead of to the element that happens to lie next to the chosen menu entry (round 14).
 */
object ContentMemory {
    var last: FocusRequester? = null
    fun restore(): Boolean = last?.let { runCatching { it.requestFocus() }.getOrDefault(false) } ?: false
}

/** Remembers this element as last focused (see [ContentMemory]). */
@Composable
fun Modifier.trackFocus(): Modifier {
    val fr = remember { FocusRequester() }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { if (ContentMemory.last === fr) ContentMemory.last = null } }
    return this.focusRequester(fr).onFocusChanged { if (it.isFocused) ContentMemory.last = fr }
}

/** Like [trackFocus], but uses the given requester so the element has only ONE focusRequester (two on one node break requestFocus). */
@Composable
fun Modifier.trackFocus(fr: FocusRequester): Modifier {
    androidx.compose.runtime.DisposableEffect(fr) { onDispose { if (ContentMemory.last === fr) ContentMemory.last = null } }
    return this.focusRequester(fr).onFocusChanged { if (it.isFocused) ContentMemory.last = fr }
}

object LastFocus {
    var pending: String? = null
}

/**
 * Episode of a series last played in the player. On coming back the detail page opens exactly at this season/episode and
 * highlights it so you can pick another right away (instead of landing on "Resume" again). Read once and deleted.
 */
object DetailReturn {
    private var id: String? = null
    private var season = 0
    private var ep = 0
    fun set(itemId: String, s: Int, e: Int) { if (s > 0 || e > 0) { id = itemId; season = s; ep = e } }
    fun take(itemId: String): Pair<Int, Int>? = if (id == itemId) { id = null; season to ep } else null
}

object HomeFlags {
    var heroDone = false
}

object NavState {
    var tab = 0
}

/**
 * App-wide navigation not bound to a screen:
 * - [homeSignal]: counts up when the app is opened again after the Home key (or app switch) → back to the home screen.
 * - [focusTab]: tab whose content should get the focus after a switch when the whole screen was replaced
 *   (e.g. Start → Search). The old screen's sidebar is already gone and can no longer do that.
 */
object AppNav {
    var homeSignal by androidx.compose.runtime.mutableIntStateOf(0)
    /** The View settings ask the movie area to open its categories screen (counts up). */
    var openCatsSignal by androidx.compose.runtime.mutableIntStateOf(0)
    /**
     * W-002: set when the movie or series area was chosen in the menu and Settings > General > "Movies and series straight
     * to the categories" is on. The area opens its categories screen as soon as its data is ready (consumed once).
     */
    var pendingVodCats by androidx.compose.runtime.mutableIntStateOf(-1)
    /** Same for the "All movies" grid (consumed by the movie area). */
    var openBrowseSignal by androidx.compose.runtime.mutableIntStateOf(0)
    /** View settings preview: scroll the home screen to the first of these block keys ("a|b"); "" = nothing, "top" = to the top. */
    var homeScrollKey by androidx.compose.runtime.mutableStateOf("")
    /** Player open? Then the background refresh postpones reloading the playlist (no stutter in the middle of a movie). */
    @Volatile var playing = false
    /** Channel to be opened (tapping a reminder notification). */
    var openChannel by androidx.compose.runtime.mutableStateOf<String?>(null)
    @Volatile var leftByUser = false
    @Volatile var focusTab = -1
    /** Open the TV guide at the next start of the player (Live TV > "TV guide"). */
    @Volatile var openGuide = false
}

object RowStates {
    private val m = HashMap<String, LazyListState>()
    fun get(k: String): LazyListState = m.getOrPut(k) { LazyListState() }
}

/**
 * T-069b: the cards with [restoreFocus] that are composed right now, by title id (newest last). A screen that stayed composed
 * under a detail page (HomeScreen) puts the focus back on the card of the opened title with [request]; a screen built again
 * does it in [restoreFocus] itself, as before. Main thread only (composition effects).
 */
object FocusReturn {
    private val byId = HashMap<String, ArrayList<FocusRequester>>()
    fun add(id: String, fr: FocusRequester) { byId.getOrPut(id) { ArrayList(1) }.add(fr) }
    fun remove(id: String, fr: FocusRequester) {
        val l = byId[id] ?: return
        l.remove(fr)
        if (l.isEmpty()) byId.remove(id)
    }
    /** Focuses the last composed card of title [id]; false if none is composed or it cannot take the focus. */
    fun request(id: String): Boolean {
        val l = byId[id] ?: return false
        for (i in l.indices.reversed()) if (runCatching { l[i].requestFocus() }.getOrDefault(false)) return true
        return false
    }
}

/** T-069d: pause between two focus attempts of [focusSoon]. */
private const val FOCUS_STEP_MS = 40L

/**
 * T-069d: asks for the focus as soon as the target can take it, instead of after a fixed wait: first after the next frame
 * (the target is laid out then), then every 40 ms, and a last time once [maxMs] (the fixed wait used before) has passed.
 * Returns true as soon as [request] succeeded. Needs a frame clock (LaunchedEffect, rememberCoroutineScope).
 */
suspend fun focusSoon(maxMs: Long, request: () -> Boolean): Boolean {
    androidx.compose.runtime.withFrameNanos { }
    var waited = 0L
    while (waited < maxMs) {
        if (request()) return true
        delay(FOCUS_STEP_MS)
        waited += FOCUS_STEP_MS
    }
    return request()
}

@Composable
fun Modifier.restoreFocus(id: String): Modifier {
    val fr = remember { FocusRequester() }
    androidx.compose.runtime.DisposableEffect(id, fr) { FocusReturn.add(id, fr); onDispose { FocusReturn.remove(id, fr) } }
    LaunchedEffect(Unit) {
        if (LastFocus.pending == id) {
            // T-069d: as soon as the card is laid out (before: always after 150 ms)
            focusSoon(150) { runCatching { fr.requestFocus() }.getOrDefault(false) }
            LastFocus.pending = null
        }
    }
    return this.focusRequester(fr)
}
