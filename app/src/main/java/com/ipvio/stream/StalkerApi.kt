package com.ipvio.stream

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.Writer
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Connection to a Stalker/Ministra portal (see [StalkerLogic]): login with the MAC address, channels, movies and
 * series as a list, play address at start. Series appear as placeholders in the list; [episodeList] fetches their episodes
 * only when a series is opened.
 */
object StalkerApi {
    private val client: OkHttpClient = Net.base.newBuilder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).addInterceptor(TlsFallback.interceptor()).build()

    /** Logged-in session: API and token, reused for a few minutes. */
    private class Session(val src: StalkerLogic.Source, val endpoint: String, val token: String, val at: Long)
    private val sessions = HashMap<Int, Session>()
    private const val SESSION_MS = 10 * 60_000L
    /**
     * Upper limits for the whole list. Before 1.4 movies/series were capped at 6,000/4,000 and 40–600 per category, so large
     * portals (100,000 movies) showed only a small part. Now only a safety limit; memory is guarded while parsing.
     */
    private const val MAX_MOVIES = 250_000
    private const val MAX_SERIES = 100_000
    private const val MAX_CHANNELS = 30000
    /** Most pages of one list (14 items per page on most portals). */
    private const val MAX_PAGES = 20_000

    /**
     * List pages are fetched by a few workers at the same time: a portal delivers ~14 titles per page, so 100,000 movies
     * are ~7,000 requests – one after another that took far too long. A failed page is retried alone.
     */
    private const val WORKERS = 4
    private const val WINDOW = 8
    private val pool = java.util.concurrent.Executors.newFixedThreadPool(WORKERS) { r -> Thread(r, "stalker-pages").apply { isDaemon = true } }

    /** App files folder (for the channel id table), set by [init]. */
    @Volatile private var dir: java.io.File? = null

    fun init(c: Context) { if (dir == null) dir = c.applicationContext.filesDir }

    /**
     * The portal's guide asks for the channel id ("ch_id"), the play address in the list only holds the command.
     * The table "hash of play address → channel id" is written with every list download (file "stk_ch_KEY.map").
     */
    private fun chFile(key: Int): java.io.File? = dir?.let { java.io.File(it, "stk_ch_$key.map") }
    private val chIds = HashMap<Int, Map<Int, String>>()

    private fun saveChannelIds(key: Int, ids: Map<Int, String>) {
        val f = chFile(key) ?: return
        runCatching {
            val t = java.io.File(f.path + ".tmp")
            t.bufferedWriter().use { w -> for ((h, id) in ids) { w.write(h.toString()); w.write("\t"); w.write(id); w.write("\n") } }
            f.delete(); t.renameTo(f)
        }
        synchronized(chIds) { chIds[key] = ids }
    }

    private fun channelId(key: Int, url: String): String? {
        val m = synchronized(chIds) { chIds[key] } ?: run {
            val f = chFile(key)?.takeIf { it.exists() } ?: return null
            val read = HashMap<Int, String>()
            runCatching {
                f.forEachLine { l ->
                    val i = l.indexOf('\t')
                    if (i > 0) l.substring(0, i).toIntOrNull()?.let { read[it] = l.substring(i + 1) }
                }
            }
            synchronized(chIds) { chIds[key] = read }
            read
        }
        return m[url.hashCode()]
    }

    /** Live channel of a Stalker portal ("stalker://KEY/live/…")? */
    fun isLive(url: String): Boolean = url.startsWith("stalker://") && url.contains("/live/")

    /**
     * Running and next shows of a channel from the portal (action get_short_epg). null = not possible (no channel id yet:
     * list loaded with an older version, until the next refresh).
     */
    fun shortEpg(url: String): List<EpgEntry>? {
        val ref = StalkerLogic.itemRef(url) ?: return null
        val src = sourceOf(ref.key) ?: return null
        val ch = channelId(ref.key, url) ?: return null
        val s = runCatching { login(src) }.getOrNull() ?: return null
        val r = js(src, StalkerLogic.api(s.endpoint, "itv", "get_short_epg", "&ch_id=" + enc(ch) + "&size=10"), s.token)
        val a = (r as? JSONArray) ?: (r as? JSONObject)?.optJSONArray("data") ?: return null
        return StalkerLogic.epgEntries((0 until a.length()).mapNotNull { a.optJSONObject(it)?.let(::fields) })
    }

    /** Last response of the portal in short form (only code and kind, never address or MAC) for understandable error texts. */
    @Volatile private var note = ""

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun req(src: StalkerLogic.Source, url: String, token: String?): Request {
        val b = Request.Builder().url(url)
            .header("User-Agent", StalkerLogic.UA)
            .header("X-User-Agent", StalkerLogic.X_UA)
            .header("Accept", "*/*")
            .header("Referer", src.portal)
            .header("Cookie", StalkerLogic.cookie(src.mac))
        if (token != null) b.header("Authorization", "Bearer $token")
        return b.build()
    }

    /** JSON from a response; portals sometimes send a BOM or text before the JSON. */
    private fun jsonOf(body: String): JSONObject? {
        val i = body.indexOf('{')
        if (i < 0) return null
        return runCatching { JSONObject(body.substring(i)) }.getOrNull()
    }

    /** Response as the "js" part, null on error (the reason is then in [note]). */
    private fun js(src: StalkerLogic.Source, url: String, token: String?): Any? = try {
        client.newCall(req(src, url, token)).execute().use { r ->
            val body = r.body?.string().orEmpty()
            if (!r.isSuccessful) { note = "HTTP ${r.code}"; null }
            else {
                val o = jsonOf(body)
                if (o == null) { note = "keine gültige Antwort"; null } else { note = ""; o.opt("js") }
            }
        }
    } catch (e: Exception) {
        note = "keine Verbindung"
        null
    }

    /** Log in: find the API, get the token, fetch the profile (some portals only unlock with it). */
    private fun login(src: StalkerLogic.Source, force: Boolean = false): Session {
        synchronized(sessions) {
            val s = sessions[src.key]
            if (!force && s != null && System.currentTimeMillis() - s.at < SESSION_MS) return s
        }
        var seen = ""
        for (ep in StalkerLogic.endpoints(src.portal)) {
            // First with, then without "prehash" (old portals do not know it)
            var hs: JSONObject? = null
            for (extra in listOf("&token=&prehash=0", "&token=")) {
                hs = js(src, StalkerLogic.api(ep, "stb", "handshake", extra), null) as? JSONObject
                if (hs?.optString("token").isNullOrBlank()) hs = null else break
                if (note.isNotEmpty()) seen = note
            }
            val token = hs?.optString("token")?.takeIf { it.isNotBlank() } ?: continue
            js(src, StalkerLogic.api(ep, "stb", "get_profile", StalkerLogic.profileExtra(src)), token)
            val s = Session(src, ep, token, System.currentTimeMillis())
            synchronized(sessions) { sessions[src.key] = s }
            return s
        }
        throw PlaylistError(
            if (seen.isNotEmpty()) "Das Portal antwortet nicht richtig ($seen). Adresse und MAC-Adresse prüfen."
            else "Das Portal hat die Anmeldung abgelehnt. Adresse und MAC-Adresse prüfen."
        )
    }

    private fun categories(s: Session, type: String, action: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        val a = js(s.src, StalkerLogic.api(s.endpoint, type, action), s.token) as? JSONArray ?: return out
        for (i in 0 until a.length()) {
            val o = a.optJSONObject(i) ?: continue
            val id = o.opt("id")?.toString().orEmpty()
            val t = o.optString("title").ifBlank { o.optString("name") }
            if (id.isNotEmpty() && t.isNotBlank()) out[id] = t.trim()
        }
        return out
    }

    private fun fields(o: JSONObject): Map<String, String> {
        val f = HashMap<String, String>()
        for (k in o.keys()) { val v = o.opt(k); if (v is String || v is Number) f[k] = v.toString() }
        return f
    }

    /**
     * Fetch all pages of a list ([extra] = parameters), [each] per entry. Stops at [maxItems], an empty page or
     * a repeating page. true = at least one page arrived.
     */
    private fun paged(s: Session, type: String, extra: String, maxItems: Int, check: () -> Unit, each: (JSONObject) -> Unit): Boolean {
        var page = 1
        var pages = 1
        var got = 0
        var ok = false
        var lastFirst: String? = null
        while (page <= pages && got < maxItems) {
            check()
            val o = js(s.src, StalkerLogic.api(s.endpoint, type, "get_ordered_list", "$extra&p=$page"), s.token) as? JSONObject ?: break
            ok = true
            val a = o.optJSONArray("data") ?: break
            if (a.length() == 0) break
            val first = a.optJSONObject(0)?.let { it.opt("id")?.toString() ?: it.opt("name")?.toString() }
            if (first != null && first == lastFirst) break
            lastFirst = first
            val total = o.optInt("total_items", 0)
            val per = o.optInt("max_page_items", 14).coerceAtLeast(1)
            // Without a total count just keep paging until a page is empty
            pages = if (total > 0) ((total + per - 1) / per).coerceIn(1, 800) else (page + 1).coerceAtMost(800)
            for (i in 0 until a.length()) { a.optJSONObject(i)?.let { each(it); got++ } }
            page++
        }
        return ok
    }

    private fun page(s: Session, type: String, extra: String, p: Int): JSONObject? =
        js(s.src, StalkerLogic.api(s.endpoint, type, "get_ordered_list", "$extra&p=$p"), s.token) as? JSONObject

    private fun firstId(a: JSONArray): String? = a.optJSONObject(0)?.let { it.opt("id")?.toString() ?: it.opt("name")?.toString() }

    /**
     * Like [paged], but pages 2…n are fetched in parallel ([WORKERS]) and handed to [each] in page order.
     * Needs the total count from page 1; without it falls back to [paged]. Stops at an empty page, at a page that
     * repeats page 1 (portal ignores the page number) or at [maxItems].
     */
    private fun pagedFast(s: Session, type: String, extra: String, maxItems: Int, check: () -> Unit, each: (JSONObject) -> Unit): Boolean {
        check()
        val first = page(s, type, extra, 1) ?: return false
        val a1 = first.optJSONArray("data") ?: return true
        val total = first.optInt("total_items", 0)
        val per = first.optInt("max_page_items", 14).coerceAtLeast(1)
        if (total <= 0) return paged(s, type, extra, maxItems, check, each)
        var got = 0
        for (i in 0 until a1.length()) { if (got >= maxItems) return true; a1.optJSONObject(i)?.let { each(it); got++ } }
        if (a1.length() == 0) return true
        val id1 = firstId(a1)
        val pages = ((total + per - 1) / per).coerceIn(1, MAX_PAGES)
        var next = 2
        while (next <= pages && got < maxItems) {
            check()
            val end = minOf(next + WINDOW - 1, pages)
            val batch = (next..end).map { p -> p to pool.submit<JSONObject?> { page(s, type, extra, p) } }
            try {
                for ((p, f) in batch) {
                    check()
                    val o = runCatching { f.get() }.getOrNull() ?: page(s, type, extra, p)
                    val a = o?.optJSONArray("data")
                    if (a == null || a.length() == 0 || (id1 != null && firstId(a) == id1)) return true
                    for (i in 0 until a.length()) { if (got >= maxItems) return true; a.optJSONObject(i)?.let { each(it); got++ } }
                }
            } finally {
                for ((_, f) in batch) f.cancel(false)
            }
            next = end + 1
        }
        return true
    }

    /**
     * Write the whole list to [out] (M3U). [onPhase] = progress, [check] throws on abort.
     * Return value: number of entries.
     */
    fun write(src: StalkerLogic.Source, out: Writer, onPhase: (String) -> Unit, check: () -> Unit): Int {
        onPhase("Zugang wird geprüft …")
        val s = login(src, force = true)
        var n = 0
        out.write("#EXTM3U\n")

        // ---- Channels
        onPhase("Sender werden geladen …")
        val genres = categories(s, "itv", "get_genres")
        var live = 0
        val ids = HashMap<Int, String>()
        val addLive: (Map<String, String>) -> Unit = { f ->
            StalkerLogic.liveEntry(src, f, genres)?.let { e ->
                out.write(e); out.write("\n"); n++; live++
                f["id"]?.takeIf { it.isNotBlank() }?.let { ids[e.substringAfterLast('\n').hashCode()] = it }
            }
        }
        val all = js(src, StalkerLogic.api(s.endpoint, "itv", "get_all_channels"), s.token)
        val data = (all as? JSONObject)?.optJSONArray("data") ?: (all as? JSONArray)
        if (data != null) for (i in 0 until data.length()) {
            val o = data.optJSONObject(i) ?: continue
            addLive(fields(o))
            if (i % 1000 == 0) check()
        }
        if (live == 0) {
            // Fallback: many portals block "all channels at once" and deliver only page by page
            pagedFast(s, "itv", "&genre=*&force_ch_link_check=&fav=0&sortby=number&hd=0", MAX_CHANNELS, check) { o ->
                addLive(fields(o))
                if (live % 400 == 0) onPhase("Sender werden geladen ($live) …")
            }
        }
        if (ids.isNotEmpty()) saveChannelIds(src.key, ids)
        val seenSeries = HashSet<String>()

        // ---- Movies (series in the movie area are marked as series)
        onPhase("Filme werden geladen …")
        var cats = categories(s, "vod", "get_categories").filterKeys { it != "*" }
        // No categories delivered: some portals only list everything at once
        if (cats.isEmpty()) cats = mapOf("*" to "Filme")
        var movies = 0
        var vodSeries = 0
        for ((cid, cname) in cats) {
            check()
            if (movies >= MAX_MOVIES) break
            pagedFast(s, "vod", "&category=$cid&sortby=added", MAX_MOVIES - movies, check) { o ->
                val f = fields(o)
                if (o.optString("is_series") == "1") {
                    val nm = (f["name"] ?: "").trim().lowercase()
                    if (nm.isNotEmpty() && seenSeries.add(nm))
                        StalkerLogic.seriesEntry(src, f, cid, cname, "v")?.let { out.write(it); out.write("\n"); n++; vodSeries++ }
                } else {
                    StalkerLogic.movieEntry(src, f, cname)?.let { out.write(it); out.write("\n"); n++; movies++ }
                }
                if (movies % 500 == 0 && movies > 0) onPhase("Filme werden geladen ($movies) …")
            }
        }
        val movieNote = note

        // ---- Series (own area, placeholders; episodes only on opening)
        onPhase("Serien werden geladen …")
        val sCats = categories(s, "series", "get_categories").filterKeys { it != "*" }
        val catNote = note
        var series = 0
        var seriesOk = false
        val addSeries: (JSONObject, String, String) -> Unit = { o, cid, cname ->
            val f = fields(o)
            val nm = (f["name"] ?: f["title"] ?: "").trim().lowercase()
            if (nm.isNotEmpty() && seenSeries.add(nm))
                StalkerLogic.seriesEntry(src, f, cid, cname, "s")?.let { out.write(it); out.write("\n"); n++; series++ }
            if (series % 500 == 0 && series > 0) onPhase("Serien werden geladen ($series) …")
        }
        for ((cid, cname) in sCats) {
            check()
            if (series >= MAX_SERIES) break
            if (pagedFast(s, "series", "&category=$cid&sortby=added", MAX_SERIES - series, check) { addSeries(it, cid, cname) }) seriesOk = true
        }
        // No series yet: try the whole series area at once (portals without series categories, or categories that fail)
        if (series == 0) {
            check()
            if (pagedFast(s, "series", "&category=*&sortby=added", MAX_SERIES, check) { addSeries(it, "*", "Serien") }) seriesOk = true
        }
        // Short note for the diagnostics area (counts only, never address or MAC): makes "no series" explainable
        if (series == 0 && vodSeries == 0) Diag.report(
            "Stalker: keine Serien (Kategorien ${sCats.size}" + (if (catNote.isNotEmpty()) ", $catNote" else "") +
                (if (!seriesOk && note.isNotEmpty()) ", Liste: $note" else "") + ")"
        )
        if (movies == 0) Diag.report("Stalker: keine Filme (Kategorien ${cats.size}" + (if (movieNote.isNotEmpty()) ", $movieNote" else "") + ")")
        out.flush()
        if (n == 0) {
            val why = if (note.isNotEmpty()) " ($note)" else ""
            throw PlaylistError("Das Portal hat keine Sender geliefert$why. MAC-Adresse und Abo beim Anbieter prüfen.")
        }
        return n
    }

    private fun sourceOf(key: Int): StalkerLogic.Source? =
        PlaylistStore.lists.asSequence().mapNotNull { StalkerLogic.parse(it.url) }.firstOrNull { it.key == key }

    /** Real play address for "stalker://…" (freshly generated by the portal). null = not possible. */
    suspend fun resolve(c: Context, url: String): String? = withContext(Dispatchers.IO) {
        val ref = StalkerLogic.itemRef(url) ?: return@withContext null
        val src = sourceOf(ref.key) ?: return@withContext null
        for (attempt in 0 until 2) {
            val s = runCatching { login(src, force = attempt > 0) }.getOrNull() ?: return@withContext null
            val r = js(src, StalkerLogic.createLink(s.endpoint, ref.type, ref.cmd, ref.series), s.token) as? JSONObject
            val u = StalkerLogic.stripCmd(r?.optString("cmd"))
            if (u != null) return@withContext u
        }
        // Some portals already deliver a directly playable address in the channel
        StalkerLogic.stripCmd(ref.cmd)?.takeIf { !it.contains("localhost") }
    }

    /**
     * Seasons and episodes of a series ([url] = placeholder "stalker://…/series/…"). null = portal does not answer;
     * empty list = series without episodes. Two portal constructions are recognized: seasons with episode numbers ("series" list)
     * and seasons that deliver their episodes as separate entries.
     */
    fun episodeList(url: String): List<Episode>? {
        val ref = StalkerLogic.seriesRef(url) ?: return null
        val src = sourceOf(ref.key) ?: return null
        val s = runCatching { login(src) }.getOrNull() ?: return null
        val type = if (ref.mode == "v") "vod" else "series"
        val seen = HashSet<Long>()
        val out = ArrayList<Episode>()

        fun add(season: Int, num: Int, cmd: String, series: String?) {
            if (cmd.isBlank() || !seen.add((season.toLong() shl 32) or num.toLong())) return
            out.add(Episode(season, num, EpTitles.of(num), StalkerLogic.itemUrl(src.key, "movie", cmd, series)))
        }

        /** Entries of a season: episode numbers or single episodes. true = something found. */
        fun fill(o: JSONObject, season: Int, fallbackNum: Int): Boolean {
            val cmd = o.optString("cmd").trim()
            val nums = o.optJSONArray("series")
            if (nums != null && nums.length() > 0) {
                var any = false
                for (i in 0 until nums.length()) {
                    val n = nums.opt(i)?.toString()?.trim()?.toIntOrNull() ?: continue
                    add(season, n, cmd, n.toString()); any = true
                }
                if (any) return true
            }
            if (cmd.isNotEmpty() && !isSeason(o)) {
                add(season, StalkerLogic.episodeNumber(fields(o), fallbackNum), cmd, null)
                return true
            }
            return false
        }

        val base = "&movie_id=${enc(ref.id)}&category=${enc(ref.cat)}&fav=0&sortby=added&hd=0&not_ended=0"
        val level1 = ArrayList<JSONObject>()
        if (!paged(s, type, "$base&season_id=0&episode_id=0", 600, {}) { level1.add(it) }) return null
        var idx = 0
        for (o in level1) {
            idx++
            val f = fields(o)
            val season = if (level1.size == 1 && !isSeason(o)) 1 else StalkerLogic.seasonNumber(f, idx)
            if (fill(o, season, idx)) continue
            // Season without own episodes: fetch its episodes individually
            val sid = o.opt("id")?.toString().orEmpty()
            if (sid.isEmpty()) continue
            var k = 0
            paged(s, type, "$base&season_id=${enc(sid)}&episode_id=0", 1000, {}) { e ->
                k++
                fill(e, season, k)
            }
        }
        out.sortWith(compareBy({ it.season }, { it.num }))
        return out
    }

    private fun isSeason(o: JSONObject): Boolean {
        val v = o.opt("is_season")
        if (v == true || v == 1 || v == "1" || v == "true") return true
        val nm = o.optString("name")
        return o.optJSONArray("series")?.length() == 0 && Regex("""(?i)(season|staffel|sezon|saison|temporada|stagione)""").containsMatchIn(nm)
    }
}
