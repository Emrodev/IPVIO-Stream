package com.ipvio.stream

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Device sync over Google Drive (W-010, based on the Drive backup of 1.1). Optional.
 *
 * Sign-in like on other TV apps: the TV shows a code, the user confirms it on the phone (google.com/device);
 * this also works on Fire TV without Google services. Storage is the hidden app folder of Drive ("appDataFolder"):
 * the app only sees its own files, nothing else in the user's Drive. Every device writes only its own file
 * (see [DriveLogic.fileName]), so no device overwrites another one. Restoring is always a manual step in the UI.
 *
 * Needs an own OAuth client (type "TVs and Limited Input devices") in secrets.properties, see docs/GOOGLE_DRIVE.md.
 * Without it the section stays hidden.
 */
object DriveSync {
    private const val SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    private const val PREFS = "ipvio_drive"
    private const val FILES = "https://www.googleapis.com/drive/v3/files"
    private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3/files"
    private val client = Net.withTimeouts(15, 60)
    private val JSON = "application/json; charset=UTF-8".toMediaType()

    val clientId: String get() = BuildConfig.GOOGLE_CLIENT_ID.trim()
    private val secret: String get() = BuildConfig.GOOGLE_CLIENT_SECRET.trim()

    fun available(): Boolean = clientId.isNotEmpty() && secret.isNotEmpty()

    var connected by mutableStateOf(false)
        private set
    /** Time of this device's last upload (ms), 0 = never. */
    var lastBackup by mutableLongStateOf(0L)
        private set

    private var access: String? = null
    private var accessUntil = 0L

    /** Why a Drive call failed; the UI shows a translated text for it. */
    enum class Reason { NOT_CONNECTED, NETWORK, GOOGLE, NOT_FOUND, TOO_BIG }

    class DriveException(val reason: Reason, val code: Int = 0) : Exception("Drive: $reason $code")

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun init(c: Context) {
        connected = refreshToken(c) != null
        lastBackup = prefs(c).getLong("last", 0L)
    }

    private fun refreshToken(c: Context): String? = prefs(c).getString("rt", null)?.let { Secrets.decrypt(it) }

    class DeviceCode(val device: String, val user: String, val url: String, val interval: Int, val expiresAt: Long)

    private fun post(url: String, form: Map<String, String>): JSONObject? {
        val b = FormBody.Builder()
        for ((k, v) in form) b.add(k, v)
        return client.newCall(Request.Builder().url(url).post(b.build()).build()).execute().use { r ->
            runCatching { JSONObject(r.body?.string().orEmpty()) }.getOrNull()
        }
    }

    private fun <T> net(block: () -> T): T = try {
        block()
    } catch (e: DriveException) {
        throw e
    } catch (e: java.io.IOException) {
        throw DriveException(Reason.NETWORK)
    }

    /** Step 1: get the code the user enters on the phone at the shown address. */
    suspend fun start(): Result<DeviceCode> = withContext(Dispatchers.IO) {
        runCatching {
            net {
                val o = post("https://oauth2.googleapis.com/device/code", mapOf("client_id" to clientId, "scope" to SCOPE))
                    ?: throw DriveException(Reason.GOOGLE)
                val user = o.optString("user_code").ifBlank { throw DriveException(Reason.GOOGLE) }
                DeviceCode(
                    o.optString("device_code"), user, o.optString("verification_url").ifBlank { "https://www.google.com/device" },
                    o.optInt("interval", 5).coerceIn(2, 30), System.currentTimeMillis() + o.optLong("expires_in", 900) * 1000
                )
            }
        }
    }

    enum class SignIn { CONNECTED, DENIED, EXPIRED, FAILED }

    /** Step 2: wait until the user has agreed on the phone. Cancelling the coroutine stops the waiting. */
    suspend fun poll(c: Context, d: DeviceCode): SignIn = withContext(Dispatchers.IO) {
        var wait = d.interval * 1000L
        while (System.currentTimeMillis() < d.expiresAt) {
            delay(wait)
            val o = runCatching {
                post("https://oauth2.googleapis.com/token", mapOf(
                    "client_id" to clientId, "client_secret" to secret, "device_code" to d.device,
                    "grant_type" to "urn:ietf:params:oauth:grant-type:device_code"
                ))
            }.getOrNull() ?: continue
            val err = o.optString("error")
            if (err.isEmpty()) {
                val rt = o.optString("refresh_token").ifBlank { return@withContext SignIn.FAILED }
                val enc = Secrets.encrypt(rt) ?: return@withContext SignIn.FAILED
                prefs(c).edit().putString("rt", enc).apply()
                access = o.optString("access_token")
                accessUntil = System.currentTimeMillis() + (o.optLong("expires_in", 3600) - 120) * 1000
                connected = true
                return@withContext SignIn.CONNECTED
            }
            when (DriveLogic.pollAnswer(err)) {
                DriveLogic.Poll.WAIT -> {}
                DriveLogic.Poll.SLOWER -> wait += 5000
                DriveLogic.Poll.DENIED -> return@withContext SignIn.DENIED
                DriveLogic.Poll.EXPIRED -> return@withContext SignIn.EXPIRED
                DriveLogic.Poll.FAILED -> return@withContext SignIn.FAILED
            }
        }
        SignIn.EXPIRED
    }

