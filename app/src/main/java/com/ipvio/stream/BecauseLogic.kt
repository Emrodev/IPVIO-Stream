package com.ipvio.stream

/** "Because you watched" row on Home (T-053). Pure logic, no Android imports. */
object BecauseLogic {
    /** Similar titles in the own playlist needed before the row is shown (was 4 before T-053). */
    const val MIN_MATCHES = 2

    /** Number of the most recent started movies/series the row is built from (T-053: was 1). */
    const val SOURCES = 3

    fun show(matches: Int): Boolean = matches >= MIN_MATCHES
}
