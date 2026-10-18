package com.phonelink.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.phonelink.app.feature.alarm.AlarmScheduler
import com.phonelink.app.util.Prefs

/** After a reboot or app update: re-arm alarms, the watchdog, and restart the link. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                runCatching { AlarmScheduler.rescheduleAll(context) }
                if (Prefs.enabled(context) && Prefs.isConfigured(context)) {
                    KeepAlive.schedule(context)
                    PhoneLinkService.start(context)
                }
            }
        }
    }
}
