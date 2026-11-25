package com.ipvio.stream

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer

/**
 * Sound levelling settings (T-028): a device setting stored per profile, like the audio delay in [PlayerSync].
 * Off by default ([AudioLevelLogic.DEFAULT_MODE]): the user switches it on in Settings, so existing sound does not change.
 * Changes apply at once, also during playback: the processor reads [current] for every buffer.
 */
object AudioLevel {
    private const val PREFS = "ipvio_audio"

    var mode by mutableIntStateOf(AudioLevelLogic.DEFAULT_MODE)
        private set
    var boost by mutableIntStateOf(AudioLevelLogic.DEFAULT_BOOST)
        private set
    var level by mutableIntStateOf(AudioLevelLogic.DEFAULT_LEVEL)
        private set
    var speech by mutableIntStateOf(AudioLevelLogic.DEFAULT_SPEECH)
        private set

    /** Read on the playback thread by [AudioLevelProcessor]. */
    @Volatile var current: AudioLevelLogic.Params = AudioLevelLogic.params(AudioLevelLogic.DEFAULT_MODE)
        private set

    /** Reads the active profile's settings; called at app start and whenever a player is created. */
    fun init(c: Context) {
        loadAudioLevelTexts()
        val p = Profiles.sp(c, PREFS)
        mode = AudioLevelLogic.clampMode(p.getInt("mode", AudioLevelLogic.DEFAULT_MODE))
        boost = AudioLevelLogic.clampBoost(p.getInt("boost", AudioLevelLogic.DEFAULT_BOOST))
        level = AudioLevelLogic.clampLevel(p.getInt("level", AudioLevelLogic.DEFAULT_LEVEL))
        speech = AudioLevelLogic.clampSpeech(p.getInt("speech", AudioLevelLogic.DEFAULT_SPEECH))
        update()
    }

    fun setMode(c: Context, v: Int) {
        mode = AudioLevelLogic.clampMode(v)
        save(c)
    }

    fun setCustom(c: Context, boostIdx: Int = boost, levelIdx: Int = level, speechIdx: Int = speech) {
        boost = AudioLevelLogic.clampBoost(boostIdx)
        level = AudioLevelLogic.clampLevel(levelIdx)
        speech = AudioLevelLogic.clampSpeech(speechIdx)
        save(c)
    }

    /** Back to the default (Standard). */
    fun reset(c: Context) {
        mode = AudioLevelLogic.DEFAULT_MODE
        boost = AudioLevelLogic.DEFAULT_BOOST
        level = AudioLevelLogic.DEFAULT_LEVEL
        speech = AudioLevelLogic.DEFAULT_SPEECH
        save(c)
    }

    private fun save(c: Context) {
        update()
        Profiles.sp(c, PREFS).edit().putInt("mode", mode).putInt("boost", boost).putInt("level", level)
            .putInt("speech", speech).apply()
    }

    private fun update() { current = AudioLevelLogic.params(mode, boost, level, speech) }
}

/**
 * Media3 audio processor that runs [AudioLevelEngine] on decoded PCM (16-bit or float), before the audio track.
 * Encoded passthrough (AC3/E-AC3/DTS sent to a receiver) never reaches audio processors, so it stays untouched.
 * Sound levelling switched off: the samples are copied unchanged.
 */
class AudioLevelProcessor : BaseAudioProcessor() {
    private val engine = AudioLevelEngine()
    private var scratch = FloatArray(0)
    private var isFloat = false
    private var channels = 2

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val enc = inputAudioFormat.encoding
        if (enc != C.ENCODING_PCM_16BIT && enc != C.ENCODING_PCM_FLOAT) return AudioProcessor.AudioFormat.NOT_SET
        isFloat = enc == C.ENCODING_PCM_FLOAT
        channels = inputAudioFormat.channelCount
        engine.configure(inputAudioFormat.sampleRate, channels)
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        val out = replaceOutputBuffer(size)
        val p = AudioLevel.current
        engine.setParams(p)
        if (p.bypass) {
            out.put(inputBuffer)
            out.flip()
            return
        }
        val samples = if (isFloat) size / 4 else size / 2
        if (scratch.size < samples) scratch = FloatArray(samples)
        if (isFloat) {
            for (i in 0 until samples) scratch[i] = inputBuffer.getFloat()
        } else {
            for (i in 0 until samples) scratch[i] = inputBuffer.getShort() / 32768f
        }
        engine.process(scratch, samples / channels)
        if (isFloat) {
            for (i in 0 until samples) out.putFloat(scratch[i])
        } else {
            for (i in 0 until samples) out.putShort((scratch[i] * 32767f).toInt().coerceIn(-32768, 32767).toShort())
        }
        inputBuffer.position(inputBuffer.limit())
        out.flip()
    }

    /** Seek or track change: filters restart, the automatic gain keeps its level (no slow fade-in after every seek). */
    override fun onFlush() { engine.flush() }

    override fun onReset() { engine.reset() }
}
