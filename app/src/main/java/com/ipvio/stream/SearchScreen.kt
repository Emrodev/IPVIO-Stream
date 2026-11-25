package com.ipvio.stream

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import coil.compose.AsyncImage
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val S_SORT_AZ = 6
private const val S_SORT_PLAYLIST = 8

/** Index of the current list – is kept when leaving and coming back. */
internal object SearchCache {
    private var idx: SearchIndex? = null
    @Synchronized fun get(all: List<Item>): SearchIndex? = idx?.takeIf { it.all === all }
    @Synchronized fun build(all: List<Item>): SearchIndex = get(all) ?: SearchIndex(all).also { idx = it }

    private var sug: Pair<List<Item>, WordSuggest>? = null
    @Synchronized fun suggest(all: List<Item>): WordSuggest = sug?.takeIf { it.first === all }?.second ?: WordSuggest(all).also { sug = all to it }
}

/** Programme hits for the typed text: [live] = shows on other channels running now, [sport] = sport shows running now or in the next 24 hours. */
private class ProgState(val live: List<ProgSearchLogic.Hit>, val sport: List<ProgSearchLogic.Hit>)

/** One card of a result row: the channel, and the programme when the card is a programme hit (null = channel name hit). */
private class HitCard(val item: Item, val program: ProgSearchLogic.Hit?)

/**
 * Programme search for the TV (T-051b): the running shows of the live channels and the sport shows, matched against the typed text.
 * XMLTV guide first; Xtream channels without XMLTV get their short guide (at most [ProgSearchLogic.XTREAM_FETCH_PER_REQUEST], 9 s).
 */
private suspend fun progHits(all: List<Item>, q: String): ProgState {
    val xt = all.filter { it.kind == Kind.LIVE && !Xmltv.hasItem(it) && Epg.supported(it) && Epg.peek(it) == null }
        .take(ProgSearchLogic.XTREAM_FETCH_PER_REQUEST)
    if (xt.isNotEmpty()) withTimeoutOrNull(9_000) { coroutineScope { xt.map { async { Epg.now(it) } }.awaitAll() } }
    val now = System.currentTimeMillis() / 1000
    val sportIds = HashSet<String>()
    val hits = ArrayList<ProgSearchLogic.Hit>()
    for (c in all) {
        if (c.kind != Kind.LIVE) continue
        if (SearchSport.isSport(c)) sportIds.add(c.id)
        val list = if (Xmltv.hasItem(c)) Xmltv.forItem(c) else Epg.peek(c).orEmpty()
        for (e in list) if (ProgSearchLogic.matches(e.title, q)) hits.add(ProgSearchLogic.Hit(c.id, c.title, e.title, e.start, e.end))
    }
    val live = ProgSearchLogic.select(hits.filter { it.channelId !in sportIds }, now, ProgSearchLogic.Mode.LIVE)
    val sport = (ProgSearchLogic.select(hits.filter { it.channelId in sportIds }, now, ProgSearchLogic.Mode.LIVE) +
        ProgSearchLogic.select(hits.filter { it.channelId in sportIds }, now, ProgSearchLogic.Mode.UPCOMING)).sortedBy { it.start }
    return ProgState(live, sport)
}

/** Live channel cards: the channel name hits (with their running show if one matches), then the programme hits of other channels. */
private fun liveCardsOf(names: List<Item>, progs: List<ProgSearchLogic.Hit>, byId: Map<String, Item>): List<HitCard> {
    val nameIds = names.mapTo(HashSet()) { it.id }
    val out = ArrayList<HitCard>()
    for (c in names) out.add(HitCard(c, progs.firstOrNull { it.channelId == c.id }))
    for (h in progs) if (h.channelId !in nameIds) byId[h.channelId]?.let { out.add(HitCard(it, h)) }
    return out
}

