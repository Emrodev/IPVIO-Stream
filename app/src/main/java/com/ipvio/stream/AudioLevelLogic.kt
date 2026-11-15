package com.ipvio.stream

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure rules for sound levelling (T-028), without Android so they can be tested.
 *
 * Why: IPTV channels are mixed to broadcast loudness (about −23 LUFS, EBU R128), while streaming apps such as YouTube
 * and Netflix play at about −14 to −16 LUFS. Switching from those apps to live TV therefore sounds 7–9 dB quieter, the
 * device volume is often already at its maximum, and speech gets lost under music and effects.
 *
 * Modes are presets of three controls: maximum extra volume, levelling strength (compression) and speech lift.
 */
object AudioLevelLogic {
    const val MODE_OFF = 0
    const val MODE_STANDARD = 1
    const val MODE_SPEECH = 2
    const val MODE_NIGHT = 3
    const val MODE_CUSTOM = 4

    /** On by default: fixes quiet IPTV channels without any setup. */
    const val DEFAULT_MODE = MODE_OFF

    /** Choices of the custom controls (the index is stored). */
    val BOOST_DB = intArrayOf(0, 3, 6, 9, 12)
    val SPEECH_DB = intArrayOf(0, 3, 6, 9)
    const val LEVEL_OFF = 0
    const val LEVEL_LIGHT = 1
    const val LEVEL_MEDIUM = 2
    const val LEVEL_STRONG = 3

    const val DEFAULT_BOOST = 3 // +9 dB
    const val DEFAULT_LEVEL = LEVEL_MEDIUM
    const val DEFAULT_SPEECH = 1 // +3 dB

    /** Loudness the automatic gain aims for (like streaming apps). */
    const val TARGET_LUFS = -16f

    /** Peak ceiling of the limiter (dBFS); leaves headroom for the TV or receiver. */
    const val CEILING_DB = -1f

    /** Silence gate of the loudness meter: below this the automatic gain holds. */
    const val GATE_LUFS = -50f

    /** Engine settings. [maxBoostDb] = 0, [level] = 0 and [speechDb] = 0 mean: audio passes unchanged. */
    data class Params(val maxBoostDb: Float, val level: Int, val speechDb: Float) {
        val bypass: Boolean get() = maxBoostDb <= 0f && level == LEVEL_OFF && speechDb <= 0f
    }

    val OFF = Params(0f, LEVEL_OFF, 0f)

    fun clampMode(m: Int): Int = m.coerceIn(MODE_OFF, MODE_CUSTOM)
    fun clampBoost(i: Int): Int = i.coerceIn(0, BOOST_DB.size - 1)
    fun clampLevel(i: Int): Int = i.coerceIn(LEVEL_OFF, LEVEL_STRONG)
    fun clampSpeech(i: Int): Int = i.coerceIn(0, SPEECH_DB.size - 1)

    /** Engine settings for a mode; the custom indexes are only used in [MODE_CUSTOM]. */
    fun params(mode: Int, boostIdx: Int = DEFAULT_BOOST, levelIdx: Int = DEFAULT_LEVEL, speechIdx: Int = DEFAULT_SPEECH): Params =
        when (clampMode(mode)) {
            MODE_STANDARD -> Params(9f, LEVEL_MEDIUM, 3f)
            MODE_SPEECH -> Params(9f, LEVEL_MEDIUM, 6f)
            MODE_NIGHT -> Params(12f, LEVEL_STRONG, 4f)
            MODE_CUSTOM -> Params(
                BOOST_DB[clampBoost(boostIdx)].toFloat(),
                clampLevel(levelIdx),
                SPEECH_DB[clampSpeech(speechIdx)].toFloat()
            )
            else -> OFF
        }

    /** Compressor threshold (dBFS) per levelling strength. */
    fun thresholdDb(level: Int): Float = when (level) {
        LEVEL_LIGHT -> -20f
        LEVEL_MEDIUM -> -26f
        LEVEL_STRONG -> -32f
        else -> 0f
    }

    /** Compressor ratio per levelling strength. */
    fun ratio(level: Int): Float = when (level) {
        LEVEL_LIGHT -> 2f
        LEVEL_MEDIUM -> 3f
        LEVEL_STRONG -> 5f
        else -> 1f
    }

    /** Static gain curve with a soft knee: gain change in dB (≤ 0) for a detector level in dBFS. */
    fun compressorGainDb(levelDb: Float, thresholdDb: Float, ratio: Float, kneeDb: Float = 6f): Float {
        if (ratio <= 1f) return 0f
        val over = levelDb - thresholdDb
        val slope = 1f / ratio - 1f
        return when {
            2f * over < -kneeDb -> 0f
            2f * over > kneeDb -> slope * over
            else -> slope * (over + kneeDb / 2f).pow(2) / (2f * kneeDb)
        }
    }

