package com.ipvio.stream

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeKeepLogicTest {
    private fun base(
        playing: Boolean = false, browse: Boolean = false, categories: Boolean = false, adding: Boolean = false,
        boomer: Boolean = false, search: Boolean = false, downloads: Boolean = false
    ) = HomeKeepLogic.homeIsBase(playing, browse, categories, adding, boomer, search, downloads)

    @Test
    fun homeIsBaseOnlyWithoutAnotherFullScreen() {
        assertTrue(base())
        assertFalse(base(playing = true))
        assertFalse(base(browse = true))
        assertFalse(base(categories = true))
        assertFalse(base(adding = true))
        assertFalse(base(boomer = true))
        assertFalse(base(search = true))
        assertFalse(base(downloads = true))
    }

    @Test
    fun keptHiddenUnderADetailPageWhenMemoryAllows() {
        assertTrue(HomeKeepLogic.keepHidden(base = true, pageOnTop = true, heapTight = false, enabled = true))
        assertFalse(HomeKeepLogic.keepHidden(base = true, pageOnTop = true, heapTight = true, enabled = true))
        assertFalse(HomeKeepLogic.keepHidden(base = true, pageOnTop = false, heapTight = false, enabled = true))
        assertFalse(HomeKeepLogic.keepHidden(base = false, pageOnTop = true, heapTight = false, enabled = true))
        assertFalse(HomeKeepLogic.keepHidden(base = true, pageOnTop = true, heapTight = false, enabled = false))
    }

    @Test
    fun composedWhenShownOrKept() {
        // Shown as before
        assertTrue(HomeKeepLogic.composeHome(base = true, pageOnTop = false, keepHidden = false))
        // Kept under the page
        assertTrue(HomeKeepLogic.composeHome(base = true, pageOnTop = true, keepHidden = true))
        // Page on top without keeping (memory tight, switch off): replaced, as before
        assertFalse(HomeKeepLogic.composeHome(base = true, pageOnTop = true, keepHidden = false))
        // Another screen is the base (title list, player, search …)
        assertFalse(HomeKeepLogic.composeHome(base = false, pageOnTop = false, keepHidden = false))
    }
}
