package com.phonelink.app.feature

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.app.NotificationManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.provider.Settings
import org.json.JSONObject

/** Reads benign phone status: battery, storage, network, screen, ringer, volumes. */
object DeviceState {

    fun collect(ctx: Context): JSONObject {
        val o = JSONObject()
        o.put("model", "${Build.MANUFACTURER} ${Build.MODEL}")
        o.put("android", Build.VERSION.RELEASE)
        battery(ctx, o)
        storage(o)
        o.put("network", networkType(ctx))
        o.put("screenOn", (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive)
        audio(ctx, o)
        o.put("dnd", dndState(ctx))
        o.put("brightness", brightness(ctx))
        return o
    }

    private fun battery(ctx: Context, o: JSONObject) {
        val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        o.put("batteryPct", bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY))
        val status = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = status?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        o.put("charging", plugged != 0)
    }

    private fun storage(o: JSONObject) {
        val stat = StatFs(Environment.getDataDirectory().path)
        val freeGb = stat.availableBytes / 1_000_000_000.0
        val totalGb = stat.totalBytes / 1_000_000_000.0
        o.put("storageFreeGb", String.format("%.1f", freeGb).toDouble())
        o.put("storageTotalGb", String.format("%.1f", totalGb).toDouble())
    }

    private fun networkType(ctx: Context): String {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val net = cm.activeNetwork ?: return "none"
        val caps = cm.getNetworkCapabilities(net) ?: return "none"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            else -> "other"
        }
    }

    private fun audio(ctx: Context, o: JSONObject) {
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        o.put("ringerMode", when (am.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> "silent"
            AudioManager.RINGER_MODE_VIBRATE -> "vibrate"
            else -> "normal"
        })
        val vol = JSONObject()
        for ((name, stream) in listOf(
            "media" to AudioManager.STREAM_MUSIC,
            "ring" to AudioManager.STREAM_RING,
            "alarm" to AudioManager.STREAM_ALARM,
            "call" to AudioManager.STREAM_VOICE_CALL,
            "notification" to AudioManager.STREAM_NOTIFICATION
        )) {
            val max = am.getStreamMaxVolume(stream)
            val cur = am.getStreamVolume(stream)
            vol.put(name, if (max == 0) 0 else (cur * 100 / max))
        }
        o.put("volume", vol)
    }

    private fun dndState(ctx: Context): String {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return when (nm.currentInterruptionFilter) {
            NotificationManager.INTERRUPTION_FILTER_NONE -> "total_silence"
            NotificationManager.INTERRUPTION_FILTER_PRIORITY -> "priority_only"
            NotificationManager.INTERRUPTION_FILTER_ALARMS -> "alarms_only"
            NotificationManager.INTERRUPTION_FILTER_ALL -> "off"
            else -> "unknown"
        }
    }

    private fun brightness(ctx: Context): Int = try {
        val raw = Settings.System.getInt(ctx.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        raw * 100 / 255
    } catch (e: Exception) { -1 }
}
