package com.ipvio.stream

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Sports home: the overview of the Sports Hub laid out like the app's home screen, but only sports –
 * a large banner with the featured games (slideshow, team colours and crests instead of a scene image),
 * the quick tiles of the home screen, then rows with "Show all": live games, your sports channels with
 * what is on now, coming up, results, leagues, sports and news. [header] = the hub's tabs, drawn over the banner
 * like the home screen's header over its big picture.
 */
@Composable
internal fun SportHome(
    feed: SportFeed.State, hub: HubCtx, header: @Composable (Modifier) -> Unit, onRetry: () -> Unit
) {
    val model by produceState<SportHomeModel?>(null, feed.games, hub.live) {
        value = withContext(Dispatchers.Default) { buildSportHome(feed.games, hub) }
    }
    val m = model
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = dpx(60))) {
        item(key = "hero") {
            when {
                m != null && m.featured.isNotEmpty() -> SportBanner(m.featured, hub, dpx(470), header)
                else -> Column {
                    header(Modifier)
                    Box(Modifier.padding(horizontal = DS.PageX, vertical = dpx(10))) {
                        when {
                            m == null || feed.loading -> HubLoading("Lade Spiele …")
                            feed.failed -> HubEmpty(AIcon.Warning, "Daten konnten nicht geladen werden",
                                "ESPN antwortet gerade nicht. Prüfe die Internetverbindung und versuche es erneut.", "Erneut versuchen") { onRetry() }
                            else -> HubEmpty(AIcon.Info, "Heute keine Spiele gefunden.",
                                "In deinen Ligen ist heute nichts angesetzt. Im Spielplan siehst du die nächsten Tage.", "Spielplan öffnen") { hub.goTab(2, null) }
                        }
                    }
                }
            }
        }
        if (m == null) return@LazyColumn
        item(key = "tiles") { QuickTiles(sportTiles(m, hub)) }
        if (m.live.isNotEmpty()) item(key = "live") {
            GameRow("Jetzt live", m.live, m.withCh, onAll = { hub.goTab(1, null) }) { hub.open(it) }
        }
        if (m.channels.isNotEmpty()) item(key = "chs") {
            // The home screen's channel cards: logo, what is on now, time and progress
            CardRow("Auf deinen Sportsendern", m.channels, CARD_POSTER, { c -> hub.play(c, m.channels) }, { hub.goTab(6, null) },
                RowStates.get("sport|chnow"))
        }
        if (m.upcoming.isNotEmpty()) item(key = "up") {
            GameRow("Demnächst", m.upcoming, m.withCh, onAll = { hub.goTab(2, null) }) { hub.open(it) }
        }
        if (m.mine.isNotEmpty()) item(key = "mine") {
            GameRow("Meine Teams", m.mine, m.withCh, onAll = { hub.goTab(5, null) }) { hub.open(it) }
        }
        if (m.finals.isNotEmpty()) item(key = "fin") {
            GameRow("Ergebnisse von heute", m.finals, m.withCh, onAll = null) { hub.open(it) }
        }
        if (m.popular.isNotEmpty()) item(key = "leagues") {
            Column {
                RowTitle("Beliebte Ligen", null, null) { hub.goTab(3, null) }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(dpx(14)), contentPadding = PaddingValues(horizontal = RowPadX, vertical = dpx(18))) {
                    items(m.popular, key = { it.path }) { l ->
                        LogoTile(l.name, SportData.leagueLogos[l.path], l.name, marked = SportHubFavs.leagues.contains(l.path)) { hub.nav.open(HubPage.League(l)) }
                    }
                }
            }
        }
        item(key = "sports") {
            Column {
                RowTitle("Sportarten", null, null, onAll = null)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(dpx(14)), contentPadding = PaddingValues(horizontal = RowPadX, vertical = dpx(18))) {
                    items(SportData.SPORTS, key = { it.key }) { s ->
                        val logo = SportData.LEAGUES.firstOrNull { it.sport == s.key }?.let { SportData.leagueLogos[it.path] }
                        val n = m.counts[s.key] ?: 0
                        LogoTile(Lang.tr(s.label) + (if (n > 0) " ($n)" else ""), logo, s.label, marked = SportHubFavs.sports.contains(s.key)) { hub.goTab(1, s.key) }
                    }
                }
            }
        }
        if (m.newsLeagues.isNotEmpty()) item(key = "news") {
            Box(Modifier.padding(horizontal = RowPadX).padding(top = dpx(18))) { HubNewsRow(m.newsLeagues) }
        }
        if (!SportData.lastOk && m.games.isNotEmpty()) item(key = "stale") {
            Text("Hinweis: Die letzte Aktualisierung hat nicht geklappt. Angezeigt wird der letzte Stand.", color = Muted, fontSize = spx(14),
                modifier = Modifier.padding(horizontal = RowPadX + dpx(5), vertical = dpx(16)))
        }
    }
}

