package com.ipvio.stream

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.background
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Density
import coil.Coil

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Load settings early so colors and size are right in the very first frame.
        Profiles.init(this)
        SublistStore.clearActiveOnce(this)   // T-035: the sublist tab is gone; drop active filters once
        Profiles.prewarm(this)
        TlsFallback.init(this)   // #69: hosts the user agreed to (ipvio_tls)
        val pick = Profiles.needsPicker(this)
        if (!pick) AppSettings.init(this)
        General.init(this)
        AppDns.init(this)   // T-049f: before the first connection
        // T-047: read the stored playlist while the first screen is built (not with the profile picker: the profile is not chosen yet)
        if (!pick) Thread({ runCatching { PlaylistStore.preread(applicationContext) } }, "list-preread").start()
        // T-047: parse the dictionary of the UI language off the main thread while the first screen is built (German: nothing to do)
        Thread({ runCatching { Lang.prewarm() } }, "lang-prewarm").start()
        Boomer.init(this)
        HomeLayout.init(this); BoomerLayout.init(this)
        SidebarPrefs.init(this)
        Config.init(this)
        Config.initSubs(this)
        LiveSettings.init(this)
        Recorder.attach(this)
        MobilePrefs.apply(this)
        // Fresh start (not just a rotated/recreated screen): "Resume last watched at start" may apply again.
        // The process often lives on after quitting; the marker flag therefore stayed set from the first start and nothing was opened any more.
        if (savedInstanceState == null) General.startupHandled = false
        // WorkManager sets up its database on first use: off the main thread, so the first screen is not delayed
        val app = applicationContext
        Thread({ runCatching { Updater.apply(app) } }, "updater-apply").start()
        CrashLog.install(this)
        // T-070: image cache size and decoder count follow "Device performance" (Automatic: the detected memory, as before)
        DevicePerf.init(this)
        Coil.setImageLoader(DevicePerf.imageLoader(this))
        ScreenAwake.attach(this)
        intent?.getStringExtra(Reminders.EXTRA_CH)?.let { AppNav.openChannel = it }
        setContent { App(pick) }
    }

    /** T-049a: OK on a sports alert (goal, kick-off) jumps to a channel that shows the game; every other key as usual. */
    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        val ok = event.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER || event.keyCode == android.view.KeyEvent.KEYCODE_ENTER
        if (ok && SportAlerts.current?.game != null && SportAlerts.jumpTarget(SportAlerts.current) != null) {
            if (event.action == android.view.KeyEvent.ACTION_DOWN && event.repeatCount == 0) SportAlerts.jump()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    /** Reminder tapped while the app is already running: open the channel. */
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(Reminders.EXTRA_CH)?.let { AppNav.openChannel = it }
    }

    override fun onStart() {
        super.onStart()
        Reminders.foreground = true
        // W-010b: fetch changes from the other devices (only once AppContent has started the live sync)
        LiveSync.onForeground()
    }

    override fun onStop() {
        Reminders.foreground = false
        // W-010b: upload pending changes when leaving the app
        LiveSync.onBackground()
        super.onStop()
    }

    /** Home key (or app switch) by the user – not on the screensaver and not on trailer/external player. */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        AppNav.leftByUser = true
    }

    /** Back in the app after the Home key: to the home screen (see AppContent). */
    override fun onRestart() {
        super.onRestart()
        if (AppNav.leftByUser) { AppNav.leftByUser = false; AppNav.homeSignal++ }
    }

    override fun onResume() {
        super.onResume()
        // After standby or app switch set the window flag again in case the system dropped it
        ScreenAwake.apply()
    }

    override fun onDestroy() {
        ScreenAwake.detach(this)
        super.onDestroy()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        @Suppress("DEPRECATION")
        val low = TRIM_MEMORY_RUNNING_LOW
        if (level >= low) Mem.trim(this)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onLowMemory() {
        super.onLowMemory()
        Mem.trim(this)
    }
}

/** T-077: set by the settings page while it is open (read by [SmoothSpec] on every focus step). */
object FocusScrollMode {
    var edgeOnly = false
}

