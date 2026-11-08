package com.ipvio.stream

import android.content.Context
import android.net.VpnService
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import java.io.StringReader
import java.io.BufferedReader
import java.math.BigInteger
import java.net.InetAddress
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

/** Profile text is encrypted before it reaches preferences. Only the profile label and encrypted blob persist. */
object VpnProfiles {
    private const val PREFS = "wireguard_profiles"
    private const val BLOB = "profile_blob"
    private const val KEY_ALIAS = "wireguard_profile_v1"
    private const val MAX_BYTES = 128 * 1024

    fun hasProfile(context: Context) = prefs(context).contains(BLOB)
    fun label(context: Context): String = prefs(context).getString("label", "WireGuard profile") ?: "WireGuard profile"

    fun validate(text: String): String {
        require(text.toByteArray(Charsets.UTF_8).size in 1..MAX_BYTES) { "Profile must be smaller than 128 KB." }
        val config = Config.parse(BufferedReader(StringReader(text)))
        require(config.getPeers().isNotEmpty()) { "Add at least one [Peer] section." }
        return text.trim()
    }

    fun save(context: Context, displayName: String, raw: String) {
        val clean = validate(raw)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.iv + cipher.doFinal(clean.toByteArray(Charsets.UTF_8))
        prefs(context).edit().putString(BLOB, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString("label", displayName.take(48).ifBlank { "WireGuard profile" }).apply()
    }

    fun delete(context: Context) {
        prefs(context).edit().clear().apply()
        // The key is shared by the single profile and removing it makes any orphaned blob unrecoverable.
        runCatching { keyStore().deleteEntry(KEY_ALIAS) }
    }

    fun config(context: Context, bypassPrivate: Boolean): Config {
        val encoded = prefs(context).getString(BLOB, null) ?: error("No WireGuard profile is saved.")
        val encrypted = Base64.decode(encoded, Base64.NO_WRAP)
        require(encrypted.size > 12) { "Saved profile cannot be read." }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, encrypted, 0, 12))
        val plain = cipher.doFinal(encrypted, 12, encrypted.size - 12).toString(Charsets.UTF_8)
        val routed = if (bypassPrivate) subtractPrivateRoutes(plain) else plain
        val runtime = appAllowlist(routed, context.packageName)
        return Config.parse(BufferedReader(StringReader(runtime)))
    }

    /** Keep all installed apps outside this VPN by using WireGuard's included-app setting. */
    private fun appAllowlist(text: String, packageName: String): String {
        val lines = text.lines().filterNot { it.trimStart().startsWith("IncludedApplications", true) || it.trimStart().startsWith("ExcludedApplications", true) }
        val out = mutableListOf<String>()
        var inInterface = false
        var inserted = false
        for (line in lines) {
            if (line.trim().startsWith("[")) {
                if (inInterface && !inserted) { out += "IncludedApplications = $packageName"; inserted = true }
                inInterface = line.trim().equals("[Interface]", true)
            }
            out += line
        }
        if (inInterface && !inserted) out += "IncludedApplications = $packageName"
        return out.joinToString("\n")
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun key(): java.security.Key {
        val store = keyStore()
        (store.getKey(KEY_ALIAS, null) as? java.security.Key)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build())
        return generator.generateKey()
    }
    private fun keyStore() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    /** Remove private/local destination ranges from AllowedIPs in the runtime copy only. */
    private fun subtractPrivateRoutes(text: String): String {
        val blocks = listOf("10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16", "169.254.0.0/16", "127.0.0.0/8", "100.64.0.0/10",
            "fc00::/7", "fe80::/10", "::1/128").map(::cidr)
        return text.lineSequence().joinToString("\n") { line ->
            val match = Regex("^(\\s*AllowedIPs\\s*=\\s*)(.*?)(\\s*(?:#.*)?)$", RegexOption.IGNORE_CASE).matchEntire(line) ?: return@joinToString line
            val routes = match.groupValues[2].split(',').map { it.trim() }.filter(String::isNotEmpty)
            val expanded = routes.flatMap { route -> runCatching { subtract(cidr(route), blocks) }.getOrElse { listOf(route) } }
            match.groupValues[1] + expanded.joinToString(", ") + match.groupValues[3]
        }
    }

    private data class Range(val start: BigInteger, val end: BigInteger, val bits: Int)
    private fun cidr(value: String): Range {
        val address = InetAddress.getByName(value.substringBefore('/')).address
        val bits = address.size * 8
        val prefix = value.substringAfter('/', bits.toString()).toInt().coerceIn(0, bits)
        val ip = BigInteger(1, address)
        val size = BigInteger.ONE.shiftLeft(bits - prefix)
        val start = ip.divide(size).multiply(size)
        return Range(start, start + size - BigInteger.ONE, bits)
    }
    private fun subtract(source: Range, blocks: List<Range>): List<String> {
        var parts = listOf(source)
        for (block in blocks.filter { it.bits == source.bits }) {
            parts = parts.flatMap { part ->
                if (block.end < part.start || block.start > part.end) listOf(part) else buildList {
                    if (block.start > part.start) add(part.copy(end = block.start - BigInteger.ONE))
                    if (block.end < part.end) add(part.copy(start = block.end + BigInteger.ONE))
                }
            }
        }
        return parts.flatMap(::toCidrs)
    }
    private fun toCidrs(range: Range): List<String> {
        val out = mutableListOf<String>()
        var start = range.start
        while (start <= range.end) {
            var sizeBits = if (start == BigInteger.ZERO) range.bits else start.lowestSetBit.coerceAtMost(range.bits)
            while (sizeBits > 0 && start + BigInteger.ONE.shiftLeft(sizeBits) - BigInteger.ONE > range.end) sizeBits--
            val bytes = start.toByteArray().takeLast(range.bits / 8).toByteArray().let { ByteArray(range.bits / 8 - it.size) + it }
            out += "${InetAddress.getByAddress(bytes).hostAddress.orEmpty().substringBefore('%')}/${range.bits - sizeBits}"
            start += BigInteger.ONE.shiftLeft(sizeBits)
        }
        return out
    }
}

