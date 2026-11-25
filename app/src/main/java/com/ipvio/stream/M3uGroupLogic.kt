package com.ipvio.stream

/**
 * #145: the category (group) of an M3U entry (pure logic, no Android).
 *
 * Most lists use `group-title="Name"`. Some providers write the key in capitals, use single quotes, call it `tvg-group`,
 * or put the name on its own `#EXTGRP:Name` line. Before, all of those landed in "Sonstige". The name is returned exactly
 * as written (no trimming): category names are keys for favorites, hidden categories and the saved order.
 */
object M3uGroupLogic {
    private val KEYS = listOf("group-title", "tvg-group")

    /** The group from an #EXTINF line, or null if it names none. */
    fun fromInfo(line: String): String? {
        for (key in KEYS) {
            // The usual spelling first (fast path), then the other spellings
            for (ignoreCase in booleanArrayOf(false, true)) {
                for (quote in charArrayOf('"', '\'')) {
                    val tag = "$key=$quote"
                    val at = line.indexOf(tag, ignoreCase = ignoreCase)
                    if (at < 0) continue
                    val from = at + tag.length
                    val end = line.indexOf(quote, from)
                    if (end < 0) continue
                    val v = line.substring(from, end)
                    if (v.isNotBlank()) return v
                }
            }
        }
        return null
    }

    /** The name of an `#EXTGRP:Name` line, or null if the line is not one or has no name. */
    fun fromExtGrp(line: String): String? =
        if (line.startsWith("#EXTGRP:", ignoreCase = true)) line.substring(8).trim().takeIf { it.isNotEmpty() } else null
}
