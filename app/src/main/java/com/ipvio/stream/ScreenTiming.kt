package com.ipvio.stream

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos

/**
 * T-056: measures how long a screen takes from its first composition to the next drawn frame, and writes one line
 * to the diagnostics log ("Opened Search: 412 ms"). Called once per screen change in [MainActivity]; the value is
 * shown in Settings > Diagnostics and in the phone report, so slow screens can be compared on the device.
 */
@Composable
fun ScreenTimer(name: String) {
    val t0 = remember(name) { SystemClock.elapsedRealtime() }
    LaunchedEffect(name) {
        withFrameNanos { }
        Diag.note("Opened $name: " + (SystemClock.elapsedRealtime() - t0) + " ms")
    }
}
