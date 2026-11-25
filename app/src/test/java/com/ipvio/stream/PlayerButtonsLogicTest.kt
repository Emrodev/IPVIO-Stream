package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-062: the stored button list of the Live TV player (pure, no Android). */
class PlayerButtonsLogicTest {

    @Test
    fun neverChanged_isDefault_andDefaultHasNoMenuButtons() {
        assertEquals(PlayerButtonsLogic.DEFAULT, PlayerButtonsLogic.decode(null))
        assertTrue(PlayerButtonsLogic.ASPECT !in PlayerButtonsLogic.DEFAULT)
        assertTrue(PlayerButtonsLogic.SECOND !in PlayerButtonsLogic.DEFAULT)
        assertTrue(PlayerButtonsLogic.EXTERNAL !in PlayerButtonsLogic.DEFAULT)
        assertTrue(PlayerButtonsLogic.DEFAULT.all { it in PlayerButtonsLogic.ALL })
    }

    @Test
    fun emptyText_meansNoButtons() {
        assertEquals(emptyList<String>(), PlayerButtonsLogic.decode(""))
    }

    @Test
    fun brokenText_dropsUnknownIdsAndRepeats() {
        assertEquals(listOf("fav", "rec"), PlayerButtonsLogic.decode(" fav, nonsense ,rec,fav,,"))
        assertEquals(emptyList<String>(), PlayerButtonsLogic.decode("???"))
    }

    @Test
    fun encodeThenDecode_keepsTheOrder() {
        val l = listOf("tracks", "backlive", "fav")
        assertEquals(l, PlayerButtonsLogic.decode(PlayerButtonsLogic.encode(l)))
    }

    @Test
    fun toggle_hidesAndShowsAtTheEnd() {
        val l = listOf("rec", "fav")
        assertEquals(listOf("fav"), PlayerButtonsLogic.toggle(l, "rec"))
        assertEquals(listOf("rec", "fav", "tracks"), PlayerButtonsLogic.toggle(l, "tracks"))
        assertEquals(l, PlayerButtonsLogic.toggle(l, "unknown"))
    }

    @Test
    fun move_putsTheButtonAtThePosition() {
        val l = listOf("a", "rec", "fav", "tracks").filter { it != "a" }
        assertEquals(listOf("tracks", "rec", "fav"), PlayerButtonsLogic.move(l, "tracks", 0))
        assertEquals(listOf("fav", "tracks", "rec"), PlayerButtonsLogic.move(l, "rec", 99))
        assertEquals(l, PlayerButtonsLogic.move(l, "backlive", 0))   // hidden: unchanged
    }
}
