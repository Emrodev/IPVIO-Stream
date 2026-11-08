package com.ipvio.stream

import java.io.ByteArrayOutputStream
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.security.SecureRandom
import kotlin.concurrent.thread

/** Local one-use HTTP upload endpoint. Its lifetime is owned by the Settings page composition. */
class VpnPhonePairing(private val onProfile: (String) -> Unit) {
    @Volatile private var code = newCode()
    @Volatile private var server: ServerSocket? = null
    @Volatile private var used = false
    @Volatile private var failedAttempts = 0
    @Volatile var port: Int = 0
        private set
    fun start(): Boolean {
        if (server != null) return true
        val ss = (8765..8775).firstNotNullOfOrNull { runCatching { ServerSocket(it) }.getOrNull() } ?: return false
        server = ss; port = ss.localPort
        thread(isDaemon = true, name = "vpn-profile-pairing") {
            while (!ss.isClosed) {
                val socket = runCatching { ss.accept() }.getOrNull() ?: break
                thread(isDaemon = true, name = "vpn-profile-upload") { runCatching { handle(socket) }; runCatching { socket.close() } }
            }
        }
        return true
    }
    fun stop() { runCatching { server?.close() }; server = null }
    fun restart(): Boolean { stop(); used = false; failedAttempts = 0; code = newCode(); return start() }
    fun accessCode() = code
    private fun newCode() = (100000 + SecureRandom().nextInt(900000)).toString()
    fun localIp(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp && !it.isLoopback }
            .flatMap { it.inetAddresses.toList() }.firstOrNull { it is Inet4Address && it.isSiteLocalAddress }?.hostAddress
    }.getOrNull()
    @Synchronized private fun attemptCode(given: String): Boolean? {
        if (used || failedAttempts >= 5) return false
        if (given != code) { failedAttempts++; return null }
        used = true
        return true
    }
    @Synchronized private fun failedUpload() { if (!used) failedAttempts++ }

    private fun handle(socket: Socket) {
        socket.soTimeout = 15000
        val input = socket.getInputStream()
        val head = ByteArrayOutputStream(); var tail = 0
        while (head.size() < 8192) {
            val b = input.read(); if (b < 0) return
            head.write(b); tail = (tail shl 8) or b
            if (tail == 0x0D0A0D0A) break
        }
        val lines = head.toString("ISO-8859-1").split("\r\n")
        val path = lines.firstOrNull().orEmpty()
        val length = lines.firstOrNull { it.startsWith("Content-Length:", true) }?.substringAfter(':')?.trim()?.toLongOrNull() ?: 0L
        val body = if (path.startsWith("POST ")) {
            if (length !in 1..(140 * 1024).toLong() || used) null else ByteArray(length.toInt()).also { bytes ->
                var offset = 0
                while (offset < bytes.size) { val n = input.read(bytes, offset, bytes.size - offset); if (n < 0) return; offset += n }
            }
        } else null
        val boundary = lines.firstOrNull { it.startsWith("Content-Type:", true) }?.substringAfter("boundary=")?.trim()?.trim('"')
        val uploaded = if (body != null && boundary != null) parseUpload(body, boundary) else null
        val page = when {
            used || failedAttempts >= 5 -> "<h2>This one-use transfer has ended.</h2>"
            body == null -> "<h1>Send a WireGuard profile to your TV</h1><p>Enter the one-use code shown on the TV and choose a .conf file (maximum 128 KB).</p><form method='post' enctype='multipart/form-data'><input name='code' placeholder='One-use code' required><input name='profile' type='file' accept='.conf,text/plain' required><button>Send profile</button></form>"
            uploaded == null -> { failedUpload(); "<h2>Code or profile was invalid. Return to the TV.</h2>" }
            else -> {
                when (attemptCode(uploaded.first)) {
                    true -> { onProfile(uploaded.second.toString(Charsets.UTF_8)); "<h2>Profile received. Return to the TV to review and save it.</h2>" }
                    null -> "<h2>Code or profile was invalid. Return to the TV.</h2>"
                    else -> "<h2>This one-use transfer has ended.</h2>"
                }
            }
        }
        val bytes = ("<!doctype html><html><meta name='viewport' content='width=device-width'><body style='font:16px system-ui;background:#0b0f19;color:white;padding:22px;max-width:560px'>$page</body></html>").toByteArray()
        val out = socket.getOutputStream()
        out.write("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nContent-Security-Policy: default-src 'none'; style-src 'unsafe-inline'; form-action 'self'; frame-ancestors 'none'\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
        out.write(bytes); out.flush()
    }

    /** Parses one multipart file and its matching code, rejecting malformed or oversized requests. */
    private fun parseUpload(body: ByteArray, boundary: String): Pair<String, ByteArray>? = runCatching {
        val data = body.toString(Charsets.ISO_8859_1)
        val parts = data.split("--$boundary")
        var entered = ""; var file: ByteArray? = null
        for (part in parts) {
            val split = part.indexOf("\r\n\r\n"); if (split < 0) continue
            val headers = part.substring(0, split)
            val value = part.substring(split + 4).removeSuffix("\r\n").toByteArray(Charsets.ISO_8859_1)
            if (headers.contains("name=\"code\"")) entered = value.toString(Charsets.UTF_8).trim()
            if (headers.contains("name=\"profile\"")) file = value
        }
        val bytes = file ?: return null
        require(bytes.size in 1..128 * 1024)
        entered to bytes
    }.getOrNull()
}
