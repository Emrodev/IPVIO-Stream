package com.ipvio.stream

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun PersonCard(p: Person, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var f by remember { mutableStateOf(false) }
    val s by animateFloatAsState(if (f) focusScale(1.08f) else 1f, label = "person")
    Column(
        modifier.width(116.dp).graphicsLayer { scaleX = s; scaleY = s }
            .onFocusChanged { f = it.isFocused }.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(104.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Surf2, Surf)))
                .border(3.dp, if (f) Accent2 else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (p.photo != null) AsyncImage(p.photo, p.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            else Text(raw = true, text = p.name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase(), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Text(raw = true, text = p.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
        p.role?.let { Text(raw = true, text = it, color = Muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center) }
    }
}

/**
 * Remembers which card (cast "c:ID" / similar title "s:ID") was last clicked on the detail page, so that
 * Back lands there instead of on "Resume" again. Read once and deleted when the page is reopened.
 */
object DetailFocus {
    val last = HashMap<String, String>()
}

// Row padding: otherwise the enlarged focused card is clipped at the edge of the row
private val RowEdge = 16.dp

@Composable
fun CastRow(
    cast: List<Person>, state: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    restoreId: Int? = null, restoreReq: FocusRequester? = null, onPerson: (Person) -> Unit
) {
    if (cast.isEmpty()) return
    Column(Modifier.padding(top = 30.dp)) {
        Text("Besetzung", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        LazyRow(
            Modifier.offset(x = -RowEdge), state = state,
            contentPadding = PaddingValues(horizontal = RowEdge, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(cast.distinctBy { it.id }, key = { it.id }) { p ->
                PersonCard(p, { onPerson(p) }, if (restoreReq != null && p.id == restoreId) Modifier.focusRequester(restoreReq) else Modifier)
            }
        }
    }
}

@Composable
fun SimilarRow(
    title: String, list: List<Item>, state: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    restoreId: String? = null, restoreReq: FocusRequester? = null, onOpen: (Item) -> Unit
) {
    if (list.isEmpty()) return
    Column(Modifier.padding(top = 26.dp)) {
        Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        LazyRow(
            Modifier.offset(x = -RowEdge), state = state,
            contentPadding = PaddingValues(horizontal = RowEdge, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(list.distinctBy { it.id }, key = { it.id }) { i ->
                PosterCard(i, { onOpen(i) }, Modifier.width(150.dp).then(if (restoreReq != null && i.id == restoreId) Modifier.focusRequester(restoreReq) else Modifier))
            }
        }
    }
}

/**
 * Page of a person (T-053b): profile column on the left (photo, name, facts, biography), tabs on the right.
 * "Filmography" shows all titles of the person: titles in the own playlist are normal cards, the others are dimmed and marked
 * "Not in your playlist". Overview lists the titles that are in the playlist.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PersonScreen(ref: PersonRef, all: List<Item>, onOpen: (Item) -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    var info by remember(ref.id) { mutableStateOf<PersonInfo?>(null) }
    var loaded by remember(ref.id) { mutableStateOf(false) }
    LaunchedEffect(ref.id) { info = CreditsRepo.person(ref.id); loaded = true }
    // Every title of the person, with its playlist item when it is in the own playlist
    val entries by produceState(emptyList<FilmoEntry>(), info, all) {
        val i = info ?: return@produceState
        value = withContext(Dispatchers.Default) {
            val idx = LibMatch.indexFor(all)
            val list = i.titles.map { r -> FilmoEntry(r.title, r.year?.toIntOrNull(), r.isTv, r.poster, idx.find(r)) }
            // The title they came from is always included (TMDB does not know every work for some people)
            val origin = ref.from?.let { id -> all.firstOrNull { it.id == id } }
            if (origin != null && list.none { it.item?.id == origin.id })
                listOf(FilmoEntry(origin.title, origin.year, origin.kind == Kind.SERIES, origin.logo, origin)) + list
            else list
        }
    }
    val owned = entries.mapNotNull { it.item }
    var expanded by remember(ref.id) { mutableStateOf(false) }
    var overflow by remember(ref.id) { mutableStateOf(false) }
    // Tabs: 0 overview, 1 filmography (opens first, like the design)
    var tab by remember(ref.id) { mutableIntStateOf(1) }
    // Filmography filter: 0 all, 1 movies, 2 series
    var kindFilter by remember(ref.id) { mutableIntStateOf(0) }
    val first = remember { FocusRequester() }
    LaunchedEffect(owned.isNotEmpty()) { if (owned.isNotEmpty()) { delay(150); runCatching { first.requestFocus() } } }
    val tabNames = listOf("Übersicht", "Filmografie")

    Box(Modifier.fillMaxSize().background(Bg)) {
        Row(Modifier.fillMaxSize().padding(horizontal = DS.PageX, vertical = 36.dp), horizontalArrangement = Arrangement.spacedBy(36.dp)) {
            // Left: profile, facts and biography
            Column(Modifier.width(380.dp).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.size(170.dp).clip(CircleShape).background(Surf)) {
                    val ph = info?.photo ?: ref.photo
                    if (ph != null) AsyncImage(ph, ref.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    else Text(raw = true, text = ref.name.take(1), color = Color.White, fontSize = 60.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.align(Alignment.Center))
                }
                Text(raw = true, text = ref.name, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                val i = info
                val dept = i?.dept?.let { when (it) { "Directing" -> "Regie"; "Acting" -> "Schauspiel"; "Writing" -> "Drehbuch"; else -> it } }
                if (dept != null) Text(dept, color = Muted, fontSize = 15.sp)
                // Facts card
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(DS.RadiusM)).background(Surf).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    i?.born?.let { FactRow("Geboren", it) }
                    i?.place?.let { FactRow("Geburtsort", it) }
                    if (i != null) FactRow("Titel", i.titles.size.toString())
                }
                // Biography card
                val bio = i?.bio
                if (bio != null) Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(DS.RadiusM)).background(Surf).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Biografie", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(raw = true, text = bio, color = DS.TextSoft, fontSize = 15.sp, lineHeight = 22.sp,
                        maxLines = if (expanded) Int.MAX_VALUE else 6, overflow = TextOverflow.Ellipsis,
                        onTextLayout = { if (!expanded) overflow = it.hasVisualOverflow })
                    if (overflow || expanded) SmallPill(if (expanded) "Weniger" else "Mehr lesen", trailing = if (expanded) AIcon.Up else AIcon.Down) { expanded = !expanded }
                } else if (loaded && i == null) Text("Infos konnten nicht geladen werden.", color = Muted, fontSize = 15.sp)
            }
            // Right: tabs and the selected tab
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tabNames.forEachIndexed { n, name ->
                        PillButton(name, light = tab == n, raw = false) { tab = n }
                    }
                }
                when (tab) {
                    0 -> OverviewTab(owned, onOpen, first)
                    else -> FilmographyTab(entries, kindFilter, { kindFilter = it }, onOpen, first, loaded && info == null)
                }
            }
        }
    }
}

@Composable
private fun FactRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, color = Muted, fontSize = 14.sp, modifier = Modifier.width(110.dp))
        Text(raw = true, text = value, color = Color.White, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun OverviewTab(owned: List<Item>, onOpen: (Item) -> Unit, first: FocusRequester) {
    Text(
        if (owned.isEmpty()) "Keine Titel in deiner Playlist" else "In deiner Playlist  ·  ${owned.size}",
        color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold
    )
    if (owned.isNotEmpty()) LazyRow(
        Modifier.offset(x = (-14).dp),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 18.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(owned.size) { n ->
            val it = owned[n]
            PosterCard(it, { onOpen(it) }, Modifier.width(150.dp).then(if (n == 0) Modifier.focusRequester(first) else Modifier))
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FilmographyTab(
    entries: List<FilmoEntry>, kindFilter: Int, onFilter: (Int) -> Unit, onOpen: (Item) -> Unit, first: FocusRequester, failed: Boolean
) {
    val shown = PersonFilmoLogic.visible(entries, kindFilter)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            Lang.tr("In deiner Playlist: {0} von {1}").replace("{0}", PersonFilmoLogic.inPlaylist(entries).toString()).replace("{1}", entries.size.toString()),
            color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, raw = true, modifier = Modifier.weight(1f)
        )
        listOf("Alle", "Filme", "Serien").forEachIndexed { n, name ->
            PillButton(name, light = kindFilter == n) { onFilter(n) }
        }
    }
    when {
        entries.isEmpty() && failed -> Text("Keine Titel in deiner Playlist", color = Muted, fontSize = 16.sp)
        shown.isEmpty() -> Text("Keine Titel für diesen Filter.", color = Muted, fontSize = 16.sp)
        else -> LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(shown.size) { n ->
                val e = shown[n]
                val mod = Modifier.fillMaxWidth().then(if (n == 0) Modifier.focusRequester(first) else Modifier)
                val item = e.item
                if (item != null) PosterCard(item, { onOpen(item) }, mod)
                else MissingCard(e, mod)
            }
        }
    }
}

/** A title of the person that is NOT in the own playlist: dimmed, marked, not clickable. */
@Composable
private fun MissingCard(e: FilmoEntry, modifier: Modifier) {
    Column(modifier.graphicsLayer { alpha = 0.5f }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(2f / 3f).clip(RoundedCornerShape(DS.RadiusM)).background(Surf),
            contentAlignment = Alignment.Center
        ) {
            if (e.poster != null) AsyncImage(e.poster, e.title, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxSize().background(Color(0x66000000)), contentAlignment = Alignment.Center) {
                Text("Nicht in der Playlist", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
        Text(raw = true, text = e.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        e.year?.let { Text(raw = true, text = it.toString(), color = Muted, fontSize = 13.sp) }
    }
}
