package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Issue #10: Cluster scan and byte estimate for Matroska files without Cues. */
class MkvSeekLogicTest {
    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    /** Cluster with an 8-byte unknown size, Timecode 2-byte value 0x1F40 (8000 ms). */
    private val cluster = bytes(0x1F, 0x43, 0xB6, 0x75, 0x01, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xE7, 0x82, 0x1F, 0x40)

    @Test fun findsClusterAfterGarbage() {
        val buf = bytes(0x00, 0x1F, 0x43, 0x12) + cluster + bytes(0xA3, 0x00)
        val hit = MkvSeekLogic.findCluster(buf, buf.size)
        assertNotNull(hit)
        assertEquals(4, hit!!.offset)
        assertEquals(8000L, hit.timecode)
    }

    @Test fun oneByteSizeAndCrcBeforeTimecode() {
        // size 0x8A (1-byte vint), CRC-32 element, Timecode 1 byte = 5
        val buf = bytes(0x1F, 0x43, 0xB6, 0x75, 0x8A, 0xBF, 0x84, 1, 2, 3, 4, 0xE7, 0x81, 0x05)
        assertEquals(5L, MkvSeekLogic.findCluster(buf, buf.size)!!.timecode)
    }

    @Test fun rejectsIdWithoutTimecodeAndTruncatedCluster() {
        val noTc = bytes(0x1F, 0x43, 0xB6, 0x75, 0x8A, 0xA3, 0x81, 0x05)
        assertNull(MkvSeekLogic.findCluster(noTc, noTc.size))
        assertNull(MkvSeekLogic.findCluster(cluster, cluster.size - 1))
        // Invalid size vint (0x00) is not a cluster
        val badSize = bytes(0x1F, 0x43, 0xB6, 0x75, 0x00, 0xE7, 0x81, 0x05)
        assertNull(MkvSeekLogic.findCluster(badSize, badSize.size))
    }

    @Test fun estimateIsProportionalWithLead() {
        val d = 100_000_000L
        assertEquals(480_000L, MkvSeekLogic.estimate(50_000_000L, d, 1_000_000L))
        assertEquals(0L, MkvSeekLogic.estimate(1_000_000L, d, 1_000_000L))
        assertEquals(999_999L, MkvSeekLogic.estimate(d * 2, d, 1_000_000L))
        assertEquals(0L, MkvSeekLogic.estimate(5L, 0L, 1_000_000L))
    }

    @Test fun refineInterpolatesBetweenMeasuredPoints() {
        val d = 100_000_000L; val len = 1_000_000L
        // First half of the file is denser: 50 s already at byte 200,000
        val next = MkvSeekLogic.refine(62_000_000L, listOf(MkvSeekLogic.Point(200_000L, 50_000_000L)), d, len)
        // aim 60 s: 200,000 + 800,000 * (10 / 50) = 360,000
        assertEquals(360_000L, next)
        // Overshoot point after the target pulls the guess back
        val back = MkvSeekLogic.refine(32_000_000L, listOf(MkvSeekLogic.Point(500_000L, 70_000_000L)), d, len)
        assertEquals(500_000L * 30 / 70, back)
    }

    @Test fun repeatedOvershootClosesInFaster() {
        val d = 600_000_000L; val len = 2_700_000_000L
        // Target 180 s sits in a small low-bitrate start; every guess so far landed after 300 s
        val pts = listOf(
            MkvSeekLogic.Point(800_000_000L, 360_000_000L),
            MkvSeekLogic.Point(400_000_000L, 330_000_000L),
            MkvSeekLogic.Point(200_000_000L, 315_000_000L)
        )
        val plain = 200_000_000L * 178 / 315
        val next = MkvSeekLogic.refine(180_000_000L, pts, d, len)
        assertTrue("next=$next should be well below plain interpolation $plain", next < plain / 2)
        assertTrue(next > 0)
    }

    @Test fun closeEnoughWindow() {
        assertTrue(MkvSeekLogic.closeEnough(95_000_000L, 100_000_000L))
        assertTrue(MkvSeekLogic.closeEnough(101_000_000L, 100_000_000L))
        assertFalse(MkvSeekLogic.closeEnough(90_000_000L, 100_000_000L))
        assertFalse(MkvSeekLogic.closeEnough(102_000_000L, 100_000_000L))
    }
}
