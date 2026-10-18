package com.phonelink.app.util

import android.content.Context
import android.content.SharedPreferences

/** Simple persisted settings for Phone Link. */
object Prefs {
    private const val FILE = "phonelink_prefs"
    const val KEY_WS_URL = "ws_url"
    const val KEY_DEVICE_KEY = "device_key"
    const val KEY_ENABLED = "enabled"

    private fun sp(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Base wss URL of the Worker device endpoint, e.g. wss://phonelinkmcp.shneur.workers.dev */
    fun wsUrl(ctx: Context): String = sp(ctx).getString(KEY_WS_URL, "") ?: ""
    fun setWsUrl(ctx: Context, v: String) = sp(ctx).edit().putString(KEY_WS_URL, v.trim()).apply()

    /** Device secret, matched against a Worker secret. */
    fun deviceKey(ctx: Context): String = sp(ctx).getString(KEY_DEVICE_KEY, "") ?: ""
    fun setDeviceKey(ctx: Context, v: String) = sp(ctx).edit().putString(KEY_DEVICE_KEY, v.trim()).apply()

    /** Master on/off switch the user controls in the app. */
    fun enabled(ctx: Context): Boolean = sp(ctx).getBoolean(KEY_ENABLED, false)
    fun setEnabled(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean(KEY_ENABLED, v).apply()

    fun isConfigured(ctx: Context): Boolean =
        wsUrl(ctx).isNotEmpty() && deviceKey(ctx).isNotEmpty()

    /** Whether the owner wants the DNS monitor on (so it restarts after reboot). */
    fun vpnEnabled(ctx: Context): Boolean = sp(ctx).getBoolean("vpn_enabled", false)
    fun setVpnEnabled(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("vpn_enabled", v).apply()
}
