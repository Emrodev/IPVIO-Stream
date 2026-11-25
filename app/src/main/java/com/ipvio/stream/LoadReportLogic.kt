package com.ipvio.stream

/**
 * T-067: one line for Settings > Diagnostics after a playlist was parsed: how many entries the provider's list had and
 * where they went. Answers "the provider has more channels, movies or series than the app shows" without a logcat.
 * English only (the Diagnostics log is not translated), numbers and the host only: no address, no login.
 * Pure logic, no Android, testable.
 */
object LoadReportLogic {
    fun line(host: String, r: ParseReport, live: Int, movies: Int, series: Int, shown: Int, total: Int, hiddenLangs: Collection<String>): String {
        val sb = StringBuilder()
        sb.append(host).append(": list has ").append(r.entries).append(" entries → ")
            .append(live).append(" live, ").append(movies).append(" movies, ").append(series).append(" series")
        if (r.episodes > 0) sb.append(" (from ").append(r.episodes).append(" series entries)")
        val left = ArrayList<String>()
        if (r.separators > 0) left.add("${r.separators} divider lines")
        if (r.hiddenLang > 0) left.add("${r.hiddenLang} by the language filter (" + hiddenLangs.sorted().joinToString(",") + ")")
        if (r.hiddenMode > 0) left.add("${r.hiddenMode} by the content mode of the playlist")
        sb.append("; left out: ").append(if (left.isEmpty()) "none" else left.joinToString(", "))
        if (shown < total) sb.append("; hidden by parental control, ignored categories or broken titles: ").append(total - shown)
        return sb.toString()
    }
}
