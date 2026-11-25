package com.ipvio.stream

/**
 * Pure logic for the Trakt device login (T-031). No Android imports, so JVM tests can cover it.
 *
 * The status codes and the 5-second back-off follow the usual device-flow rules (RFC 8628 style).
 * Trakt's own codes are **unverified** against the official Trakt documentation; check them before relying on them.
 */
object TraktLogic {
    /** What the next step of the device login is after one poll of the token endpoint. */
    enum class PollAction { WAIT, SLOW_DOWN, DONE, FAILED }

    /** Maps the HTTP status of a token poll to the next step. */
    fun pollAction(httpStatus: Int): PollAction = when (httpStatus) {
        200 -> PollAction.DONE
        400 -> PollAction.WAIT // authorization still pending: the user has not confirmed the code yet
        429 -> PollAction.SLOW_DOWN // polled too often: wait longer
        else -> PollAction.FAILED // unknown code, already used, expired or denied
    }

    /** Seconds to wait before the next poll. A slow_down answer adds 5 seconds, never below 1 second. */
    fun nextIntervalSeconds(currentSeconds: Int, action: PollAction): Int {
        val base = currentSeconds.coerceAtLeast(1)
        return if (action == PollAction.SLOW_DOWN) base + 5 else base
    }

    /** True when the device code has run out. [startedMs] is when the code was requested. */
    fun codeExpired(startedMs: Long, expiresInSeconds: Long, nowMs: Long): Boolean =
        nowMs >= startedMs + expiresInSeconds * 1000

    /** Expiry time of the access token, with a 60-second safety margin so a token is renewed before it fails. */
    fun accessExpiresAt(issuedMs: Long, expiresInSeconds: Long): Long =
        issuedMs + expiresInSeconds * 1000 - SAFETY_MARGIN_MS

    /** True while the stored access token can still be used without renewal. */
    fun isAccessValid(nowMs: Long, expiresAtMs: Long): Boolean = nowMs < expiresAtMs

    /** A client ID is usable when it is not blank and contains no whitespace. */
    fun hasClientId(clientId: String): Boolean =
        clientId.isNotBlank() && clientId.none { it.isWhitespace() }

    private const val SAFETY_MARGIN_MS = 60_000L
}
