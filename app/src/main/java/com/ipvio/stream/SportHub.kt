package com.ipvio.stream

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Shared data service of the hub: one refresh loop per league with a sport-specific interval, the newest version of every
 * game by key (pages read it from here instead of fetching again).
 */
object SportFeed {
    val latest = mutableStateMapOf<String, SportData.Game>()

    class State(val games: List<SportData.Game>, val done: Int, val total: Int, val failed: Boolean) {
        val loading: Boolean get() = done < total && games.isEmpty()
    }

    fun put(list: List<SportData.Game>) {
        if (latest.size > 4000) latest.clear()
        list.forEach { latest[it.key] = it }
    }

    /** Refresh interval per league: fast while something runs (depending on the sport), slow when nothing happens. */
    fun interval(l: SportData.League, games: List<SportData.Game>): Long {
        val now = System.currentTimeMillis() / 1000
        return when {
            games.any { it.state == 1 } -> when (l.kind) {
                SportData.KIND_GOLF, SportData.KIND_CRICKET -> 60_000L
                SportData.KIND_FIGHT -> 45_000L
                else -> 30_000L
            }
            games.any { it.state == 0 && it.start - now in 0..900 } -> 60_000L
            else -> 180_000L
        }
    }
}

/** What the tabs and pages of the hub need: page stack, channels, play, change tab. */
class HubCtx(val nav: HubNav, val live: List<Item>, private val onPlay: (Item, List<Item>) -> Unit, val goTab: (Int, String?) -> Unit) {
    fun chans(g: SportData.Game): List<Item> = SportWatch.channels(g, live)
    /** Remember the open pages (Match Center, Watch Live …): Back from the player returns to exactly there. */
    fun play(ch: Item, list: List<Item>) { HubMemory.keep = nav.stack.toList(); nav.clear(); onPlay(ch, list) }
    /** Watch Live: start the best channel directly, otherwise show the Watch Live page (assign by hand). */
    fun watch(g: SportData.Game) {
        val c = chans(g)
        if (c.isNotEmpty()) play(c[0], c) else nav.open(HubPage.Watch(g))
    }
    fun open(g: SportData.Game) = nav.open(HubPage.Match(g))
}

/** Tab and filters survive leaving the hub (e.g. after Watch Live and back from the player). */
private object HubMemory {
    /** Pages that were open when a live stream started (restored when the hub comes back). */
    var keep: List<HubPage> = emptyList()
    var tab = 0
    var sport: String? = null
    var day = 0
}

private val HUB_TABS = listOf("Übersicht", "Live", "Spielplan", "Ligen", "Teams", "Favoriten", "Sender")

/**
 * Sports Hub: Overview, Live, Schedule, Leagues, Teams, Favorites and the channel area (previous sports page).
 * Match Center, League Center, Team Center, players and Watch Live open as pages above the tabs; Back closes them.
 */
