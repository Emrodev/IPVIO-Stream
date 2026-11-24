package com.ipvio.stream

import java.net.URI
import java.util.Locale

/**
 * T-052a: HLS playlists for the downloads (pure logic, no Android imports).
 * A master playlist lists the quality variants (#EXT-X-STREAM-INF); a media playlist has the segments and no variants.
 */
object HlsLogic {
    /** One quality of a master playlist: label for the user, bandwidth in bit/s, absolute address of its media playlist. */
    class Variant(val label: String, val bandwidth: Long, val uri: String)

    /** True when the address is an HLS playlist. */
    fun isHls(url: String): Boolean = url.substringBefore('?').lowercase().endsWith(".m3u8")

    /** Variants of a master playlist, in the order of the file. Empty for a media playlist or broken text. */
    fun variants(text: String, baseUrl: String): List<Variant> {
        val out = ArrayList<Variant>()
        var pending: Pair<Long, String?>? = null
        for (raw in text.lines()) {
            val l = raw.trim()
            if (l.startsWith("#EXT-X-STREAM-INF")) {
                val bw = Regex("BANDWIDTH=(\\d+)").find(l)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
                val res = Regex("RESOLUTION=(\\d+x\\d+)").find(l)?.groupValues?.get(1)
                pending = bw to res
            } else if (pending != null && l.isNotEmpty() && !l.startsWith("#")) {
                val (bw, res) = pending
                out.add(Variant(label(res, bw), bw, resolve(baseUrl, l)))
                pending = null
            }
        }
        return out
    }

    /** "1080p · 5.0 Mbit/s", or only the bitrate when the resolution is missing. */
    fun label(resolution: String?, bandwidth: Long): String {
        val mbit = String.format(Locale.ROOT, "%.1f Mbit/s", bandwidth / 1_000_000.0)
        val height = resolution?.substringAfter('x')?.toIntOrNull()
        return if (height != null) "${height}p · $mbit" else mbit
    }

    /** Absolute address of [ref] relative to [base]; the original text when it cannot be resolved. */
    fun resolve(base: String, ref: String): String =
        runCatching { URI(base).resolve(ref).toString() }.getOrDefault(ref)
}