    /** Automatic gain in dB that brings [loudness] to [TARGET_LUFS], limited to 0 … [maxDb] (never quieter). */
    fun agcGainDb(loudness: Float, maxDb: Float): Float = (TARGET_LUFS - loudness).coerceIn(0f, max(0f, maxDb))

    fun dbToGain(db: Float): Float = 10f.pow(db / 20f)
    fun gainToDb(g: Float): Float = if (g <= 1e-9f) -180f else 20f * log10(g)
}

/** Biquad filter (RBJ audio EQ cookbook), direct form I, one state per channel. */
internal class Biquad(channels: Int) {
    private var b0 = 1f
    private var b1 = 0f
    private var b2 = 0f
    private var a1 = 0f
    private var a2 = 0f
    private val x1 = FloatArray(channels)
    private val x2 = FloatArray(channels)
    private val y1 = FloatArray(channels)
    private val y2 = FloatArray(channels)

    fun peaking(sr: Int, f0: Float, q: Float, gainDb: Float) {
        val a = 10f.pow(gainDb / 40f)
        val w = 2.0 * PI * f0 / sr
        val alpha = (sin(w) / (2.0 * q)).toFloat()
        val c = cos(w).toFloat()
        set(1 + alpha * a, -2 * c, 1 - alpha * a, 1 + alpha / a, -2 * c, 1 - alpha / a)
    }

    fun highShelf(sr: Int, f0: Float, gainDb: Float) {
        val a = 10f.pow(gainDb / 40f)
        val w = 2.0 * PI * f0 / sr
        val c = cos(w).toFloat()
        val alpha = (sin(w) / 2.0 * sqrt(2.0)).toFloat() // shelf slope 1
        val sq = 2f * sqrt(a) * alpha
        set(
            a * ((a + 1) + (a - 1) * c + sq), -2 * a * ((a - 1) + (a + 1) * c), a * ((a + 1) + (a - 1) * c - sq),
            (a + 1) - (a - 1) * c + sq, 2 * ((a - 1) - (a + 1) * c), (a + 1) - (a - 1) * c - sq
        )
    }

    fun highPass(sr: Int, f0: Float, q: Float) {
        val w = 2.0 * PI * f0 / sr
        val alpha = (sin(w) / (2.0 * q)).toFloat()
        val c = cos(w).toFloat()
        set((1 + c) / 2, -(1 + c), (1 + c) / 2, 1 + alpha, -2 * c, 1 - alpha)
    }

    private fun set(nb0: Float, nb1: Float, nb2: Float, na0: Float, na1: Float, na2: Float) {
        b0 = nb0 / na0; b1 = nb1 / na0; b2 = nb2 / na0; a1 = na1 / na0; a2 = na2 / na0
    }

    fun run(ch: Int, x: Float): Float {
        val y = b0 * x + b1 * x1[ch] + b2 * x2[ch] - a1 * y1[ch] - a2 * y2[ch]
        x2[ch] = x1[ch]; x1[ch] = x; y2[ch] = y1[ch]; y1[ch] = y
        return y
    }

    fun clear() { x1.fill(0f); x2.fill(0f); y1.fill(0f); y2.fill(0f) }
}

/**
 * Sound levelling engine for interleaved float PCM (−1 … 1). Chain per frame:
 * 1. speech lift: presence boost around 2.5 kHz on the centre (mid of stereo, centre channel of 5.1/7.1),
 * 2. compressor (channel-linked, soft knee) so quiet and loud parts come closer together,
 * 3. automatic gain towards [AudioLevelLogic.TARGET_LUFS], measured with a K-weighted loudness meter
 *    (ITU-R BS.1770 style); it rises slowly, never exceeds the maximum boost and holds during silence,
 * 4. peak limiter at [AudioLevelLogic.CEILING_DB] so the extra volume never clips.
 * With [AudioLevelLogic.Params.bypass] the samples are not touched at all.
 */
class AudioLevelEngine {
    private var sr = 48_000
    private var ch = 2
    private var params = AudioLevelLogic.OFF

    private var speech = Biquad(2)
    private var kShelf = Biquad(2)
    private var kHp = Biquad(2)

    private var compEnvDb = -120f
    private var compGainDb = 0f
    private var msLoud = 0.0
    private var agcDb = 0f
    private var limGain = 1f

    // Time constants, set in configure()
    private var envAtt = 0f
    private var envRel = 0f
    private var loudCoef = 0.0
    private var agcUpPerSample = 0f
    private var agcDownPerSample = 0f
    private var limRel = 0f

