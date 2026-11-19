package com.ipvio.stream

import java.net.URI

/**
 * Decision rules for a provider host whose HTTPS certificate does not match its address (#69).
 * No silent downgrade: plain http is used only for a host the user has agreed to. Pure logic, no Android, testable.
 */
object TlsFallbackLogic {
    /** What a connection should do with an https address after a certificate check. */
    enum class Decision {
        /** Not an https address, or the certificate matches: connect as requested. */
        USE_AS_IS,
        /** Certificate does not match and the user has agreed for this host: use the same address over http. */
        USE_HTTP,
        /** Certificate does not match and there is no agreement yet: ask the user, do not connect over http. */
        ASK_CONSENT,
    }

    /** Lower-case host of an https address, null for anything else. */
    fun httpsHost(url: String): String? {
        val u = parse(url) ?: return null
        if (!u.scheme.equals("https", ignoreCase = true)) return null
        return u.host?.lowercase()
    }

    /** https address -> same address over http. Null if [url] is not an https address. */
    fun downgrade(url: String): String? {
        val u = parse(url) ?: return null
        if (!u.scheme.equals("https", ignoreCase = true)) return null
        val port = if (u.port == -1 || u.port == 443) -1 else u.port
        return URI("http", u.userInfo, u.host, port, u.rawPath, u.rawQuery, u.rawFragment).toString()
    }

    /**
     * [mismatch] = the certificate error is "certificate does not match the host name".
     * [agreed] = hosts the user has already agreed to (lower-case).
     */
    fun decide(url: String, mismatch: Boolean, agreed: Set<String>): Decision {
        val host = httpsHost(url) ?: return Decision.USE_AS_IS
        if (!mismatch) return Decision.USE_AS_IS
        return if (host in agreed) Decision.USE_HTTP else Decision.ASK_CONSENT
    }

    /** Agreed hosts plus the host of [url]. Unchanged if [url] is not https. */
    fun agree(agreed: Set<String>, url: String): Set<String> =
        httpsHost(url)?.let { agreed + it } ?: agreed

    private fun parse(url: String): URI? = try { URI(url.trim()) } catch (e: Exception) { null }
}
