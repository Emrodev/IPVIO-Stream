package com.ipvio.stream

/**
 * T-067: matching playlist channels with the channels of an XMLTV guide when the tvg-id is missing or not exactly the
 * guide's ID. Before, only an exact tvg-id gave a channel its program; a list without tvg-ids got nothing from XMLTV at all,
 * and an ID like "DasErste.de" next to the guide's "daserste.de@HD" stayed empty.
 *
 * Order: exact tvg-id (unchanged) → the same tvg-id after [normId] → the channel name after [normName] against the guide's
 * display names. A name used by several guide channels is only taken when the guide ID's country ending fits the channel's
 * language; otherwise the channel gets no program (better none than a wrong one).
 * Pure logic, no Android, testable.
 */
object EpgMatchLogic {
    private val MARKS = Regex("""\p{Mn}+""")
    // Language or country tag at the start: "DE: ", "DE | ", "[DE] ", "|DE| ", "DE - "
    private val LEAD = Regex("""^\W*[A-Za-z]{2,4}\s*(?:[\])|:]|\s[-–])\s*""")
    // Quality and technical words that are not part of the channel name
    private val TECH = Regex("""\b(?:uhd|fhd|hd|sd|4k|8k|hevc|h\.?26[45]|1080[pi]?|720p?|2160p?|50fps|60fps|raw|backup|vip|multi)\b""")
    private val NON_ALNUM = Regex("""[^\p{L}\p{N}+]+""")
    // Ending of a guide ID: country ".de", ".uk", a variant "@HD", or both
    private val ID_END = Regex("""(?:@[^.]*)?(?:\.[a-z]{2,3})?(?:@[^.]*)?$""")

    /** Comparison form of a channel name: no language tag, no quality words, no accents, no punctuation ("+1" stays). */
    fun normName(name: String?): String {
        if (name.isNullOrBlank()) return ""
        var t = name.trim()
        val m = LEAD.find(t)
        if (m != null && LangTagLogic.detect(m.value) != null && m.range.last + 1 < t.length) t = t.substring(m.range.last + 1)
        t = java.text.Normalizer.normalize(t.lowercase(), java.text.Normalizer.Form.NFD).replace(MARKS, "")
            .replace("&", " and ").replace("ß", "ss")
        t = t.replace(TECH, " ")
        return t.replace(NON_ALNUM, "").trim()
    }

    /** Comparison form of a tvg-id or guide ID: lower case, without country ending and variant ("DasErste.de@HD" → "daserste"). */
    fun normId(id: String?): String {
        if (id.isNullOrBlank()) return ""
        val t = id.trim().lowercase()
        val cut = t.replace(ID_END, "").ifEmpty { t }
        return cut.replace(NON_ALNUM, "")
    }

    /** Country ending of a guide ID ("ard.de" → "de", "bbcone.uk" → "uk"), null = none. */
    fun idCountry(id: String): String? {
        val t = id.lowercase().substringBefore('@')
        val dot = t.lastIndexOf('.')
        if (dot < 0 || dot == t.length - 1) return null
        val c = t.substring(dot + 1)
        return if (c.length in 2..3 && c.all { it in 'a'..'z' }) c else null
    }

    /** Does a guide ID's country fit the channel language from [LangTagLogic] ("uk"/"us"/"gb" for English)? */
    fun countryFits(country: String?, lang: String?): Boolean {
        if (country == null || lang == null) return false
        if (country == lang) return true
        return when (lang) {
            "en" -> country in setOf("uk", "us", "gb", "ie", "ca", "au")
            "de" -> country in setOf("at", "ch")
            "pt" -> country == "br"
            "es" -> country == "mx"
            else -> false
        }
    }

    /** A live channel of the playlist as far as matching needs it. */
    class Channel(val id: String, val name: String, val tvgId: String?, val lang: String?)

    /**
     * Result: [redirect] = tvg-id (lower case) that is not in the guide → guide ID to use for it;
     * [byItem] = channel ID without tvg-id → guide ID. Guide IDs in lower case.
     */
    class Result(val redirect: Map<String, String>, val byItem: Map<String, String>) {
        val targets: Set<String> get() = redirect.values.toSet() + byItem.values
    }

    /**
     * [guide] = guide ID → display names, as read from the `<channel>` elements of one XMLTV file.
     * [done] = tvg-ids (lower case) and channel IDs already resolved by an earlier guide file: left alone.
     */
    fun match(channels: List<Channel>, guide: Map<String, List<String>>, done: Set<String> = emptySet()): Result {
        if (guide.isEmpty()) return Result(emptyMap(), emptyMap())
        val ids = HashMap<String, String>(guide.size * 2)
        for (g in guide.keys) ids[g.lowercase()] = g.lowercase()
        val byNormId = HashMap<String, MutableSet<String>>()
        val byName = HashMap<String, MutableSet<String>>()
        for ((g, names) in guide) {
            val gl = g.lowercase()
            normId(gl).takeIf { it.isNotEmpty() }?.let { byNormId.getOrPut(it) { HashSet() }.add(gl) }
            for (n in names) normName(n).takeIf { it.isNotEmpty() }?.let { byName.getOrPut(it) { HashSet() }.add(gl) }
        }
        fun pick(cands: Set<String>?, lang: String?): String? = when {
            cands.isNullOrEmpty() -> null
            cands.size == 1 -> cands.first()
            else -> cands.filter { countryFits(idCountry(it), lang) }.singleOrNull()
        }
        val redirect = HashMap<String, String>()
        val byItem = HashMap<String, String>()
        for (ch in channels) {
            val tid = ch.tvgId?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
            if (tid != null) {
                if (tid in ids || tid in done || tid in redirect) continue
                val hit = pick(byNormId[normId(tid)], ch.lang) ?: pick(byName[normName(ch.name)], ch.lang) ?: continue
                redirect[tid] = hit
            } else {
                if (ch.id in done) continue
                val hit = pick(byName[normName(ch.name)], ch.lang) ?: continue
                byItem[ch.id] = hit
            }
        }
        return Result(redirect, byItem)
    }
}
