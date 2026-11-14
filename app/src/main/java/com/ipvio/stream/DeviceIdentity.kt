package com.ipvio.stream

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest
import java.util.UUID

object DeviceIdentity {
    private const val PREFS = "ipvio_device"
    private const val KEY_ID = "device_id"
    private const val KEY_MAC = "device_mac"

    fun getDeviceId(ctx: Context): String {
        val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = sp.getString(KEY_ID, null)
        if (!existing.isNullOrBlank()) return existing

        val androidId = runCatching {
            Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull()

        val id = if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
            androidId
        } else {
            UUID.randomUUID().toString().replace("-", "")
        }

        sp.edit().putString(KEY_ID, id).apply()
        return id
    }

    fun getVirtualMac(ctx: Context): String {
        val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = sp.getString(KEY_MAC, null)
        if (!existing.isNullOrBlank()) return existing

        val devId = getDeviceId(ctx)
        val md = MessageDigest.getInstance("MD5").digest(devId.toByteArray())
        val b1 = String.format("%02X", md[0])
        val b2 = String.format("%02X", md[1])
        val b3 = String.format("%02X", md[2])
        val mac = "00:1A:79:$b1:$b2:$b3"

        sp.edit().putString(KEY_MAC, mac).apply()
        return mac
    }

    fun getPairingCode(ctx: Context): String {
        val sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val existing = sp.getString("pairing_code", null)
        if (!existing.isNullOrBlank()) return existing

        val mac = getVirtualMac(ctx)
        val clean = mac.replace(":", "")
        val hex = if (clean.length >= 6) clean.takeLast(6).uppercase() else clean.uppercase()
        val code = if (hex.length == 6) "${hex.take(3)}-${hex.takeLast(3)}" else hex
        sp.edit().putString("pairing_code", code).apply()
        return code
    }

    fun getDeviceModel(): String {
        val m = android.os.Build.MODEL ?: "Android TV"
        val b = android.os.Build.BRAND ?: ""
        return if (b.isNotBlank() && !m.startsWith(b, ignoreCase = true)) "$b $m" else m
    }
}