    /** Current automatic gain in dB (for tests and diagnostics). */
    val currentAgcDb: Float get() = agcDb

    fun configure(sampleRate: Int, channels: Int) {
        sr = max(8_000, sampleRate)
        ch = max(1, channels)
        kShelf = Biquad(ch).also { it.highShelf(sr, 1_500f, 4f) }
        kHp = Biquad(ch).also { it.highPass(sr, 38f, 0.5f) }
        envAtt = coef(0.010f)
        envRel = coef(0.250f)
        loudCoef = exp(-1.0 / (3.0 * sr))
        agcUpPerSample = 2f / sr // +2 dB per second
        agcDownPerSample = 8f / sr // −8 dB per second
        limRel = coef(0.080f)
        applySpeech()
        reset()
    }

    fun setParams(p: AudioLevelLogic.Params) {
        if (p == params) return
        params = p
        applySpeech()
    }

    /** Clears all state (new player). The automatic gain starts at 0 dB and rises slowly again. */
    fun reset() {
        flush()
        agcDb = 0f
    }

    /** Seek or track change: filter and detector state restart, the automatic gain keeps its level. */
    fun flush() {
        speech.clear(); kShelf.clear(); kHp.clear()
        compEnvDb = -120f; compGainDb = 0f; msLoud = 0.0; limGain = 1f
    }

    private fun coef(seconds: Float): Float = exp(-1.0 / (seconds * sr)).toFloat()

    private fun applySpeech() {
        speech = Biquad(ch).also { if (params.speechDb > 0f) it.peaking(sr, 2_500f, 0.8f, params.speechDb) }
    }

    private fun isLfe(c: Int): Boolean = (ch == 6 || ch == 8) && c == 3

    /** Processes [frames] frames of [buf] in place. */
    fun process(buf: FloatArray, frames: Int) {
        val p = params
        if (p.bypass) return
        val thr = AudioLevelLogic.thresholdDb(p.level)
        val ratio = AudioLevelLogic.ratio(p.level)
        val ceiling = AudioLevelLogic.dbToGain(AudioLevelLogic.CEILING_DB)
        val maxBoost = max(0f, p.maxBoostDb)
        val n = min(frames, buf.size / ch)
        for (f in 0 until n) {
            val o = f * ch

            // 1. Speech lift on the centre
            if (p.speechDb > 0f) {
                when {
                    ch == 2 -> {
                        val mid = (buf[o] + buf[o + 1]) * 0.5f
                        val side = (buf[o] - buf[o + 1]) * 0.5f
                        val m = speech.run(0, mid)
                        buf[o] = m + side
                        buf[o + 1] = m - side
                    }
                    ch == 1 -> buf[o] = speech.run(0, buf[o])
                    else -> buf[o + 2] = speech.run(2, buf[o + 2])
                }
            }

            // 2. Compressor, detector on the loudest channel
            var peak = 0f
            for (c in 0 until ch) peak = max(peak, abs(buf[o + c]))
            if (p.level != AudioLevelLogic.LEVEL_OFF) {
                val lvl = AudioLevelLogic.gainToDb(peak)
                val k = if (lvl > compEnvDb) envAtt else envRel
                compEnvDb = lvl + k * (compEnvDb - lvl)
                compGainDb = AudioLevelLogic.compressorGainDb(compEnvDb, thr, ratio)
            } else {
                compGainDb = 0f
            }
            val compG = AudioLevelLogic.dbToGain(compGainDb)

            // 3. Loudness of the compressed signal, automatic gain
            var sum = 0.0
            for (c in 0 until ch) {
                if (isLfe(c)) continue
                val k = kHp.run(c, kShelf.run(c, buf[o + c] * compG))
                sum += (k * k).toDouble()
            }
            msLoud = sum + loudCoef * (msLoud - sum)
            val loud = (-0.691 + 10.0 * log10(max(msLoud, 1e-12))).toFloat()
            if (loud > AudioLevelLogic.GATE_LUFS) {
                val want = AudioLevelLogic.agcGainDb(loud, maxBoost)
                agcDb = if (want > agcDb) min(want, agcDb + agcUpPerSample) else max(want, agcDb - agcDownPerSample)
            }
            agcDb = agcDb.coerceIn(0f, maxBoost)
            val g = compG * AudioLevelLogic.dbToGain(agcDb)

            // 4. Limiter: instant attack, smooth release
            val outPeak = peak * g
            val need = if (outPeak > ceiling) ceiling / outPeak else 1f
            limGain = if (need < limGain) need else limRel * limGain + (1f - limRel) * need
            val total = g * limGain
            for (c in 0 until ch) buf[o + c] = (buf[o + c] * total).coerceIn(-ceiling, ceiling)
        }
    }
}