/** Share of a running show that has passed, 0..1. */
private fun progressOf(h: ProgSearchLogic.Hit): Float {
    val now = System.currentTimeMillis() / 1000
    return ((now - h.start).toFloat() / (h.end - h.start).coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
}

/** Search (tab 4). T-051b layout (from the maintainer's sketch, 1920x1080): navigation on the left, a fixed search column
 *  (heading, field, hit count, suggestions or last searches, on-screen keyboard), and on the right the hero of the focused hit,
 *  the category tiles and the result rows. Search logic and filters are unchanged; programme hits are added for the live and sport rows. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    tab: Int, state: LazyListState,
    onOpen: (Item) -> Unit, onBrowse: (BrowseReq) -> Unit, onTab: (Int) -> Unit, onPerson: (PersonRef) -> Unit = {}
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    // Private categories (T-028) are not searchable; they stay reachable through their own category
    val all = PlaylistStore.channels.let { PrivateCats.visible(it) }
    val byId = remember(all) { all.associateBy { it.id } }
    // Build the index in the background; searches also run in the background and only after a short typing pause
    // With very large playlists memory can run out while the index is built: then free the image cache and try once more,
    // and if it still does not fit, the search stays empty instead of the app crashing.
    val index by produceState(SearchCache.get(all), all) {
        value = withContext(Dispatchers.Default) {
            if (Mem.heapTight(0.6)) Mem.trim(ctx)
            try { SearchCache.build(all) } catch (e: OutOfMemoryError) {
                Mem.trim(ctx)
                try { SearchCache.build(all) } catch (e2: OutOfMemoryError) { null }
            }
        }
    }
    // Word suggestions are optional: built only after the index, skipped when memory is tight
    val suggestIdx by produceState<WordSuggest?>(null, all, index != null) {
        if (index == null) return@produceState
        value = withContext(Dispatchers.Default) {
            if (Mem.heapTight(0.7)) null else try { SearchCache.suggest(all) } catch (e: OutOfMemoryError) { null }
        }
    }
    val firstSugg = remember { FocusRequester() }
    // Recently watched: resolved in one pass over the playlist in the background (order of the history is kept)
    val hist by produceState(emptyList<Item>(), all, History.ids.size) {
        val want = History.ids.toList()
        value = withContext(Dispatchers.Default) {
            val pos = HashMap<String, Int>(want.size * 2)
            want.forEachIndexed { i, id -> pos.putIfAbsent(id, i) }
            val out = arrayOfNulls<Item>(want.size)
            for (x in all) { val p = pos[x.id] ?: continue; if (out[p] == null) out[p] = x }
            out.filterNotNull()
        }
    }
    val firstRes = remember { FocusRequester() }
    val firstHistory = remember { FocusRequester() }
    val sideFocus = remember { FocusRequester() }
    val tilesFirst = remember { FocusRequester() }
    val heroFirst = remember { FocusRequester() }
    var inSidebar by remember { mutableStateOf(false) }

    SideEffect { NavState.tab = tab; ContentFocus.tab = tab }
    var canVoice by remember { mutableStateOf(VoiceSearch.available(ctx)) }
    val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        res.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.takeIf { it.isNotBlank() }?.let {
            SearchState.query = it
            AppSettings.addSearch(ctx, it)
        }
    }
    var fieldFocused by remember { mutableStateOf(false) }
    var focusItem by remember { mutableStateOf<Item?>(null) }
    // When opening the search go straight to the search field (not when returning from a detail page: then to the chosen title).
    // Only stop when the field REALLY has the focus – the sidebar still holds it briefly when switching.
    LaunchedEffect(Unit) {
        if (LastFocus.pending != null) return@LaunchedEffect
        AppNav.focusTab = -1
        repeat(20) {
            delay(100)
            if (fieldFocused) return@LaunchedEffect
            runCatching { ContentFocus.req.requestFocus() }
        }
    }
    BackHandler(enabled = inSidebar && tab != 0) { onTab(0) }

    val q = SearchState.query.trim()
    val none = Triple(emptyList<Item>(), emptyList<Item>(), emptyList<Item>())
    // A result always belongs to exactly one input: while still searching it says "Searching …" instead of "No hits"
    val found by produceState("" to none, q, index) {
        val idx = index ?: return@produceState
        if (q.length < 2) { value = q to none; return@produceState }
        delay(250)
        // Also the de-duplication of movie versions runs here in the background (for short queries it can be tens of
        // thousands of hits, before it ran on the main thread while typing)
        value = q to withContext(Dispatchers.Default) {
            idx.find(q).let { (m, s, l) -> Triple(PlaylistLogic.firstPerKey(m) { it.mk + "|" + it.lang }, s, l) }
        }
    }
    val searching = q.length >= 2 && found.first != q
    val (rm0, rs0, rl0) = if (q.length < 2 || searching) none else found.second
    val fl = SearchState.filter
    // Word suggestions for the word being typed
    val sugg = remember(SearchState.query, suggestIdx) { suggestIdx?.suggest(WordSuggest.lastWord(SearchState.query)).orEmpty() }
    // Programme hits (running shows, sport shows); the result is kept only for the query it was made for
    val progFor by produceState(q to ProgState(emptyList(), emptyList()), q, all) {
        if (q.length < 2) { value = q to ProgState(emptyList(), emptyList()); return@produceState }
        delay(250)
        value = q to progHits(all, q)
    }
    val prog = if (progFor.first == q && !searching) progFor.second else ProgState(emptyList(), emptyList())
    // Sport: channel name hits that look like sports channels (sport filter), and the sport shows
    val rlSport = remember(rl0) { rl0.filter { SearchSport.isSport(it) } }
    val liveAll = remember(rl0, prog.live, byId) { liveCardsOf(rl0, prog.live, byId) }
    val sportAll = remember(prog.sport, byId) { prog.sport.mapNotNull { h -> byId[h.channelId]?.let { HitCard(it, h) } } }
    // People (T-053c): TMDB person search, debounced; empty without a TMDB key
    val people by produceState(emptyList<PersonRef>(), q) {
        if (q.length < 2) { value = emptyList(); return@produceState }
        delay(350)
        value = CreditsRepo.searchPeople(q)
    }
    val pe = if (fl == 0 || fl == 5) people else emptyList()
    // Rows of the selected category
    val rm = if (fl == 0 || fl == 1) rm0 else emptyList()
    val rs = if (fl == 0 || fl == 2) rs0 else emptyList()
    val liveCards: List<HitCard> = when (fl) { 0, 3 -> liveAll; 4 -> rlSport.map { HitCard(it, null) }; else -> emptyList() }
    val sportCards: List<HitCard> = if (fl == 0 || fl == 4) sportAll else emptyList()
    val firstKey = when { rm.isNotEmpty() -> "rm"; rs.isNotEmpty() -> "rs"; liveCards.isNotEmpty() -> "rl"; sportCards.isNotEmpty() -> "sp"; pe.isNotEmpty() -> "pe"; else -> "" }   // people last, like in rowOrder: with only people hits Down must still reach them
    // Back: from a hit (or any other element) first to the input (field or app keyboard); from the input to the sidebar
    BackHandler(enabled = !inSidebar) {
        // requestFocus returns false (no exception) when the target is not on screen: then go on to the sidebar instead of doing nothing
        val toInput = !fieldFocused && runCatching { ContentFocus.req.requestFocus() }.getOrDefault(false)
        if (!toInput) runCatching { sideFocus.requestFocus() }
    }

    // Hero: the focused hit, or the first hit (the best match) when nothing is focused
    val preview = focusItem ?: (rm.firstOrNull() ?: rs.firstOrNull() ?: liveCards.firstOrNull()?.item ?: sportCards.firstOrNull()?.item ?: hist.firstOrNull())
    // Category tiles: the counts are derived from the hits (sport channels are part of the live channels too)
    val countAll = rm0.size + rs0.size + liveAll.size + sportAll.size + people.size
    val tileCounts = listOf(
        Triple(0, AIcon.Grid, countAll),
        Triple(2, AIcon.Layers, rs0.size),
        Triple(3, AIcon.Live, liveAll.size),
        Triple(4, AIcon.Pulse, rlSport.size + sportAll.size),
        Triple(1, AIcon.Categories, rm0.size),
        Triple(5, AIcon.Person, people.size)
    )
    // Result rows: the category of the best match comes first
    val bestKey = when {
        preview != null && sportAll.any { it.item.id == preview.id } -> "sp"
        preview?.kind == Kind.SERIES -> "rs"
        preview?.kind == Kind.LIVE -> "rl"
        else -> "rm"
    }
    val rowOrder = listOf(bestKey) + listOf("rm", "rs", "rl", "sp", "pe").filter { it != bestKey }
    // Suggestions: titles of the hits that match the typed text
    val suggItems = remember(rm0, rs0, rl0, q, searching) {
        if (q.length < 2 || searching) emptyList() else (rm0 + rs0 + rl0).distinctBy { it.title }.take(3)
    }
    val selectedEmpty = when (fl) {
        1 -> rm.isEmpty()
        2 -> rs.isEmpty()
        3 -> liveCards.isEmpty()
        4 -> liveCards.isEmpty() && sportCards.isEmpty()
        5 -> people.isEmpty()
        else -> rm.isEmpty() && rs.isEmpty() && liveCards.isEmpty() && sportCards.isEmpty()
    }

    Box(Modifier.fillMaxSize()) {
        // Backdrop of the focused title; the left panel covers its part. Darkened (dim 0.5) so white text stays readable
        BillboardBackdrop(preview, Modifier.fillMaxSize(), dim = 0.5f)
        Row(Modifier.fillMaxSize().sidebarPad()) {
            // Left panel (sketch: 601 px wide, 37 px side margins, own background, thin line on the right):
            // heading, field, hit count, fixed area (suggestions or last searches), on-screen keyboard
            Column(
                Modifier.width(sk(601)).fillMaxHeight().background(Bg.copy(alpha = 0.94f))
                    .drawBehind { drawLine(DS.Hairline, Offset(size.width - 1f, 0f), Offset(size.width - 1f, size.height), 2f) }
                    .padding(start = sk(37), end = sk(37), top = sk(44), bottom = sk(40))
            ) {
                Text(Lang.tr("Suche"), color = Color.White, fontSize = skf(36), fontWeight = FontWeight.ExtraBold, raw = true)
                Spacer(Modifier.height(sk(22)))
                Box(Modifier.fillMaxWidth()) {
                    if (AppSettings.systemKeyboard) {
                        // The device's own keyboard (e.g. Gboard) in a normal text field: OK or a tap opens it
                        Field(
                            SearchState.query, { SearchState.query = it.take(60) }, "Filme, Serien oder Sender suchen…",
                            Modifier.fillMaxWidth(),
                            inner = Modifier.focusRequester(ContentFocus.req).onFocusChanged { fieldFocused = it.isFocused || it.hasFocus },
                            onDone = { SearchState.query.trim().takeIf { it.length >= 2 }?.let { AppSettings.addSearch(ctx, it) } },
                            height = sk(79)
                        )
                    } else {
                        // Tappable: OK or a tap opens the device keyboard for typing (the app keyboard below stays available).
                        // Deliberately not wired to fieldFocused: that flag belongs to the BackHandler and tracks the app keyboard only.
                        Field(
                            SearchState.query, { SearchState.query = it.take(60) }, "Filme, Serien oder Sender suchen…",
                            Modifier.fillMaxWidth(),
                            onDone = { SearchState.query.trim().takeIf { it.length >= 2 }?.let { AppSettings.addSearch(ctx, it) } },
                            height = sk(79)
                        )
                    }
                    // Clear button (X) at the right end of the field
                    if (SearchState.query.isNotEmpty()) Box(
                        Modifier.align(Alignment.CenterEnd).padding(end = sk(14)).size(sk(48)).clip(RoundedCornerShape(sk(10)))
                            .background(Surf2).clickable { SearchState.query = "" },
                        contentAlignment = Alignment.Center
                    ) { AppIcon(AIcon.Close, Color.White, size = sk(20)) }
                }
                Spacer(Modifier.height(sk(12)))
                Text(Lang.tr("{0} Treffer in {1} Kategorien").replace("{0}", countAll.toString())
                    .replace("{1}", listOf(rm0.size, rs0.size, liveAll.size, sportAll.size).count { it > 0 }.toString()),
                    color = Muted, fontSize = skf(19), modifier = Modifier.padding(start = sk(4)), raw = true)
                Spacer(Modifier.height(sk(24)))
                // Fixed area: the same height in both states, so the keyboard below never moves
                Column(Modifier.fillMaxWidth().height(sk(290)).clipToBounds(), verticalArrangement = Arrangement.spacedBy(sk(9))) {
                    if (SearchState.query.isNotBlank()) {
                        Text(Lang.tr("Vorschläge"), color = Muted, fontSize = skf(20), fontWeight = FontWeight.Bold, raw = true)
                        // Up to 4 rows: titles of the hits first, then completions of the word being typed
                        val titleRows = suggItems.take(4)
                        val wordRows = sugg.map { WordSuggest.apply(SearchState.query, it) }
                            .filter { w -> titleRows.none { it.title.equals(w.trim(), ignoreCase = true) } }
                            .distinct().take(4 - titleRows.size)
                        titleRows.forEachIndexed { i, item ->
                            SuggestionRow(item.title, suggestLabel(item), q, if (i == 0) Modifier.focusRequester(firstSugg) else Modifier) {
                                SearchState.query = item.title.take(60)
                                runCatching { ContentFocus.req.requestFocus() }
                            }
                        }
                        wordRows.forEachIndexed { i, w ->
                            SuggestionRow(w.trim(), "Alle", q, if (i == 0 && titleRows.isEmpty()) Modifier.focusRequester(firstSugg) else Modifier) {
                                SearchState.query = w.take(60)
                                // #101: the list changes after a pick and the focused row disappears, so focus would fall
                                // to the sidebar. Keep it in the search screen: back to the input (same target as Back).
                                runCatching { ContentFocus.req.requestFocus() }
                            }
                        }
                    } else {
                        // Field empty: last searches as chips, "Clear history" on the right of the heading
                        val allRecent = AppSettings.searchHistory.toList()
                        val recent = allRecent.take(3)
                        var showAllHistory by remember { mutableStateOf(false) }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(Lang.tr("Letzte Suchen"), color = Muted, fontSize = skf(20), fontWeight = FontWeight.Bold, raw = true, modifier = Modifier.weight(1f))
                            if (allRecent.isNotEmpty()) SearchLink("Verlauf löschen") { AppSettings.clearSearch(ctx) }
                        }
                        if (allRecent.isNotEmpty()) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(sk(10)), verticalAlignment = Alignment.CenterVertically) {
                            recent.forEachIndexed { i, term ->
                                // OK searches again, long OK / menu key removes just this entry
                                MiniChip(term, false, (if (i == 0) Modifier.focusRequester(firstHistory) else Modifier)
                                    .menuKeys(onMenu = { AppSettings.removeSearch(ctx, term) }, onLong = { AppSettings.removeSearch(ctx, term) }), raw = true, icon = AIcon.History) { SearchState.query = term }
                            }
                            if (allRecent.size > 3) MiniChip("Alle anzeigen", false) { showAllHistory = true }
                        }
                        if (showAllHistory) Dialog(onDismissRequest = { showAllHistory = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
                            Column(Modifier.width(620.dp).heightIn(max = 640.dp).clip(RoundedCornerShape(DS.RadiusL)).background(Tok.GlassStrong).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(Lang.tr("Letzte Suchanfragen"), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        allRecent.forEach { term ->
                                            MiniChip(term, false, raw = true) { SearchState.query = term; showAllHistory = false }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // Keyboard at the bottom of the panel (maintainer, 23 Nov 2026: no empty space below it)
                Spacer(Modifier.weight(1f))
                if (!AppSettings.systemKeyboard) {
                // Own on-screen keyboard (instead of the TV's keyboard): hits appear while typing
                SketchKeyboard(
                    Modifier.fillMaxWidth(),
                    onKey = { SearchState.query = (SearchState.query + it).take(60) },
                    onSpace = { if (SearchState.query.isNotEmpty() && !SearchState.query.endsWith(" ")) SearchState.query += " " },
                    onDelete = { SearchState.query = SearchState.query.dropLast(1) },
                    onClear = { SearchState.query = "" },
                    onPhone = { RemoteUi.show = true },
                    onVoice = if (canVoice) ({ runCatching { voice.launch(VoiceSearch.intent()) }.onFailure { canVoice = false } }) else null,
                    firstKey = ContentFocus.req,
                    onFocusChange = { fieldFocused = it },
                    onRight = { runCatching { tilesFirst.requestFocus() }.isSuccess }
                )
                }
            }
            // Right side (sketch: 64 px margins): hero of the focused hit, its buttons, category tiles, result rows
            Column(Modifier.weight(1f).fillMaxHeight()) {
                // Fixed height, so the tiles below do not jump when the focused hit changes
                SearchHeroInfo(preview, Modifier.fillMaxWidth().height(sk(337)).padding(start = sk(64), end = sk(64), top = sk(56)))
                Spacer(Modifier.height(sk(20)))
                // Detail page: a marked title is opened with OK (no own "Watch" button, maintainer decision 22 and 23 Nov 2026)
                if (preview != null) Row(Modifier.padding(start = sk(64)), horizontalArrangement = Arrangement.spacedBy(sk(14)), verticalAlignment = Alignment.CenterVertically) {
                    val watched = Library.isWatch(preview.id)
                    HeroButton("Details", AIcon.Info, Modifier.focusRequester(heroFirst)) { onOpen(preview) }
                    HeroButton(if (watched) "Auf der Merkliste" else "Merkliste", if (watched) AIcon.Check else AIcon.Plus) { Library.toggleWatch(ctx, preview.id) }
                } else Spacer(Modifier.height(sk(64)))
                // Category tiles: the selected one filters the rows below; tiles with 0 hits are dimmed but still focusable
                Row(
                    Modifier.fillMaxWidth().padding(start = sk(64), end = sk(64), top = sk(36)).onPreviewKeyEvent { e ->
                        if (e.type != KeyEventType.KeyDown) false
                        else when (e.key) {
                            Key.DirectionUp -> runCatching { heroFirst.requestFocus() }.isSuccess
                            Key.DirectionDown -> runCatching { firstRes.requestFocus() }.isSuccess
                            else -> false
                        }
                    },
                    horizontalArrangement = Arrangement.spacedBy(sk(15))
                ) {
                    tileCounts.forEachIndexed { i, (filter, icon, count) ->
                        CategoryTile(
                            icon, count, SEARCH_FILTERS[filter], fl == filter,
                            Modifier.weight(1f).then(if (i == 0) Modifier.focusRequester(tilesFirst) else Modifier)
                        ) { SearchState.filter = filter }
                    }
                }
                // Result rows (scroll below the tiles)
                Box(Modifier.weight(1f).fillMaxWidth()) {
                CompositionLocalProvider(LocalSearchFocus provides { focusItem = it }) {
                    LazyColumn(Modifier.fillMaxWidth(), state = state, contentPadding = PaddingValues(bottom = sk(48))) {
                if (fl == 4) item(key = "hubbtn") {
                    Row(Modifier.padding(start = sk(64), end = sk(64), top = sk(24))) {
                        PillButton("Sports Hub öffnen", icon = AIcon.Live) { onTab(MenuLogic.SPORT) }
                    }
                }
                if (q.length < 2) {
                    val hm = hist.filter { it.kind == Kind.MOVIE }
                    val hs = hist.filter { it.kind == Kind.SERIES }
                    val hl = hist.filter { it.kind == Kind.LIVE }
                    if (hm.isNotEmpty()) item(key = "hm") {
                        SearchRow("Zuletzt geschaut · Filme", hm.take(30), onOpen) {
                            onBrowse(BrowseReq("Zuletzt geschaut · Filme", hm, S_SORT_PLAYLIST))
                        }
                    }
                    if (hs.isNotEmpty()) item(key = "hs") {
                        SearchRow("Zuletzt geschaut · Serien", hs.take(30), onOpen) {
                            onBrowse(BrowseReq("Zuletzt geschaut · Serien", hs, S_SORT_PLAYLIST))
                        }
                    }
                    if (hl.isNotEmpty()) item(key = "hl") {
                        SearchRow("Zuletzt geschaut · Live TV", hl.take(30), onOpen) {
                            onBrowse(BrowseReq("Zuletzt geschaut · Live TV", hl, S_SORT_PLAYLIST))
                        }
                    }
                } else {
                    val openS: (Item) -> Unit = { i -> AppSettings.addSearch(ctx, q); onOpen(i) }
                    if (searching) item {
                        Text("Suche …", color = Muted, fontSize = skf(20), modifier = Modifier.padding(horizontal = sk(64), vertical = sk(32)))
                    } else if (selectedEmpty) {
                        if (fl == 0) item {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                EmptyState(AIcon.Info, "Keine Treffer", "Prüfe die Schreibweise oder suche nach einem Teil des Titels. Kleine Tippfehler findet die Suche trotzdem.")
                            }
                        } else item(key = "nohits") {
                            // Selected category has no hits (sketch: dashed box): say so, name the categories that do have hits, offer all hits
                            val others = listOf("Filme" to rm0.size, "Serien" to rs0.size, "Live TV" to liveAll.size, "Sport" to sportAll.size, "Personen" to people.size)
                                .filter { it.second > 0 }.joinToString(", ") { Lang.tr(it.first) }
                            val shape = RoundedCornerShape(sk(18))
                            Row(
                                Modifier.padding(start = sk(64), end = sk(64), top = sk(32)).fillMaxWidth()
                                    .drawBehind {
                                        drawRoundRect(
                                            DS.Overlay4, cornerRadius = androidx.compose.ui.geometry.CornerRadius(sk(18).toPx()),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f,
                                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 8f)))
                                        )
                                    }
                                    .clip(shape).padding(horizontal = sk(36), vertical = sk(30)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIcon(tileCounts.firstOrNull { it.first == fl }?.second ?: AIcon.Info, Muted, size = sk(40))
                                Spacer(Modifier.width(sk(28)))
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(sk(6))) {
                                    Text(Lang.tr("Keine {0} zu „{1}“").replace("{0}", Lang.tr(SEARCH_FILTERS[fl])).replace("{1}", q),
                                        color = Color.White, fontSize = skf(24), fontWeight = FontWeight.Bold, raw = true)
                                    if (others.isNotEmpty()) Text(Lang.tr("Treffer gibt es in {0}.").replace("{0}", others), color = Muted, fontSize = skf(19), raw = true)
                                    if (fl == 5 && !MetaRepo.enabled()) Text(Lang.tr("Personen-Suche braucht einen TMDB-Schlüssel."), color = Muted, fontSize = skf(19), raw = true)
                                }
                                PillButton("Alle Treffer zeigen", light = true) { SearchState.filter = 0 }
                            }
                        }
                    }
                    rowOrder.forEach { key ->
                        when (key) {
                            "rm" -> if (rm.isNotEmpty()) item(key = "rm") {
                                SearchRow("Filme", rm, openS, if (firstKey == "rm") firstRes else null) {
                                    AppSettings.addSearch(ctx, q); onBrowse(BrowseReq("Suche „$q“ · Filme", rm, S_SORT_AZ))
                                }
                            }
                            "rs" -> if (rs.isNotEmpty()) item(key = "rs") {
                                SearchRow("Serien", rs, openS, if (firstKey == "rs") firstRes else null) {
                                    AppSettings.addSearch(ctx, q); onBrowse(BrowseReq("Suche „$q“ · Serien", rs, S_SORT_AZ))
                                }
                            }
                            "rl" -> if (liveCards.isNotEmpty()) item(key = "rl") {
                                HitRow("Live TV", liveCards, false, openS, if (firstKey == "rl") firstRes else null) {
                                    AppSettings.addSearch(ctx, q); onBrowse(BrowseReq("Suche „$q“ · Live TV", liveCards.map { it.item }, S_SORT_AZ))
                                }
                            }
                            "pe" -> if (pe.isNotEmpty()) item(key = "pe") { PeopleRow(pe, if (firstKey == "pe") firstRes else null, onPerson) }
                            "sp" -> if (sportCards.isNotEmpty()) item(key = "sp") {
                                HitRow("Sport", sportCards, true, openS, if (firstKey == "sp") firstRes else null) {
                                    AppSettings.addSearch(ctx, q); onBrowse(BrowseReq("Suche „$q“ · Sport", sportCards.map { it.item }, S_SORT_AZ))
                                }
                            }
                        }
                    }
                }
                    }
                }
                }
            }
        }
        Sidebar(
            tab,
            { id -> if (id == tab) scope.launch { state.scrollToItem(0) }; onTab(id) },
            Modifier.align(SidebarPrefs.align()), sideFocus
        ) { inSidebar = it }
    }
}

/**
 * Sizes of the maintainer's search sketch (1920 x 1080 px) in sketch pixels. The app's design scale ([dpx], [spx]) is based on
 * a 1658 px wide design, so sketch pixels are converted with 1658 / 1920: the screen then has the proportions of the sketch
 * on every TV width.
 */
private const val SKETCH = 1658f / 1920f
private fun sk(px: Number): Dp = dpx(px.toFloat() * SKETCH)
private fun skf(px: Number): androidx.compose.ui.unit.TextUnit = spx(px.toFloat() * SKETCH)

/** Category label of a suggested title (sketch: "Serie", "Live TV", "Sport", "Film"). */
private fun suggestLabel(i: Item): String = when (i.kind) {
    Kind.MOVIE -> "Film"
    Kind.SERIES -> "Serie"
    Kind.LIVE -> if (SearchSport.isSport(i)) "Sport" else "Live TV"
}

/**
 * Category tile (sketch: 91 px high): icon in a small square, big hit count, name below. The selected tile is filled light
 * with dark text; a tile with 0 hits is dimmed but stays focusable.
 */
@Composable
private fun CategoryTile(icon: AIcon, count: Int, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(sk(14))
    val ink = if (selected) DS.FocusInk else Color.White
    Row(
        modifier.height(sk(91)).graphicsLayer { alpha = if (count == 0) 0.45f else 1f }.clip(shape)
            .background(if (selected) DS.FocusFill else Color.Transparent)
            .border(if (f) DS.FocusBorder else DS.Border, if (f) Accent2 else if (selected) DS.FocusFill else DS.Overlay3, shape)
            .onFocusChanged { f = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = sk(18)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(sk(48)).clip(RoundedCornerShape(sk(10))).background(if (selected) DS.FocusInk else Surf2),
            contentAlignment = Alignment.Center
        ) { AppIcon(icon, Color.White, size = sk(22)) }
        Spacer(Modifier.width(sk(14)))
        Column {
            Text(count.toString(), color = ink, fontSize = skf(30), fontWeight = FontWeight.ExtraBold, maxLines = 1, raw = true)
            Text(label, color = if (selected) Muted else Tok.TextMid, fontSize = skf(18), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

/** Suggestion row (sketch: 53 px high): the typed part in bold, the rest normal, the category on the right in a small badge. */
@Composable
private fun SuggestionRow(text: String, label: String, typed: String, modifier: Modifier = Modifier, onPick: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(sk(10))
    val starts = typed.isNotEmpty() && text.startsWith(typed, ignoreCase = true)
    val ink = if (f) DS.FocusInk else Color.White
    Row(
        modifier.fillMaxWidth().height(sk(53)).clip(shape).background(if (f) DS.FocusFill else Surf)
            .onFocusChanged { f = it.isFocused }
            .clickable(onClick = onPick)
            .padding(horizontal = sk(16)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f)) {
            if (starts) Text(text.take(typed.length), color = ink, fontSize = skf(20), fontWeight = FontWeight.Bold, maxLines = 1, raw = true)
            Text(if (starts) text.drop(typed.length) else text, color = ink, fontSize = skf(20), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, raw = true)
        }
        Spacer(Modifier.width(sk(10)))
        Box(Modifier.clip(RoundedCornerShape(sk(6))).background(if (f) DS.Overlay2 else DS.Overlay2).padding(horizontal = sk(8), vertical = sk(3))) {
            Text(label, color = if (f) DS.FocusInk else Tok.TextMid, fontSize = skf(14), fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

/** Small text button (sketch: "Clear history" next to "Last searches"). */
@Composable
private fun SearchLink(label: String, modifier: Modifier = Modifier, trailing: AIcon? = null, onClick: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    val ink = if (f) DS.FocusInk else Muted
    Row(
        modifier.clip(RoundedCornerShape(sk(8))).background(if (f) DS.FocusFill else Color.Transparent)
            .onFocusChanged { f = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = sk(10), vertical = sk(4)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(sk(6))
    ) {
        Text(label, color = ink, fontSize = skf(19), maxLines = 1)
        if (trailing != null) AppIcon(trailing, ink, size = sk(18))
    }
}

/** Button of the hero (sketch: 64 px high, rounded corners, icon and text). */
@Composable
private fun HeroButton(label: String, icon: AIcon, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    val s by animateFloatAsState(if (f) focusScale(1.05f) else 1f, label = "herobtn")
    val shape = RoundedCornerShape(sk(12))
    val ink = if (f) DS.FocusInk else Color.White
    Row(
        modifier.height(sk(64)).graphicsLayer { scaleX = s; scaleY = s }.clip(shape)
            .background(if (f) DS.FocusFill else Surf2)
            .onFocusChanged { f = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = sk(30)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(sk(12))
    ) {
        AppIcon(icon, ink, size = sk(22))
        Text(label, color = ink, fontSize = skf(22), fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Row heading (sketch): title and count badge on the left, "Show all ›" on the right (focus target above the cards). */
@Composable
private fun SearchRowHeader(title: String, count: Int, headerReq: FocusRequester, firstCard: FocusRequester, rowState: androidx.compose.foundation.lazy.LazyListState, onAll: () -> Unit) {
    val sc = rememberCoroutineScope()
    val key = NavState.tab.toString() + "|" + title
    LaunchedEffect(Unit) {
        if (PillMemory.key == key) {
            delay(250)
            runCatching { headerReq.requestFocus() }
            PillMemory.key = null
        }
    }
    Row(Modifier.fillMaxWidth().padding(start = sk(64), end = sk(64), top = sk(32)), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = Color.White, fontSize = skf(26), fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.width(sk(12)))
        Box(Modifier.clip(RoundedCornerShape(50)).background(DS.Overlay2).padding(horizontal = sk(10), vertical = sk(2))) {
            Text(count.toString(), color = Tok.TextMid, fontSize = skf(16), fontWeight = FontWeight.SemiBold, raw = true)
        }
        Spacer(Modifier.weight(1f))
        SearchLink("Alle anzeigen", Modifier.focusRequester(headerReq).onPreviewKeyEvent { ev ->
            if (ev.type == KeyEventType.KeyDown && ev.key == Key.DirectionDown) {
                // First card not in view (row shifted sideways) or not yet composed: first to the start, then highlight.
                // Before, the focus got stuck here because the failure was swallowed.
                if (runCatching { firstCard.requestFocus() }.isFailure) sc.launch {
                    rowState.scrollToItem(0)
                    repeat(3) { withFrameNanos { } }
                    runCatching { firstCard.requestFocus() }
                }
                true
            } else false
        }, trailing = AIcon.Right) { PillMemory.key = key; onAll() }
    }
}

/** Row of movies or series (sketch: posters 168 px wide, 22 px apart). */
@Composable
private fun SearchRow(
    title: String, list: List<Item>, onOpen: (Item) -> Unit,
    firstReq: FocusRequester? = null, onAll: () -> Unit
) {
    val rowState = RowStates.get("${NavState.tab}|$title")
    val headerReq = remember { FocusRequester() }
    val firstCard = firstReq ?: remember { FocusRequester() }
    val report = LocalSearchFocus.current
    // A focused card brings its whole row into view, heading included: before, the list scrolled just enough for the card
    // and the heading with "Show all" slid under the category tiles
    val bring = remember { BringIntoViewRequester() }
    val rowScope = rememberCoroutineScope()
    val showRow: () -> Unit = { rowScope.launch { runCatching { bring.bringIntoView() } } }
    Column(Modifier.bringIntoViewRequester(bring)) {
        SearchRowHeader(title, list.size, headerReq, firstCard, rowState, onAll)
        Box(
            Modifier.onPreviewKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionUp && e.nativeKeyEvent.repeatCount == 0) {
                    runCatching { headerReq.requestFocus() }.isSuccess
                } else false
            }
        ) {
            LazyRow(
                state = rowState,
                contentPadding = PaddingValues(horizontal = sk(64), vertical = sk(16)),
                horizontalArrangement = Arrangement.spacedBy(sk(22))
            ) {
                itemsIndexed(list.take(60)) { i, x ->
                    val fr = (if (i == 0) Modifier.focusRequester(firstCard) else Modifier).onFocusChanged { if (it.hasFocus) { report(x); showRow() } }
                    if (x.kind == Kind.LIVE) LiveCard(x, { onOpen(x) }, Modifier.width(sk(300)).then(fr))
                    else PosterCard(x, { onOpen(x) }, Modifier.width(sk(168)).restoreFocus(x.id).then(fr))
                }
            }
        }
    }
}

/** Row of live channels or sport shows (T-051b): a live card shows the running show and its progress, a sport card the event, time and channel. */
@Composable
private fun HitRow(
    title: String, list: List<HitCard>, sport: Boolean, onOpen: (Item) -> Unit,
    firstReq: FocusRequester? = null, onAll: () -> Unit
) {
    val rowState = RowStates.get("${NavState.tab}|$title")
    val headerReq = remember { FocusRequester() }
    val firstCard = firstReq ?: remember { FocusRequester() }
    val report = LocalSearchFocus.current
    // A focused card brings its whole row into view, heading included: before, the list scrolled just enough for the card
    // and the heading with "Show all" slid under the category tiles
    val bring = remember { BringIntoViewRequester() }
    val rowScope = rememberCoroutineScope()
    val showRow: () -> Unit = { rowScope.launch { runCatching { bring.bringIntoView() } } }
    Column(Modifier.bringIntoViewRequester(bring)) {
        SearchRowHeader(title, list.size, headerReq, firstCard, rowState, onAll)
        Box(
            Modifier.onPreviewKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown && e.key == Key.DirectionUp && e.nativeKeyEvent.repeatCount == 0) {
                    runCatching { headerReq.requestFocus() }.isSuccess
                } else false
            }
        ) {
            LazyRow(
                state = rowState,
                contentPadding = PaddingValues(horizontal = sk(64), vertical = sk(16)),
                horizontalArrangement = Arrangement.spacedBy(sk(22))
            ) {
                itemsIndexed(list.take(60)) { i, c ->
                    val fr = (if (i == 0) Modifier.focusRequester(firstCard) else Modifier).onFocusChanged { if (it.hasFocus) { report(c.item); showRow() } }
                    val w = Modifier.width(sk(300)).then(fr)
                    if (sport) SportCard(c, { onOpen(c.item) }, w)
                    else LiveCard(c.item, { onOpen(c.item) }, w, nowTitle = c.program?.title, progress = c.program?.let { progressOf(it) })
                }
            }
        }
    }
}

/** Sport card (T-051b): league (the channel group) and event on top, the event title, time and channel below. */
@Composable
private fun SportCard(c: HitCard, onClick: () -> Unit, modifier: Modifier) {
    var f by remember { mutableStateOf(false) }
    val s by animateFloatAsState(if (f) focusScale(1.04f) else 1f, label = "sportcard")
    val shape = RoundedCornerShape(DS.RadiusM)
    val p = c.program
    Column(
        modifier.graphicsLayer { scaleX = s; scaleY = s }.focusGlow(f, shape).clip(shape).background(Surf)
            .border(DS.FocusBorder, if (f) Accent2 else Surf2, shape)
            .onFocusChanged { f = it.isFocused }
            .clickable(onClick = onClick)
    ) {
        Box(
            Modifier.fillMaxWidth().height(110.dp)
                .background(Brush.linearGradient(listOf(colorFor(c.item).copy(alpha = .8f), Surf2)))
                .padding(12.dp)
        ) {
            Text(raw = true, text = c.item.group.ifBlank { c.item.title }, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.TopStart))
        }
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(raw = true, text = p?.title ?: c.item.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text(raw = true, text = (if (p != null) hhmm(p.start) + " – " + hhmm(p.end) + "  ·  " else "") + c.item.title, color = Muted, fontSize = 13.sp,
                maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
    }
}

private val SEARCH_FILTERS = listOf("Alle", "Filme", "Serien", "Live TV", "Sport", "Personen")

/** Heuristic for the sports hub filter: channel name or category looks like sports. */
internal object SearchSport {
    private val WORDS = listOf("sport", "dazn", "espn", "eurosport", "bein", "arena", "sky sport", "nba", "nfl", "nhl", "ufc", "f1", "fußball", "fussball", "football", "soccer", "tennis", "golf")
    fun isSport(i: Item): Boolean {
        val t = (i.title + " " + i.group).lowercase()
        return WORDS.any { it in t }
    }
}

private val KB_DIGITS = "1234567890"
private val KB_LETTERS = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
private val KB_SYMBOLS = listOf("-/:;()&@\"", ".,?!'+=_%", "#*<>[]{}~")
// Special letters for other languages (German, French, Spanish, Turkish, Nordic, Slavic ...)
private val KB_SPECIAL = listOf("äöüßéèêàâç", "ñışğčćšžđå", "øæœíóúáãõ")

/**
 * QWERTY on-screen keyboard: a number row on top (no extra layer needed), three key rows, then layer switch (symbols / special letters),
 * backspace, wide space bar and clear.
 */
@Composable
internal fun SearchKeyboard(
    modifier: Modifier, onKey: (String) -> Unit, onSpace: () -> Unit, onDelete: () -> Unit, onClear: () -> Unit,
    onPhone: (() -> Unit)?, onVoice: (() -> Unit)?,
    firstKey: FocusRequester, onFocusChange: (Boolean) -> Unit, onRight: () -> Boolean
) {
    var symbols by remember { mutableStateOf(false) }
    var special by remember { mutableStateOf(false) }
    val rows = if (symbols) KB_SYMBOLS else if (special) KB_SPECIAL else KB_LETTERS
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Number row (always visible)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            KB_DIGITS.forEachIndexed { c, k ->
                KbKey(k.toString(), Modifier.weight(1f).then(if (c == 0) Modifier.focusRequester(firstKey) else Modifier), onFocusChange, if (c == KB_DIGITS.lastIndex) onRight else null) { onKey(k.toString()) }
            }
        }
        rows.forEachIndexed { r, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                // Row 2 is indented half a key like on a real keyboard
                if (r == 1 && !symbols) Spacer(Modifier.weight(0.5f))
                if (r == 2) KbKey(if (symbols) "ABC" else "#+=", Modifier.weight(1.5f), onFocusChange, null) { symbols = !symbols; special = false }
                row.forEachIndexed { c, k ->
                    KbKey(k.toString(), Modifier.weight(1f), onFocusChange, if (r < 2 && c == row.lastIndex) onRight else null) { onKey(k.toString()) }
                }
                if (r == 1 && !symbols) Spacer(Modifier.weight(0.5f))
                if (r == 2) KbKey("⌫", Modifier.weight(1.5f), onFocusChange, onRight) { onDelete() }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            KbKey("Leer", Modifier.weight(if (onPhone == null) (if (onVoice != null) 3.9f else 4.5f) else if (onVoice != null) 2f else 2.6f), onFocusChange, null) { onSpace() }
            if (onPhone != null) KbKey("Handy", Modifier.weight(1.9f), onFocusChange, null, icon = AIcon.Phone) { onPhone() }
            if (onVoice != null) KbKey("", Modifier.weight(0.9f), onFocusChange, null, icon = AIcon.Mic) { onVoice() }
            KbKey(if (special) "abc" else "äöü", Modifier.weight(1.5f), onFocusChange, null) { special = !special; symbols = false }
            KbKey("Alles", Modifier.weight(1.8f), onFocusChange, onRight) { onClear() }
        }
    }
}

@Composable
private fun KbKey(label: String, modifier: Modifier, onFocusChange: (Boolean) -> Unit, onRight: (() -> Boolean)?, icon: AIcon? = null, onClick: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    val sc by animateFloatAsState(if (f) focusScale(1.08f) else 1f, label = "kbkey")
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier.height(42.dp).graphicsLayer { scaleX = sc; scaleY = sc }.clip(shape)
            .background(if (f) DS.FocusFill else DS.Overlay2)
            .onPreviewKeyEvent { e ->
                if (onRight != null && e.type == KeyEventType.KeyDown && e.key == Key.DirectionRight) onRight() else false
            }
            .onFocusChanged { if (f != it.isFocused) { f = it.isFocused; onFocusChange(it.isFocused) } }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (icon != null) AppIcon(icon, if (f) DS.FocusInk else Color.White, size = 18.dp)
            if (label.isNotEmpty()) Text(label, color = if (f) DS.FocusInk else Color.White, fontSize = if (label.length > 1) 15.sp else 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

/**
 * On-screen keyboard (sketch: keys 57 px high, 9 px apart, 5 rows): numbers, three letter rows with backspace at the end of
 * the second one and "-" at the end of the third one, then special letters, space, phone input and "Clear all".
 * The voice key appears only on devices with speech input (it is not in the sketch). Used by the search screen only;
 * the TV guide and channel searches keep [SearchKeyboard].
 */
@Composable
private fun SketchKeyboard(
    modifier: Modifier, onKey: (String) -> Unit, onSpace: () -> Unit, onDelete: () -> Unit, onClear: () -> Unit,
    onPhone: (() -> Unit)?, onVoice: (() -> Unit)?,
    firstKey: FocusRequester, onFocusChange: (Boolean) -> Unit, onRight: () -> Boolean
) {
    var symbols by remember { mutableStateOf(false) }
    var special by remember { mutableStateOf(false) }
    // QWERTY in every language (maintainer, 23 Nov 2026), not the German QWERTZ of the sketch
    val rows = if (symbols) KB_SYMBOLS else if (special) KB_SPECIAL else KB_LETTERS
    val gap = sk(9)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(sk(10))) {
        // Number row (always visible)
        Row(horizontalArrangement = Arrangement.spacedBy(gap), modifier = Modifier.fillMaxWidth()) {
            KB_DIGITS.forEachIndexed { c, k ->
                SketchKey(k.toString(), Modifier.weight(1f).then(if (c == 0) Modifier.focusRequester(firstKey) else Modifier), onFocusChange, if (c == KB_DIGITS.lastIndex) onRight else null) { onKey(k.toString()) }
            }
        }
        rows.forEachIndexed { r, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(gap), modifier = Modifier.fillMaxWidth()) {
                if (r == 2) SketchKey(if (symbols) "ABC" else "#+=", Modifier.weight(1.5f), onFocusChange, null) { symbols = !symbols; special = false }
                row.forEachIndexed { c, k ->
                    val last = c == row.lastIndex && r == 0
                    SketchKey(k.toString(), Modifier.weight(1f), onFocusChange, if (last) onRight else null) { onKey(k.toString()) }
                }
                if (r == 1) SketchKey("⌫", Modifier.weight(1f), onFocusChange, onRight) { onDelete() }
                if (r == 2) SketchKey("-", Modifier.weight(1f), onFocusChange, onRight) { onKey("-") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(gap), modifier = Modifier.fillMaxWidth()) {
            SketchKey(if (special) "abc" else "äöü", Modifier.weight(103f), onFocusChange, null) { special = !special; symbols = false }
            SketchKey("Leer", Modifier.weight(147f), onFocusChange, null, big = true) { onSpace() }
            if (onPhone != null) SketchKey("Handy", Modifier.weight(102f), onFocusChange, null) { onPhone() }
            if (onVoice != null) SketchKey("", Modifier.weight(55f), onFocusChange, null, icon = AIcon.Mic) { onVoice() }
            SketchKey("Alles löschen", Modifier.weight(148f), onFocusChange, onRight) { onClear() }
        }
    }
}

@Composable
private fun SketchKey(label: String, modifier: Modifier, onFocusChange: (Boolean) -> Unit, onRight: (() -> Boolean)?, icon: AIcon? = null, big: Boolean = false, onClick: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    val sc by animateFloatAsState(if (f) focusScale(1.08f) else 1f, label = "kbkey")
    val shape = RoundedCornerShape(sk(10))
    Box(
        modifier.height(sk(57)).graphicsLayer { scaleX = sc; scaleY = sc }.clip(shape)
            .background(if (f) DS.FocusFill else Surf2)
            .onPreviewKeyEvent { e ->
                if (onRight != null && e.type == KeyEventType.KeyDown && e.key == Key.DirectionRight) onRight() else false
            }
            .onFocusChanged { if (f != it.isFocused) { f = it.isFocused; onFocusChange(it.isFocused) } }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val ink = if (f) DS.FocusInk else if (label.length > 1 && !big) Tok.TextMid else Color.White
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(sk(6))) {
            if (icon != null) AppIcon(icon, ink, size = sk(22))
            if (label.isNotEmpty()) Text(label, color = ink, fontSize = if (label.length > 1 && !big) skf(20) else skf(24), fontWeight = if (big) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
        }
    }
}

private val LocalSearchFocus = compositionLocalOf<(Item) -> Unit> { {} }

/** Cinema-style info of the highlighted title on top of the scene image: kind, big title, year | genre | length | rating, description. */
@Composable
private fun SearchHeroInfo(item: Item?, modifier: Modifier) {
    Box(modifier) {
        if (item == null) {
            Text("Suche nach einem Titel: hier erscheinen Beschreibung, Bewertung und Format des markierten Treffers.", color = Muted, fontSize = 16.sp,
                modifier = Modifier.align(Alignment.CenterStart).widthIn(max = 560.dp))
            return@Box
        }
        val m = rememberFullMeta(item, rememberMeta(item))
        // Live channels: what is on right now
        val epg by produceState(emptyList<EpgEntry>(), item.id) {
            value = emptyList()
            if (item.kind != Kind.LIVE) return@produceState
            delay(200)
            value = kotlinx.coroutines.withContext(Dispatchers.Default) { Epg.now(item) }
        }
        val nowE = Epg.current(epg)
        Column(verticalArrangement = Arrangement.spacedBy(sk(14))) {
            // Sketch: gold badge with the kind (no "Best match" label, maintainer decision 22 and 23 Nov 2026); the picture quality next to it
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(sk(12))) {
                Box(Modifier.clip(RoundedCornerShape(sk(6))).background(DS.Gold).padding(horizontal = sk(10), vertical = sk(3))) {
                    Text(Lang.tr(kindLabel(item.kind)).uppercase(), color = DS.FocusInk, fontSize = skf(18), fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp, maxLines = 1, raw = true)
                }
                qualityOf(item)?.let { Text(it.uppercase(), color = Tok.TextMid, fontSize = skf(20), maxLines = 1, raw = true) }
            }
            Text(
                raw = true, text = item.title, color = Tok.TextHi,
                fontSize = skf(if (item.title.length <= 28) 72 else if (item.title.length <= 40) 60 else 50), lineHeight = skf(80),
                fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            if (item.kind != Kind.LIVE) HeroMetaLine(item, m, dots = false)
            val body = when {
                item.kind == Kind.LIVE && nowE != null -> hhmm(nowE.start) + " – " + hhmm(nowE.end) + "  ·  " + nowE.title
                item.kind == Kind.LIVE -> item.group
                else -> m?.overview.orEmpty()
            }
            if (body.isNotBlank()) Text(raw = true, text = TitleText.short(body, 220), color = Tok.TextMid, fontSize = skf(22), lineHeight = skf(33),
                maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = sk(780)))
        }
    }
}

/** Compact chip for the quick filters, suggestions and last searches above the keyboard (no check mark). */
@Composable
private fun MiniChip(label: String, selected: Boolean, modifier: Modifier = Modifier, raw: Boolean = false, icon: AIcon? = null, onClick: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(50)
    val ink = if (f) DS.FocusInk else Color.White
    // Sketch: last searches as chips 44 px high with a clock icon
    Row(
        modifier.height(sk(44)).clip(shape)
            .background(if (f) DS.FocusFill else if (selected) DS.Overlay3 else Surf)
            .border(DS.Border, if (f) DS.FocusFill else if (selected) Color(0x99FFFFFF) else DS.Hairline, shape)
            .onFocusChanged { f = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = sk(16)),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(sk(8))
    ) {
        if (icon != null) AppIcon(icon, ink, size = sk(18))
        Text(label, color = ink, fontSize = skf(19), fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, maxLines = 1, raw = raw)
    }
}

/** Row of people found by the search (T-053c): round photo and name; a tap opens the person page. */
@Composable
private fun PeopleRow(list: List<PersonRef>, first: FocusRequester?, onPerson: (PersonRef) -> Unit) {
    Column(Modifier.padding(top = sk(32))) {
        Text(Lang.tr("Personen"), color = Color.White, fontSize = skf(26), fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = sk(64)))
        LazyRow(
            contentPadding = PaddingValues(horizontal = sk(64), vertical = sk(16)),
            horizontalArrangement = Arrangement.spacedBy(sk(22))
        ) {
            itemsIndexed(list.take(40)) { i, p -> PersonCard(p, 130.dp, if (i == 0 && first != null) Modifier.focusRequester(first) else Modifier) { onPerson(p) } }
        }
    }
}

@Composable
private fun PersonCard(p: PersonRef, w: Dp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var f by remember { mutableStateOf(false) }
    val s by animateFloatAsState(if (f) focusScale(1.06f) else 1f, label = "personcard")
    Column(
        modifier.width(w).graphicsLayer { scaleX = s; scaleY = s }
            .onFocusChanged { f = it.isFocused }
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier.size(w).clip(CircleShape).background(Surf2).border(if (f) DS.FocusBorder else 0.dp, if (f) Accent2 else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (p.photo != null) AsyncImage(p.photo, p.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Text(raw = true, text = p.name.take(1), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
        }
        Text(raw = true, text = p.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}
