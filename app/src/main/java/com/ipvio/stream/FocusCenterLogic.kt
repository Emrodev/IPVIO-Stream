package com.ipvio.stream

/**
 * T-074: the focused row stays in the middle of the list while the user moves. At the start or end of a list the
 * scroll stops (the lazy list clamps it), and the focus then moves towards the edge of the screen.
 */
object FocusCenterLogic {
    /**
     * Scroll distance that puts the middle of the focused item into the middle of the container.
     * [offset] is the start of the item, [size] its length, [containerSize] the visible length (all in pixels).
     * An item larger than the container is not centred: it keeps its place, so the list does not jump.
     */
    fun scrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val leading = offset
        val trailing = offset + size
        if (leading < 0f && trailing > containerSize) return 0f
        return (offset + size / 2f) - containerSize / 2f
    }
}
