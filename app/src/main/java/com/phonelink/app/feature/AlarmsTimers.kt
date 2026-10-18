package com.phonelink.app.feature

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import org.json.JSONArray
import org.json.JSONObject

/**
 * Creates real native Clock-app alarms and timers via the AlarmClock intents, and opens
 * the Clock's alarm list. Android gives no way to list, edit, delete or toggle existing
 * Clock alarms, so those aren't offered; use show_alarms to manage them by hand.
 * Launching from the background needs the overlay ("Appear on top") permission, which
 * Phone Link also uses for pop-ups; with it granted these fire without opening the UI.
 */
object AlarmsTimers {

    fun setAlarm(ctx: Context, hour: Int, minute: Int, label: String?, days: Any?, vibrate: Boolean): JSONObject {
        val dayList = parseDays(days)
        val i = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
            putExtra(AlarmClock.EXTRA_MINUTES, minute.coerceIn(0, 59))
            if (!label.isNullOrBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_VIBRATE, vibrate)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            if (dayList.isNotEmpty()) putExtra(AlarmClock.EXTRA_DAYS, ArrayList(dayList))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (i.resolveActivity(ctx.packageManager) == null)
            return JSONObject().put("error", "no clock app handles alarms")
        ctx.startActivity(i)
        return JSONObject().put("alarmSet", true)
            .put("time", String.format("%02d:%02d", hour, minute))
            .put("label", label ?: "")
    }

    fun setTimer(ctx: Context, seconds: Int, label: String?): JSONObject {
        val i = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds.coerceIn(1, 86_400))
            if (!label.isNullOrBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (i.resolveActivity(ctx.packageManager) == null)
            return JSONObject().put("error", "no clock app handles timers")
        ctx.startActivity(i)
        return JSONObject().put("timerSet", true).put("seconds", seconds).put("label", label ?: "")
    }

    fun showAlarms(ctx: Context): JSONObject {
        val i = Intent(AlarmClock.ACTION_SHOW_ALARMS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (i.resolveActivity(ctx.packageManager) == null)
            return JSONObject().put("error", "no clock app")
        ctx.startActivity(i)
        return JSONObject().put("opened", "clock alarms")
    }

    /** Accepts [1..7] (Calendar day numbers), ['mon','tue'], 'daily'/'weekdays'/'weekends'. */
    private fun parseDays(raw: Any?): List<Int> {
        if (raw == null) return emptyList()
        val map = mapOf("sun" to 1, "mon" to 2, "tue" to 3, "wed" to 4, "thu" to 5, "fri" to 6, "sat" to 7)
        if (raw is String) {
            return when (raw.lowercase()) {
                "daily", "everyday", "every day" -> (1..7).toList()
                "weekdays" -> listOf(2, 3, 4, 5, 6)
                "weekends" -> listOf(1, 7)
                "once", "" -> emptyList()
                else -> raw.split(",").mapNotNull { map[it.trim().lowercase().take(3)] }
            }
        }
        if (raw is JSONArray) {
            val out = ArrayList<Int>()
            for (i in 0 until raw.length()) {
                when (val v = raw.get(i)) {
                    is Int -> out.add(v)
                    is String -> map[v.trim().lowercase().take(3)]?.let { out.add(it) }
                }
            }
            return out
        }
        return emptyList()
    }

    fun openApp(ctx: Context, pkg: String): JSONObject {
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg)
            ?: return JSONObject().put("error", "app not found: $pkg")
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
        return JSONObject().put("opened", pkg)
    }

    fun openUrl(ctx: Context, url: String): JSONObject {
        val i = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (i.resolveActivity(ctx.packageManager) == null)
            return JSONObject().put("error", "nothing handles that url")
        ctx.startActivity(i)
        return JSONObject().put("opened", url)
    }
}
