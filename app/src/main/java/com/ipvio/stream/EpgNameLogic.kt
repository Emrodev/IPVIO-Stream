package com.ipvio.stream

/**
 * Matching TV guide channels by name when the tvg-id is missing or differs (without Android, testable).
 * "DE: Das Erste HD", "Das Erste FHD" and the XMLTV display name "Das Erste" all get the key "daserste".
 */
object EpgNameLogic {
    /** Prefix of name keys in the guide store, so they never collide with a tvg-id. */
    const val PREFIX = "\u0001n:"

    /** Quality and technical tags that providers add to channel names; they say nothing about the channel. */
    private val TAGS = setOf(
        "hd", "fhd", "uhd", "sd", "hq", "lq", "4k", "8k", "fullhd", "hevc", "h265", "h264", "raw",
        "25fps", "50fps", "60fps", "fps", "1080p", "1080i", "720p", "2160p", "1080", "720", "2160", "backup", "vip"
    )

    // "|DE|", "[DE]", "(DE)" anywhere in the name (also at the end: "Das Erste (DE)")
    private val BRACKET_TAG = Regex("""[\[(|]\s*[A-Za-z]{2,4}\s*[\])|]""")

    /** Comparison key of a channel name; "" = nothing usable. */
    fun key(name: String?): String {
        if (name.isNullOrBlank()) return ""
        var t = MatchLogic.searchTitle(name)
        t = BRACKET_TAG.replace(t) { m -> if (LangTagLogic.detect(m.value.trim('[', ']', '(', ')', '|', ' ')) != null) " " else m.value }
        val words = MatchLogic.norm(t).split(' ').filter { it.isNotEmpty() && it !in TAGS }
        return words.joinToString("")
    }
}
