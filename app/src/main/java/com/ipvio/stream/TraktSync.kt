package com.ipvio.stream

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Trakt sync, read-only (T-031 steps 4 and 5). Takes the watched movies and episodes and the watchlist from the connected
 * Trakt account into the app:
 * - categories "Trakt · Gesehen" and "Trakt · Merkliste" in Movies and Series ([ImportedLists], replaced on every sync);
 * - watched marks in the episode lists and on movies ([EpProgress]), added only, never removed or lowered.
 * Nothing is written to Trakt. Runs after the sign-in, at app start at most every 6 hours, and on "Sync now".
 * Tokens never go into logs; the diagnostics line has numbers only.
 */
object TraktSync {
    enum class Result { OK, NOT_CONNECTED, NO_PLAYLIST, SIGN_IN_AGAIN, NETWORK }

    private const val PREFS = "ipvio_trakt"
    private const val LAST = "lastSync"
    private const val SUMMARY = "lastSummary"
    private val JSON = "application/json".toMediaType()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** True while a sync runs (the settings page shows it). */
    var running by mutableStateOf(false)
        private set
    /** Short result of the last sync for the settings page ("" = none yet). */
    var status by mutableStateOf("")
        private set

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun lastSyncMs(c: Context): Long = prefs(c).getLong(LAST, 0L)
    fun lastSummary(c: Context): String = prefs(c).getString(SUMMARY, "") ?: ""

    /** App start: sync when connected, a playlist is loaded and the last sync is older than 6 hours. */
    fun autoSync(c: Context) {
        val app = c.applicationContext
        if (running || !TraktAuth.isConnected(app)) return
        if (!TraktSyncLogic.autoDue(System.currentTimeMillis(), lastSyncMs(app))) return
        if (PlaylistStore.channels.isEmpty()) return
        syncAsync(app)
    }

    /** "Sync now" and right after the sign-in. */
    fun syncAsync(c: Context) {
        val app = c.applicationContext
        if (running) return
        running = true
        scope.launch {
            val r = try { run(app) } catch (e: Exception) { Result.NETWORK } finally { running = false }
            status = when (r) {
                Result.OK -> lastSummary(app)
                Result.NOT_CONNECTED -> "Nicht mit Trakt verbunden."
                Result.NO_PLAYLIST -> "Erst eine Playlist laden, dann synchronisieren."
                Result.SIGN_IN_AGAIN -> "Trakt hat die Anmeldung abgelehnt. Bitte neu verbinden."
                Result.NETWORK -> "Trakt ist gerade nicht erreichbar. Später erneut versuchen."
            }
        }
    }

    /** Disconnect: ends the sign-in at Trakt (best effort), deletes the tokens and the Trakt categories. Marks stay. */
    fun disconnect(c: Context) {
        val app = c.applicationContext
        val t = TraktAuth.load(app)
        TraktAuth.signOut(app)
        for (st in ListImportLogic.Status.values()) ImportedLists.set(app, "Trakt", st, emptyList())
        prefs(app).edit().remove(LAST).remove(SUMMARY).apply()
        status = ""
        if (t != null) scope.launch {
            runCatching {
                val body = JSONObject().put("token", t.access).put("client_id", BuildConfig.TRAKT_CLIENT_ID.trim()).toString()
                Net.base.newCall(Request.Builder().url(TraktSyncLogic.API_URL + "/oauth/revoke").post(body.toRequestBody(JSON)).build()).execute().close()
            }
        }
    }

