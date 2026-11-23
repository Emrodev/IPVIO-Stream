@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.ipvio.stream

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateMapOf
import androidx.media3.datasource.DataSpec
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Requirements
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * T-052a: the download engine (Media3 offline). One manager per profile (own folder), the files live in the app's own folder
 * on external storage. The service [VodDownloadService] runs the downloads in the foreground.
 * [progress]: per download id, percent 0..100, or [FAILED] / [DONE]. Compose reads it, so the buttons update.
 */
object DownloadStore {
    const val DONE = 100
    const val FAILED = -2

    private const val PREFS = "ipvio_vod"
    /** Last use is written at most once per hour per download (keeps the preferences quiet during playback). */
    private const val USE_STEP_MS = 60L * 60 * 1000

    /** Progress per download id (observable). */
    val progress = mutableStateMapOf<String, Int>()

    private var manager: DownloadManager? = null
    private var store: SimpleCache? = null
    /** Addresses of finished downloads (address → download id): the player reads these from the cache (cache key = address). */
    private val doneIds: MutableMap<String, String> = ConcurrentHashMap()
    private val bg = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Folder of the downloads of the active profile (app-specific, no permission needed). */
    fun folder(c: Context): File = File(c.getExternalFilesDir(null) ?: c.filesDir, DownloadLogic.folderName(Profiles.scopedName("downloads", Profiles.active)))

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Setting "download only on Wi-Fi" (default off: the user decides). */
    fun wifiOnly(c: Context): Boolean = runCatching { prefs(c).getBoolean("wifi_only", false) }.getOrDefault(false)

    /** Changes the setting; running downloads pause or continue at once. */
    fun setWifiOnly(c: Context, on: Boolean) {
        runCatching { prefs(c).edit().putBoolean("wifi_only", on).apply() }
        manager(c).setRequirements(requirementsFor(on))
    }

    /** Automatic deletion: days without use after which a finished download is removed (0 = off, the default). */
    fun autoDeleteDays(c: Context): Int = runCatching { prefs(c).getInt("auto_days", 0) }.getOrDefault(0)

    /** Changes the setting and checks the downloads at once. */
    fun setAutoDeleteDays(c: Context, days: Int) {
        runCatching { prefs(c).edit().putInt("auto_days", days).apply() }
        bg.launch { cleanup(c) }
    }

    /** Only unmetered networks (Wi-Fi, Ethernet) when [wifi] is set, otherwise any network. */
    private fun requirementsFor(wifi: Boolean): Requirements =
        Requirements(if (wifi) Requirements.NETWORK_UNMETERED else Requirements.NETWORK)

    /** Free bytes on the download storage. */
    fun freeBytes(c: Context): Long = runCatching { folder(c).apply { mkdirs() }.usableSpace }.getOrDefault(0L)

    @Synchronized
    fun manager(c: Context): DownloadManager {
        manager?.let { return it }
        val app = c.applicationContext
        val db = StandaloneDatabaseProvider(app)
        val dir = folder(app).apply { mkdirs() }
        val cache = SimpleCache(dir, NoOpCacheEvictor(), db)
        store = cache
        val http = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
        val m = DownloadManager(app, db, cache, DefaultDataSource.Factory(app, http), Executors.newFixedThreadPool(2))
        // One download at a time: a slow connection gets the full bandwidth and the progress stays readable
        m.setMaxParallelDownloads(1)
        m.setRequirements(requirementsFor(wifiOnly(app)))
        m.addListener(object : DownloadManager.Listener {
            override fun onDownloadChanged(manager: DownloadManager, download: Download, finalException: Exception?) = update(download)
            override fun onDownloadRemoved(manager: DownloadManager, download: Download) {
                progress.remove(download.request.id)
                doneIds.remove(download.request.uri.toString())
            }
        })
        m.currentDownloads.forEach { update(it) }
        manager = m
        bg.launch { cleanup(app) }
        return m
    }