data class VpnStatus(val state: String = "Disconnected", val endpoint: String = "—", val rx: Long = 0, val tx: Long = 0, val handshakeMs: Long = 0, val error: String = "")

/** Owns the single app-controlled WireGuard tunnel. No profile material is logged or placed in diagnostics. */
object VpnController {
    private const val PREFS = "wireguard_settings"
    private val tunnel = object : Tunnel {
        override fun getName() = "WireGuard"
        override fun onStateChange(newState: Tunnel.State) {
            val label = stateLabel(newState)
            status = status.copy(state = label, error = if (label == "Error") status.error else "")
        }
    }
    var status by mutableStateOf(VpnStatus())
        private set
    var bypassPrivate by mutableStateOf(true)
        private set
    var connectOnLaunch by mutableStateOf(false)
        private set
    private var settings: android.content.SharedPreferences? = null
    private var settingsLoaded = false
    @Volatile private var backend: GoBackend? = null
    @Volatile private var activeConfig: Config? = null

    fun init(context: Context) {
        settings = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!settingsLoaded) {
            bypassPrivate = settings?.getBoolean("bypass_private", true) ?: true
            connectOnLaunch = settings?.getBoolean("connect_on_launch", false) ?: false
            settings?.edit()?.remove("last_error")?.apply()
            settingsLoaded = true
        }
    }
    fun updateBypassPrivate(value: Boolean) {
        bypassPrivate = value
        settings?.edit()?.putBoolean("bypass_private", value)?.apply()
    }
    fun updateConnectOnLaunch(value: Boolean) {
        connectOnLaunch = value
        settings?.edit()?.putBoolean("connect_on_launch", value)?.apply()
    }
    fun consentIntent(context: Context) = VpnService.prepare(context)
    fun connect(context: Context, onDone: (String?) -> Unit = {}) {
        status = status.copy(state = "Connecting", error = "")
        Thread({
            try {
                val config = VpnProfiles.config(context, bypassPrivate)
                val b = backend ?: synchronized(this) { backend ?: GoBackend(context.applicationContext).also { backend = it } }
                val state = b.setState(tunnel, Tunnel.State.UP, config)
                activeConfig = config
                val peer = config.getPeers().firstOrNull()
                val ep = peer?.endpoint?.orElse(null)
                val endpoint = ep?.let { "${it.host}:${it.port}" }?.take(96) ?: "Configured endpoint"
                status = VpnStatus(stateLabel(state), endpoint)
                refreshDiagnostics()
                android.os.Handler(android.os.Looper.getMainLooper()).post { onDone(null) }
            } catch (_: Exception) {
                val message = "Could not start the tunnel. Check the profile and network, then try again."
                status = VpnStatus(state = "Error", error = message)
                android.os.Handler(android.os.Looper.getMainLooper()).post { onDone(message) }
            }
        }, "wireguard-connect").start()
    }
    fun disconnect(context: Context, onDone: () -> Unit = {}) {
        status = status.copy(state = "Disconnecting", error = "")
        Thread({
            runCatching { backend?.setState(tunnel, Tunnel.State.DOWN, null) }
            activeConfig = null
            status = VpnStatus()
            android.os.Handler(android.os.Looper.getMainLooper()).post { onDone() }
        }, "wireguard-disconnect").start()
    }
    fun refreshDiagnostics() {
        val b = backend ?: return
        val config = activeConfig ?: return
        Thread({ runCatching {
            val stats = b.getStatistics(tunnel)
            val firstKey = config.getPeers().firstOrNull()?.publicKey
            val handshake = firstKey?.let { stats.peer(it)?.latestHandshakeEpochMillis() } ?: 0L
            status = status.copy(rx = stats.totalRx(), tx = stats.totalTx(), handshakeMs = handshake)
        } }, "wireguard-diagnostics").start()
    }
    fun maybeConnectAtLaunch(context: Context) {
        init(context)
        if (connectOnLaunch && VpnProfiles.hasProfile(context) && consentIntent(context) == null) connect(context)
    }
    private fun stateLabel(state: Tunnel.State) = when (state) {
        Tunnel.State.UP -> "Connected"
        Tunnel.State.DOWN -> "Disconnected"
        else -> "Connecting"
    }
}
