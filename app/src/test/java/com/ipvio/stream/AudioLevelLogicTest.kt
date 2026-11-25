package com.ipvio.stream

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

class AudioLevelLogicTest {
    private val sr = 48_000

    /** Interleaved stereo; [l] and [r] give the sample of each channel at time t (seconds). */
    private fun stereo(seconds: Double, l: (Double) -> Double, r: (Double) -> Double): FloatArray {
        val n = (seconds * sr).toInt()
        return FloatArray(n * 2) { i -> val t = (i / 2).toDouble() / sr; (if (i % 2 == 0) l(t) else r(t)).toFloat() }
    }

    private fun tone(f: Double, amp: Double) = { t: Double -> amp * sin(2 * PI * f * t) }

    private fun rmsDb(buf: FloatArray, from: Int = 0, ch: Int = 2, onlyCh: Int = -1): Double {
        var s = 0.0
        var n = 0
        for (i in from until buf.size) {
            if (onlyCh >= 0 && i % ch != onlyCh) continue
            s += buf[i] * buf[i]; n++
        }
        return 20 * log10(sqrt(s / n).coerceAtLeast(1e-12))
    }

    private fun run(p: AudioLevelLogic.Params, buf: FloatArray, ch: Int = 2): AudioLevelEngine {
        val e = AudioLevelEngine()
        e.configure(sr, ch)
        e.setParams(p)
        // Feed in blocks like Media3 does
        val block = 1024 * ch
        var o = 0
        while (o < buf.size) {
            val len = minOf(block, buf.size - o)
            val tmp = buf.copyOfRange(o, o + len)
            e.process(tmp, len / ch)
            tmp.copyInto(buf, o)
            o += len
        }
        return e
    }

    @Test fun standard_ist_vorgabe_und_aus_laesst_durch() {
        assertEquals(AudioLevelLogic.MODE_OFF, AudioLevelLogic.DEFAULT_MODE)
        assertTrue(AudioLevelLogic.params(AudioLevelLogic.MODE_OFF).bypass)
        val std = AudioLevelLogic.params(AudioLevelLogic.MODE_STANDARD)
        assertEquals(9f, std.maxBoostDb)
        assertEquals(AudioLevelLogic.LEVEL_MEDIUM, std.level)
        assertEquals(3f, std.speechDb)
    }

    @Test fun eigene_einstellung_wird_begrenzt() {
        val p = AudioLevelLogic.params(AudioLevelLogic.MODE_CUSTOM, boostIdx = 99, levelIdx = -4, speechIdx = 2)
        assertEquals(12f, p.maxBoostDb)
        assertEquals(AudioLevelLogic.LEVEL_OFF, p.level)
        assertEquals(6f, p.speechDb)
        assertTrue(AudioLevelLogic.params(AudioLevelLogic.MODE_CUSTOM, 0, 0, 0).bypass)
        assertEquals(AudioLevelLogic.MODE_CUSTOM, AudioLevelLogic.clampMode(42))
    }

    @Test fun kompressor_kurve() {
        // Well below threshold: no change; well above: (1/ratio − 1) * overshoot
        assertEquals(0f, AudioLevelLogic.compressorGainDb(-40f, -26f, 3f), 1e-6f)
        assertEquals(-12f, AudioLevelLogic.compressorGainDb(-8f, -26f, 3f), 1e-4f)
        assertEquals(0f, AudioLevelLogic.compressorGainDb(-8f, -26f, 1f), 1e-6f)
        // Knee is continuous at its upper edge
        assertEquals((1f / 3f - 1f) * 3f, AudioLevelLogic.compressorGainDb(-23f, -26f, 3f), 1e-3f)
    }

    @Test fun automatische_anhebung_nie_leiser_und_begrenzt() {
        assertEquals(7f, AudioLevelLogic.agcGainDb(-23f, 9f), 1e-6f)
        assertEquals(9f, AudioLevelLogic.agcGainDb(-40f, 9f), 1e-6f)
        assertEquals(0f, AudioLevelLogic.agcGainDb(-10f, 9f), 1e-6f)
    }

    @Test fun aus_ist_bitgenau() {
        val buf = stereo(0.5, tone(440.0, 0.3), tone(660.0, 0.2))
        val copy = buf.copyOf()
        run(AudioLevelLogic.OFF, buf)
        assertArrayEquals(copy, buf, 0f)
    }

    @Test fun leiser_sender_wird_lauter_aber_nicht_ueber_ziel() {
        // Speech-band tone at about −26 dBFS RMS, roughly like a −23 LUFS broadcast
        val buf = stereo(20.0, tone(1_000.0, 0.07), tone(1_000.0, 0.07))
        val inDb = rmsDb(buf)
        val e = run(AudioLevelLogic.params(AudioLevelLogic.MODE_STANDARD), buf)
        val outDb = rmsDb(buf, from = buf.size / 2) // second half: gain has settled
        assertTrue("gain ${outDb - inDb} dB", outDb - inDb >= 6.0)
        assertTrue("too loud: $outDb dBFS", outDb <= -12.0)
        assertTrue(e.currentAgcDb > 0f)
    }