    private suspend fun run(c: Context): Result {
        var tokens = TraktAuth.load(c) ?: return Result.NOT_CONNECTED
        val clientId = BuildConfig.TRAKT_CLIENT_ID.trim()
        if (!TraktLogic.hasClientId(clientId)) return Result.NOT_CONNECTED
        if (!TraktLogic.isAccessValid(System.currentTimeMillis(), tokens.expiresAtMs)) {
            tokens = refresh(c, tokens, clientId) ?: return Result.SIGN_IN_AGAIN
        }
        val items = PlaylistStore.channels
        if (items.isEmpty()) return Result.NO_PLAYLIST

        // On 401 the token is renewed once and the list read again
        var t = tokens
        fun read(path: (Int) -> String): String? {
            val all = org.json.JSONArray()
            var page = 1
            while (true) {
                val r = get(path(page), clientId, t.access) ?: return null
                if (r.code == 401) return null
                val arr = runCatching { org.json.JSONArray(r.body) }.getOrNull() ?: return null
                for (i in 0 until arr.length()) all.put(arr.get(i))
                if (!TraktSyncLogic.hasNextPage(page, r.pageCount, arr.length())) break
                page++
            }
            return all.toString()
        }
        fun readAll(): List<String>? {
            val out = ArrayList<String>(4)
            for (p in listOf<(Int) -> String>(
                { TraktSyncLogic.watchedPath(false, it) }, { TraktSyncLogic.watchedPath(true, it) },
                { TraktSyncLogic.watchlistPath(false, it) }, { TraktSyncLogic.watchlistPath(true, it) }
            )) out.add(read(p) ?: return null)
            return out
        }
        var lists = readAll()
        if (lists == null) {
            // Either the network failed or the token was refused: renew once and try again
            t = refresh(c, t, clientId) ?: return if (lastRefused) Result.SIGN_IN_AGAIN else Result.NETWORK
            lists = readAll() ?: return Result.NETWORK
        }

        val movies = TraktSyncLogic.parseWatchedMovies(lists[0])
        val shows = TraktSyncLogic.parseWatchedShows(lists[1])
        val watchlist = TraktSyncLogic.parseWatchlist(lists[2], false) + TraktSyncLogic.parseWatchlist(lists[3], true)
        val plan = TraktSyncLogic.plan(
            movies, shows, watchlist, items,
            existing = { id -> EpProgress.of(c, id).keys.takeIf { it.isNotEmpty() } },
            stored = EpProgress.count(c),
            capacity = EpProgress.capacity(),
        )
        withContext(Dispatchers.Main) {
            ImportedLists.set(c, "Trakt", ListImportLogic.Status.WATCHED, plan.watchedIds)
            ImportedLists.set(c, "Trakt", ListImportLogic.Status.WATCHLIST, plan.watchlistIds)
            for ((id, keys) in plan.marks) EpProgress.addWatched(c, id, keys)
        }
        val summary = "${plan.watchedIds.size} gesehen · ${plan.watchlistIds.size} auf der Merkliste"
        prefs(c).edit().putLong(LAST, System.currentTimeMillis()).putString(SUMMARY, summary).apply()
        Diag.note("Trakt sync: ${plan.read} titles read, ${plan.watchedIds.size} watched and ${plan.watchlistIds.size} on the watchlist found in the playlist, ${plan.marks.size} titles got watched marks")
        return Result.OK
    }

    private class Resp(val code: Int, val body: String, val pageCount: Int?)

    private fun get(path: String, clientId: String, access: String): Resp? = try {
        val b = Request.Builder().url(TraktSyncLogic.API_URL + path)
        TraktSyncLogic.headers(clientId, access, "IPVIO Stream/" + BuildConfig.VERSION_NAME).forEach { (k, v) -> b.header(k, v) }
        Net.base.newCall(b.get().build()).execute().use { r ->
            Resp(r.code, r.body?.string().orEmpty(), r.header("X-Pagination-Page-Count")?.trim()?.toIntOrNull())
        }
    } catch (e: Exception) {
        null
    }

    /** True when the last refresh was refused by Trakt (not a network error): the user must sign in again. */
    @Volatile private var lastRefused = false

    private fun refresh(c: Context, t: TraktLogic.Tokens, clientId: String): TraktLogic.Tokens? {
        lastRefused = false
        val body = TraktSyncLogic.refreshBody(t.refresh, clientId, TraktLogic.REDIRECT_URI).toRequestBody(JSON)
        val raw = try {
            Net.base.newCall(Request.Builder().url(TraktLogic.TOKEN_URL).header("User-Agent", "IPVIO Stream/" + BuildConfig.VERSION_NAME).post(body).build()).execute().use { r ->
                if (!r.isSuccessful) { lastRefused = r.code in 400..499; return null }
                r.body?.string().orEmpty()
            }
        } catch (e: Exception) {
            return null
        }
        val o = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        val n = TraktLogic.tokensFrom(o.optString("access_token"), o.optString("refresh_token"), o.optLong("expires_in", 0L), System.currentTimeMillis())
            ?: return null
        return if (TraktAuth.save(c, n)) n else null
    }
}