@Composable
fun SportHub(live: List<Item>, onPlay: (Item, List<Item>) -> Unit) {
    SideEffect { ContentFocus.tab = MenuLogic.SPORT }
    val ctx = LocalContext.current
    SportFavs.init(ctx)
    SportLeagues.init(ctx)
    SportHubFavs.init(ctx)
    SportFix.init(ctx)
    var tab by remember { mutableIntStateOf(HubMemory.tab) }
    var sport by remember { mutableStateOf(HubMemory.sport) }
    var retry by remember { mutableIntStateOf(0) }
    var scoreDlg by remember { mutableStateOf(false) }
    if (scoreDlg) ScoreOverlayDialog { scoreDlg = false }
    val nav = remember { HubNav().also { n -> HubMemory.keep.forEach { n.open(it) }; HubMemory.keep = emptyList() } }
    val hub = remember(live, nav) {
        HubCtx(nav, live, onPlay) { t, s -> tab = t; HubMemory.tab = t; if (s != null || t == 1) { sport = s; HubMemory.sport = s } }
    }

    // Today's games of all active leagues, each league with its own refresh interval
    val leaguesKey = SportLeagues.off.joinToString("|")
    val feed by produceState(SportFeed.State(emptyList(), 0, 0, false), leaguesKey, General.sportLive, retry) {
        if (!General.sportLive) { value = SportFeed.State(emptyList(), 0, 0, false); return@produceState }
        val ls = SportLeagues.enabled()
        val m = HashMap<String, List<SportData.Game>>()
        // Opening the hub again: the last loaded games appear at once (no "Loading games" wait), fresh ones replace them as they arrive
        val today = SportData.ymd(0, General.zone())
        for (l in ls) SportData.peek(l, today)?.let { m[l.path] = it }
        val cached = m.values.flatten()
        value = SportFeed.State(cached, if (cached.isEmpty()) 0 else m.size, ls.size, false)
        coroutineScope {
            ls.forEach { l ->
                launch {
                    while (true) {
                        val r = SportData.fetch(l, SportData.ymd(0, General.zone()), 15_000)
                        m[l.path] = r
                        SportFeed.put(r)
                        val all = m.values.flatten()
                        value = SportFeed.State(all, m.size, ls.size, m.size == ls.size && all.isEmpty() && !SportData.lastOk)
                        delay(SportFeed.interval(l, r))
                    }
                }
            }
        }
    }
    // Sports shows of the TV guide for Watch Live (once a minute, off the main thread)
    LaunchedEffect(live, Xmltv.channelCount) {
        while (true) {
            val s = withContext(Dispatchers.Default) {
                SportLogic.slots(live, { Xmltv.forChannel(it.tvgId) }, System.currentTimeMillis() / 1000, 48 * 3600L, 600)
            }
            SportWatch.setSlots(s.first + s.second)
            delay(60_000)
        }
    }

    val sportReq = remember { FocusRequester() }
    // The hub's tabs; on the overview they sit over the banner, like the home screen's header over its big picture
    val tabs: @Composable (Modifier) -> Unit = { mod ->
        LazyRow(
            mod,
            contentPadding = PaddingValues(horizontal = DS.PageX, vertical = dpx(10)),
            horizontalArrangement = Arrangement.spacedBy(dpx(10))
        ) {
            item(key = "scoreov") { FilterPill("Score-Overlay", SportAlerts.miniOn, aicon = AIcon.Live) { scoreDlg = true } }
            itemsIndexed(HUB_TABS) { i, t ->
                // The chosen tab is where the focus lands when coming from the sidebar (the channel tab has its own target)
                val base = if (i == tab && tab != 6) Modifier.focusRequester(ContentFocus.req) else Modifier.trackFocus()
                // Live tab: Down goes to the sport filter first (not to the competition row below it)
                FilterPill(t, tab == i, if (tab == 1) base.focusProperties { down = sportReq } else base) {
                    tab = i; HubMemory.tab = i
                }
            }
        }
    }
    Column(Modifier.fillMaxSize()) {
        val home = tab == 0 && General.sportLive
        if (!home) tabs(Modifier)
        if (!General.sportLive && tab != 6) {
            HubEmpty(AIcon.Live, "Live-Ergebnisse sind aus", "Ergebnisse, Tabellen und Spielpläne kommen von ESPN (inoffizielle Quelle). Schalte sie ein, um den Sports Hub zu nutzen.",
                "Einschalten") { General.setSportLive(ctx, true) }
        } else when (tab) {
            0 -> SportHome(feed, hub, tabs) { retry++ }
            1 -> HubLive(feed, hub, sport, sportReq) { sport = it; HubMemory.sport = it }
            2 -> HubSchedule(hub, sport) { sport = it; HubMemory.sport = it }
            3 -> HubLeagues(hub)
            4 -> HubTeams(hub)
            5 -> HubFavorites(feed, hub)
            else -> SportTab(live, onPlay)
        }
    }
    HubFocusRestore(nav)
    nav.stack.forEachIndexed { i, p ->
        key(i, p) {
            when (p) {
                is HubPage.Match -> MatchCenterPage(p.g, hub)
                is HubPage.League -> LeagueCenterPage(p.l, hub)
                is HubPage.Team -> TeamCenterPage(p.l, p.t, hub)
                is HubPage.Player -> SportPlayerPage(p.l, p.id, p.name, hub)
                is HubPage.Watch -> WatchPage(p.g, hub)
            }
        }
    }
}

private fun current(list: List<SportData.Game>): List<SportData.Game> = list.map { SportFeed.latest[it.key] ?: it }

private fun popularity(g: SportData.Game): Int = SportData.POPULAR.indexOf(g.league).let { if (it < 0) 99 else it }

private fun sportMatch(g: SportData.Game, sport: String?) = sport == null || SportData.sportOf(g) == sport

/** Games grouped by competition: header with logo, LIVE marker and count, then one row per game. */
private fun LazyListScope.groupedGames(games: List<SportData.Game>, hub: HubCtx, timeFirst: Boolean) {
    games.groupBy { it.groupName }.forEach { (name, list) ->
        val l = SportData.leagueOf(list.first())
        item(key = "h$name") { CompetitionHeader(name, l?.let { SportData.leagueLogos[it.path] }, list.count { it.state == 1 }, list.size) }
        items(list, key = { "g" + it.key }) { g -> MatchRow(g, hub.chans(g).isNotEmpty(), timeFirst) { hub.open(g) } }
    }
}

