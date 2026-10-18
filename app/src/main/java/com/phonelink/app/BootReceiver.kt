package com.phonelink.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.phonelink.app.util.Prefs

/** After a reboot or app update: restart the watchdog and the link. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                if (Prefs.enabled(context) && Prefs.isConfigured(context)) {
                    KeepAlive.schedule(context)
                    PhoneLinkService.start(context)
                }
            }
        }
    }
}