    private fun update(d: Download) {
        val addr = d.request.uri.toString()
        if (d.state == Download.STATE_COMPLETED) doneIds[addr] = d.request.id
        else if (d.state != Download.STATE_DOWNLOADING) doneIds.remove(addr)
        progress[d.request.id] = when (d.state) {
            Download.STATE_COMPLETED -> DONE
            Download.STATE_FAILED -> FAILED
            else -> d.percentDownloaded.toInt().coerceIn(0, 99)
        }
    }

    /**
     * Starts the download of a movie or episode. [variantUrl] = the chosen quality of an HLS master playlist (null = [url] itself).
     * False = refused (not supported, or not enough free space).
     */
    fun enqueue(c: Context, item: Item, url: String, variantUrl: String? = null): Boolean {
        val src = variantUrl ?: url
        if (!DownloadLogic.supported(src) || !DownloadLogic.hasRoom(freeBytes(c))) return false
        val id = DownloadLogic.keyFor(item)
        progress[id] = 0
        // No custom cache key: the cache key is the address, so the player finds the download by the address it plays.
        // The title travels with the request (shown in the download list).
        val builder = DownloadRequest.Builder(id, Uri.parse(src)).setData(item.title.toByteArray(Charsets.UTF_8))
        if (HlsLogic.isHls(src)) builder.setMimeType(MimeTypes.APPLICATION_M3U8)
        if (DownloadLogic.isDash(src)) builder.setMimeType(MimeTypes.APPLICATION_MPD)
        DownloadService.sendAddDownload(c, VodDownloadService::class.java, builder.build(), true)
        return true
    }

    /**
     * Episodes (T-052a): one tap downloads the episode. An HLS master playlist is resolved to its best quality without a question.
     * Runs in the background; the start itself happens on the main thread (progress state).
     */
    fun startBest(c: Context, item: Item, url: String) {
        val app = c.applicationContext
        bg.launch {
            val best = if (HlsLogic.isHls(url)) {
                EpgNet.fetch(url)?.let { HlsLogic.variants(it, url) }.orEmpty().maxByOrNull { it.bandwidth }?.uri
            } else null
            Handler(Looper.getMainLooper()).post { enqueue(app, item, url, best) }
        }
    }

    /** Address to play for [item] when it is downloaded (the chosen quality for HLS), otherwise null. */
    fun localUrlFor(c: Context, item: Item): String? {
        val id = DownloadLogic.keyFor(item)
        return manager(c).currentDownloads.firstOrNull { it.request.id == id && it.state == Download.STATE_COMPLETED }?.request?.uri?.toString()
    }

    /** One row of the download list: id, title, Media3 state, bytes on the storage. */
    class Entry(val id: String, val title: String, val state: Int, val bytes: Long, val url: String)

    /** All downloads (finished, running, failed), for the download list. */
    fun entries(c: Context): List<Entry> = manager(c).currentDownloads.map { d ->
        Entry(d.request.id, runCatching { String(d.request.data ?: ByteArray(0), Charsets.UTF_8) }.getOrDefault(d.request.id), d.state, d.bytesDownloaded, d.request.uri.toString())
    }

    /** Stop reason used for a paused download (any value other than "none" keeps it stopped). */
    private const val PAUSE_REASON = 1

    /** Pauses a download; the file stays and the download can continue later (T-052a queue). */
    fun pause(c: Context, id: String) =
        DownloadService.sendSetStopReason(c, VodDownloadService::class.java, id, PAUSE_REASON, false)

    /** Continues a paused download. */
    fun resume(c: Context, id: String) =
        DownloadService.sendSetStopReason(c, VodDownloadService::class.java, id, Download.STOP_REASON_NONE, false)

    /** Starts a failed download again: removes it, then adds the same request again (after the removal has been processed). */
    fun retry(c: Context, id: String) {
        val request = manager(c).currentDownloads.firstOrNull { it.request.id == id }?.request ?: return
        DownloadService.sendRemoveDownload(c, VodDownloadService::class.java, id, false)
        Handler(Looper.getMainLooper()).postDelayed({
            DownloadService.sendAddDownload(c, VodDownloadService::class.java, request, true)
        }, 500L)
    }

