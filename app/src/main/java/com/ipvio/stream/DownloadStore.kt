@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.ipvio.stream

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
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

    /** Folder of the downloads (app-specific, no permission needed). */
    fun folder(c: Context): File = File(c.getExternalFilesDir(null) ?: c.filesDir, "downloads")

    /** Free bytes on the download storage. */
    fun freeBytes(c: Context): Long = runCatching { folder(c).apply { mkdirs() }.usableSpace }.getOrDefault(0L)

    @Synchronized
    fun manager(c: Context): DownloadManager {
        manager?.let { return it }
        val app = c.applicationContext
        val db = StandaloneDatabaseProvider(app)
        val dir = folder(app).apply { mkdirs() }
        val cache = SimpleCache(dir, NoOpCacheEvictor(), db)
        val http = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)
        val m = DownloadManager(app, db, cache, DefaultDataSource.Factory(app, http), Executors.newFixedThreadPool(2))
        // One movie at a time: a slow connection gets the full bandwidth and the progress stays readable
        m.setMaxParallelDownloads(1)
        m.addListener(object : DownloadManager.Listener {
            override fun onDownloadChanged(manager: DownloadManager, download: Download, finalException: Exception?) = update(download)
            override fun onDownloadRemoved(manager: DownloadManager, download: Download) { progress.remove(download.request.id) }
        })
        m.currentDownloads.forEach { update(it) }
        manager = m
        return m
    }

    private fun update(d: Download) {
        progress[d.request.id] = when (d.state) {
            Download.STATE_COMPLETED -> DONE
            Download.STATE_FAILED -> FAILED
            else -> d.percentDownloaded.toInt().coerceIn(0, 99)
        }
    }

    /** Starts the download of a movie. False = refused (not supported, or not enough free space). */
    fun enqueue(c: Context, item: Item, url: String): Boolean {
        if (!DownloadLogic.supported(url) || !DownloadLogic.hasRoom(freeBytes(c))) return false
        val id = DownloadLogic.keyFor(item)
        progress[id] = 0
        val request = DownloadRequest.Builder(id, Uri.parse(url)).setCustomCacheKey(id).build()
        DownloadService.sendAddDownload(c, VodDownloadService::class.java, request, true)
        return true
    }

    /** Removes a download and its file. */
    fun remove(c: Context, item: Item) {
        DownloadService.sendRemoveDownload(c, VodDownloadService::class.java, DownloadLogic.keyFor(item), false)
    }
}
