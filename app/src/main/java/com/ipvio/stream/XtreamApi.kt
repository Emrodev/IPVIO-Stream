package com.ipvio.stream

import android.util.JsonReader
import android.util.JsonToken
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.io.Writer

/**
 * Loads an Xtream login via player_api.php and writes a list in get.php format from it (see [XtreamLogic]).
 * Large responses (tens of thousands of movies) are read entry by entry and written immediately, never completely in memory.
 */
class XtreamApi(private val client: OkHttpClient, private val ua: String) {

    /** Progress: text for the loading screen. */
    var onPhase: (String) -> Unit = {}
    /** Called regularly and throws on abort. */
    var check: () -> Unit = {}
    /**
     * Growth (or, negative, removal) of the characters in the temporary files of movies and series. The loading screen adds them
     * to the bytes of the list; without them the display stood still at the size of the channel list until everything was there.
     * Called from the download threads.
     */
    var onTempBytes: (Long) -> Unit = {}

    private fun req(url: String) = Request.Builder().url(url)
        .header("User-Agent", ua)
        .header("Accept", "*/*")
        .header("Accept-Language", "en-US,en;q=0.9,de;q=0.8")
        .header("Connection", "keep-alive")
        .build()

    private fun text(url: String): String {
        client.newCall(req(url)).execute().use { r ->
            if (!r.isSuccessful) throw PlaylistError(PlaylistLogic.httpError(r.code))
            return r.body?.string() ?: ""
        }
    }

    /** Checks the login. Throws an understandable message if the login is rejected. */
    fun verify(a: XtreamLogic.Acct) {
        val body = text(XtreamLogic.api(a))
        val o = runCatching { JSONObject(body) }.getOrNull() ?: throw PlaylistError("Die Schnittstelle des Anbieters antwortet nicht wie erwartet.")
        val ui = o.optJSONObject("user_info")
        val ok = XtreamLogic.authOk(ui?.opt("auth")?.toString(), ui?.optString("status"))
        if (ok == false) throw PlaylistError("Zugang abgelehnt. Benutzername, Passwort oder Abo prüfen.")
    }

    /** Names of the live categories (small request), to check a list from get.php for completeness. Empty on error. */
    fun liveCategoryNames(a: XtreamLogic.Acct): Collection<String> = categories(a, "get_live_categories").values

