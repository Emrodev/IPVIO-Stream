package com.ipvio.stream

/**
 * T-068: may the current list stay on screen while the stored copy of another playlist is read? Pure logic, testable.
 *
 * The stored copy is a compact binary file; the items built from it take several times its size in memory (strings as
 * UTF-16, one object per item and episode). [FACTOR] is a cautious estimate (not measured on a device); the read itself
 * still stops when memory gets tight, so a wrong estimate costs only the old behaviour (loading screen), not a crash.
 */
object SwitchLogic {
    const val FACTOR = 5L
    const val RESERVE = 64L * 1024 * 1024

    /** [cacheBytes] = size of the stored copy (0 = none), [freeHeap] = heap that can still be used. */
    fun roomForBoth(cacheBytes: Long, freeHeap: Long): Boolean =
        cacheBytes > 0 && freeHeap > cacheBytes * FACTOR + RESERVE
}
