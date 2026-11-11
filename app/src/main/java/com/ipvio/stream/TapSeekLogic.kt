package com.ipvio.stream

/** Touch seeking on the player timeline (issue #25): a tap or drag at a horizontal position jumps to that time. */
object TapSeekLogic {
    /**
     * Target time for a touch at [x] on a timeline [width] pixels wide, for a stream of [durMs].
     * -1 when there is nothing to seek (unknown duration, e.g. live). Stays one second before the end, like the D-pad seek.
     */
    fun target(x: Float, width: Int, durMs: Long): Long {
        if (durMs <= 0L || width <= 0) return -1L
        val fr = (x / width).coerceIn(0f, 1f)
        return (durMs * fr).toLong().coerceAtMost((durMs - 1000L).coerceAtLeast(0L))
    }
}
