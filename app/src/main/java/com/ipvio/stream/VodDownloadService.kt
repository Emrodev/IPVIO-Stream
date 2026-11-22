@file:OptIn(androidx.media3.common.util.UnstableApi::class)

package com.ipvio.stream

import android.app.Notification
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler

/** T-052a: runs the movie downloads in the foreground (notification while downloading). Declared in AndroidManifest.xml. */
class VodDownloadService : DownloadService(
    FOREGROUND_ID, FOREGROUND_UPDATE_MS, CHANNEL_ID, R.string.vod_download_channel, R.string.vod_download_channel
) {
    override fun getDownloadManager(): DownloadManager = DownloadStore.manager(this)

    /** No automatic restart by the system: downloads are started and stopped by the user. */
    override fun getScheduler(): Scheduler? = null

    override fun getForegroundNotification(downloads: MutableList<Download>, notMetRequirements: Int): Notification =
        DownloadNotificationHelper(this, CHANNEL_ID)
            .buildProgressNotification(this, android.R.drawable.stat_sys_download, null, null, downloads, notMetRequirements)

    companion object {
        const val CHANNEL_ID = "vod_downloads"
        const val FOREGROUND_ID = 7201
        const val FOREGROUND_UPDATE_MS = 1000L
    }
}