    @Test fun nie_uebersteuert() {
        val ceiling = AudioLevelLogic.dbToGain(AudioLevelLogic.CEILING_DB)
        val buf = stereo(3.0, tone(200.0, 1.0), tone(3_000.0, 1.0))
        run(AudioLevelLogic.params(AudioLevelLogic.MODE_NIGHT), buf)
        assertTrue(buf.all { abs(it) <= ceiling + 1e-6f })
    }

    @Test fun dialog_wird_in_der_mitte_angehoben_seite_bleibt() {
        val speechOnly = AudioLevelLogic.Params(0f, AudioLevelLogic.LEVEL_OFF, 6f)
        val mid = stereo(1.0, tone(2_500.0, 0.1), tone(2_500.0, 0.1))
        val side = stereo(1.0, tone(2_500.0, 0.1), tone(2_500.0, -0.1))
        val midIn = rmsDb(mid)
        val sideIn = rmsDb(side)
        run(speechOnly, mid)
        run(speechOnly, side)
        assertEquals(6.0, rmsDb(mid, from = mid.size / 2) - midIn, 0.5)
        assertEquals(0.0, rmsDb(side, from = side.size / 2) - sideIn, 0.1)
        // Bass in the middle is not lifted
        val bass = stereo(1.0, tone(100.0, 0.1), tone(100.0, 0.1))
        val bassIn = rmsDb(bass)
        run(speechOnly, bass)
        assertEquals(0.0, rmsDb(bass, from = bass.size / 2) - bassIn, 0.3)
    }

    @Test fun stille_wird_nicht_hochgezogen() {
        val buf = FloatArray(sr * 2 * 5) // 5 s silence
        val e = run(AudioLevelLogic.params(AudioLevelLogic.MODE_STANDARD), buf)
        assertEquals(0f, e.currentAgcDb, 1e-6f)
        assertTrue(buf.all { it == 0f })
    }

    @Test fun mehrkanal_hebt_center_an_und_laeuft() {
        val ch = 6
        val n = sr * 2
        val buf = FloatArray(n * ch) { i -> val t = (i / ch).toDouble() / sr; (0.1 * sin(2 * PI * 2_500.0 * t)).toFloat() }
        val centreIn = rmsDb(buf, ch = ch, onlyCh = 2)
        val frontIn = rmsDb(buf, ch = ch, onlyCh = 0)
        run(AudioLevelLogic.Params(0f, AudioLevelLogic.LEVEL_OFF, 6f), buf, ch)
        assertEquals(6.0, rmsDb(buf, from = buf.size / 2, ch = ch, onlyCh = 2) - centreIn, 0.5)
        assertEquals(0.0, rmsDb(buf, from = buf.size / 2, ch = ch, onlyCh = 0) - frontIn, 0.1)
    }

    @Test fun flush_behaelt_anhebung_reset_nicht() {
        val e = AudioLevelEngine()
        e.configure(sr, 2)
        e.setParams(AudioLevelLogic.params(AudioLevelLogic.MODE_STANDARD))
        val buf = stereo(10.0, tone(1_000.0, 0.07), tone(1_000.0, 0.07))
        e.process(buf, buf.size / 2)
        val g = e.currentAgcDb
        assertTrue(g > 0f)
        e.flush()
        assertEquals(g, e.currentAgcDb, 1e-6f)
        e.reset()
        assertEquals(0f, e.currentAgcDb, 1e-6f)
    }

    @Test fun texte_in_allen_sprachen() {
        loadAudioLevelTexts()
        assertEquals("Sound levelling", I18nCore.translate("Ton angleichen", I18nCore.CODES.indexOf("en")))
        assertEquals("Geluid egaliseren", I18nCore.translate("Ton angleichen", I18nCore.CODES.indexOf("nl")))
        assertEquals("Ujednačavanje zvuka", I18nCore.translate("Ton angleichen", I18nCore.CODES.indexOf("hr")))
        // Every row has all nine columns (de … it); the Dutch column covers every row
        val rows = DictAudio.lineSequence().filter { it.contains('¦') }.toList()
        assertTrue(rows.all { it.split('¦').size == 9 })
        val nlKeys = DictNlAudio.lineSequence().filter { it.contains('¦') }.map { it.substringBefore('¦') }.toSet()
        assertEquals(rows.map { it.substringBefore('¦') }.toSet(), nlKeys)
    }
}
