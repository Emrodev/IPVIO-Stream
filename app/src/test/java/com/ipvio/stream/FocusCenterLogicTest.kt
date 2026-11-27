package com.ipvio.stream

import org.junit.Assert.*
import org.junit.Test

class FocusCenterLogicTest {
    @Test fun itemBelowMiddleIsScrolledToTheMiddle() {
        // container 1000, item 100 long starting at 900: its middle (950) moves to the middle (500)
        assertEquals(450f, FocusCenterLogic.scrollDistance(900f, 100f, 1000f), 0.001f)
    }

    @Test fun itemAboveMiddleIsScrolledBackwards() {
        // middle of the item 150 moves to the middle of the container 500: scroll back by 350
        assertEquals(-350f, FocusCenterLogic.scrollDistance(100f, 100f, 1000f), 0.001f)
    }

    @Test fun centredItemDoesNotMove() {
        assertEquals(0f, FocusCenterLogic.scrollDistance(450f, 100f, 1000f), 0.001f)
    }

    @Test fun itemLargerThanContainerDoesNotJump() {
        assertEquals(0f, FocusCenterLogic.scrollDistance(-200f, 1500f, 1000f), 0.001f)
    }

    @Test fun edgeModeDoesNotMoveAVisibleItem() {
        assertEquals(0f, FocusCenterLogic.edgeDistance(100f, 100f, 1000f), 0.001f)
    }

    @Test fun edgeModeMovesOnlyWhenCutOffAtTheBottom() {
        // item from 950 to 1050 in a 1000 container: scroll by 50 + the 90 px margin
        assertEquals(140f, FocusCenterLogic.edgeDistance(950f, 100f, 1000f), 0.001f)
    }
}
