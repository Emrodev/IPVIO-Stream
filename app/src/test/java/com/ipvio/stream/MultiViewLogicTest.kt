package com.ipvio.stream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiViewLogicTest {
    private fun ch(id: String) = Item(id, Kind.LIVE, "Channel $id", null, "http://x/$id", null, "Group")

    @Test fun threeLayoutsWithGridLast() {
        assertEquals(3, MultiViewLogic.LAYOUT_NAMES.size)
        assertEquals(MultiViewLogic.GRID4, MultiViewLogic.LAYOUT_NAMES.size - 1)
    }

    @Test fun gridShowsFourTilesAndSideBySideTwo() {
        assertEquals(4, MultiViewLogic.visibleTiles(MultiViewLogic.GRID4))
        assertEquals(2, MultiViewLogic.visibleTiles(MultiViewLogic.GRID))
        assertEquals(2, MultiViewLogic.visibleTiles(MultiViewLogic.BIG))
    }

    @Test fun startChannelsTakeFavoritesThenHistoryThenListUpToFour() {
        val live = (1..6).map { ch("c$it") }
        val picked = MultiViewLogic.startChannels(live, favs = listOf("c5"), hist = listOf("c3", "c5", "c9"))
        assertEquals(listOf("c5", "c3", "c1", "c2"), picked.map { it.id })
    }

    @Test fun startChannelsWithFewChannelsReturnsWhatExists() {
        assertEquals(listOf("c1"), MultiViewLogic.startChannels(listOf(ch("c1")), emptyList(), emptyList()).map { it.id })
    }

    @Test fun startStepIsSmallButPositive() {
        assertTrue(MultiViewLogic.START_STEP_MS in 100L..1000L)
    }
}
