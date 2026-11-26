package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-070: what "Device performance" switches. */
class DevicePerfLogicTest {
    @Test
    fun automaticOnMediumAndHighMemoryIsAsBefore() {
        for (tier in 1..2) {
            val p = DevicePerfLogic.profile(DevicePerfLogic.AUTO, tier)
            assertFalse(p.reduceVisuals)
            assertFalse(p.smallImages)
            assertFalse(p.searchOnDemand)
            assertTrue(p.keepHome)
            assertEquals(StartupLogic.SEARCH_INDEX_DELAY_MS, p.searchDelayMs)
            assertEquals(8_000L, p.genreDelayMs)
            assertEquals(tier, p.imageTier)
            assertEquals(if (tier == 2) 4 else 2, p.decoders)
        }
    }

    @Test
    fun automaticOnLowMemoryOnlyMovesInvisibleWorkLater() {
        val p = DevicePerfLogic.profile(DevicePerfLogic.AUTO, 0)
        // Nothing visible changes
        assertFalse(p.reduceVisuals)
        assertFalse(p.smallImages)
        assertTrue(p.keepHome)
        assertFalse(p.searchOnDemand)
        // Background work starts later
        assertEquals(DevicePerfLogic.SEARCH_DELAY_LIGHT_MS, p.searchDelayMs)
        assertEquals(DevicePerfLogic.GENRE_DELAY_LIGHT_MS, p.genreDelayMs)
        assertEquals(0, p.imageTier)
        assertEquals(2, p.decoders)
    }

    @Test
    fun weakDeviceSwitchesEverythingOff() {
        for (tier in 0..2) {
            val p = DevicePerfLogic.profile(DevicePerfLogic.WEAK, tier)
            assertTrue(p.reduceVisuals)
            assertTrue(p.smallImages)
            assertTrue(p.searchOnDemand)
            assertFalse(p.keepHome)
            assertEquals(DevicePerfLogic.GENRE_DELAY_LIGHT_MS, p.genreDelayMs)
            assertEquals(0, p.imageTier)
            assertEquals(2, p.decoders)
        }
    }

    @Test
    fun strongDeviceSwitchesEverythingOnAlsoWithLittleMemory() {
        val p = DevicePerfLogic.profile(DevicePerfLogic.STRONG, 0)
        assertFalse(p.reduceVisuals)
        assertFalse(p.smallImages)
        assertFalse(p.searchOnDemand)
        assertTrue(p.keepHome)
        assertEquals(StartupLogic.SEARCH_INDEX_DELAY_MS, p.searchDelayMs)
        assertEquals(8_000L, p.genreDelayMs)
        assertEquals(1, p.imageTier)
        assertEquals(4, p.decoders)
        assertEquals(2, DevicePerfLogic.profile(DevicePerfLogic.STRONG, 2).imageTier)
    }

    @Test
    fun cardImageSizes() {
        val poster = "https://image.tmdb.org/t/p/w500/abc.jpg"
        assertEquals("https://image.tmdb.org/t/p/w342/abc.jpg", DevicePerfLogic.cardPoster(poster, false))
        assertEquals("https://image.tmdb.org/t/p/w185/abc.jpg", DevicePerfLogic.cardPoster(poster, true))
        val scene = "https://image.tmdb.org/t/p/w1280/xyz.jpg"
        assertEquals("https://image.tmdb.org/t/p/w780/xyz.jpg", DevicePerfLogic.smallBackdrop(scene, false))
        assertEquals("https://image.tmdb.org/t/p/w300/xyz.jpg", DevicePerfLogic.smallBackdrop(scene, true))
        // Provider images (not TMDB) stay as they are
        assertEquals("http://provider/logo.png", DevicePerfLogic.cardPoster("http://provider/logo.png", true))
    }
}
