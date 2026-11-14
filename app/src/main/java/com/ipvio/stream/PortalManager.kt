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

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var job: Job? = null

    fun getPortalUrl(ctx: Context): String {
        val stored = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PORTAL_URL, "")?.trim().orEmpty()
        return if (stored.isNotBlank()) stored else "http://192.168.1.100:3000"
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
            registerDevice(ctx, url)

            while (isActive) {
                delay(60_000)
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

            val json = JSONObject().apply {
                put("deviceId", devId)
                put("mac", mac)
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

            val json = JSONObject().apply {
                put("deviceId", devId)
                put("versionCode", verCode)
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
                        }
                    }
                }
            }
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