/** Everything the sports home shows, computed off the main thread (sorting and channel matching are too slow in composition). */
internal class SportHomeModel(
    val games: List<SportData.Game>,
    val featured: List<SportData.Game>,
    val live: List<SportData.Game>,
    val upcoming: List<SportData.Game>,
    val mine: List<SportData.Game>,
    val finals: List<SportData.Game>,
    val withCh: Set<String>,
    val channels: List<Item>,
    val popular: List<SportData.League>,
    val newsLeagues: List<SportData.League>,
    val counts: Map<String, Int>
)

private fun popularityOf(g: SportData.Game): Int = SportData.POPULAR.indexOf(g.league).let { if (it < 0) 99 else it }

internal fun buildSportHome(raw: List<SportData.Game>, hub: HubCtx): SportHomeModel {
    val games = raw.map { SportFeed.latest[it.key] ?: it }
    val hasCh = HashMap<String, Boolean>()
    fun ch(g: SportData.Game): Boolean = hasCh.getOrPut(g.key) { hub.chans(g).isNotEmpty() }
    val fav = { g: SportData.Game -> SportHubFavs.isFavGame(g) }
    val live = games.filter { it.state == 1 }
        .sortedWith(compareBy({ if (fav(it)) 0 else 1 }, { if (ch(it)) 0 else 1 }, { popularityOf(it) }))
    val upcoming = games.filter { it.state == 0 && statusKind(it) == 0 }.sortedBy { it.start }
    upcoming.take(30).forEach { ch(it) }
    val finals = games.filter { it.state == 2 }.sortedByDescending { it.start }
    val mine = (live + upcoming + finals).filter(fav)
    // Banner: live games you can watch first, then live top games, then the next top games – at most 5, team games only
    val featured = (live.filter { !it.solo && ch(it) } + live.filter { !it.solo } +
        upcoming.filter { !it.solo }.sortedWith(compareBy({ if (fav(it)) 0 else 1 }, { if (ch(it)) 0 else 1 }, { popularityOf(it) }, { it.start })) +
        finals.filter { !it.solo }.sortedBy { popularityOf(it) })
        .distinctBy { it.key }.take(5)
    // Sports channels: the ones showing something now first
    val chans = hub.live.filter { SportLogic.isSportChannel(it) }
    val popular = SportData.POPULAR.mapNotNull { SportData.league(it) }.filter { SportLeagues.on(it) }
    val newsLeagues = (popular.filter { p -> games.any { it.league == p.name } } + games.mapNotNull { SportData.leagueOf(it) }).distinctBy { it.path }
    val counts = games.groupingBy { SportData.sportOf(it) }.eachCount()
    return SportHomeModel(games, featured, live.take(30), upcoming.take(30), mine.take(30), finals.take(30),
        hasCh.filterValues { it }.keys, chans.take(60), popular, newsLeagues, counts)
}

/** The quick tiles of the home screen, for sports: live, schedule, leagues, my teams, sports channels. */
private fun sportTiles(m: SportHomeModel, hub: HubCtx): List<QuickTile> {
    val liveLogos = m.live.flatMap { listOf(it.home.logo, it.away.logo) }.filterNotNull().distinct().take(3)
    val leagueLogos = m.popular.mapNotNull { SportData.leagueLogos[it.path] }.take(3)
    val chLogos = m.channels.mapNotNull { it.logo }.take(3)
    val mineLogos = m.mine.flatMap { listOf(it.home, it.away) }.filter { SportHubFavs.isTeam(it) }.mapNotNull { it.logo }.distinct().take(3)
    val liveN = m.live.size
    return listOf(
        QuickTile("Jetzt live", if (liveN == 1) "1 Spiel" else "$liveN Spiele", MenuLogic.SPORT, Color(0xFF7F1D1D), Color(0xFF3A0B0B), Color(0xFFFB7185), logos = liveLogos) { hub.goTab(1, null) },
        QuickTile("Spielplan", "Heute und die nächsten Tage", 0, Color(0xFF1E3A8A), Color(0xFF0F1B45), Color(0xFF60A5FA)) { hub.goTab(2, null) },
        QuickTile("Ligen", "${m.popular.size} beliebte Ligen", MenuLogic.SPORT, Color(0xFF134E4A), Color(0xFF082725), Color(0xFF2DD4BF), logos = leagueLogos) { hub.goTab(3, null) },
        QuickTile("Favoriten", "Deine Teams und Ligen", 7, Color(0xFF4C1D95), Color(0xFF22104A), Color(0xFFC084FC), logos = mineLogos) { hub.goTab(5, null) },
        QuickTile("Sportsender", "${m.channels.size} Sender", 3, Color(0xFF6B3A0E), Color(0xFF2B1708), Color(0xFFF59E0B), logos = chLogos) { hub.goTab(6, null) }
    )
}

/** A row of game cards under a home-style row title ("Show all →"). */
@Composable
private fun GameRow(title: String, list: List<SportData.Game>, withCh: Set<String>, onAll: (() -> Unit)?, onOpen: (SportData.Game) -> Unit) {
    Column {
        RowTitle(title, null, null, onAll = onAll)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(dpx(16)), contentPadding = PaddingValues(horizontal = RowPadX, vertical = dpx(18))) {
            items(list, key = { it.key }) { g -> MatchCard(g, g.key in withCh) { onOpen(g) } }
        }
    }
}