    /** True when at least one download exists (the sidebar tab is shown only then). */
    fun hasAny(c: Context): Boolean {
        manager(c)
        return progress.isNotEmpty()
    }

    /** Removes a download by id and its file (and its use record). */
    fun removeId(c: Context, id: String) {
        runCatching { prefs(c).edit().remove("used_$id").apply() }
        DownloadService.sendRemoveDownload(c, VodDownloadService::class.java, id, false)
    }

    /** Removes a download and its file. */
    fun remove(c: Context, item: Item) = removeId(c, DownloadLogic.keyFor(item))

    /** Last use of a finished download in ms; a download never used yet counts from the first check (no instant deletion). */
    private fun lastUsed(c: Context, id: String, now: Long): Long {
        val p = prefs(c)
        val v = p.getLong("used_$id", 0L)
        if (v > 0L) return v
        p.edit().putLong("used_$id", now).apply()
        return now
    }

    /** Automatic deletion (T-052a): removes finished downloads that were not used for the chosen number of days. */
    fun cleanup(c: Context) {
        val days = autoDeleteDays(c)
        if (days <= 0) return
        val now = System.currentTimeMillis()
        manager(c).currentDownloads
            .filter { it.state == Download.STATE_COMPLETED }
            .filter { DownloadLogic.expired(lastUsed(c, it.request.id, now), now, days) }
            // Removing starts the service: that is done on the main thread, not from the background
            .forEach { d -> Handler(Looper.getMainLooper()).post { removeId(c, d.request.id) } }
    }

    /** Called when a finished download is played: keeps the "last used" date (at most once per hour). */
    private fun touch(id: String) {
        val c = appContext ?: return
        val p = prefs(c)
        val now = System.currentTimeMillis()
        if (now - p.getLong("used_$id", 0L) > USE_STEP_MS) p.edit().putLong("used_$id", now).apply()
    }

    @Volatile private var appContext: Context? = null

    /**
     * Data source factory for playback (T-052a): a finished download is read from the cache, every other address goes to
     * [upstream] exactly as before. The cache is read only, so streaming never fills it.
     */
    fun playbackFactory(c: Context, upstream: DataSource.Factory): DataSource.Factory {
        manager(c)
        appContext = c.applicationContext
        val cacheFactory = CacheDataSource.Factory()
            .setCache(store!!)
            .setUpstreamDataSourceFactory(upstream)
            .setCacheWriteDataSinkFactory(null)
        return DataSource.Factory { DownloadAwareDataSource(cacheFactory.createDataSource(), upstream.createDataSource()) { isLocal(it) } }
    }

    /** Read from the cache: a finished download, or a segment or playlist that the cache already holds (HLS). */
    private fun isLocal(u: Uri): Boolean {
        val id = doneIds[u.toString()]
        if (id != null) { touch(id); return true }
        return (store?.getCachedLength(u.toString(), 0, 1) ?: 0L) > 0L
    }
}

/** Picks the cache or the network per opened address (T-052a). Keeps the choice until the address is closed. */
@androidx.media3.common.util.UnstableApi
class DownloadAwareDataSource(
    private val local: DataSource, private val remote: DataSource, private val isDone: (Uri) -> Boolean
) : DataSource {
    private var current: DataSource? = null

    override fun open(dataSpec: DataSpec): Long {
        val d = if (isDone(dataSpec.uri)) local else remote
        current = d
        return d.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = current?.read(buffer, offset, length) ?: -1

    override fun getUri(): Uri? = current?.uri

    override fun getResponseHeaders(): Map<String, List<String>> = current?.responseHeaders ?: emptyMap()

    override fun close() {
        current?.close()
        current = null
    }

    override fun addTransferListener(transferListener: TransferListener) {
        local.addTransferListener(transferListener)
        remote.addTransferListener(transferListener)
    }
}
