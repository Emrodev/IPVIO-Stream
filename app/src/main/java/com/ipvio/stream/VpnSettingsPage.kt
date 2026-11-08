package com.ipvio.stream

import android.content.Context
import android.net.VpnService
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.ByteArrayOutputStream

@Composable
fun VpnSettingsPage(ctx: Context) {
    VpnController.init(ctx)
    LaunchedEffect(Unit) {
        while (true) { VpnController.refreshDiagnostics(); kotlinx.coroutines.delay(2000) }
    }
    var showPaste by remember { mutableStateOf(false) }
    var profileText by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var deletePrompt by remember { mutableStateOf(false) }
    var serving by remember { mutableStateOf(false) }
    val pairing = remember {
        VpnPhonePairing { value ->
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                profileText = value; label = "Phone WireGuard profile"; showPaste = true
            }
        }
    }
    DisposableEffect(pairing) { onDispose { pairing.stop() } }
    val chooser = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val bytes = ctx.contentResolver.openInputStream(uri)?.use { input ->
                    val out = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var total = 0
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        require(total <= 128 * 1024) { "Profile must be smaller than 128 KB." }
                        out.write(buffer, 0, read)
                    }
                    out.toByteArray()
                } ?: error("Could not read that file.")
                profileText = bytes.toString(Charsets.UTF_8)
                label = (uri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "WireGuard profile").take(48)
                showPaste = true
                error = ""
            } catch (e: Exception) { error = e.message?.take(180) ?: "Could not read that file." }
        }
    }
    val consent = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) VpnController.connect(ctx) else error = "VPN permission was not granted."
    }
    fun requestConnect() {
        error = ""
        val intent = VpnService.prepare(ctx)
        if (intent == null) VpnController.connect(ctx) else consent.launch(intent)
    }

    val connected = VpnController.status.state == "Connected"
    Row(
        Modifier.fillMaxWidth().background(if (connected) Color(0x1A38D878) else Color(0x12FFFFFF), RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(10.dp).background(if (connected) Color(0xFF49D17D) else Muted, RoundedCornerShape(50)))
        Text(VpnController.status.state, color = if (connected) Color(0xFF49D17D) else Muted, fontSize = 19.sp, fontWeight = FontWeight.Bold)
    }
    SectionTitle("VPN")
    Text("Routes this app's traffic through the selected WireGuard profile. The package allowlist excludes other Android apps. Traffic within this app is not routed separately, and no fail-closed behavior is provided.", color = Muted, fontSize = 15.sp)
    Spacer(Modifier.height(4.dp))
    Text("Profile", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
    Text(if (VpnProfiles.hasProfile(ctx)) VpnProfiles.label(ctx) else "No profile saved", color = if (VpnProfiles.hasProfile(ctx)) Accent2 else Muted, fontSize = 16.sp)
    if (showPaste) {
        Text("Paste or review a standard WireGuard .conf. The text stays here only until you save or cancel.", color = Muted, fontSize = 14.sp)
        BasicTextField(value = profileText, onValueChange = { if (it.toByteArray().size <= 128 * 1024) profileText = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 240.dp).background(Color(0xFF10141D), RoundedCornerShape(12.dp)).border(1.dp, Color(0x334B5563), RoundedCornerShape(12.dp)).padding(12.dp), textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton("Save encrypted profile") {
                try {
                    VpnProfiles.save(ctx, label, profileText)
                    profileText = ""; label = ""; showPaste = false; error = ""
                    Toast.makeText(ctx, "WireGuard profile saved", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) { error = "Invalid WireGuard profile. Check its format and required fields." }
            }
            PillButton("Cancel") { profileText = ""; label = ""; showPaste = false }
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton("Choose .conf file") {
                try { chooser.launch(arrayOf("*/*")) }
                catch (_: android.content.ActivityNotFoundException) { error = "This device does not have a file picker. Use clipboard paste or phone transfer." }
            }
            PillButton("Paste from clipboard") {
                val clip = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                val value = clip?.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(ctx)?.toString().orEmpty()
                if (value.isBlank()) error = "Clipboard is empty." else if (value.toByteArray().size > 128 * 1024) error = "Profile must be smaller than 128 KB." else {
                    profileText = value; label = "Pasted WireGuard profile"; showPaste = true; error = ""
                }
            }
            if (VpnProfiles.hasProfile(ctx)) PillButton("Delete profile") { deletePrompt = true }
        }
        SectionTitle("Transfer from phone")
        if (!serving) PillButton("Open one-use phone pairing") { serving = pairing.restart(); if (!serving) error = "Could not open the local transfer page." }
        else {
            val ip = remember { pairing.localIp() }
            if (ip == null) Text("No local Wi-Fi address is available. Connect the TV and phone to the same network.", color = Color(0xFFFFB86B), fontSize = 14.sp)
            else {
                val address = "http://$ip:${pairing.port}/"
                Text("On the phone, open (same local network):", color = Muted, fontSize = 14.sp)
                Text(address, color = Accent2, fontSize = 21.sp, fontWeight = FontWeight.Bold, raw = true)
                QrWithLabel(address)
                Text("One-use code: ${pairing.accessCode()}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, raw = true)
                Text("This local transfer uses HTTP, not HTTPS. Anyone on the same network may observe or alter the upload. The page stops when you leave settings or end transfer. Uploads are limited to 128 KB.", color = Color(0xFFFFB86B), fontSize = 14.sp)
            }
            PillButton("End phone transfer") { pairing.stop(); serving = false }
        }
    }
    if (error.isNotBlank()) Text(error, color = Color(0xFFFF8585), fontSize = 14.sp)
    if (VpnController.status.endpoint != "—") Text("Endpoint: ${VpnController.status.endpoint}", color = Muted, fontSize = 14.sp)
    if (VpnController.status.error.isNotBlank()) Text(VpnController.status.error, color = Color(0xFFFF8585), fontSize = 14.sp)
    val tunnelBusy = VpnController.status.state == "Connecting" || VpnController.status.state == "Disconnecting"
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PillButton(when { connected -> "Disconnect"; tunnelBusy -> VpnController.status.state; else -> "Connect" }) {
            when {
                tunnelBusy -> Unit
                connected -> VpnController.disconnect(ctx)
                !VpnProfiles.hasProfile(ctx) -> error = "Import a profile first."
                else -> requestConnect()
            }
        }
    }
    ToggleRow("Bypass private and local networks", "On by default. Excludes private destination prefixes from runtime routes; the saved profile is unchanged. This is destination-based routing.", VpnController.bypassPrivate) {
        VpnController.updateBypassPrivate(it)
        if (connected) VpnController.disconnect(ctx) { requestConnect() }
    }
    val consentGranted = VpnService.prepare(ctx) == null
    ToggleRow("Connect when the app launches", "Starts this profile when the app opens. Requires a saved profile and prior Android VPN consent. This is not device boot autostart.", VpnController.connectOnLaunch) { enabled ->
        if (!enabled) VpnController.updateConnectOnLaunch(false)
        else if (!VpnProfiles.hasProfile(ctx)) error = "Save a profile before enabling this option."
        else if (!consentGranted) error = "Connect once and grant Android VPN permission first."
        else VpnController.updateConnectOnLaunch(true)
    }
    SectionTitle("Diagnostics")
    Text("Tunnel: ${VpnController.status.state} · Endpoint: ${VpnController.status.endpoint}", color = Muted, fontSize = 14.sp)
    Text("Received ${SettingsLogic.fmtBytes(VpnController.status.rx)} · Sent ${SettingsLogic.fmtBytes(VpnController.status.tx)}", color = Muted, fontSize = 14.sp)
    val handshake = VpnController.status.handshakeMs
    Text(if (handshake <= 0) "Latest handshake: none yet" else "Latest handshake: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(handshake))}", color = Muted, fontSize = 14.sp)
    Text("Counters and handshake are tunnel health signals, not proof that every stream is reachable. Keys and full configuration are never shown here.", color = Muted, fontSize = 14.sp)
    Text("Other Android apps are excluded by the WireGuard package allowlist. All traffic within this app shares the same app VPN.", color = Muted, fontSize = 14.sp)

    if (deletePrompt) {
        SectionTitle("Delete WireGuard profile?")
        Text("This removes the encrypted profile and disables connect on app launch.", color = Muted, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton("Delete") { VpnController.disconnect(ctx) { VpnProfiles.delete(ctx); VpnController.updateConnectOnLaunch(false); deletePrompt = false } }
            PillButton("Keep") { deletePrompt = false }
        }
    }
}