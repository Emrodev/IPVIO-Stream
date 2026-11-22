package com.ipvio.stream

/**
 * T-052a: downloads of movies (pure rules, no Android imports).
 * Step 1 handles progressive files (mp4, mkv, ts ...). HLS (.m3u8) and DASH (.mpd) follow in a later step.
 */
object DownloadLogic {
    /** Free space that must be left on the storage before a download may start. A floor, not a size estimate. */
    const val MIN_FREE_BYTES = 2L * 1024 * 1024 * 1024

    /** Id of a download: one per movie, stable across playlist refreshes. */
    fun keyFor(item: Item): String = "vod:" + item.id

    /** True when the address is a single file that can be downloaded in step 1. */
    fun supported(url: String): Boolean {
        val u = url.substringBefore('?').lowercase()
        return u.startsWith("http") && !u.endsWith(".m3u8") && !u.endsWith(".mpd")
    }

    /** True when enough space is free to start a download. */
    fun hasRoom(freeBytes: Long): Boolean = freeBytes >= MIN_FREE_BYTES
}
