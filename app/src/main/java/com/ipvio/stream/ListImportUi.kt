package com.ipvio.stream

import android.content.Context
import android.os.Handler
import android.os.Looper
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Settings > Import list: bring a Letterboxd or MyAnimeList export into the app.
 * Two ways: pick the file on this device, or upload it from the phone over the Wi-Fi (small web page, same as the transfer page).
 */
@Composable
internal fun ListImportPage(ctx: Context) {
    Hint("Hole deine Listen von Letterboxd, MyAnimeList in die App. Die Titel werden mit deiner Playlist abgeglichen: Merkliste und Gesehen werden übernommen.")
    Hint("Letterboxd: ZIP oder CSV · MyAnimeList: XML")
    val pick = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = uri.lastPathSegment ?: "list"
        val bytes = runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
        if (bytes != null && bytes.size <= 16 * 1024 * 1024) ListImportApply.run(ctx, name, bytes)
    }
    var noPicker by remember { mutableStateOf(false) }
    PillButton("Datei vom Gerät wählen", light = true, icon = AIcon.Download) {
        try { pick.launch(arrayOf("*/*")) } catch (e: Exception) { noPicker = true }
    }
    if (noPicker) Text("Auf diesem Gerät gibt es keine Dateiauswahl. Nutze den Import im WLAN.", color = Color(0xFFFFB86B), fontSize = 15.sp)

    var serving by remember { mutableStateOf(false) }
    // Which panel the running server shows: the list upload or the Trakt sign-in (own button)
    var traktMode by remember { mutableStateOf(false) }
    val ip = remember { PhoneImport.localIp() }
    val server = remember {
        PhoneImport { }.apply {
            uploadDir = ImportFiles.tmpDir(ctx)
            onList = { name, bytes -> Handler(Looper.getMainLooper()).post { ListImportApply.run(ctx, name, bytes) } }
            // Trakt sign-in (T-031): same server, address /trakt
            traktStart = { TraktSignIn.start() }
            traktFinish = { code, state -> TraktSignIn.finish(ctx, code, state) }
        }
    }
    // Read again after a sign-in (the server stops), after a sync and after disconnecting
    var traktVer by remember { mutableStateOf(0) }
    val traktConnected = remember(serving, traktVer, TraktSync.running) { TraktAuth.isConnected(ctx) }
    DisposableEffect(Unit) { onDispose { server.stop() } }
    if (!serving) {
        PillButton("Import im WLAN starten", icon = AIcon.Phone) { traktMode = false; serving = server.start() }
        if (!traktConnected) PillButton("Mit Trakt verbinden", icon = AIcon.Person) { traktMode = true; serving = server.start() }
        else TraktSyncPanel(ctx) { traktVer++ }
    } else {
        Column(
            Modifier.clip(RoundedCornerShape(DS.RadiusM)).background(Color(0x14FFFFFF)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (ip == null) Text("Das Gerät ist nicht mit einem WLAN verbunden.", color = Color(0xFFFFB86B), fontSize = 16.sp)
            else {
                Text("Am Handy (im selben WLAN) öffnen:", color = Muted, fontSize = 15.sp)
                if (!traktMode) {
                    val addr = "http://$ip:${server.port}/"
                    Text(addr, color = Accent2, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, raw = true)
                    QrWithLabel(addr)
                } else {
                    Text("Sign in to your Trakt account on the phone, with the same code:", color = Muted, fontSize = 15.sp)
                    val traktAddr = "http://$ip:${server.port}/trakt"
                    Text(traktAddr, color = Accent2, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, raw = true)
                    QrWithLabel(traktAddr)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Code", color = Muted, fontSize = 16.sp)
                    Text(server.code, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, raw = true)
                }
                if (!traktMode) Text("Am Handy „Liste importieren“ wählen, den Code eingeben und die Exportdatei hochladen.", color = Muted, fontSize = 14.sp)
                else Text(if (traktConnected) "Connected to Trakt." else "Not connected to Trakt.", color = Muted, fontSize = 14.sp)
            }
            PillButton("Import beenden") { server.stop(); serving = false }
        }
    }
}

/**
 * T-031 steps 4 and 5: Trakt is connected. Shows when it last synced and what was found, "Sync now" and "Disconnect".
 * The sync only reads from Trakt: watched titles get their check mark, history and watchlist become two categories.
 */
@Composable
private fun TraktSyncPanel(ctx: android.content.Context, onDisconnected: () -> Unit) {
    val running = TraktSync.running
    val last = remember(running) { TraktSync.lastSyncMs(ctx) }
    val summary = remember(running) { TraktSync.lastSummary(ctx) }
    Column(
        Modifier.clip(RoundedCornerShape(DS.RadiusM)).background(Color(0x14FFFFFF)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Mit Trakt verbunden", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Gesehene Titel bekommen ihr Häkchen, Verlauf und Merkliste erscheinen als Kategorien „Trakt“ bei Filme und Serien. Die App ändert nichts in deinem Trakt-Konto.", color = Muted, fontSize = 14.sp)
        when {
            running -> Text("Wird synchronisiert …", color = Accent2, fontSize = 15.sp)
            TraktSync.status.isNotEmpty() -> Text(TraktSync.status, color = Muted, fontSize = 15.sp)
            last > 0 -> Text(summary, color = Muted, fontSize = 15.sp)
        }
        if (last > 0 && !running) Text("Zuletzt synchronisiert: {0}".replace("{0}", dayLabel(last / 1000) + " " + hhmm(last / 1000)), color = Muted, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton("Jetzt synchronisieren", icon = AIcon.Refresh) { TraktSync.syncAsync(ctx) }
            PillButton("Trakt trennen", light = false) { TraktSync.disconnect(ctx); onDisconnected() }
        }
    }
}
