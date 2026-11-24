package com.ipvio.stream

/**
 * Seeking in Matroska files without a Cues index (issue #10), pure part without Media3.
 * Media3 reports such files as unseekable, so every seek restarted at 0:00. [CuelessMkvExtractor] instead guesses a byte
 * position from the time, finds the next Cluster there and reads its timecode, and corrects the guess until it is close.
 */
object MkvSeekLogic {
    /** Cluster element ID. */
    private val CLUSTER = byteArrayOf(0x1F, 0x43, 0xB6.toByte(), 0x75)
    private const val TIMECODE_ID = 0xE7
    private const val CRC32_ID = 0xBF

    /** Close enough when the cluster starts at most this long before the target (the player decodes the rest forward). */
    const val BEFORE_US = 8_000_000L
    /** ... or at most this long after it. */
    const val AFTER_US = 1_500_000L
    /** Aim this far before the target, so the hit tends to land just before it. */
    const val LEAD_US = 2_000_000L
    const val MAX_HOPS = 10

    /** A Cluster found in a buffer: [offset] in the buffer, [timecode] in TimecodeScale units (normally ms). */
    data class Hit(val offset: Int, val timecode: Long)

    /** A measured point: Cluster at byte [pos] starts at [timeUs]. */
    data class Point(val pos: Long, val timeUs: Long)

    /**
     * Searches [buf] (valid bytes `0 until len`) from [from] for a Cluster whose first child is its Timecode
     * (an optional CRC-32 element before it is skipped). Returns null if none is complete within the buffer.
     */
    fun findCluster(buf: ByteArray, len: Int, from: Int = 0): Hit? {
        var i = from.coerceAtLeast(0)
        while (i + 4 <= len) {
            if (buf[i] == CLUSTER[0] && buf[i + 1] == CLUSTER[1] && buf[i + 2] == CLUSTER[2] && buf[i + 3] == CLUSTER[3]) {
                timecodeAfter(buf, len, i + 4)?.let { return Hit(i, it) }
            }
            i++
        }
        return null
    }

    /** Reads the Cluster size at [p] and the Timecode child after it; null if the bytes do not fit that layout. */
    private fun timecodeAfter(buf: ByteArray, len: Int, p0: Int): Long? {
        var p = p0
        val sizeLen = vintLength(buf, len, p) ?: return null
        p += sizeLen
        if (p >= len) return null
        if ((buf[p].toInt() and 0xFF) == CRC32_ID) {
            // CRC-32 element: ID, size byte 0x84, four bytes
            if (p + 6 > len || (buf[p + 1].toInt() and 0xFF) != 0x84) return null
            p += 6
        }
        if (p + 2 > len || (buf[p].toInt() and 0xFF) != TIMECODE_ID) return null
        // The size of the Timecode value is a one-byte vint 0x81..0x88 (the length is in the byte itself)
        val sb = buf[p + 1].toInt() and 0xFF
        if (sb !in 0x81..0x88) return null
        val n = sb and 0x7F
        p += 2
        if (p + n > len) return null
        var v = 0L
        for (k in 0 until n) v = (v shl 8) or (buf[p + k].toLong() and 0xFF)
        return v
    }

    /** Length of the EBML vint at [p] (1..8), or null for an invalid first byte or a vint that does not fit. */
    private fun vintLength(buf: ByteArray, len: Int, p: Int): Int? {
        if (p >= len) return null
        val b = buf[p].toInt() and 0xFF
        if (b == 0) return null
        val n = Integer.numberOfLeadingZeros(b) - 23
        return if (p + n <= len) n else null
    }

    /** First guess: byte position proportional to the time, aiming a little before the target. */
    fun estimate(targetUs: Long, durationUs: Long, length: Long): Long {
        if (durationUs <= 0 || length <= 0) return 0L
        val t = (targetUs - LEAD_US).coerceIn(0L, durationUs)
        return (length.toDouble() * t / durationUs).toLong().coerceIn(0L, length - 1)
    }

    /**
     * Next guess from the points measured so far: interpolates between the nearest known points before and after the
     * target (the file start and end count as points), so a changing bitrate is corrected with every hop.
     * When several guesses in a row land on the same side, the far end of the bracket would barely move (plain
     * interpolation then only creeps towards the target). Like the Illinois method, the stuck end then gets more
     * weight with each further hop, so the guesses close in quickly.
     */
    fun refine(targetUs: Long, points: List<Point>, durationUs: Long, length: Long): Long {
        val aim = (targetUs - LEAD_US).coerceAtLeast(0L)
        val last = (length - 1).coerceAtLeast(0L)
        val all = points + Point(0L, 0L) + Point(length, durationUs)
        val lo = all.filter { it.timeUs <= aim }.maxByOrNull { it.timeUs } ?: Point(0L, 0L)
        val hi = all.filter { it.timeUs > aim && it.pos > lo.pos }.minByOrNull { it.timeUs } ?: Point(length, durationUs)
        if (hi.timeUs <= lo.timeUs || hi.pos <= lo.pos) return lo.pos.coerceIn(0L, last)
        // Same side several times in a row: pull the guess towards the end that stays put (halved weight per extra hop)
        val over = points.lastOrNull()?.let { it.timeUs > aim } ?: false
        val streak = points.asReversed().takeWhile { (it.timeUs > aim) == over }.size
        val boost = if (streak >= 2) Math.pow(0.5, (streak - 1).toDouble()) else 1.0
        val fLo = (aim - lo.timeUs).toDouble()
        val fHi = (hi.timeUs - aim).toDouble()
        val wLo = if (over) fLo * boost else fLo
        val wHi = if (!over) fHi * boost else fHi
        val frac = wLo / (wLo + wHi)
        return (lo.pos + (hi.pos - lo.pos) * frac).toLong().coerceIn(0L, last)
    }

    /** True if a Cluster starting at [timeUs] is close enough to [targetUs]. */
    fun closeEnough(timeUs: Long, targetUs: Long): Boolean =
        timeUs in (targetUs - BEFORE_US)..(targetUs + AFTER_US)
}
