package com.ipvio.stream

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Downloads for Movies and Series (VOD).
 * Checks subscription validity before playback.
 */
object VodDownloader {
    private const val CHANNEL = "vod_downloads"
    private const val NOTIF_ID = 2026

    data class Task(
        val id: String,
        val itemId: String,
        val kind: Kind,
        val title: String,
        val subtitle: String?,
        val url: String,
        val logo: String?,
        val file: File,
        val totalBytes: Long = 0L,
        @Volatile var downloadedBytes: Long = 0L,
        @Volatile var isRunning: Boolean = true,
        @Volatile var isCompleted: Boolean = false,
        @Volatile var error: String? = null
    ) {
        internal var job: Job? = null
    }

    data class DownloadedItem(
        val file: File,
        val itemId: String,
        val kind: Kind,
        val title: String,
        val subtitle: String?,
        val logo: String?,
        val sizeBytes: Long,
        val downloadedAt: Long
    )

    val active = mutableStateListOf<Task>()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    @Volatile private var appCtx: Context? = null
    fun attach(c: Context) { appCtx = c.applicationContext }

    fun dirs(c: Context): List<File> {
        val ext = runCatching { c.getExternalFilesDirs("vod_downloads").filterNotNull() }.getOrDefault(emptyList())
        return ext.ifEmpty { listOf(File(c.filesDir, "vod_downloads")) }
    }

    fun dir(c: Context): File {
        val d = dirs(c)
        return (d.getOrNull(LiveSettings.recDir) ?: d.first()).also { it.mkdirs() }
    }

    private fun metaFile(f: File) = File(f.path.removeSuffix(".mp4") + ".json")

    fun isDownloaded(c: Context, itemId: String): Boolean {
        return list(c).any { it.itemId == itemId }
    }

    fun isDownloading(itemId: String): Boolean {
        return active.any { it.itemId == itemId && it.isRunning }
    }

    fun getTask(itemId: String): Task? = active.firstOrNull { it.itemId == itemId }

    /** Start downloading a movie or episode. */
    fun start(c: Context, item: Item, ep: Episode? = null, customUrl: String? = null): Task? {
        val targetId = if (ep != null) "${item.id}_S${ep.season}E${ep.num}" else item.id
        active.firstOrNull { it.id == targetId && it.isRunning }?.let { return it }

        val downloadUrl = customUrl ?: ep?.url ?: item.url
        if (downloadUrl.isBlank() || downloadUrl.startsWith("file://") || downloadUrl.startsWith("rec:")) return null

        val dir = dir(c)
        if (dir.usableSpace < 500L * 1024 * 1024) return null

        val ext = downloadUrl.substringAfterLast('.', "mp4").substringBefore('?').takeIf { it.length in 2..5 } ?: "mp4"
        val cleanTitle = (if (ep != null) "${item.title}_S${ep.season}E${ep.num}" else item.title).replace(Regex("""[^\w.-]"""), "_")
        val file = File(dir, "${cleanTitle}_${System.currentTimeMillis()}.$ext")

        val sub = if (ep != null) "S${ep.season} · E${ep.num} ${ep.title}" else (item.year?.toString())
        val task = Task(
            id = targetId,
            itemId = targetId,
            kind = item.kind,
            title = item.title,
            subtitle = sub,
            url = downloadUrl,
            logo = item.logo,
            file = file
        )

        active.add(task)
        VodDownloadService.update(c)

        task.job = CoroutineScope(Dispatchers.IO).launch {
            runDownload(c, task)
        }
        return task
    }

    private suspend fun runDownload(c: Context, task: Task) {
        var raf: RandomAccessFile? = null
        try {
            val req = Request.Builder().url(task.url).build()
            client.newCall(req).execute().use { res ->
                if (!res.isSuccessful) {
                    task.error = "HTTP ${res.code}"
                    return
                }
                val body = res.body ?: run {
                    task.error = "Empty body"
                    return
                }
                val len = body.contentLength()
                raf = RandomAccessFile(task.file, "rw")
                val buf = ByteArray(64 * 1024)
                val inStream = body.byteStream()
                while (task.isRunning) {
                    val read = inStream.read(buf)
                    if (read == -1) break
                    raf.write(buf, 0, read)
                    task.downloadedBytes += read
                }
                if (task.isRunning) {
                    task.isCompleted = true
                    writeMeta(task)
                }
            }
        } catch (e: Exception) {
            task.error = e.javaClass.simpleName
        } finally {
            runCatching { raf?.close() }
            task.isRunning = false
            done(c, task)
        }
    }

