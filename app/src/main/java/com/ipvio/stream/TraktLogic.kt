package com.ipvio.stream

import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Pure logic for the Trakt sign-in with PKCE (T-031). No Android imports, so JVM tests can cover it.
 *
 * Trakt issues no client secret for this app. The sign-in uses the authorization code flow with PKCE (S256):
 * a one-time code verifier stays on the device, its SHA-256 hash (the challenge) goes to the authorize URL,
 * and the token exchange sends the verifier. Redirect URIs must be https (see docs/tasks/T-031).
 *
 * Source: Trakt PKCE guide (https://developer.trakt.tv/docs/pkce). The token host below is **unverified**.
 */
object TraktLogic {
    const val AUTHORIZE_URL = "https://auth.trakt.tv/oauth/authorize"
    /** Redirect URI registered in the Trakt app: the static helper page on GitHub Pages (branch gh-pages). */
    const val REDIRECT_URI = "https://emrodev.github.io/IPVIO-Stream/trakt-callback/"
    /** Token endpoint host (**unverified** here: the PKCE page names the path /oauth/token only). */
    const val TOKEN_URL = "https://api.trakt.tv/oauth/token"

    private const val VERIFIER_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
    private const val B64URL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
    private const val SAFETY_MARGIN_MS = 60_000L

    /** A verifier is 43 to 128 characters from the unreserved set (RFC 7636). */
    fun isValidVerifier(verifier: String): Boolean =
        verifier.length in 43..128 && verifier.all { it in VERIFIER_CHARS }

    /** New random verifier for one sign-in. Keep it only for that sign-in and discard it afterwards. */
    fun newVerifier(random: SecureRandom = SecureRandom(), length: Int = 64): String {
        require(length in 43..128) { "PKCE verifier length must be 43 to 128" }
        val sb = StringBuilder(length)
        repeat(length) { sb.append(VERIFIER_CHARS[random.nextInt(VERIFIER_CHARS.length)]) }
        return sb.toString()
    }

    /** S256 challenge: base64url (no padding) of SHA-256 over the ASCII verifier. */
    fun challenge(verifier: String): String =
        base64UrlNoPad(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII)))

    /** Authorize URL with PKCE parameters. [state] protects against forged callbacks; keep it per sign-in. */
    fun authorizeUrl(clientId: String, redirectUri: String, challenge: String, state: String): String =
        AUTHORIZE_URL + "?" + listOf(
            "response_type" to "code",
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "code_challenge" to challenge,
            "code_challenge_method" to "S256",
            "state" to state,
        ).joinToString("&") { (k, v) -> enc(k) + "=" + enc(v) }

    /** Form body for the token exchange. No client_secret is sent. */
    fun tokenBody(code: String, verifier: String, clientId: String, redirectUri: String): Map<String, String> = linkedMapOf(
        "grant_type" to "authorization_code",
        "code" to code,
        "code_verifier" to verifier,
        "client_id" to clientId,
        "redirect_uri" to redirectUri,
    )

    /** Only https redirect URIs are accepted: no custom schemes, no localhost, no urn:ietf:wg:oauth:2.0:oob. */
    fun isHttpsRedirect(uri: String): Boolean =
        uri.startsWith("https://") && uri.length > "https://".length && uri.none { it.isWhitespace() }

    /** A client ID is usable when it is not blank and contains no whitespace. */
    fun hasClientId(clientId: String): Boolean =
        clientId.isNotBlank() && clientId.none { it.isWhitespace() }

    /** Expiry time of the access token, with a 60-second safety margin so a token is renewed before it fails. */
    fun accessExpiresAt(issuedMs: Long, expiresInSeconds: Long): Long =
        issuedMs + expiresInSeconds * 1000 - SAFETY_MARGIN_MS

    /** True while the stored access token can still be used without renewal. */
    fun isAccessValid(nowMs: Long, expiresAtMs: Long): Boolean = nowMs < expiresAtMs

    /** Random `state` for one sign-in (16 bytes, base64url). Compare with [stateMatches]. */
    fun newState(random: SecureRandom = SecureRandom()): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return base64UrlNoPad(bytes)
    }

    /** Compares the `state` returned by Trakt with the expected one in constant time. Null or empty never matches. */
    fun stateMatches(expected: String, returned: String?): Boolean {
        if (returned.isNullOrEmpty() || expected.isEmpty()) return false
        if (returned.length != expected.length) return false
        var diff = 0
        for (i in expected.indices) diff = diff or (expected[i].code xor returned[i].code)
        return diff == 0
    }

    /** Tokens as the app keeps them. The refresh token is kept so the sign-in does not have to be repeated. */
    data class Tokens(val access: String, val refresh: String, val expiresAtMs: Long)

    /** Builds tokens from a token response, or null when a field is missing or the lifetime is not positive. */
    fun tokensFrom(access: String, refresh: String, expiresInSeconds: Long, nowMs: Long): Tokens? {
        if (access.isBlank() || refresh.isBlank() || expiresInSeconds <= 0L) return null
        return Tokens(access, refresh, accessExpiresAt(nowMs, expiresInSeconds))
    }

    /** Splits the "code|state" text copied from the helper page. Null when a part is missing or has unexpected characters. */
    fun parseCallback(text: String): Pair<String, String>? {
        val parts = text.trim().split('|')
        if (parts.size != 2) return null
        val code = parts[0]
        val state = parts[1]
        if (code.length !in 8..512 || state.length !in 8..128) return null
        if (code.any { it.isWhitespace() } || state.any { it.isWhitespace() }) return null
        return code to state
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    /** Standard base64url alphabet without padding (RFC 4648 section 5). Written out because minSdk 24 lacks java.util.Base64. */
    fun base64UrlNoPad(bytes: ByteArray): String {
        val sb = StringBuilder()
        var i = 0
        while (i + 2 < bytes.size) {
            val n = ((bytes[i].toInt() and 0xff) shl 16) or ((bytes[i + 1].toInt() and 0xff) shl 8) or (bytes[i + 2].toInt() and 0xff)
            sb.append(B64URL[n shr 18 and 63]).append(B64URL[n shr 12 and 63]).append(B64URL[n shr 6 and 63]).append(B64URL[n and 63])
            i += 3
        }
        when (bytes.size - i) {
            1 -> {
                val n = (bytes[i].toInt() and 0xff) shl 16
                sb.append(B64URL[n shr 18 and 63]).append(B64URL[n shr 12 and 63])
            }
            2 -> {
                val n = ((bytes[i].toInt() and 0xff) shl 16) or ((bytes[i + 1].toInt() and 0xff) shl 8)
                sb.append(B64URL[n shr 18 and 63]).append(B64URL[n shr 12 and 63]).append(B64URL[n shr 6 and 63])
            }
        }
        return sb.toString()
    }
}

/**
 * One sign-in attempt. The verifier and state live only in memory and only until they expire.
 * Nothing here is written to storage, logs or backups.
 */
class PendingSignIn(
    val verifier: String,
    val state: String,
    private val startedMs: Long,
    private val ttlMs: Long = DEFAULT_TTL_MS,
) {
    fun expired(nowMs: Long): Boolean = nowMs >= startedMs + ttlMs

    /** True when the attempt is still open and the returned state matches. */
    fun accepts(returnedState: String?, nowMs: Long): Boolean =
        !expired(nowMs) && TraktLogic.stateMatches(state, returnedState)

    companion object {
        /** Ten minutes: enough to sign in on the phone, short enough that a forgotten attempt expires. */
        const val DEFAULT_TTL_MS = 600_000L
    }
}

