package com.ipvio.stream

import java.net.URLEncoder

/**
 * Asking a Stalker portal for a play address ("create_link") and reading its answer. Without Android, testable.
 *
 * Portals differ here, mostly for movies and series:
 * - Some reject the parameters a MAG box sends ("forced_storage=undefined"), others need them. So for movies
 *   several request forms are tried in turn ([requests]).
 * - The address comes back in "cmd" ("ffmpeg http://…"), in "url", as a relative path, or only as a file id plus a
 *   play token that the app turns into ".../play/movie.php?..." itself ([address]).
 */
object StalkerLink {
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun api(endpoint: String, type: String, extra: String) =
        "$endpoint?type=$type&action=create_link$extra&JsHttpRequest=1-xml"

    /**
     * create_link requests to try in this order. Channels ("itv") keep the single MAG form, so zapping stays fast.
     * [series] = episode number if the command belongs to a whole season.
     */
    fun requests(endpoint: String, type: String, cmd: String, series: String? = null): List<String> {
        val c = enc(cmd.trim())
        val s = series?.let { enc(it) } ?: ""
        val withSeries = if (series != null) "&series=$s" else ""
        val out = LinkedHashSet<String>()
        // 1. As a MAG box sends it
        out.add(api(endpoint, type, "&cmd=$c&series=$s&forced_storage=undefined&disable_ad=0&download=0"))
        if (type != "vod") return out.toList()
        // 2. Empty storage instead of "undefined" (Ministra video club)
        out.add(api(endpoint, type, "&cmd=$c&series=$s&forced_storage=&disable_ad=0&download=0&force_ch_link_check=0"))
        // 3. Only the command (many Xtream-based portals)
        out.add(api(endpoint, type, "&cmd=$c$withSeries"))
        // 4. A bare file id: the form the video club uses itself
        val id = cmd.trim()
        if (id.isNotEmpty() && id.all(Char::isDigit)) out.add(api(endpoint, type, "&cmd=" + enc("/media/file_$id.mpg") + withSeries))
        return out.toList()
    }

    /** "ffmpeg http://…" / "auto http://…" → the address; null if there is no http(s) address. */
    fun strip(cmd: String?): String? {
        val c = cmd?.trim() ?: return null
        val i = c.indexOf("http://").let { if (it >= 0) it else c.indexOf("https://") }
        if (i < 0) return null
        return c.substring(i).trim().substringBefore(' ').takeIf { it.length > 8 }
    }

    /** "http://host:port" of the portal. */
    fun origin(portal: String): String? = Regex("""^(https?://[^/?#]+)""", RegexOption.IGNORE_CASE).find(portal.trim())?.value

    /**
     * Play address from the "js" part of a create_link answer ([f] = its fields; a bare string counts as "cmd").
     * null = the portal gave nothing playable.
     */
    fun address(portal: String, mac: String, f: Map<String, String>, type: String): String? {
        val host = origin(portal)
        for (k in listOf("url", "cmd")) {
            val v = f[k]?.trim()?.takeIf { it.isNotEmpty() && it != "null" } ?: continue
            strip(v)?.let { return it }
            // Relative path to the portal's own player ("/play/movie.php?…"); not an echo of the command ("/media/…")
            val p = v.substringAfterLast(' ')
            if (host != null && p.startsWith("/") && (p.contains("/play/") || p.contains("play_token="))) return host + p
        }
        // Only file id + play token: build the address of the portal's movie player
        if (type == "vod" && host != null) {
            val token = f["play_token"]?.trim().orEmpty()
            var id = f["id"]?.trim().orEmpty()
            if (token.isNotEmpty() && id.isNotEmpty() && id != "null" && id != "0") {
                if (!id.contains('.')) id += ".mp4"
                return "$host/play/movie.php?mac=${enc(mac)}&stream=${enc(id)}&play_token=${enc(token)}&type=movie"
            }
        }
        return null
    }
}
