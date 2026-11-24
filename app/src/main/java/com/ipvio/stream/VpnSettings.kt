package com.ipvio.stream

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * T-049e: Settings > General > VPN: import a WireGuard file, connect, disconnect, remove.
 * T-050a: the file can also be sent from the phone over the Wi-Fi (same small page as the playlist import).
 * Some devices have no file picker: then the picker button shows a hint instead of closing the app.
 */
@Composable
internal fun VpnSection(ctx: Context) {
    val scope = rememberCoroutineScope()
    var info by remember { mutableStateOf<String?>(null) }
    // Recomputed whenever a configuration is stored or removed (also from the phone)
    val has = remember(AppVpn.configVersion) { AppVpn.hasConfig(ctx) }
    var noPicker by remember { mutableStateOf(false) }

    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK) scope.launch { AppVpn.connect(ctx) } else info = "Die VPN-Berechtigung wurde nicht erteilt."
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val text = withContext(Dispatchers.IO) {
                runCatching {
                    ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { r ->
                        // Read until the end, but never more than one byte over the limit (the check then refuses the file)
                        val sb = StringBuilder()
                        val buf = CharArray(4096)
                        while (sb.length <= VpnLogic.MAX_BYTES) {
                            val n = r.read(buf)
                            if (n < 0) break
                            sb.append(buf, 0, n)
                        }
                        sb.toString()
                    }
                }.getOrNull()
            }
            info = AppVpn.save(ctx, text.orEmpty()) ?: "Konfiguration gespeichert."
        }
    }

    // WLAN import: the phone sends the file, the TV stores it (callback runs on the server thread, so post to the main thread)
    var serving by remember { mutableStateOf(false) }
    val ip = remember { PhoneImport.localIp() }
    val server = remember {
        PhoneImport { }.apply {
            onVpn = { text -> Handler(Looper.getMainLooper()).post { info = AppVpn.save(ctx, text) ?: "Konfiguration gespeichert." } }
        }
    }
    DisposableEffect(Unit) { onDispose { server.stop() } }

    SectionTitle("VPN")
    Hint("Verbindet das Gerät über WireGuard mit dem Server deines VPN-Anbieters, auch für den Videostream. Lade bei deinem Anbieter eine WireGuard-Konfigurationsdatei (.conf) herunter und wähle sie hier aus. Der Schlüssel wird verschlüsselt gespeichert und nie angezeigt. Es gibt keinen Kill-Switch: bricht die Verbindung ab, läuft der Verkehr ohne VPN weiter. Ob das VPN auf deinem Gerät läuft, ist nicht geprüft.")
    Hint("Oder die Datei vom Handy über das WLAN senden.")
    if (!has) {
        PillButton("WireGuard-Datei auswählen", light = true) {
            try { pick.launch(arrayOf("*/*")) } catch (e: Exception) { noPicker = true }
        }
    } else {
        AppVpn.server(ctx)?.let { Text("Server: $it", color = Muted, fontSize = 15.sp, raw = true) }
        val st = AppVpn.state
        StatusLine(
            when (st) {
                AppVpn.St.ON -> "VPN verbunden"
                AppVpn.St.CONNECTING -> "Verbinde …"
                AppVpn.St.ERROR -> AppVpn.message ?: "Fehler"
                AppVpn.St.OFF -> "VPN aus"
            }
        )
        if (st == AppVpn.St.ON || st == AppVpn.St.CONNECTING) {
            PillButton("VPN trennen") { scope.launch { AppVpn.disconnect(ctx) } }
        } else {
            PillButton("VPN verbinden", light = true) {
                val i = AppVpn.consentIntent(ctx)
                if (i == null) scope.launch { AppVpn.connect(ctx) } else consent.launch(i)
            }
        }
        PillButton("Andere Datei auswählen") {
            try { pick.launch(arrayOf("*/*")) } catch (e: Exception) { noPicker = true }
        }
        ConfirmPill("VPN-Konfiguration löschen") { scope.launch { AppVpn.disconnect(ctx); AppVpn.remove(ctx); info = null } }
    }
    if (noPicker) Text("Auf diesem Gerät gibt es keine Dateiauswahl. Nutze den Import im WLAN.", color = Color(0xFFFFB86B), fontSize = 15.sp)

    if (!serving) PillButton("Import im WLAN starten", icon = AIcon.Phone) { serving = server.start() }
    else {
        Column(
            Modifier.clip(RoundedCornerShape(DS.RadiusM)).background(Color(0x14FFFFFF)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (ip == null) Text("Das Gerät ist nicht mit einem WLAN verbunden.", color = Color(0xFFFFB86B), fontSize = 16.sp)
            else {
                Text("Am Handy (im selben WLAN) öffnen:", color = Muted, fontSize = 15.sp)
                val addr = "http://$ip:${server.port}/"
                Text(addr, color = Accent2, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, raw = true)
                QrWithLabel(addr)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Code", color = Muted, fontSize = 16.sp)
                    Text(server.code, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, raw = true)
                }
                Text("Am Handy „WireGuard-Konfiguration (VPN)“ wählen, den Code eingeben und die .conf-Datei hochladen.", color = Muted, fontSize = 14.sp)
            }
            PillButton("Import beenden") { server.stop(); serving = false }
        }
    }
    StatusLine(info)
}
