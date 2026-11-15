package com.ipvio.stream

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object PortalManager {
    private const val PREFS = "ipvio_portal"
    private const val KEY_PORTAL_URL = "portal_url"
    private const val KEY_LAST_PLAYLIST_UPDATE = "last_pl_update"

    var isBlocked = false
        private set

    var lastSyncStatus = ""
        private set

    var onPlaylistLoaded: (() -> Unit)? = null

    var currentPlayingTitle: String? = null
        private set
    var currentPlayingType: String? = null
        private set

    fun setNowPlaying(ctx: Context, title: String?, type: String?) {
        currentPlayingTitle = title
        currentPlayingType = type
        val url = getPortalUrl(ctx)
        if (url.isBlank()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val devId = DeviceIdentity.getDeviceId(ctx)
                val json = JSONObject().apply {
                    put("deviceId", devId)
                    put("title", title ?: JSONObject.NULL)
                    put("type", type ?: JSONObject.NULL)
                }
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val req = Request.Builder()
                    .url("$url/api/device/now-playing")
                    .post(body)
                    .build()
                client.newCall(req).execute().close()
            } catch (_: Exception) {}
        }
    }

    fun getAccountKey(ctx: Context): String {
        val url = PlaylistStore.savedUrl(ctx) ?: return ""
        return try {
            val u = android.net.Uri.parse(url)
            val host = u.host.orEmpty()
            val user = u.getQueryParameter("username") ?: ""
            if (host.isNotBlank() && user.isNotBlank()) "$host:$user" else url.take(80)
        } catch (_: Exception) {
            url.take(80)
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var job: Job? = null

    fun getPortalUrl(ctx: Context): String {
        val stored = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PORTAL_URL, "")?.trim().orEmpty()
        return if (stored.isNotBlank()) stored else BuildConfig.PORTAL_URL
    }

    fun setPortalUrl(ctx: Context, url: String) {
        val clean = url.trim().removeSuffix("/")
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PORTAL_URL, clean)
            .apply()
        start(ctx.applicationContext)
    }

    fun syncNow(ctx: Context, onResult: (Boolean, String) -> Unit) {
        val url = getPortalUrl(ctx)
        if (url.isBlank()) {
            onResult(false, Lang.tr("URL do Portal não configurada."))
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            val ok = registerDevice(ctx, url)
            withContext(Dispatchers.Main) {
                if (ok) {
                    lastSyncStatus = "Conectado"
                    onResult(true, Lang.tr("Conectado ao portal com sucesso!"))
                } else {
                    lastSyncStatus = "Erro de conexão"
                    onResult(false, Lang.tr("Não foi possível conectar ao portal."))
                }
            }
        }
    }

    fun start(ctx: Context) {
        job?.cancel()
        val url = getPortalUrl(ctx)
        if (url.isBlank()) return

        job = CoroutineScope(Dispatchers.IO).launch {
            if (registerDevice(ctx, url)) {
                pullSharedProgress(ctx)
            }

            while (isActive) {
                val delayTime = if (PlaylistStore.savedUrl(ctx) == null) 4_000L else 45_000L
                delay(delayTime)
                sendHeartbeat(ctx, url)
            }
        }
    }

    private suspend fun registerDevice(ctx: Context, baseUrl: String): Boolean {
        return try {
            val devId = DeviceIdentity.getDeviceId(ctx)
            val mac = DeviceIdentity.getVirtualMac(ctx)
            val model = DeviceIdentity.getDeviceModel()
            val verName = AppUpdate.currentName(ctx)
            val verCode = AppUpdate.currentCode(ctx)
            val pairingCode = DeviceIdentity.getPairingCode(ctx)

            val json = JSONObject().apply {
                put("deviceId", devId)
                put("mac", mac)
                put("pairingCode", pairingCode)
                put("model", model)
                put("appVersion", verName)
                put("versionCode", verCode)
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("$baseUrl/api/device/register")
                .post(body)
                .build()

            val res = client.newCall(req).execute()
            if (res.isSuccessful) {
                val str = res.body?.string().orEmpty()
                handleServerResponse(ctx, baseUrl, JSONObject(str))
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun sendHeartbeat(ctx: Context, baseUrl: String): Boolean {
        return try {
            val devId = DeviceIdentity.getDeviceId(ctx)
            val verCode = AppUpdate.currentCode(ctx)
            val pairingCode = DeviceIdentity.getPairingCode(ctx)

            val json = JSONObject().apply {
                put("deviceId", devId)
                put("pairingCode", pairingCode)
                put("versionCode", verCode)
                put("nowPlaying", currentPlayingTitle ?: JSONObject.NULL)
                put("nowPlayingType", currentPlayingType ?: JSONObject.NULL)
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("$baseUrl/api/device/heartbeat")
                .post(body)
                .build()

            val res = client.newCall(req).execute()
            if (res.isSuccessful) {
                val str = res.body?.string().orEmpty()
                handleServerResponse(ctx, baseUrl, JSONObject(str))
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun handleServerResponse(ctx: Context, baseUrl: String, json: JSONObject) {
        val status = json.optString("status", "active")
        isBlocked = (status == "blocked")

        // 1. Mensagem na tela (Push)
        val msgObj = json.optJSONObject("message")
        if (msgObj != null) {
            val msgId = msgObj.optString("id")
            val msgText = msgObj.optString("text")
            if (msgText.isNotBlank()) {
                mainHandler.post {
                    Toast.makeText(ctx, "📢 $msgText", Toast.LENGTH_LONG).show()
                }
                ackMessage(baseUrl, DeviceIdentity.getDeviceId(ctx), msgId)
            }
        }

        // 2. Playlist remota
        val plObj = json.optJSONObject("playlist")
        if (plObj != null) {
            val updatedAt = plObj.optLong("updatedAt", 0L)
            val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val lastUpdate = sp.getLong(KEY_LAST_PLAYLIST_UPDATE, 0L)

            if (updatedAt > 0 && updatedAt > lastUpdate) {
                val type = plObj.optString("type")
                var targetUrl: String? = null

                if (type == "xtream") {
                    val host = plObj.optString("host")
                    val user = plObj.optString("user")
                    val pass = plObj.optString("pass")
                    targetUrl = PlaylistStore.xtreamUrl(host, user, pass)
                } else if (type == "m3u") {
                    targetUrl = plObj.optString("url")
                }

                if (!targetUrl.isNullOrBlank()) {
                    val ok = withContext(Dispatchers.IO) {
                        PlaylistStore.load(ctx, targetUrl, save = true, ask = false)
                    }
                    if (ok) {
                        sp.edit().putLong(KEY_LAST_PLAYLIST_UPDATE, updatedAt).apply()
                        mainHandler.post {
                            Toast.makeText(ctx, Lang.tr("Playlist sincronizada remotamente pelo portal!"), Toast.LENGTH_LONG).show()
                            onPlaylistLoaded?.invoke()
                        }
                    }
                }
            }
        }

        // 3. Atualização OTA pelo Portal Remoto
        val otaObj = json.optJSONObject("ota")
        if (otaObj != null) {
            val hasUpdate = otaObj.optBoolean("hasUpdate", false)
            val vCode = otaObj.optLong("versionCode", 0L)
            val vName = otaObj.optString("versionName", "")
            var apkUrl = otaObj.optString("url", "")
            val changelog = otaObj.optString("changelog", "")
            val mandatory = otaObj.optBoolean("mandatory", false)
            val currentCode = AppUpdate.currentCode(ctx)

            if (apkUrl.isNotBlank() && (hasUpdate || vCode > currentCode)) {
                if (apkUrl.startsWith("/")) {
                    apkUrl = "$baseUrl$apkUrl"
                } else if (apkUrl.contains("localhost")) {
                    val baseUri = android.net.Uri.parse(baseUrl)
                    val baseHost = baseUri.host ?: "192.168.1.100"
                    val baseScheme = baseUri.scheme ?: "http"
                    val basePort = if (baseUri.port > 0) ":${baseUri.port}" else ""
                    apkUrl = apkUrl.replace(Regex("https?://localhost(:\\d+)?"), "$baseScheme://$baseHost$basePort")
                }

                val upd = UpdateInfo(
                    code = vCode,
                    name = vName,
                    apkUrl = apkUrl,
                    notes = changelog,
                    mandatory = mandatory
                )
                withContext(Dispatchers.Main) {
                    AppUpdate.promptDismissed = false
                    AppUpdate.setFromPortal(upd)
                }
            }
        }
    }

    fun backupToPortal(ctx: Context, onResult: (Boolean, String) -> Unit) {
        val url = getPortalUrl(ctx)
        if (url.isBlank()) {
            onResult(false, Lang.tr("URL do Portal não configurada."))
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val backupText = Backup.exportText(ctx, withPlaylists = true)
                if (backupText == null) {
                    withContext(Dispatchers.Main) { onResult(false, Lang.tr("Falha ao gerar arquivo de backup.")) }
                    return@launch
                }
                val sum = Backup.summary(backupText)
                val sumJson = JSONObject().apply {
                    put("profiles", sum?.profiles ?: 1)
                    put("favs", sum?.favorites ?: 0)
                    put("cats", sum?.categories?.size ?: 0)
                    put("date", sum?.at ?: System.currentTimeMillis())
                }

                val devId = DeviceIdentity.getDeviceId(ctx)
                val accKey = getAccountKey(ctx)

                val payload = JSONObject().apply {
                    put("deviceId", devId)
                    put("accountKey", accKey)
                    put("backupData", backupText)
                    put("summary", sumJson)
                }

                val body = payload.toString().toRequestBody("application/json".toMediaType())
                val req = Request.Builder()
                    .url("$url/api/device/backup")
                    .post(body)
                    .build()

                val res = client.newCall(req).execute()
                val ok = res.isSuccessful
                res.close()
                withContext(Dispatchers.Main) {
                    if (ok) onResult(true, Lang.tr("Backup salvo no portal com sucesso!"))
                    else onResult(false, Lang.tr("Erro ao salvar backup no portal."))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.message ?: "Erro de conexão") }
            }
        }
    }

    fun restoreFromPortal(ctx: Context, onResult: (Boolean, String) -> Unit) {
        val url = getPortalUrl(ctx)
        if (url.isBlank()) {
            onResult(false, Lang.tr("URL do Portal não configurada."))
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val devId = DeviceIdentity.getDeviceId(ctx)
                val accKey = getAccountKey(ctx)
                val req = Request.Builder()
                    .url("$url/api/device/backup?deviceId=$devId&accountKey=$accKey")
                    .get()
                    .build()

                val res = client.newCall(req).execute()
                if (!res.isSuccessful) {
                    res.close()
                    withContext(Dispatchers.Main) { onResult(false, Lang.tr("Nenhum backup encontrado no portal.")) }
                    return@launch
                }

                val str = res.body?.string().orEmpty()
                val json = JSONObject(str)
                val backupData = json.optString("backupData")
                if (backupData.isBlank()) {
                    withContext(Dispatchers.Main) { onResult(false, Lang.tr("Arquivo de backup vazio.")) }
                    return@launch
                }

                val count = Backup.restoreText(ctx, backupData)
                withContext(Dispatchers.Main) {
                    if (count >= 0) onResult(true, Lang.tr("Backup restaurado do portal com sucesso! Reinicie o app."))
                    else onResult(false, Lang.tr("Falha ao restaurar o backup."))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, e.message ?: "Erro ao restaurar") }
            }
        }
    }

    fun syncEpProgress(ctx: Context, seriesId: String, season: Int, ep: Int, posMs: Long, durMs: Long) {
        val accKey = getAccountKey(ctx)
        val url = getPortalUrl(ctx)
        if (accKey.isBlank() || url.isBlank() || durMs <= 0) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val itemObj = JSONObject().apply {
                    put("itemId", seriesId)
                    put("syncType", "ep_progress")
                    put("data", JSONObject().apply {
                        put("season", season)
                        put("ep", ep)
                        put("pos", posMs)
                        put("dur", durMs)
                    })
                    put("updatedAt", System.currentTimeMillis())
                }
                val arr = org.json.JSONArray().put(itemObj)
                val payload = JSONObject().apply {
                    put("accountKey", accKey)
                    put("items", arr)
                }
                val body = payload.toString().toRequestBody("application/json".toMediaType())
                val req = Request.Builder().url("$url/api/device/sync/progress").post(body).build()
                client.newCall(req).execute().close()
            } catch (_: Exception) {}
        }
    }

    fun pullSharedProgress(ctx: Context) {
        val accKey = getAccountKey(ctx)
        val url = getPortalUrl(ctx)
        if (accKey.isBlank() || url.isBlank()) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val req = Request.Builder()
                    .url("$url/api/device/sync/progress?accountKey=$accKey")
                    .get()
                    .build()
                val res = client.newCall(req).execute()
                if (res.isSuccessful) {
                    val str = res.body?.string().orEmpty()
                    val json = JSONObject(str)
                    val items = json.optJSONArray("items") ?: return@launch
                    for (i in 0 until items.length()) {
                        val it = items.getJSONObject(i)
                        val syncType = it.optString("syncType")
                        val itemId = it.optString("itemId")
                        val data = it.optJSONObject("data") ?: continue

                        if (syncType == "ep_progress") {
                            val season = data.optInt("season", 0)
                            val ep = data.optInt("ep", 0)
                            val pos = data.optLong("pos", 0L)
                            val dur = data.optLong("dur", 0L)
                            if (season > 0 && ep > 0 && dur > 0) {
                                EpProgress.put(ctx, itemId, season, ep, pos, dur)
                            }
                        }
                    }
                }
                res.close()
            } catch (_: Exception) {}
        }
    }

    private fun ackMessage(baseUrl: String, deviceId: String, messageId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val json = JSONObject().apply {
                put("deviceId", deviceId)
                put("messageId", messageId)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("$baseUrl/api/device/message/ack")
                .post(body)
                .build()
            runCatching { client.newCall(req).execute() }
        }
    }
}
