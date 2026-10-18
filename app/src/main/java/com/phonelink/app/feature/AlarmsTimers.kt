package com.phonelink.app.feature

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import org.json.JSONObject

/**
 * Sets real Clock-app alarms and timers via the AlarmClock intents.
 * Launching from the background requires the overlay ("Appear on top") permission,
 * which Phone Link also uses for pop-ups; with it granted these fire without opening the UI.
 */
object AlarmsTimers {

    fun setAlarm(ctx: Context, hour: Int, minute: Int, label: String?): JSONObject {
        val i = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
            putExtra(AlarmClock.EXTRA_MINUTES, minute.coerceIn(0, 59))
            if (!label.isNullOrBlank()) putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
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
