package com.ipvio.stream

/**
 * #146: when the player asks for tunneled playback (pure logic, no Android).
 *
 * Tunneling lets video and audio run through one hardware path of the device. It is only asked for in Live TV, and not
 * while a second picture is shown, because a device usually has one tunneled decoder. The player itself falls back to the
 * normal path when the device cannot tunnel the stream.
 */
object TunnelingLogic {
    fun wanted(enabled: Boolean, live: Boolean, secondPictureOn: Boolean): Boolean = enabled && live && !secondPictureOn
}
