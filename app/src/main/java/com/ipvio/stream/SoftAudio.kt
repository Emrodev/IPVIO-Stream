package com.ipvio.stream

import android.content.Context
import android.os.Handler
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink

/**
 * Software audio decoder (FFmpeg): plays audio formats like AC3, E-AC3, DTS or TrueHD even when the device cannot
 * decode them itself – typical with Bluetooth headphones (AirPods), where there is no passthrough output to the receiver.
 * Stays as before if the library is missing or does not match the Media3 version: the test creates the
 * decoder once on trial before it is switched on and catches every error (no crash during playback).
 */
object SoftAudio {
    private var checked = false
    private var ok = false

    fun available(ctx: Context): Boolean {
        if (checked) return ok
        checked = true
        ok = try {
            val c = Class.forName("androidx.media3.decoder.ffmpeg.FfmpegAudioRenderer")
                .getConstructor(Handler::class.java, AudioRendererEventListener::class.java, AudioSink::class.java)
            c.newInstance(null, null, DefaultAudioSink.Builder(ctx.applicationContext).build())
            true
        } catch (t: Throwable) {
            false
        }
        return ok
    }

    fun factory(ctx: Context): DefaultRenderersFactory {
        AudioLevel.init(ctx)
        return LevelledRenderersFactory(ctx)
            .setExtensionRendererMode(if (available(ctx)) DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON else DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
            .setEnableDecoderFallback(true)
    }

    /**
     * Same audio sink as Media3's default (DefaultRenderersFactory.buildAudioSink in 1.11.1), plus sound levelling
     * (T-028). The sink is shared by the platform and the FFmpeg audio renderer, so both decoders are covered.
     */
    private class LevelledRenderersFactory(ctx: Context) : DefaultRenderersFactory(ctx) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioOutputPlaybackParams: Boolean
        ): AudioSink = DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParams)
            .setAudioProcessors(arrayOf(AudioLevelProcessor()))
            .build()
    }
}
