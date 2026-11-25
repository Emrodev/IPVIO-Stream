package com.ipvio.stream

/**
 * T-062: rewinding a live catch-up channel (pure logic, no Android).
 *
 * The archive link of a show is built from the start and end of that show (see [CatchupLogic]), and many servers take the
 * start only to the minute. So "back N seconds" plays the show that contains the target time from its start and then
 * jumps to the offset inside it, instead of asking the server for an arbitrary start time.
 */
object CatchupSeekLogic {
    /** The [show] that contains the target time and the offset of the target in it. */
    class Pick(val show: EpgEntry, val offsetSec: Long)

    /** Target time in seconds: [stepSec] before [nowSec]. */
    fun target(nowSec: Long, stepSec: Long): Long = nowSec - stepSec.coerceAtLeast(1L)

    /**
     * The show of [list] that is on air at [target] (seconds), or null if the schedule has none.
     * If shows overlap, the one that started last wins.
     */
    fun pick(list: List<EpgEntry>, target: Long): Pick? {
        val e = list.filter { it.end > it.start && target >= it.start && target < it.end }.maxByOrNull { it.start } ?: return null
        return Pick(e, target - e.start)
    }
}
