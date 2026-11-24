package com.ipvio.stream

/** Up / Down on the remote in Live TV (T-052). Pure logic, no Android imports. */
object LiveKeyLogic {
    /**
     * Channel step for a key press: +1 = next channel, -1 = previous channel.
     * Default (upIsNext = false): Up = previous, Down = next. With the setting on, the two are swapped.
     */
    fun zapStep(up: Boolean, upIsNext: Boolean): Int = if (up == upIsNext) 1 else -1
}