    fun stop(c: Context, task: Task) {
        task.isRunning = false
        task.job?.cancel()
        active.remove(task)
        runCatching { task.file.delete() }
        VodDownloadService.update(c)
    }

    private fun done(c: Context, task: Task) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            active.remove(task)
            VodDownloadService.update(c)
        }
    }

    private fun writeMeta(t: Task) {
        runCatching {
            val json = JSONObject().apply {
                put("id", t.id)
                put("kind", t.kind.name)
                put("title", t.title)
                put("subtitle", t.subtitle ?: "")
                put("logo", t.logo ?: "")
                put("size", t.file.length())
                put("time", System.currentTimeMillis())
            }
            metaFile(t.file).writeText(json.toString())
        }
    }

    fun list(c: Context): List<DownloadedItem> {
        val out = ArrayList<DownloadedItem>()
        for (d in dirs(c)) {
            val files = d.listFiles { f -> f.isFile && !f.name.endsWith(".json") && !f.name.endsWith(".part") } ?: continue
            for (f in files) {
                if (active.any { it.file == f && it.isRunning }) continue
                val mf = metaFile(f)
                val m = runCatching { JSONObject(mf.readText()) }.getOrNull()
                val id = m?.optString("id") ?: f.nameWithoutExtension
                val k = runCatching { Kind.valueOf(m?.optString("kind") ?: Kind.MOVIE.name) }.getOrDefault(Kind.MOVIE)
                val title = m?.optString("title") ?: f.nameWithoutExtension
                val sub = m?.optString("subtitle")?.ifBlank { null }
                val logo = m?.optString("logo")?.ifBlank { null }
                val time = m?.optLong("time") ?: f.lastModified()
                out.add(DownloadedItem(f, id, k, title, sub, logo, f.length(), time))
            }
        }
        return out.sortedByDescending { it.downloadedAt }
    }

    fun delete(c: Context, item: DownloadedItem) {
        runCatching { item.file.delete() }
        runCatching { metaFile(item.file).delete() }
    }

    fun deleteById(c: Context, itemId: String): Boolean {
        val found = list(c).firstOrNull { it.itemId == itemId } ?: return false
        delete(c, found)
        return true
    }

    /**
     * Checks if current subscription is valid.
     * Returns Pair(isValid, reasonMessage)
     */
    fun checkValidity(c: Context): Pair<Boolean, String> {
        if (PortalManager.isBlocked) {
            return false to "Dispositivo bloqueado no painel administrativo."
        }
        val exp = PortalManager.getExpDate(c)
        if (exp.isNullOrBlank()) {
            return true to "" // Sem data cadastrada, liberado
        }
        return try {
            val now = System.currentTimeMillis()
            // Formatos comuns: "YYYY-MM-DD", "DD/MM/YYYY", timestamp numérico em seg ou ms
            val expMs = when {
                exp.toLongOrNull() != null -> {
                    val n = exp.toLong()
                    if (n < 100_000_000_000L) n * 1000L else n
                }
                exp.contains("-") -> {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(exp.take(10))?.time ?: (now + 86400000)
                }
                exp.contains("/") -> {
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(exp.take(10))?.time ?: (now + 86400000)
                }
                else -> now + 86400000
            }

            if (now > expMs) {
                false to "Assinatura IPTV expirada. Renove seu plano para reproduzir este download."
            } else {
                true to ""
            }
        } catch (_: Exception) {
            true to ""
        }
    }

    fun toPlayItem(d: DownloadedItem): Item {
        val title = if (d.subtitle != null) "${d.title} - ${d.subtitle}" else d.title
        return Item(
            id = "down:" + d.file.name.hashCode(),
            kind = d.kind,
            title = title,
            year = null,
            url = fileUrl(d.file),
            logo = d.logo,
            group = "Arquivos Baixados"
        )
    }
}

class VodDownloadService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val running = VodDownloader.active.filter { it.isRunning }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel("vod_down", "Downloads", NotificationManager.IMPORTANCE_LOW))
        }
        if (running.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        val text = running.joinToString(" · ") { it.title }
        val b = if (Build.VERSION.SDK_INT >= 26) android.app.Notification.Builder(this, "vod_down") else @Suppress("DEPRECATION") android.app.Notification.Builder(this)
        val n = b.setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Baixando vídeos")
            .setContentText(text)
            .setOngoing(true)
            .build()
        startForeground(2027, n)
        return START_STICKY
    }

    companion object {
        fun update(c: Context) {
            val i = Intent(c, VodDownloadService::class.java)
            if (VodDownloader.active.any { it.isRunning }) {
                try {
                    if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i) else c.startService(i)
                } catch (_: Exception) {}
            } else {
                c.stopService(i)
            }
        }
    }
}
