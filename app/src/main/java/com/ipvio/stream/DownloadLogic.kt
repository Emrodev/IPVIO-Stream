package com.ipvio.stream

/**
 * T-052a: downloads (pure rules, no Android imports).
 * Supported: single files (mp4, mkv, ts ...), HLS playlists (.m3u8, quality chosen by the user for movies, best quality for
 * episodes) and DASH manifests (.mpd, whole stream). Each profile has its own download folder.
 */
object DownloadLogic {
    /** Free space that must be left on the storage before a download may start. A floor, not a size estimate. */
    const val MIN_FREE_BYTES = 2L * 1024 * 1024 * 1024

    private const val DAY_MS = 24L * 60 * 60 * 1000

    /** Id of a movie download: one per movie, stable across playlist refreshes. */
    fun keyFor(item: Item): String = "vod:" + item.id

    /** Stand-in item for a series episode: its download id is built from the series and the episode number. */
    fun episodeItem(series: Item, season: Int, num: Int, title: String): Item =
        Item(series.id + ":S" + season + "E" + num, Kind.MOVIE, title, null, "", null, series.title)

    /** True when the address can be downloaded: a single file, an HLS or a DASH playlist. */
    fun supported(url: String): Boolean {
        val u = url.substringBefore('?').lowercase()
        return u.startsWith("http") || (u.isNotEmpty() && u.endsWith(".mpd"))
    }

    /** True when the address is a DASH manifest. */
    fun isDash(url: String): Boolean = url.substringBefore('?').lowercase().endsWith(".mpd")

    /** True when enough space is free to start a download. */
    fun hasRoom(freeBytes: Long): Boolean = freeBytes >= MIN_FREE_BYTES

    /** Folder name of the downloads of one profile: profile 0 uses the plain name (same rule as the other profile files). */
    fun folderName(scoped: String): String = scoped

    /** True when a download was last used more than [days] days ago (0 = automatic deletion is off). */
    fun expired(lastUsedMs: Long, nowMs: Long, days: Int): Boolean = days > 0 && nowMs - lastUsedMs > days * DAY_MS
}
