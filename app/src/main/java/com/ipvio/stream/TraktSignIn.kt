package com.ipvio.stream

import android.content.Context

/**
 * The TV side of the phone sign-in (T-031 step 3). Keeps the one pending sign-in in memory only.
 * Starting a new sign-in replaces the old one. Finishing always consumes it, so the same verifier is never used twice.
 */
object TraktSignIn {
    @Volatile private var pending: PendingSignIn? = null

    /** Starts a sign-in and returns the Trakt authorize URL, or null when no client ID is built in. */
    fun start(nowMs: Long = System.currentTimeMillis()): String? {
        val clientId = BuildConfig.TRAKT_CLIENT_ID.trim()
        if (!TraktLogic.hasClientId(clientId)) return null
        val verifier = TraktLogic.newVerifier()
        val state = TraktLogic.newState()
        pending = PendingSignIn(verifier, state, nowMs)
        return TraktLogic.authorizeUrl(clientId, TraktLogic.REDIRECT_URI, TraktLogic.challenge(verifier), state)
    }

    /** Finishes the sign-in with the code and state the phone sent. Always consumes the pending sign-in. */
    fun finish(context: Context, code: String, returnedState: String, nowMs: Long = System.currentTimeMillis()): TraktAuth.Outcome {
        val p = pending ?: return TraktAuth.Outcome.REJECTED
        pending = null
        if (!p.accepts(returnedState, nowMs)) return TraktAuth.Outcome.REJECTED
        val r = TraktAuth.exchange(context, code, p.verifier, nowMs)
        // Step 4/5: take history and watchlist into the app right after a successful sign-in
        if (r == TraktAuth.Outcome.OK) TraktSync.syncAsync(context)
        return r
    }

    fun cancel() { pending = null }
}
