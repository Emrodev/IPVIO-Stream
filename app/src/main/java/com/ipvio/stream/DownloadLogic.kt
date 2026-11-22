package com.ipvio.stream

/**
 * T-052a: downloads of movies (pure rules, no Android imports).
 * Progressive files (mp4, mkv, ts ...) and HLS playlists (.m3u8) are supported. DASH (.mpd) is not supported yet.
 */
object DownloadLogic {
    /** Free space that must be left on the storage before a download may start. A floor, not a size estimate. */
    const val MIN_FREE_BYTES = 2L * 1024 * 1024 * 1024

    /** Id of a download: one per movie, stable across playlist refreshes. */
    fun keyFor(item: Item): String = "vod:" + item.id

    /** True when the address can be downloaded: a single file or an HLS playlist. */
    fun supported(url: String): Boolean {
        val u = url.substringBefore('?').lowercase()
        return u.startsWith("http") && !u.endsWith(".mpd")
    }

    /** True when enough space is free to start a download. */
    fun hasRoom(freeBytes: Long): Boolean = freeBytes >= MIN_FREE_BYTES
}
