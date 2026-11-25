package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Test

/** #96: the automatic genre categories of Live TV can be switched off as a whole (pure, no Android). */
class LiveGenreLogicTest {
    private val keys = listOf("GEN:sport", "GEN:movies", "GEN:kids")

    @Test
    fun on_listsAllGenresExceptHiddenOnes_inOrder() {
        assertEquals(keys, LiveGenreLogic.visible(keys, emptyList(), true))
        assertEquals(listOf("GEN:sport", "GEN:kids"), LiveGenreLogic.visible(keys, listOf("GEN:movies"), true))
    }

    @Test
    fun off_listsNone_whatEverIsHidden() {
        assertEquals(emptyList<String>(), LiveGenreLogic.visible(keys, emptyList(), false))
        assertEquals(emptyList<String>(), LiveGenreLogic.visible(keys, listOf("GEN:movies"), false))
    }

    @Test
    fun noGenres_isEmpty() {
        assertEquals(emptyList<String>(), LiveGenreLogic.visible(emptyList(), emptyList(), true))
    }
}