/**
 * Banner like the home screen's big picture: the featured game across the full width, slideshow with progress dots.
 * Instead of a scene image: the two team colours, the crests large and faded on the right, the same gradients
 * into the background as the home screen; bottom left kicker, "Home vs Away", status and the buttons.
 */
@Composable
private fun SportBanner(featured: List<SportData.Game>, hub: HubCtx, h: Dp, header: @Composable (Modifier) -> Unit) {
    var idx by remember { mutableIntStateOf(0) }
    LaunchedEffect(featured.size, General.heroSec) {
        while (featured.size > 1) { delay((General.heroSec.takeIf { it > 0 } ?: 8) * 1000L); idx = (idx + 1) % featured.size }
    }
    val g0 = featured[idx.coerceIn(0, featured.lastIndex)]
    val g = SportFeed.latest[g0.key] ?: g0
    Box(Modifier.fillMaxWidth().height(h).background(Bg).clipToBounds()) {
        Crossfade(g.key, animationSpec = tween(if (Look.motion == 0) 0 else 900), label = "sportBg") { k ->
            val c = featured.firstOrNull { it.key == k } ?: g
            val a = teamColor(c.home.color) ?: Accent1
            val b = teamColor(c.away.color) ?: Accent2
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(a.copy(alpha = .55f), Bg, b.copy(alpha = .55f)))))
                // Crests large and soft on the right, like the scene image of the home screen
                Row(
                    Modifier.align(Alignment.CenterEnd).padding(end = dpx(70)).alpha(.9f),
                    horizontalArrangement = Arrangement.spacedBy(dpx(46)), verticalAlignment = Alignment.CenterVertically
                ) {
                    TeamLogo(c.home.logo, c.home.abbr, dpx(210))
                    TeamLogo(c.away.logo, c.away.abbr, dpx(210))
                }
            }
        }
        // The home screen's gradients: left and bottom into the background, subtle at the top behind the tabs
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to Bg, 0.3f to Bg.copy(alpha = .9f), 0.55f to Bg.copy(alpha = .35f), 0.8f to Color.Transparent)))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0f to Bg.copy(alpha = .6f), 0.22f to Color.Transparent, 0.62f to Color.Transparent, 0.9f to Bg.copy(alpha = .85f), 1f to Bg)))
        header(Modifier.align(Alignment.TopStart))
        Column(
            Modifier.align(Alignment.BottomStart).padding(start = dpx(39), bottom = dpx(30)).widthIn(max = dpx(860)),
            verticalArrangement = Arrangement.spacedBy(dpx(10))
        ) {
            Crossfade(g.key, animationSpec = tween(if (Look.motion == 0) 0 else 500), label = "sportText") { k ->
                val c = (featured.firstOrNull { it.key == k } ?: g).let { SportFeed.latest[it.key] ?: it }
                Column(verticalArrangement = Arrangement.spacedBy(dpx(10))) {
                    val status = when (c.state) {
                        1 -> "LIVE" + (c.detail.takeIf { it.isNotBlank() }?.let { " · $it" } ?: "")
                        2 -> Lang.tr("Beendet")
                        else -> dayLabel(c.start) + " · " + hhmm(c.start)
                    }
                    Kicker(listOf(status, c.groupName).filter { it.isNotBlank() }.joinToString("  ·  ").uppercase())
                    Text(
                        raw = true, text = c.home.short + "  vs  " + c.away.short, color = Tok.TextHi, fontSize = spx(66), lineHeight = spx(70),
                        fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.2).sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    val score = if (c.state != 0) scoreLine(c) else ""
                    val chans = hub.chans(c)
                    val line = listOf(
                        score,
                        c.venue,
                        if (chans.isNotEmpty()) Lang.tr("Auf") + ": " + chans.take(2).joinToString(", ") { it.title } else Lang.tr("Nicht in deinen Sendern")
                    ).filter { it.isNotBlank() }.joinToString("   ·   ")
                    Text(raw = true, text = line, color = Tok.TextMid, fontSize = spx(20), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(Modifier.padding(top = dpx(10)), horizontalArrangement = Arrangement.spacedBy(dpx(16))) {
                WatchLiveButton(hub.chans(g)) { hub.watch(g) }
                HeroButton("Match Center", AIcon.Info, false, Modifier, 52) { hub.open(g) }
            }
        }
        if (featured.size > 1) Row(
            Modifier.align(Alignment.BottomEnd).padding(end = dpx(40), bottom = dpx(30)),
            horizontalArrangement = Arrangement.spacedBy(dpx(12)), verticalAlignment = Alignment.CenterVertically
        ) {
            featured.indices.forEach { i ->
                if (i == idx) Box(Modifier.width(dpx(40)).height(dpx(7)).clip(RoundedCornerShape(50)).background(Accent1))
                else Box(Modifier.size(dpx(7)).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .45f)))
            }
        }
    }
}
