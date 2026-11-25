package com.ipvio.stream

import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Request
import org.json.JSONObject

/**
 * Trakt sign-in, step 2 (T-031): exchanges the one-time code for tokens and stores them.
 *
 * - The exchange uses PKCE: the verifier from [PendingSignIn], no client secret.
 * - Tokens are encrypted with [Secrets]. Without a keystore nothing is stored (no plain-text fallback).
 * - Tokens live in their own preferences file, which is not in the backup list (Backup.kt, NAMES).
 * - Token host and response fields are **unverified** against the Trakt documentation.
 * - Nothing here logs tokens or codes.
 */
object TraktAuth {
    enum class Outcome { OK, NO_KEYSTORE, NETWORK, REJECTED }

    private const val PREFS = "ipvio_trakt"
    private const val KEY = "tokens"

    /** Exchanges [code] for tokens and stores them when the keystore is available. */
    fun exchange(context: Context, code: String, verifier: String, nowMs: Long = System.currentTimeMillis()): Outcome {
        val form = TraktLogic.tokenBody(code, verifier, BuildConfig.TRAKT_CLIENT_ID.trim(), TraktLogic.REDIRECT_URI)
        // JSON body, as the Trakt clients send it (trakt.tv for Node, trakt.py)
        val body = TraktSyncLogic.exchangeBody(form).toRequestBody("application/json".toMediaType())
        val raw: String = try {
            Net.base.newCall(
                Request.Builder().url(TraktLogic.TOKEN_URL).header("User-Agent", "IPVIO Stream/" + BuildConfig.VERSION_NAME).post(body).build()
            ).execute().use { r ->
                if (!r.isSuccessful) return Outcome.REJECTED
                r.body?.string().orEmpty()
            }
        } catch (e: Exception) {
            return Outcome.NETWORK
        }
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return Outcome.REJECTED
        val tokens = TraktLogic.tokensFrom(
            access = json.optString("access_token"),
            refresh = json.optString("refresh_token"),
            expiresInSeconds = json.optLong("expires_in", 0L),
            nowMs = nowMs,
        ) ?: return Outcome.REJECTED
        return if (save(context, tokens)) Outcome.OK else Outcome.NO_KEYSTORE
    }

    /** Encrypts and stores the tokens. Returns false (and stores nothing) when encryption is not possible. */
    fun save(context: Context, tokens: TraktLogic.Tokens): Boolean {
        val plain = JSONObject()
            .put("access", tokens.access)
            .put("refresh", tokens.refresh)
            .put("expiresAt", tokens.expiresAtMs)
            .toString()
        val enc = Secrets.encrypt(plain) ?: return false
        return prefs(context).edit().putString(KEY, enc).commit()
    }

    /** Reads and decrypts the stored tokens, or null when there are none or they cannot be read. */
    fun load(context: Context): TraktLogic.Tokens? {
        val enc = prefs(context).getString(KEY, null) ?: return null
        val plain = Secrets.decrypt(enc) ?: return null
        return runCatching {
            val o = JSONObject(plain)
            TraktLogic.Tokens(o.getString("access"), o.getString("refresh"), o.getLong("expiresAt"))
        }.getOrNull()
    }

    fun isConnected(context: Context): Boolean = load(context) != null

    /** Sign-out: removes the stored tokens. */
    fun signOut(context: Context): Boolean = prefs(context).edit().remove(KEY).commit()

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