@OptIn(ExperimentalFoundationApi::class)
val SmoothSpec = object : BringIntoViewSpec {
    // T-070 (device performance): shorter with "Weak device"
    override val scrollAnimationSpec get() = tween<Float>(ThemeMath.scrollMs(DevicePerf.reduceVisuals), easing = FastOutSlowInEasing)
    // T-074 (focus): focused item kept in the middle; at the end of a list the scroll stops and the focus moves on
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float =
        // T-077: on the settings page the page moves only when a control is cut off (edge mode), no centring
        if (FocusScrollMode.edgeOnly) FocusCenterLogic.edgeDistance(offset, size, containerSize)
        else FocusCenterLogic.scrollDistance(offset, size, containerSize)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun App(pick: Boolean = false) {
    // Display size: the design is for 960 dp width. On devices with more dp (e.g. Fire TV) it is scaled up automatically.
    val cfg = LocalConfiguration.current
    val base = LocalDensity.current
    val factor = ThemeMath.uiFactor(Look.uiScale, cfg.screenWidthDp, cfg.screenHeightDp)
    val tf = General.textFactor()
    val scaled = remember(base.density, base.fontScale, factor, tf) { Density(base.density * factor, base.fontScale * tf) }
    CompositionLocalProvider(
        LocalDensity provides scaled,
        LocalBringIntoViewSpec provides SmoothSpec,
        LocalOverscrollFactory provides null,
        // T-029: Arabic is laid out right to left (the other languages stay left to right)
        androidx.compose.ui.platform.LocalLayoutDirection provides
            (if (Lang.code() == "ar") androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr)
    ) {
        val c = LocalContext.current
        var picked by remember { mutableStateOf(!pick) }
        if (!picked) ProfilePicker { id -> Profiles.select(c, id); AppSettings.init(c); Parental.init(c); LangFilter.init(c); Boomer.init(c); HomeLayout.init(c); BoomerLayout.init(c); picked = true }
        else {
            LaunchedEffect(Unit) { delay(10_000); runCatching { SportAlerts.run(c.applicationContext) } }   // after the start: the playlist and the home screen come first
            Box(Modifier.fillMaxSize()) { AppContent(); SportAlertHost() }
        }
        // New version available: dialog with one button (above every screen)
        UpdatePromptHost()
        // Provider certificate does not match its address: ask once per host before an unencrypted connection (#69)
        TlsConsentDialog()
        // #95: keep the screen awake while the app is open, if the user switched it on (default off)
        KeepScreenOn("browse", General.keepAwakeBrowse)
        // First start without a playlist: one non-blocking card that checks for updates (T-014)
        if (picked) UpdateCardHost()
    }
}

/** "Adjust screen": margin all around (in percent of width/height) in case the TV cuts off edges. */
fun Modifier.fitScreenInset(on: Boolean): Modifier = if (!on || (General.fitX == 0 && General.fitY == 0)) this else this.layout { m, c ->
    val dx = (c.maxWidth * General.fitX / 100f).toInt()
    val dy = (c.maxHeight * General.fitY / 100f).toInt()
    val inner = c.copy(maxWidth = (c.maxWidth - 2 * dx).coerceAtLeast(0), maxHeight = (c.maxHeight - 2 * dy).coerceAtLeast(0), minWidth = 0, minHeight = 0)
    val pl = m.measure(inner)
    layout(c.maxWidth, c.maxHeight) { pl.place(dx, dy) }
}

private val SE_RE = Regex("""S(\d+)E(\d+)""")

/**
 * T-069b: like a Box around [content]; with [hidden] the content stays composed (state, rows, images) but is neither
 * measured, placed nor drawn. Not placed content gets no focus by D-pad and no touch, the same way Compose treats list
 * items that are composed ahead but not placed.
 */
@Composable
private fun KeepAliveBox(hidden: Boolean, modifier: Modifier, content: @Composable () -> Unit) {
    // One policy object per state, so recompositions do not ask for a new measure pass
    val policy = remember(hidden) {
        androidx.compose.ui.layout.MeasurePolicy { measurables, constraints ->
            if (hidden) layout(constraints.minWidth, constraints.minHeight) {}
            else {
                val loose = constraints.copy(minWidth = 0, minHeight = 0)
                val placeables = measurables.map { it.measure(loose) }
                val w = maxOf(constraints.minWidth, placeables.maxOfOrNull { it.width } ?: 0)
                val h = maxOf(constraints.minHeight, placeables.maxOfOrNull { it.height } ?: 0)
                layout(w, h) { placeables.forEach { it.placeRelative(0, 0) } }
            }
        }
    }
    androidx.compose.ui.layout.Layout(content, modifier, policy)
}

@Composable
private fun AppContent() {
    val ctx = LocalContext.current
    remember {
        AppSettings.init(ctx); WatchStore.init(ctx); History.init(ctx); Library.init(ctx); QualityStore.init(ctx); PrivateCats.init(ctx)
        PlaybackPrefs.init(ctx); PlayerSync.init(ctx); AudioLevel.init(ctx); loadWishboxTexts(); DeadLinks.init(ctx); ChannelLists.init(ctx); Reminders.init(ctx); Reminders.schedule(ctx)
        // W-010b: live sync of favorites and history (silent; only when Google Drive is connected)
        LiveSync.start(ctx); 0
    }
    // Interrupted in the middle of adding last time (crash, power loss): the add screen automatically continues loading
    val resumeAdd = remember { PlaylistStore.pending(ctx)?.autoResume == true }
    var adding by remember { mutableStateOf(PlaylistStore.savedUrl(ctx) == null || resumeAdd) }
    // Top detail page (Item or PersonRef) plus back stack, so Back retraces Movie → Person → Movie
    var top by remember { mutableStateOf<Any?>(null) }
    val trail = remember { mutableStateListOf<Any>() }
    val detail = top as? Item
    val personTop = top as? PersonRef
    fun pushTop(x: Any) { top?.let { trail.add(it) }; top = x }
    fun popTop() { top = trail.removeLastOrNull() }
    var browse by remember { mutableStateOf<BrowseState?>(null) }
    // Stalker portal: load the opened category on demand, stop when leaving
    LaunchedEffect(browse) { browse?.let { StalkerMore.attach(ctx, it) } ?: StalkerMore.detach() }
    var cats by remember { mutableStateOf<CatReq?>(null) }
    var playing by remember { mutableStateOf<PlayReq?>(null) }
    var tab by remember { mutableStateOf(SettingsLogic.resolveStartTab(General.startTab, AppSettings.menuOrder.toList(), AppSettings.menuHidden.toList())) }
    // W-002: the start page is the movie or series area: open its categories as well
    LaunchedEffect(Unit) { if (General.vodCats && (tab == 1 || tab == 2)) AppNav.pendingVodCats = tab }
    val gridState = remember(browse) { LazyGridState() }
    val states = listOf(
        rememberLazyListState(), rememberLazyListState(), rememberLazyListState(),
        rememberLazyListState(), rememberLazyListState(), rememberLazyListState(), rememberLazyListState(),
        rememberLazyListState(), rememberLazyListState(), rememberLazyListState(), rememberLazyListState()
    )

    // W-004: "What's new" once after an update to a new version
    LaunchedEffect(Unit) { runCatching { WhatsNew.check(ctx) } }
    // T-056: the search index is built in the background as soon as the list is there (and again after every list change),
    // so opening the search does not wait for it. Same list object as the search screen uses, so the cache is hit.
    val searchList = PlaylistStore.channels
    LaunchedEffect(searchList) {
        // T-070: "Weak device" builds the index only when the search opens (saves memory); low memory in "Automatic": later
        val perf = DevicePerf.profile
        if (perf.searchOnDemand) return@LaunchedEffect
        delay(perf.searchDelayMs)   // T-069: the home rows and the first scrolling get the CPU first
        withContext(Dispatchers.Default) { runCatching { SearchCache.build(PrivateCats.visible(searchList)) } }
    }
    // T-031: Trakt history and watchlist into the app once the playlist is there (at most every 6 hours, needs a sign-in).
    // Waits 8 s so the first screens and the playlist refresh get the CPU and the network first.
    val traktReady = searchList.isNotEmpty() && !PlaylistStore.loading
    LaunchedEffect(traktReady) {
        if (traktReady) { kotlinx.coroutines.delay(8_000); runCatching { TraktSync.autoSync(ctx) } }
    }
    // New app version on GitHub? Check silently once a day; a find appears in the bell at the top right.
    LaunchedEffect(Unit) {
        delay(3_000)   // only after the start, the playlist takes priority
        runCatching { AppUpdate.startCheck(ctx) }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) { General.cleanupLegacy(ctx) }
        if (resumeAdd) return@LaunchedEffect
        PlaylistStore.savedUrl(ctx)?.let { u ->
            // T-048b: the plan of this playlist (its own, otherwise the general one)
            val skip = !SettingsLogic.refreshDue(PlaylistRefresh.effective(ctx, u), PlaylistRefresh.lastFor(ctx, u), System.currentTimeMillis(), General.zone())
            val ok = PlaylistStore.load(ctx, u, false, skip, startup = true)
            if (ok && !skip) PlaylistRefresh.markFor(ctx, u)
            // Other playlists with their own plan that is due: in the background, after the first screens
            delay(20_000)
            runCatching { PlaylistStore.refreshOthersDue(ctx) }
        }
    }

    // Back in the app after the Home key: to the home screen (a running playback and adding stay as they are)
    LaunchedEffect(AppNav.homeSignal) {
        if (AppNav.homeSignal > 0 && playing == null && !adding) {
            val changed = top != null || browse != null || cats != null || tab != 0
            top = null; trail.clear(); browse = null; cats = null
            if (changed) AppNav.focusTab = 0
            if (tab != 0) { tab = 0; CatMemory.reset() }
        }
    }

    // Left the add screen without anything loaded (e.g. resuming cancelled): load the saved playlist.
    // Not when a list was already shown and is only empty because of the playlist mode (see AddFlowLogic).
    var wasAdding by remember { mutableStateOf(adding) }
    LaunchedEffect(adding) {
        if (AddFlowLogic.shouldReloadAfterAddClosed(wasAdding, adding, PlaylistStore.channels.isEmpty(), PlaylistStore.loading, PlaylistStore.shownUrl != null)) {
            PlaylistStore.savedUrl(ctx)?.let { PlaylistStore.load(ctx, it, false, true) }
        }
        wasAdding = adding
    }

    val all = PlaylistStore.channels
    val liveAll = remember(all) { all.filter { it.kind == Kind.LIVE } }
    SideEffect { SportAlerts.liveChannels = liveAll }
    // Do not fall into the screensaver while downloading and parsing the playlist
    KeepScreenOn("playlist", PlaylistStore.loading)
    // Genre detection in the background; restarts cleanly with every new list (reload, language filter, parental control)
    // T-070: 30 s instead of 8 s on weak devices and with little memory (DevicePerfLogic)
    LaunchedEffect(all) { kotlinx.coroutines.delay(DevicePerf.profile.genreDelayMs); if (!DemoMode.active) Classifier.restart(ctx, all) }
    // Private categories (T-028): ids of their titles, so history and resume can skip them
    LaunchedEffect(all) { PrivateCats.reload(all) }   // after the first screen is up, and debounced while the list is still changing
    // T-069: lists kept for the next screen visit belong to one playlist; a new list must not keep the old titles in memory.
    // T-069c: then the movie source index for Play is built for the new list in the background.
    LaunchedEffect(all) {
        ScreenCaches.clear()
        if (all.isEmpty()) return@LaunchedEffect
        delay(StartupLogic.SOURCES_INDEX_DELAY_MS)
        // Assigned only if this list is still the current one (a list change cancels the effect, and the result is dropped)
        val idx = withContext(Dispatchers.Default) { runCatching { Sources.buildIndex(all) }.getOrNull() } ?: return@LaunchedEffect
        Sources.use(idx)
    }

    fun start(item: Item, title: String, url: String, startMs: Long = 0L) {
        if (DemoMode.active) return   // sample entries have no stream address
        History.add(ctx, item.id)
        val sm = SE_RE.find(title)
        val season = sm?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val ep = sm?.groupValues?.get(2)?.toIntOrNull() ?: 0
        val tmdb = MetaRepo.cached(item)?.tmdbId?.takeIf { it > 0 } ?: 0
        var playUrl = url
        var alts = emptyList<Variant>()
        var note: String? = null
        if (item.kind == Kind.MOVIE && !item.id.startsWith("rec:")) {
            // Same movie in other qualities (own entries of the provider): choose to suit the device, the rest as fallbacks
            val list = Sources.alternatives(item, all)
            if (list.size > 1) {
                alts = Sources.asVariants(list)
                val chosen = alts.indexOfFirst { it.url == url }.coerceAtLeast(0)
                val cap = PlaybackPrefs.cap(ctx)
                val best = QualityLogic.order(alts.map { it.quality }, cap).first()
                val rc = QualityLogic.rank(alts[chosen].quality)
                // Quality chosen explicitly on the detail page takes precedence over the automatic mode
                val vp = VariantPref.get(ctx, item.id)
                // Explicitly chosen source (detail page, round 14) before explicitly chosen quality before automatic
                val fixedSrc = vp.third?.let { l -> alts.indexOfFirst { it.label == l } }?.takeIf { it >= 0 }
                val fixed = vp.first?.let { q -> alts.indexOfFirst { it.quality == q } }?.takeIf { it >= 0 }
                if (fixedSrc != null && !DeadLinks.isDead(alts[fixedSrc].url)) {
                    playUrl = alts[fixedSrc].url
                } else if (fixed != null) {
                    playUrl = alts[fixed].url
                } else if (PlaybackPrefs.quality == QualityLogic.AUTO && rc > cap && QualityLogic.rank(alts[best].quality) <= cap) {
                    playUrl = alts[best].url
                    note = (alts[chosen].quality ?: "Diese Qualität") + " passt nicht zu diesem Gerät – spiele " + (alts[best].quality ?: "SD")
                } else if (DeadLinks.isDead(url)) {
                    // chosen source is known to be broken: take another one right away
                    alts.indices.firstOrNull { it != chosen && !DeadLinks.isDead(alts[it].url) }?.let { playUrl = alts[it].url; note = "Quelle gestört – spiele eine andere" }
                }
            }
        }
        playing = PlayReq(title, playUrl, item.id, item.kind == Kind.LIVE, startMs, tmdb, season, ep, alts, note)
    }

    /** EPG (sidebar/tile): start the last watched channel (otherwise the first) and show the TV guide right away. */
    fun openEpg() {
        val ch = History.ids.firstNotNullOfOrNull { id -> liveAll.firstOrNull { it.id == id } } ?: liveAll.firstOrNull() ?: return
        AppNav.focusTab = -1
        ZapContext.list = liveAll; ZapContext.key = "ALL"
        AppNav.openGuide = true
        start(ch, ch.title, ch.url)
    }

    /** Switch area; "EPG" is not a screen of its own but opens the TV guide in the player. */
    // Area switched (tile, "Show all", sidebar): put the focus into the new content.
    // Otherwise it falls back to the first element, which is the sidebar – it expands by itself (round 15).
    var jumpTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(jumpTick) {
        if (jumpTick == 0) return@LaunchedEffect
        val id = tab
        // T-069d: from the next frame on, every 40 ms (before: first try after 150 ms, then every 120 ms); same 2.4 s in total
        withFrameNanos { }
        repeat(60) {
            if (ContentFocus.tab == id && runCatching { ContentFocus.req.requestFocus() }.getOrDefault(false)) return@LaunchedEffect
            delay(40)
        }
    }
    fun goTab(id: Int) {
        if (id == MenuLogic.EPG) { openEpg(); return }
        tab = id; CatMemory.reset()
        // W-002: movies/series straight to their categories (the area opens them when its data is ready)
        AppNav.pendingVodCats = if (General.vodCats && !ViewLive.active && (id == 1 || id == 2)) id else -1
        jumpTick++
    }

    val open: (Item) -> Unit = { i ->
        if (i.kind == Kind.SERIES) NewEpisodes.dismiss(ctx, i.id)
        LastFocus.pending = i.id
        if (i.kind == Kind.LIVE) start(i, i.title, i.url) else pushTop(i)
    }

    // Reminder notification tapped: open the channel as soon as the list is loaded
    LaunchedEffect(AppNav.openChannel, liveAll) {
        val id = AppNav.openChannel ?: return@LaunchedEffect
        val ch = liveAll.firstOrNull { it.id == id } ?: return@LaunchedEffect
        AppNav.openChannel = null
        General.startupHandled = true
        top = null; browse = null; cats = null; adding = false
        start(ch, ch.title, ch.url)
    }
    // Phone search: title the phone wants to start (movie, episode, channel), as soon as the playlist is loaded
    LaunchedEffect(RemoteHub.playReq, all) {
        val r = RemoteHub.playReq ?: return@LaunchedEffect
        val it = all.firstOrNull { x -> x.id == r.id } ?: return@LaunchedEffect
        RemoteHub.playReq = null
        RemoteUi.show = false
        General.startupHandled = true
        top = null; trail.clear(); browse = null; cats = null; adding = false
        when (it.kind) {
            Kind.LIVE -> { ZapContext.list = emptyList(); ZapContext.key = null; start(it, it.title, it.url) }
            Kind.MOVIE -> {
                // Already started? Then continue where you left off (as on the TV)
                val we = WatchStore.entries.firstOrNull { w -> w.itemId == it.id && !w.next }
                start(it, it.title, it.url, we?.pos ?: 0L)
            }
            Kind.SERIES -> {
                val ep = LazySeries.full(it).episodes.firstOrNull { e -> e.season == r.season && e.num == r.ep }
                if (ep != null) {
                    // Same episode already started: continue at the remembered position
                    val we = WatchStore.entries.firstOrNull { w -> w.itemId == it.id && !w.next && w.season == ep.season && w.ep == ep.num }
                    start(it, it.title + " S" + ep.season + "E" + ep.num, pickVariant(ep, null, null, null, PlaybackPrefs.cap(ctx)).url, we?.pos ?: 0L)
                }
            }
        }
    }
    // "Stop" on the phone: close the player
    LaunchedEffect(RemoteHub.closeTick) { if (RemoteHub.closeTick > 0 && playing != null) playing = null }
    // Reminders while the app is open: card with "Switch" (instead of a notification)
    var remindNow by remember { mutableStateOf<Reminder?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            if (remindNow == null) Reminders.pending(System.currentTimeMillis() / 1000).firstOrNull()?.let { r ->
                Reminders.markDone(ctx, r); remindNow = r
            }
            delay(15_000)
        }
    }

    // Settings "Play last channel / last movie or series at start" (once per app start).
    // What counts is what ran last: a channel or a movie / series.
    LaunchedEffect(all.isNotEmpty()) {
        if (all.isNotEmpty() && !General.startupHandled) {
            General.startupHandled = true
            val lastId = History.ids.firstOrNull()
            val last = if (lastId == null || playing != null) null else all.firstOrNull { it.id == lastId }
            if (last != null && last.kind == Kind.LIVE && General.resumeLive) start(last, last.title, last.url)
            else if (last != null && last.kind != Kind.LIVE && General.resumeVod) {
                // only with a remembered position (fully watched movies do not start from the beginning; for series possibly the next episode)
                val e = WatchStore.entries.firstOrNull { it.itemId == last.id }
                if (e != null) {
                    pushTop(last)
                    val t = WatchStore.target(ctx, last, e)
                    start(last, t.first, t.second, e.pos)
                }
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().holeBackground(Bg).fitScreenInset(playing == null)) {
        val focusManager = LocalFocusManager.current
        val homeFocus = remember { BooleanArray(1) }
        // Scale of the design from the available width (see Design)
        val dk = maxWidth.value / Design.WIDTH
        SideEffect { if (Design.k != dk) Design.k = dk }
        val p = playing
        val d = detail
        val b = browse
        val c = cats
        // T-056: name of the shown screen, for the opening time in the diagnostics log
        val shownName = when {
            p != null -> "Player"
            personTop != null -> "Person"
            d != null -> "Detail"
            b != null -> "Browse"
            c != null -> "Categories"
            adding -> "Add playlist"
            tab == 4 && all.isNotEmpty() -> "Search"
            tab == MenuLogic.DOWNLOADS -> "Downloads"
            else -> "Tab $tab"
        }
        ScreenTimer(shownName)
        // T-069b: the home screen is drawn first and stays composed, hidden and not laid out, under a detail or person page
        // opened from it, so Back shows it at once (rows, scroll positions, images). Every other screen replaces it as before.
        val homeBase = HomeKeepLogic.homeIsBase(p != null, b != null, c != null, adding, Boomer.enabled, tab == 4 && all.isNotEmpty(), tab == MenuLogic.DOWNLOADS)
        val pageOnTop = personTop != null || d != null
        // Checked when a page opens and again when Android reports low memory (then the hidden home screen is let go)
        val heapTight = remember(top, Mem.trimTick) { Mem.heapTight(HomeKeepLogic.HEAP_LIMIT) }
        // T-070: not with "Weak device" (saves the memory of the kept screen)
        val homeHidden = HomeKeepLogic.keepHidden(homeBase, pageOnTop, heapTight, HomeKeepLogic.ENABLED && DevicePerf.profile.keepHome)
        // The focus must not stay on a hidden card (keys would go there until the page takes the focus)
        LaunchedEffect(homeHidden) { if (homeHidden && homeFocus[0]) runCatching { focusManager.clearFocus(force = true) } }
        if (HomeKeepLogic.composeHome(homeBase, pageOnTop, homeHidden)) {
            KeepAliveBox(homeHidden, Modifier.fillMaxSize().onFocusChanged { homeFocus[0] = it.hasFocus }.screenEnter(tab)) { HomeScreen(
                tab = tab, onTab = { goTab(it) }, states = states,
                onOpen = open,
                onPlayNow = { i -> LastFocus.pending = i.id; if (i.kind == Kind.SERIES) pushTop(i) else { if (i.kind != Kind.LIVE && !i.id.startsWith("rec:")) pushTop(i); start(i, i.title, i.url) } },
                onResume = { e, i ->
                    LastFocus.pending = i.id
                    if (i.kind != Kind.LIVE) pushTop(i)
                    // current address from the loaded playlist instead of the stored one (providers change addresses)
                    val t = WatchStore.target(ctx, i, e)
                    start(i, t.first, t.second, e.pos)
                },
                onBrowse = { browse = BrowseState(it) },
                onCategories = { cats = it },
                onAdd = { DemoMode.exit(); adding = true },
                hidden = homeHidden
            ) }
        }
        when {
            p != null -> {
                val zap = if (p.live || p.archive != null) {
                    val ctxList = ZapContext.list
                    if (ctxList.any { it.id == p.itemId }) ctxList
                    else {
                        val g = liveAll.firstOrNull { it.id == p.itemId }?.group
                        liveAll.filter { it.group == g }
                    }
                } else emptyList()
                val seriesBase = remember(p.itemId, all) {
                    if (p.live || p.archive != null) null else all.firstOrNull { it.id == p.itemId }?.let { LazySeries.full(it) }
                }
                // Series with a placeholder (e.g. Resume from the home screen): fetch episodes in the background, the player uses them right after
                val seriesItem by produceState(seriesBase, seriesBase) {
                    value = seriesBase
                    val b = seriesBase
                    if (b != null && LazySeries.isStub(b)) LazySeries.load(ctx, b)?.let { value = it }
                }
                // New start from outside (e.g. reminder "Switch") during playback: rebuild the player
                key(p) {
                    PlayerScreen(p, zap, liveAll, seriesItem) {
                        // Live: back in Live TV on the last watched channel (also after switching in the player)
                        // Live TV tab is 3 (see LiveTab: ContentFocus.tab = 3). Without this, a stream started from Home returns to Home.
                        // Exception: a stream started in the Sports Hub returns to the hub (maintainer, 23 Nov 2026); the hub restores
                        // its open pages and tab itself (HubMemory)
                        if ((p.live || p.archive != null) && tab != MenuLogic.SPORT) {
                            LastFocus.pending = History.ids.firstOrNull { id -> liveAll.any { it.id == id } }
                            tab = 3
                        }
                        playing = null
                    }
                }
            }
            // Round 17: every screen fades in softly (screenEnter); the player deliberately without an effect
            personTop != null -> key(personTop.id) { Box(Modifier.fillMaxSize().screenEnter()) { PersonScreen(personTop, all, onOpen = { pushTop(it) }, onBack = { popTop() }) } }
            d != null -> key(d.id) { Box(Modifier.fillMaxSize().screenEnter()) {
                DetailScreen(
                    d, all, onPlay = { url, t, pos -> start(d, t, url, pos) },
                    onPerson = { pushTop(it) }, onOpenItem = { pushTop(it) }, onBack = { popTop() }
                )
            } }
            b != null -> Box(Modifier.fillMaxSize().screenEnter()) { BrowseScreen(
                b, gridState, tab, onOpen = open,
                onBack = { browse = null },
                onGoTab = { if (it != MenuLogic.EPG) { browse = null; cats = null }; goTab(it) },
                // Started from the cinema view: put the title's detail page underneath so "Back" lands there (not in the cinema view again)
                all = all, onPlay = { i, t, url, pos -> if (i.kind != Kind.LIVE) pushTop(i); start(i, t, url, pos) }, onPerson = { pushTop(it) }
            ) }
            c != null -> Box(Modifier.fillMaxSize().screenEnter()) { CategoriesScreen(
                c, tab,
                onPick = { name, list, sort, genre -> browse = BrowseState(BrowseReq(name, list, sort, genre)) },
                onOpen = open,
                onBack = { cats = null },
                onGoTab = { if (it != MenuLogic.EPG) cats = null; goTab(it) }
            ) }
            adding -> Box(Modifier.fillMaxSize().screenEnter()) { AddPlaylistScreen { adding = false } }
            // Boomer mode: very simple home screen with big channel tiles instead of the normal UI
            Boomer.enabled -> BoomerHome(
                all, PlaylistStore.loading,
                onPlay = { ch, list ->
                    ZapContext.list = list; ZapContext.key = null
                    start(ch, ch.title, ch.url)
                },
                // Movie: starts immediately (Back leads to the detail page underneath); series: episode list
                onVod = { i ->
                    LastFocus.pending = i.id
                    if (i.kind == Kind.SERIES) pushTop(i) else { pushTop(i); start(i, i.title, i.url) }
                },
                onResume = { e, i ->
                    LastFocus.pending = i.id
                    pushTop(i)
                    val t = WatchStore.target(ctx, i, e)
                    start(i, t.first, t.second, e.pos)
                }
            )
            tab == 4 && all.isNotEmpty() -> Box(Modifier.fillMaxSize().screenEnter()) { SearchScreen(
                tab = tab, state = states[4],
                onOpen = open,
                onBrowse = { browse = BrowseState(it) },
                onTab = { goTab(it) },
                onPerson = { pushTop(it) }
            ) }
            tab == MenuLogic.DOWNLOADS -> Box(Modifier.fillMaxSize().screenEnter(tab)) { DownloadsTab(tab = tab, onTab = { goTab(it) },
                onPlay = { e -> start(Item(e.id, Kind.MOVIE, e.title, null, e.url, null, "Downloads"), e.title, e.url, 0L) }) }
            // Home screen: drawn above the screens (T-069b)
            else -> {}
        }
        if (RemoteUi.show) RemoteOverlay(Modifier.align(Alignment.Center))
        // W-004: not over the player or the add screen, and not at the same time as the update dialog
        if (p == null && !adding && (AppUpdate.available == null || AppUpdate.promptDismissed)) WhatsNewHost()
        // Live preview of the View settings: bar at the bottom, the real app stays visible
        if (ViewLive.active) ViewLivePanel(ctx, goTab = { goTab(it) }, closeCats = { cats = null; browse = null }) {
            ViewLive.active = false
            SettingsNav.openPage = 2
            goTab(MenuLogic.SETTINGS)
        }
        // Genre detection in the background: unobtrusive at the top right, never in the player
        if (p == null && !adding) MetaBadge(Modifier.align(Alignment.TopEnd).padding(top = 16.dp, end = 22.dp))
        remindNow?.let { r ->
            ReminderCard(r, Modifier.align(Alignment.TopCenter).padding(top = 24.dp), onClose = { remindNow = null }) {
                remindNow = null
                val ch = liveAll.firstOrNull { it.id == r.chId }
                if (ch != null) { top = null; browse = null; cats = null; start(ch, ch.title, ch.url) }
            }
        }
    }
}
