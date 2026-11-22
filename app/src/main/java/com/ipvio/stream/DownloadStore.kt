@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.ipvio.stream

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.media3.common.DataSpec
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Requirements
import java.io.File
import java.util.concurrent.Executors

/**
 * T-052a: the download engine (Media3 offline). One manager for the whole app, the files live in the app's own folder
 * on external storage. The service [VodDownloadService] runs the downloads in the foreground.
 * [progress]: per download id, percent 0..100, or [FAILED] / [DONE]. Compose reads it, so the buttons update.
 */
object DownloadStore {
    const val DONE = 100
    const val FAILED = -2

    /** Progress per download id (observable). */
    val progress = mutableStateMapOf<String, Int>()

    private var manager: DownloadManager? = null
    private var store: SimpleCache? = null
    /** Addresses of finished downloads: the player reads these from the cache (cache key = address). */
    private val doneUris: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()

    /** Folder of the downloads (app-specific, no permission needed). */
    fun folder(c: Context): File = File(c.getExternalFilesDir(null) ?: c.filesDir, "downloads")

    /** Setting "download only on Wi-Fi" (default off: the user decides). Stored in the download preferences. */
    fun wifiOnly(c: Context): Boolean =
        runCatching { c.applicationContext.getSharedPreferences("ipvio_vod", Context.MODE_PRIVATE).getBoolean("wifi_only", false) }.getOrDefault(false)

    /** Changes the setting; running downloads pause or continue at once. */
    fun setWifiOnly(c: Context, on: Boolean) {
        runCatching { c.applicationContext.getSharedPreferences("ipvio_vod", Context.MODE_PRIVATE).edit().putBoolean("wifi_only", on).apply() }
        manager(c).setRequirements(requirementsFor(on))
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
        // One movie at a time: a slow connection gets the full bandwidth and the progress stays readable
        m.setMaxParallelDownloads(1)
        m.setRequirements(requirementsFor(wifiOnly(app)))
        m.addListener(object : DownloadManager.Listener {
            override fun onDownloadChanged(manager: DownloadManager, download: Download, finalException: Exception?) = update(download)
            override fun onDownloadRemoved(manager: DownloadManager, download: Download) {
                progress.remove(download.request.id)
                doneUris.remove(download.request.uri.toString())
            }
        })
        m.currentDownloads.forEach { update(it) }
        manager = m
        return m
    }

    private fun update(d: Download) {
        val addr = d.request.uri.toString()
        if (d.state == Download.STATE_COMPLETED) doneUris.add(addr) else if (d.state != Download.STATE_DOWNLOADING) doneUris.remove(addr)
        progress[d.request.id] = when (d.state) {
            Download.STATE_COMPLETED -> DONE
            Download.STATE_FAILED -> FAILED
            else -> d.percentDownloaded.toInt().coerceIn(0, 99)
        }
    }

    /**
     * Starts the download of a movie. [variantUrl] = the chosen quality of an HLS master playlist (null = [url] itself).
     * False = refused (not supported, or not enough free space).
     */
    fun enqueue(c: Context, item: Item, url: String, variantUrl: String? = null): Boolean {
        val src = variantUrl ?: url
        if (!DownloadLogic.supported(src) || !DownloadLogic.hasRoom(freeBytes(c))) return false
        val id = DownloadLogic.keyFor(item)
        progress[id] = 0
        // No custom cache key: the cache key is the address, so the player finds the download by the address it plays
        // The title travels with the request (shown in the download list)
        val builder = DownloadRequest.Builder(id, Uri.parse(src)).setData(item.title.toByteArray(Charsets.UTF_8))
        if (HlsLogic.isHls(src)) builder.setMimeType(MimeTypes.APPLICATION_M3U8)
        val request = builder.build()
        DownloadService.sendAddDownload(c, VodDownloadService::class.java, request, true)
        return true
    }

    /** Address to play for [item] when it is downloaded (the chosen quality for HLS), otherwise null. */
    fun localUrlFor(c: Context, item: Item): String? {
        val id = DownloadLogic.keyFor(item)
        return manager(c).currentDownloads.firstOrNull { it.request.id == id && it.state == Download.STATE_COMPLETED }?.request?.uri?.toString()
    }

    /** One row of the download list: id, title, Media3 state, bytes on the storage. */
    class Entry(val id: String, val title: String, val state: Int, val bytes: Long)

    /** All downloads (finished, running, failed), for the download list. */
    fun entries(c: Context): List<Entry> = manager(c).currentDownloads.map { d ->
        Entry(d.request.id, runCatching { String(d.request.data ?: ByteArray(0), Charsets.UTF_8) }.getOrDefault(d.request.id), d.state, d.bytesDownloaded)
    }

    /** Removes a download by id and its file. */
    fun removeId(c: Context, id: String) {
        DownloadService.sendRemoveDownload(c, VodDownloadService::class.java, id, false)
    }

    /** Removes a download and its file. */
    fun remove(c: Context, item: Item) {
        DownloadService.sendRemoveDownload(c, VodDownloadService::class.java, DownloadLogic.keyFor(item), false)
    }

    /**
     * Data source factory for playback (T-052a): a finished download is read from the cache, every other address goes to
     * [upstream] exactly as before. The cache is read only, so streaming never fills it.
     */
    fun playbackFactory(c: Context, upstream: DataSource.Factory): DataSource.Factory {
        manager(c)
        val cacheFactory = CacheDataSource.Factory()
            .setCache(store!!)
            .setUpstreamDataSourceFactory(upstream)
            .setCacheWriteDataSinkFactory(null)
        return DataSource.Factory { DownloadAwareDataSource(cacheFactory.createDataSource(), upstream.createDataSource()) { isLocal(it) } }
    }

    /** Read from the cache: a finished download, or a segment or playlist that the cache already holds (HLS). */
    private fun isLocal(u: Uri): Boolean =
        doneUris.contains(u.toString()) || (store?.getCachedLength(u.toString(), 0, 1) ?: 0L) > 0L
}

/** Picks the cache or the network per opened address (T-052a). Keeps the choice until the address is closed. */
@androidx.media3.common.util.UnstableApi
class DownloadAwareDataSource(
    private val local: DataSource, private val remote: DataSource, private val isDone: (android.net.Uri) -> Boolean
) : DataSource {
    private var current: DataSource? = null

    override fun open(dataSpec: DataSpec): Long {
        val d = if (isDone(dataSpec.uri)) local else remote
        current = d
        return d.open(dataSpec)
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = current?.read(buffer, offset, length) ?: -1

    override fun getUri(): android.net.Uri? = current?.uri

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
