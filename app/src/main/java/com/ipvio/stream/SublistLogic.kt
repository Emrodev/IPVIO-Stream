package com.ipvio.stream

import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Sublist filter rules for a playlist (without Android, testable).
 *
 * A sublist lets a user load only a subset of a big M3U playlist, so the app does not
 * have to keep thousands of unwanted titles in memory. The filter runs after parsing and
 * before the items are stored ([PlaylistStore.show]).
 *
 * All fields are optional; null means "no filter on this axis". A title passes the filter
 * only if it matches every non-null criterion.
 */
data class SublistFilter(
    /** Master switch. When false, the full playlist is loaded as before. */
    val enabled: Boolean = false,
    /** Keep items of this kind only (LIVE / MOVIE / SERIES). Null = all kinds. */
    val kind: Kind? = null,
    /** Groups required per kind, e.g. {LIVE: ["UK"], MOVIE: ["News"]}. Empty = no group filter for that kind. */
    val groupsByKind: Map<Kind, List<String>> = emptyMap(),
    /** Languages required per kind, e.g. {LIVE: ["en"], MOVIE: ["de"]}. Empty = no lang filter for that kind. */
    val langsByKind: Map<Kind, List<String>> = emptyMap(),
    /** Keep items with this quality ("HD", "FHD", "4K"). Null = any quality. */
    val quality: String? = null,
    /** true = keep only items with a logo, false = keep only items without a logo, null = any. */
    val hasLogo: Boolean? = null,
    /** true = keep only items with a tvg-id, false = keep only items without one, null = any. */
    val hasTvgId: Boolean? = null,
    /** Whether adult content may pass the filter. */
    val includeAdult: Boolean = true
) {
    /** Encode to a simple "key=value" text form (no org.json, so JVM tests work). */
    fun encode(): String =
        kv(
            "e" to enabled.toString(),
            "k" to kind?.name,
            "g" to if (groupsByKind.isEmpty()) null else groupsByKind.entries.joinToString("|") { "${it.key.name}:${it.value.joinToString(",")}" },
            "l" to if (langsByKind.isEmpty()) null else langsByKind.entries.joinToString("|") { "${it.key.name}:${it.value.joinToString(",")}" },
            "q" to quality,
            "h" to hasLogo?.toString(),
            "v" to hasTvgId?.toString(),
            "a" to includeAdult.toString()
        )

    companion object {
        fun decode(s: String?): SublistFilter {
            val o = unkv(s) ?: return SublistFilter()
            return SublistFilter(
                enabled = o["e"]?.toBoolean() ?: false,
                kind = o["k"]?.let { Kind.valueOf(it) },
                groupsByKind = decodeMap(o["g"]),
                langsByKind = decodeMap(o["l"]),
                quality = o["q"],
                hasLogo = o["h"]?.toBoolean(),
                hasTvgId = o["v"]?.toBoolean(),
                includeAdult = o["a"]?.toBoolean() ?: true
            )
        }

        private fun decodeMap(s: String?): Map<Kind, List<String>> {
            if (s.isNullOrBlank()) return emptyMap()
            return s.split('|').mapNotNull { part ->
                val i = part.indexOf(':')
                if (i <= 0) null else {
                    val k = Kind.valueOf(part.substring(0, i))
                    val vals = part.substring(i + 1).split(',').filter { it.isNotEmpty() }
                    k to vals
                }
            }.toMap()
        }
    }

    /** Does [item] pass every active criterion? */
    fun matches(item: Item): Boolean {
        if (!enabled) return true
        if (!includeAdult && ParentalLogic.isAdult(item.group, item.title)) return false
        if (kind != null && item.kind != kind) return false
        val groups = groupsByKind[item.kind]
        if (groups != null && groups.isNotEmpty() && !groups.any { item.group.contains(it, ignoreCase = true) }) return false
        val langs = langsByKind[item.kind]
        if (langs != null && langs.isNotEmpty() && !langs.contains(item.lang)) return false
        if (quality != null && item.quality != quality) return false
        if (hasLogo != null && (item.logo != null) != hasLogo) return false
        if (hasTvgId != null && (item.tvgId != null) != hasTvgId) return false
        return true
    }

    /** How many of [items] pass the filter (for a preview count). */
    fun countMatching(items: List<Item>): Int = items.count { matches(it) }
}

/** A named sublist saved for one playlist URL. */
data class SublistDef(
    val name: String,          // user-chosen name, e.g. "UK channels" or "4K movies"
    val filter: SublistFilter
) {
    fun encode(): String =
        kv("n" to name, "f" to filter.encode())

    companion object {
        fun decode(s: String?): SublistDef? {
            val o = unkv(s) ?: return null
            val n = o["n"] ?: return null
            return SublistDef(n, SublistFilter.decode(o["f"]))
        }
    }
}

/** Simple "key=value" text encoding (one per line, values URL-encoded). No org.json so JVM tests work. */
private fun kv(vararg p: Pair<String, String?>): String =
    p.filter { it.second != null }.joinToString("\n") { it.first + "=" + URLEncoder.encode(it.second, "UTF-8") }

private fun unkv(s: String?): Map<String, String>? {
    if (s.isNullOrBlank()) return null
    return try {
        s.split('\n').mapNotNull { l ->
            val i = l.indexOf('=')
            if (i <= 0) null else l.substring(0, i) to URLDecoder.decode(l.substring(i + 1), "UTF-8")
        }.toMap()
    } catch (e: Exception) { null }
}
