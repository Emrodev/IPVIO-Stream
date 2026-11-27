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

    /**
     * T-077: edge-only scroll for the settings: the page moves only when the focused item is cut off, with a small
     * margin (90 px) below or above, as before T-074. The page stops moving when the last controls are visible.
     */
    fun edgeDistance(offset: Float, size: Float, containerSize: Float): Float {
        val peek = 90f
        val leading = offset
        val trailing = offset + size
        return when {
            leading >= 0f && trailing <= containerSize -> 0f
            leading < 0f && trailing > containerSize -> 0f
            leading < 0f -> leading - peek
            else -> trailing - containerSize + peek
        }
    }
}