    private fun token(c: Context): String {
        access?.let { if (System.currentTimeMillis() < accessUntil) return it }
        val rt = refreshToken(c) ?: throw DriveException(Reason.NOT_CONNECTED)
        val o = post("https://oauth2.googleapis.com/token", mapOf(
            "client_id" to clientId, "client_secret" to secret, "refresh_token" to rt, "grant_type" to "refresh_token"
        )) ?: throw DriveException(Reason.GOOGLE)
        // Access was withdrawn in the Google account: forget the sign-in
        if (o.optString("error") == "invalid_grant") { disconnect(c); throw DriveException(Reason.NOT_CONNECTED) }
        val t = o.optString("access_token").ifBlank { throw DriveException(Reason.GOOGLE) }
        access = t
        accessUntil = System.currentTimeMillis() + (o.optLong("expires_in", 3600) - 120) * 1000
        return t
    }

    private fun get(t: String, url: String): String = client.newCall(
        Request.Builder().url(url).header("Authorization", "Bearer $t").build()
    ).execute().use { r ->
        if (r.code == 404) throw DriveException(Reason.NOT_FOUND)
        if (!r.isSuccessful) throw DriveException(Reason.GOOGLE, r.code)
        r.body?.string().orEmpty()
    }

    /** A sync file in Drive. [deviceId] null = old single backup file of 1.1. */
    class Remote(val id: String, val deviceId: String?, val deviceName: String?, val modified: Long, val size: Long) {
        fun isThisDevice(c: Context) = deviceId != null && deviceId == Backup.deviceId(c)
    }

    /** All sync files, newest first. */
    suspend fun list(c: Context): Result<List<Remote>> = withContext(Dispatchers.IO) {
        runCatching {
            net {
                val t = token(c)
                val q = URLEncoder.encode(DriveLogic.listQuery(), "UTF-8")
                val body = get(t, "$FILES?spaces=appDataFolder&q=$q&orderBy=modifiedTime%20desc&pageSize=50" +
                    "&fields=files(id,name,modifiedTime,size,appProperties)")
                val a = JSONObject(body).optJSONArray("files") ?: JSONArray()
                (0 until a.length()).mapNotNull { i ->
                    val f = a.optJSONObject(i) ?: return@mapNotNull null
                    val name = f.optString("name")
                    if (!DriveLogic.isSyncFile(name)) return@mapNotNull null
                    val props = f.optJSONObject("appProperties")
                    Remote(
                        f.optString("id"), DriveLogic.deviceIdOf(name), props?.optString("deviceName")?.ifBlank { null },
                        DriveLogic.parseTime(f.optString("modifiedTime")), f.optString("size").toLongOrNull() ?: 0L
                    )
                }
            }
        }
    }

    /** Uploads this device's sync file (replaces only this device's previous upload). */
    suspend fun upload(c: Context, text: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            net {
                val t = token(c)
                val devId = Backup.deviceId(c)
                val name = DriveLogic.fileName(devId)
                val q = URLEncoder.encode("name = '$name'", "UTF-8")
                val existing = JSONObject(get(t, "$FILES?spaces=appDataFolder&q=$q&fields=files(id)"))
                    .optJSONArray("files")?.optJSONObject(0)?.optString("id")?.ifBlank { null }
                val props = JSONObject().put("deviceId", devId).put("deviceName", Backup.deviceName())
                val meta = JSONObject().put("appProperties", props)
                if (existing == null) meta.put("name", name).put("parents", JSONArray().put("appDataFolder"))
                val body = MultipartBody.Builder().setType("multipart/related".toMediaType())
                    .addPart(meta.toString().toRequestBody(JSON))
                    .addPart(text.toRequestBody(JSON))
                    .build()
                val req = Request.Builder().header("Authorization", "Bearer $t").let {
                    if (existing != null) it.url("$UPLOAD/$existing?uploadType=multipart").patch(body)
                    else it.url("$UPLOAD?uploadType=multipart").post(body)
                }.build()
                client.newCall(req).execute().use { r -> if (!r.isSuccessful) throw DriveException(Reason.GOOGLE, r.code) }
                lastBackup = System.currentTimeMillis()
                prefs(c).edit().putLong("last", lastBackup).apply()
            }
        }
    }

    /** Downloads one sync file as text. */
    suspend fun download(c: Context, r: Remote): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            net {
                if (r.size > 4L * 1024 * 1024) throw DriveException(Reason.TOO_BIG)
                val text = get(token(c), "$FILES/${URLEncoder.encode(r.id, "UTF-8")}?alt=media")
                if (text.length > 4 * 1024 * 1024) throw DriveException(Reason.TOO_BIG)
                text
            }
        }
    }

    /** Sync passphrase, remembered on this device (encrypted with the device key; not stored without a keystore). */
    fun passphrase(c: Context): String? = prefs(c).getString("pp", null)?.let { Secrets.decrypt(it) }

    fun savePassphrase(c: Context, p: String) {
        if (!SyncLogic.validPassphrase(p)) return
        val enc = Secrets.encrypt(p) ?: return
        prefs(c).edit().putString("pp", enc).apply()
    }

    /** Forgets the sign-in and the remembered passphrase on this device (the files in Drive stay). */
    fun disconnect(c: Context) {
        prefs(c).edit().remove("rt").remove("pp").apply()
        access = null
        accessUntil = 0L
        connected = false
    }
}
