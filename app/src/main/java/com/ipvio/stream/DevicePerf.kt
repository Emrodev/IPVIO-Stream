package com.ipvio.stream

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import coil.Coil
import coil.ImageLoader
import coil.memory.MemoryCache

/**
 * T-070: setting "Device performance" (Settings > General): Automatic, Weak device, Strong device. A device setting, the same
 * for every profile, stored in `ipvio_general` (key `device_perf`). What each choice switches: [DevicePerfLogic].
 * [profile] is Compose state, so the screens follow a change at once; the image cache is rebuilt with the new size.
 */
object DevicePerf {
    private const val PREFS = "ipvio_general"
    private const val KEY = "device_perf"

    var mode by mutableIntStateOf(DevicePerfLogic.AUTO)
        private set
    var profile by mutableStateOf(DevicePerfLogic.profile(DevicePerfLogic.AUTO, 1))
        private set
    private var tier = 1

    /** No animations, background picture, slideshow bar, poster preview or focus shadow ("Weak device"). */
    val reduceVisuals: Boolean get() = profile.reduceVisuals

    fun init(c: Context) {
        tier = Mem.tier(c)
        mode = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY, DevicePerfLogic.AUTO).coerceIn(0, 2)
        profile = DevicePerfLogic.profile(mode, tier)
    }

    fun set(c: Context, m: Int) {
        val v = m.coerceIn(0, 2)
        if (v == mode) return
        mode = v
        profile = DevicePerfLogic.profile(v, tier)
        c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY, v).apply()
        // New image cache size and decoder count; the old cache is emptied (images reload from the disk cache)
        val app = c.applicationContext
        runCatching {
            val old = Coil.imageLoader(app)
            Coil.setImageLoader(imageLoader(app))
            old.memoryCache?.clear()
        }
    }

    /** The app's image loader for the current [profile] (also used at start, see MainActivity). */
    fun imageLoader(c: Context): ImageLoader = ImageLoader.Builder(c)
        .memoryCache { MemoryCache.Builder(c).maxSizePercent(Mem.imageCachePercent(profile.imageTier)).build() }
        .allowRgb565(true)
        .bitmapFactoryMaxParallelism(profile.decoders)
        .crossfade(false)
        .build()
}