/** Live: all running events, grouped by competition, with sport, competition and favorite filters. */
@Composable
private fun HubLive(feed: SportFeed.State, hub: HubCtx, sport: String?, sportReq: FocusRequester, onSport: (String?) -> Unit) {
    val compReq = remember { FocusRequester() }
    var comp by remember { mutableStateOf<String?>(null) }
    var favOnly by remember { mutableStateOf(false) }
    val games = current(feed.games)
    val liveG = games.filter { it.state == 1 }
    val counts = liveG.groupingBy { SportData.sportOf(it) }.eachCount()
    val inSport = liveG.filter { sportMatch(it, sport) }
    val comps = inSport.map { it.groupName }.distinct().sortedBy { n -> inSport.firstOrNull { it.groupName == n }?.let { popularity(it) } ?: 99 }
    val c = comp?.takeIf { it in comps }
    val shown = inSport.filter { (c == null || it.groupName == c) && (!favOnly || SportHubFavs.isFavGame(it)) }
        .sortedWith(compareBy({ if (SportHubFavs.isFavGame(it)) 0 else 1 }, { popularity(it) }, { it.groupName }, { it.start }))
    Column(Modifier.fillMaxSize()) {
        SportFilter(sport, counts, sportReq, compReq) { onSport(it); comp = null }
        LazyRow(contentPadding = PaddingValues(horizontal = DS.PageX, vertical = dpx(4)), horizontalArrangement = Arrangement.spacedBy(dpx(10))) {
            item(key = "all") { FilterPill("Alle Wettbewerbe", c == null, Modifier.trackFocus(compReq).focusProperties { up = sportReq }) { comp = null } }
            item(key = "fav") { FilterPill("Nur Favoriten", favOnly, Modifier.trackFocus().focusProperties { up = sportReq }, aicon = if (favOnly) AIcon.Star else AIcon.StarOutline) { favOnly = !favOnly } }
            items(comps, key = { it }) { n -> FilterPill(n, c == n, Modifier.trackFocus().focusProperties { up = sportReq }, raw = true) { comp = if (c == n) null else n } }
        }
        LazyColumn(contentPadding = PaddingValues(horizontal = DS.PageX, vertical = dpx(8)), verticalArrangement = Arrangement.spacedBy(dpx(4))) {
            when {
                feed.loading -> item(key = "load") { HubLoading("Lade Spiele …") }
                shown.isEmpty() -> {
                    item(key = "empty") {
                        HubEmpty(AIcon.Live, if (liveG.isEmpty()) "Gerade läuft nichts" else "Keine Spiele für diese Auswahl.",
                            if (liveG.isEmpty()) "Sobald ein Spiel deiner Ligen beginnt, erscheint es hier." else "Ändere die Filter oder schau in den Spielplan.",
                            "Spielplan öffnen") { hub.goTab(2, sport) }
                    }
                    val next = games.filter { it.state == 0 && statusKind(it) == 0 && sportMatch(it, sport) }.sortedBy { it.start }.take(10)
                    if (next.isNotEmpty()) {
                        item(key = "nt") { HubSectionTitle("Als Nächstes") }
                        items(next, key = { "n" + it.key }) { g -> MatchRow(g, hub.chans(g).isNotEmpty(), timeFirst = true) { hub.open(g) } }
                    }
                }
                else -> groupedGames(shown, hub, false)
            }
        }
    }
}

/** Schedule: date bar (7 days back, 14 ahead), sport filter, games of the day grouped by competition. */
@Composable
private fun HubSchedule(hub: HubCtx, sport: String?, onSport: (String?) -> Unit) {
    var day by remember { mutableIntStateOf(HubMemory.day) }
    var retry by remember { mutableIntStateOf(0) }
    val leaguesKey = SportLeagues.off.joinToString("|")
    val games by produceState<List<SportData.Game>?>(null, day, leaguesKey, retry) {
        value = null
        val ymd = SportData.ymd(day, General.zone())
        val r = coroutineScope {
            SportLeagues.enabled().map { l -> async { SportData.fetch(l, ymd, if (day == 0) 40_000 else 300_000) } }.awaitAll()
        }.flatten().distinctBy { it.key }
        SportFeed.put(r)
        value = r
    }
    val list = current(games.orEmpty())
    val counts = list.groupingBy { SportData.sportOf(it) }.eachCount()
    val shown = list.filter { sportMatch(it, sport) }.sortedWith(compareBy({ popularity(it) }, { it.groupName }, { it.start }))
    Column(Modifier.fillMaxSize()) {
        DateSelector(day, -7..14) { day = it; HubMemory.day = it }
        Spacer(Modifier.height(dpx(6)))
        SportFilter(sport, counts) { onSport(it) }
        LazyColumn(contentPadding = PaddingValues(horizontal = DS.PageX, vertical = dpx(8)), verticalArrangement = Arrangement.spacedBy(dpx(4))) {
            when {
                games == null -> item(key = "load") { HubLoading("Lade Spiele …") }
                list.isEmpty() && !SportData.lastOk -> item(key = "err") {
                    HubEmpty(AIcon.Warning, "Daten konnten nicht geladen werden", "ESPN antwortet gerade nicht. Bitte versuche es erneut.", "Erneut versuchen") { retry++ }
                }
                shown.isEmpty() -> item(key = "empty") {
                    HubEmpty(AIcon.Info, "Keine Spiele für diese Auswahl.", "An diesem Tag ist in deinen Ligen nichts angesetzt. Tennis, Formel 1, UFC, Golf und Cricket zeigt ESPN nur für die laufenden Turniere und Wochenenden.")
                }
                else -> groupedGames(shown, hub, true)
            }
        }
    }
}