    private fun categories(a: XtreamLogic.Acct, action: String): Map<String, String> {
        val out = HashMap<String, String>()
        val arr = runCatching { JSONArray(text(XtreamLogic.api(a, action))) }.getOrNull() ?: return out
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val id = o.opt("category_id")?.toString()?.trim().orEmpty()
            val n = o.optString("category_name").trim()
            if (id.isNotEmpty() && n.isNotEmpty()) out[id] = n
        }
        return out
    }

    /** Reads a flat JSON object (texts and numbers; for lists only the first value, e.g. "category_ids"). */
    private fun flat(r: JsonReader): Map<String, String> {
        val f = HashMap<String, String>(24)
        r.beginObject()
        while (r.hasNext()) {
            val n = r.nextName()
            when (r.peek()) {
                JsonToken.STRING, JsonToken.NUMBER -> f[n] = r.nextString()
                JsonToken.BOOLEAN -> f[n] = r.nextBoolean().toString()
                JsonToken.BEGIN_ARRAY -> {
                    r.beginArray()
                    var first: String? = null
                    while (r.hasNext()) {
                        if (first == null && (r.peek() == JsonToken.STRING || r.peek() == JsonToken.NUMBER)) first = r.nextString() else r.skipValue()
                    }
                    r.endArray()
                    if (first != null && !f.containsKey(n)) f[n] = first
                }
                else -> r.skipValue()
            }
        }
        r.endObject()
        // Some providers name the category only in category_ids
        if (f["category_id"].isNullOrBlank()) f["category_ids"]?.let { f["category_id"] = it }
        return f
    }

    /**
     * Goes through a (large) list entry by entry. Return value false = the provider did not answer with a list
     * (error object, empty body, …) – the caller decides whether that is an error (see [write]).
     */
    private fun stream(url: String, each: (Map<String, String>) -> Unit): Boolean {
        client.newCall(req(url)).execute().use { r ->
            if (!r.isSuccessful) throw PlaylistError(PlaylistLogic.httpError(r.code))
            val body = r.body ?: return false
            JsonReader(body.charStream()).use { jr ->
                jr.isLenient = true
                if (jr.peek() != JsonToken.BEGIN_ARRAY) return false
                jr.beginArray()
                var k = 0
                while (jr.hasNext()) {
                    if (jr.peek() == JsonToken.BEGIN_OBJECT) each(flat(jr)) else jr.skipValue()
                    if (++k % 1000 == 0) check()
                }
                jr.endArray()
                return true
            }
        }
    }

    /**
     * Goes through the episodes of a series and reports each to [sink] (episode as flat fields, season from the key).
     * "episodes" is, depending on the provider, an object (season → list) or a list. false = error (after a second attempt).
     */
    private fun walkEpisodes(a: XtreamLogic.Acct, seriesId: String, sink: (Map<String, String>, Int?) -> Unit, reset: () -> Unit): Boolean {
        for (attempt in 0 until 2) {
            try {
                var ok = true
                client.newCall(req(XtreamLogic.api(a, "get_series_info", "&series_id=$seriesId"))).execute().use { r ->
                    if (!r.isSuccessful) { ok = false; return@use }
                    val body = r.body ?: run { ok = false; return@use }
                    JsonReader(body.charStream()).use { jr ->
                        jr.isLenient = true
                        if (jr.peek() != JsonToken.BEGIN_OBJECT) { ok = false; return@use }
                        jr.beginObject()
                        while (jr.hasNext()) {
                            if (jr.nextName() != "episodes") { jr.skipValue(); continue }
                            when (jr.peek()) {
                                JsonToken.BEGIN_OBJECT -> {
                                    jr.beginObject()
                                    while (jr.hasNext()) {
                                        val season = jr.nextName().trim().toIntOrNull()
                                        epList(jr, season, sink)
                                    }
                                    jr.endObject()
                                }
                                JsonToken.BEGIN_ARRAY -> epList(jr, null, sink)
                                else -> jr.skipValue()
                            }
                        }
                        jr.endObject()
                    }
                }
                return ok
            } catch (e: IOException) {
                reset()
                if (attempt == 1) return false
            } catch (e: Exception) {
                return false
            }
        }
        return false
    }

    private fun epList(jr: JsonReader, season: Int?, sink: (Map<String, String>, Int?) -> Unit) {
        if (jr.peek() != JsonToken.BEGIN_ARRAY) { jr.skipValue(); return }
        jr.beginArray()
        while (jr.hasNext()) {
            when (jr.peek()) {
                JsonToken.BEGIN_OBJECT -> sink(flat(jr), season)
                // Some providers nest one level deeper
                JsonToken.BEGIN_ARRAY -> epList(jr, season, sink)
                else -> jr.skipValue()
            }
        }
        jr.endArray()
    }

    /** Episodes of a series as finished lines; empty on error. */
    private fun episodes(a: XtreamLogic.Acct, s: XtreamLogic.Series): String {
        val sb = StringBuilder()
        val ok = walkEpisodes(a, s.id, { f, season -> XtreamLogic.episodeEntry(a, s, f, season)?.let { sb.append(it).append('\n') } }, { sb.setLength(0) })
        return if (ok) sb.toString() else ""
    }

    /**
     * Load episodes of a series on opening (see [LazySeries]). null = provider does not answer; empty list = series without episodes.
     * Several entries with the same season/episode (duplicates at the provider) are merged, the first address counts.
     */
    fun episodeList(a: XtreamLogic.Acct, seriesId: String): List<Episode>? {
        val seen = HashSet<Long>()
        var out = ArrayList<Episode>()
        val ok = walkEpisodes(a, seriesId, { f, season ->
            XtreamLogic.episodeParts(a, f, season)?.let { (se, n, url) ->
                if (seen.add((se.toLong() shl 32) or n.toLong())) out.add(Episode(se, n, EpTitles.of(n), url, emptyList(), null))
            }
        }, { seen.clear(); out = ArrayList() })
        if (!ok) return null
        out.sortWith(compareBy({ it.season }, { it.num }))
        return out
    }

    /**
     * Writes the whole list to [out]. Return value: number of entries (channels + movies + series).
     * For series only a placeholder arrives; [LazySeries] fetches the episodes only when a series is opened.
     * Movies, series and live streams are fetched one after the other. Some providers reject parallel
     * requests from the same client (they answer with their own error codes such as 884/885), so
     * sequential loading avoids that.
     */
    fun write(a: XtreamLogic.Acct, out: Writer): Int {
        onPhase("Zugang wird geprüft …")
        verify(a)
        var n = 0
        out.write(XtreamLogic.header(a)); out.write("\n")

        onPhase("Filme werden geladen …")
        val vodCats = categories(a, "get_vod_categories")
        var movies = 0
        val vodList = stream(XtreamLogic.api(a, "get_vod_streams")) { f ->
            XtreamLogic.movieEntry(a, f, vodCats)?.let { out.write(it); out.write("\n"); n++; movies++ }
        }
        // The provider does list film categories but delivers no film list: only the categories were answered.
        // Loading would end with a list without any film – the previous (complete) list must stay instead.
        if (movies == 0 && (vodCats.isNotEmpty() || !vodList)) {
            throw PlaylistError(
                "Der Anbieter hat die Filmliste nicht geliefert. Die bisherige Liste bleibt erhalten – bitte später erneut versuchen.",
                incomplete = true
            )
        }

        onPhase("Serien werden geladen …")
        val serCats = categories(a, "get_series_categories")
        val series = ArrayList<XtreamLogic.Series>()
        val serList = stream(XtreamLogic.api(a, "get_series")) { f -> XtreamLogic.seriesOf(f, serCats)?.let { series.add(it) } }
        if (series.isEmpty() && (serCats.isNotEmpty() || !serList)) {
            throw PlaylistError(
                "Der Anbieter hat die Serienliste nicht geliefert. Die bisherige Liste bleibt erhalten – bitte später erneut versuchen.",
                incomplete = true
            )
        }
        for (s in series) { out.write(XtreamLogic.lazyEntry(a, s)); out.write("\n"); n++ }

        onPhase("Sender werden geladen …")
        val liveCats = categories(a, "get_live_categories")
        var live = 0
        val liveList = stream(XtreamLogic.api(a, "get_live_streams")) { f ->
            XtreamLogic.liveEntry(a, f, liveCats)?.let { out.write(it); out.write("\n"); n++; live++ }
        }
        if (live == 0 && (liveCats.isNotEmpty() || !liveList)) {
            throw PlaylistError(
                "Der Anbieter hat die Senderliste nicht geliefert. Die bisherige Liste bleibt erhalten – bitte später erneut versuchen.",
                incomplete = true
            )
        }
        check()
        return n
    }

}

/** Counts the characters written (about the bytes of the mostly ASCII list) and reports the growth every 64 K and on flush. */
private class TempCounter(w: Writer, private val onGrow: (Long) -> Unit) : java.io.FilterWriter(w) {
    private var n = 0L
    private var last = 0L
    override fun write(c: Int) { out.write(c); add(1) }
    override fun write(cbuf: CharArray, off: Int, len: Int) { out.write(cbuf, off, len); add(len.toLong()) }
    override fun write(str: String, off: Int, len: Int) { out.write(str, off, len); add(len.toLong()) }
    private fun add(k: Long) { n += k; if (n - last >= 65_536) report() }
    private fun report() { if (n > last) { onGrow(n - last); last = n } }
    override fun flush() { report(); super.flush() }
}
