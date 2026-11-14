package com.ipvio.stream

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Pure logic for the Google Drive sync (W-010): file names, Drive times, sign-in poll answers. No Android here. */
object DriveLogic {
    /** One file per device in the hidden app folder, so no device overwrites another one. */
    const val PREFIX = "ipvio_sync_"
    const val SUFFIX = ".json"
    /** The single backup file of the old Drive backup (1.0 to 1.1); still offered for restore. */
    const val LEGACY_NAME = "ipvio_backup.json"

    private val ID = Regex("^[A-Za-z0-9]{1,32}$")

    fun fileName(deviceId: String): String {
        require(ID.matches(deviceId)) { "bad device id" }
        return PREFIX + deviceId + SUFFIX
    }

    fun isSyncFile(name: String): Boolean =
        name == LEGACY_NAME || (name.startsWith(PREFIX) && name.endsWith(SUFFIX) && ID.matches(deviceIdOf(name) ?: ""))

    /** Device id from a sync file name, null for the old single file or other names. */
    fun deviceIdOf(name: String): String? =
        if (name.startsWith(PREFIX) && name.endsWith(SUFFIX)) name.removePrefix(PREFIX).removeSuffix(SUFFIX).ifEmpty { null } else null

    /** Drive query for the file list (names are fixed, nothing from the user goes into it). */
    fun listQuery(): String = "name contains '$PREFIX' or name = '$LEGACY_NAME'"

    /** Drive time "2026-11-14T10:15:30.123Z" (also without milliseconds) in ms, 0 = unreadable. Works on API 24. */
    fun parseTime(s: String?): Long {
        if (s.isNullOrBlank()) return 0L
        for (p in listOf("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", "yyyy-MM-dd'T'HH:mm:ss'Z'")) {
            val f = SimpleDateFormat(p, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC"); isLenient = false }
            val pos = ParsePosition(0)
            val d = f.parse(s, pos)
            if (d != null && pos.index == s.length) return d.time
        }
        return 0L
    }

    enum class Poll { WAIT, SLOWER, DENIED, EXPIRED, FAILED }

    /** Meaning of the "error" field while waiting for the user's consent on the phone (OAuth device flow). */
    fun pollAnswer(error: String): Poll = when (error) {
        "authorization_pending" -> Poll.WAIT
        "slow_down" -> Poll.SLOWER
        "access_denied" -> Poll.DENIED
        "expired_token" -> Poll.EXPIRED
        else -> Poll.FAILED
    }
}
